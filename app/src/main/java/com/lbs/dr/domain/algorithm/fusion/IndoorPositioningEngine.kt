package com.lbs.dr.domain.algorithm.fusion

import com.lbs.dr.domain.algorithm.filter.ExtendedKalmanFilter
import com.lbs.dr.domain.algorithm.filter.ParticleFilter
import com.lbs.dr.domain.algorithm.floor.FloorDetector
import com.lbs.dr.domain.algorithm.step.HeadingEstimator
import com.lbs.dr.domain.algorithm.step.StepDetector
import com.lbs.dr.domain.model.*
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.MatrixUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main Indoor Positioning Engine that fuses all sensor data.
 *
 * Architecture:
 * ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
 * │  IMU Sensor   │   │  Barometer   │   │  WiFi/BLE    │
 * └──────┬───────┘   └──────┬───────┘   └──────┬───────┘
 *        │                   │                   │
 *   ┌────▼────┐        ┌────▼────┐        ┌─────▼─────┐
 *   │  Step   │        │  Floor  │        │  Particle │
 *   │Detector │        │Detector │        │  Filter   │
 *   │+Heading │        │         │        │           │
 *   └────┬────┘        └────┬────┘        └─────┬─────┘
 *        │                   │                   │
 *        └─────────┬─────────┴───────────────────┘
 *                  │
 *           ┌──────▼──────┐
 *           │   Extended  │
 *           │   Kalman    │
 *           │   Filter    │
 *           └──────┬──────┘
 *                  │
 *           ┌──────▼──────┐
 *           │   Fused     │
 *           │   Position  │
 *           └─────────────┘
 *
 * Processing pipeline:
 * 1. IMU → StepDetector + HeadingEstimator → Dead Reckoning
 * 2. Barometer → FloorDetector → Floor changes
 * 3. WiFi/BLE → ParticleFilter → RF-based position
 * 4. All → EKF fusion → Final position estimate
 */
@Singleton
class IndoorPositioningEngine @Inject constructor() {

    // Algorithm components
    private val ekf = ExtendedKalmanFilter()
    private val particleFilter = ParticleFilter()
    private val stepDetector = StepDetector()
    private val headingEstimator = HeadingEstimator()
    private val floorDetector = FloorDetector()

    // State
    private var lastUpdateTime = 0L
    private var currentMotionState = MotionState(
        activity = ActivityType.UNKNOWN,
        heading = 0f,
        speed = 0f,
        stepFrequency = 0f,
        strideLength = 0f,
        isStationary = true
    )
    private var currentFloorInfo: FloorInfo? = null

    val isInitialized: Boolean
        get() = ekf.isInitialized

    /**
     * Initialize the engine with a known starting position.
     */
    fun initialize(latitude: Double, longitude: Double, altitude: Double, floor: Int = 0) {
        // Convert lat/lon to local meters (approximate)
        val x = longitudeToMeters(longitude, latitude)
        val y = latitudeToMeters(latitude)

        ekf.initialize(x, y, altitude, floor = floor)
        particleFilter.initialize(x, y, altitude, floor)
        lastUpdateTime = System.currentTimeMillis()
    }

    /**
     * Process IMU reading for dead reckoning.
     * This is the highest-rate update (~50 Hz).
     */
    fun processImu(imu: SensorReading.Imu): Position {
        val now = imu.timestamp
        val dt = if (lastUpdateTime > 0) (now - lastUpdateTime) / 1000.0 else 0.02
        lastUpdateTime = now

        // Update heading
        val heading = headingEstimator.update(imu)

        // Detect steps
        val stepResult = stepDetector.process(imu)

        // EKF prediction step
        val processNoiseScale = if (stepResult.stepDetected) 1.0 else 0.1
        ekf.predict(dt, processNoiseScale)

        // If step detected, update EKF with dead reckoning measurement
        if (stepResult.stepDetected) {
            val strideLength = stepResult.strideLength
            val dx = strideLength * kotlin.math.cos(heading)
            val dy = strideLength * kotlin.math.sin(heading)

            // Dead reckoning measurement: position displacement
            val z = ArrayRealVector(doubleArrayOf(
                ekf.x.getEntry(ExtendedKalmanFilter.IDX_X) + dx,
                ekf.x.getEntry(ExtendedKalmanFilter.IDX_Y) + dy
            ))

            // Measurement matrix: observing x, y directly
            val H = Array2DRowRealMatrix(2, ExtendedKalmanFilter.STATE_DIM)
            H.setEntry(0, ExtendedKalmanFilter.IDX_X, 1.0)
            H.setEntry(1, ExtendedKalmanFilter.IDX_Y, 1.0)

            // Dead reckoning noise: depends on stride estimation quality
            val R = Array2DRowRealMatrix(arrayOf(
                doubleArrayOf(0.5, 0.0),
                doubleArrayOf(0.0, 0.5)
            ))

            ekf.update(z, H, R)

            // Also propagate particle filter
            particleFilter.predict(
                stepLength = strideLength.toDouble(),
                heading = heading.toDouble()
            )

            currentMotionState = currentMotionState.copy(
                activity = classifyActivity(stepResult.stepFrequency),
                heading = heading,
                speed = strideLength * stepResult.stepFrequency,
                stepFrequency = stepResult.stepFrequency,
                strideLength = strideLength,
                isStationary = false,
                timestamp = now
            )
        } else {
            currentMotionState = currentMotionState.copy(
                isStationary = true,
                speed = 0f,
                timestamp = now
            )
        }

        // Update heading in EKF state
        ekf.x.setEntry(ExtendedKalmanFilter.IDX_HEADING, heading.toDouble())

        return buildPosition()
    }

    /**
     * Process barometric pressure for floor detection.
     */
    fun processPressure(pressure: SensorReading.Pressure): FloorInfo {
        val floorInfo = floorDetector.process(pressure, currentMotionState.activity)
        currentFloorInfo = floorInfo

        // Update EKF with altitude measurement
        val altZ = ArrayRealVector(doubleArrayOf(floorInfo.altitudeEstimate.toDouble()))
        val altH = Array2DRowRealMatrix(1, ExtendedKalmanFilter.STATE_DIM)
        altH.setEntry(0, ExtendedKalmanFilter.IDX_Z, 1.0)
        val altR = Array2DRowRealMatrix(arrayOf(doubleArrayOf(2.0))) // ~2m altitude noise
        ekf.update(altZ, altH, altR)

        // Update floor in EKF state
        ekf.x.setEntry(ExtendedKalmanFilter.IDX_FLOOR, floorInfo.currentFloor.toDouble())

        // Update particle filter with floor
        particleFilter.updateWithFloor(floorInfo.currentFloor, floorInfo.confidence.toDouble())

        return floorInfo
    }

    /**
     * Process WiFi scan results including RTT measurements.
     * Provides absolute position corrections to reduce dead reckoning drift.
     */
    fun processWifi(
        wifiScan: SensorReading.WifiScan,
        apPositions: Map<String, Pair<Double, Double>>
    ) {
        // RTT-based update (high precision, ~1-2m)
        val rttDistances = mutableMapOf<String, Double>()
        val rssiMap = mutableMapOf<String, Int>()

        for (ap in wifiScan.accessPoints) {
            if (ap.rttDistanceMm != null && ap.rttDistanceMm > 0) {
                rttDistances[ap.bssid] = ap.rttDistanceMm / 1000.0
            }
            rssiMap[ap.bssid] = ap.rssi
        }

        // Update particle filter with RTT (preferred) or RSSI
        if (rttDistances.isNotEmpty()) {
            particleFilter.updateWithRtt(rttDistances, apPositions, distanceSigma = 1.5)
        }

        if (rssiMap.isNotEmpty()) {
            particleFilter.updateWithWifi(rssiMap, apPositions)
        }

        // Use particle filter estimate as EKF measurement
        if (particleFilter.isInitialized) {
            val pfEstimate = particleFilter.getEstimate()
            val pfSpread = particleFilter.getSpread()

            // Only use PF estimate if particles have converged reasonably
            if (pfSpread < 20.0) {
                val z = ArrayRealVector(doubleArrayOf(pfEstimate[0], pfEstimate[1]))
                val H = Array2DRowRealMatrix(2, ExtendedKalmanFilter.STATE_DIM)
                H.setEntry(0, ExtendedKalmanFilter.IDX_X, 1.0)
                H.setEntry(1, ExtendedKalmanFilter.IDX_Y, 1.0)

                // Noise proportional to particle spread
                val noiseVar = (pfSpread * pfSpread).coerceAtLeast(1.0)
                val R = Array2DRowRealMatrix(arrayOf(
                    doubleArrayOf(noiseVar, 0.0),
                    doubleArrayOf(0.0, noiseVar)
                ))

                ekf.update(z, H, R)
            }
        }
    }

    /**
     * Process BLE beacon scan results.
     */
    fun processBle(
        bleScan: SensorReading.BleScan,
        beaconPositions: Map<String, Pair<Double, Double>>
    ) {
        val rssiMap = bleScan.beacons
            .filter { beaconPositions.containsKey(it.address) }
            .associate { it.address to it.rssi }

        if (rssiMap.isNotEmpty()) {
            particleFilter.updateWithBle(rssiMap, beaconPositions)

            // Feed PF estimate to EKF
            if (particleFilter.isInitialized) {
                val pfEstimate = particleFilter.getEstimate()
                val pfSpread = particleFilter.getSpread()

                if (pfSpread < 15.0) {
                    val z = ArrayRealVector(doubleArrayOf(pfEstimate[0], pfEstimate[1]))
                    val H = Array2DRowRealMatrix(2, ExtendedKalmanFilter.STATE_DIM)
                    H.setEntry(0, ExtendedKalmanFilter.IDX_X, 1.0)
                    H.setEntry(1, ExtendedKalmanFilter.IDX_Y, 1.0)

                    val noiseVar = (pfSpread * pfSpread).coerceAtLeast(2.0)
                    val R = Array2DRowRealMatrix(arrayOf(
                        doubleArrayOf(noiseVar, 0.0),
                        doubleArrayOf(0.0, noiseVar)
                    ))

                    ekf.update(z, H, R)
                }
            }
        }
    }

    /**
     * Process GPS fix for absolute position correction (mostly outdoor/near-window).
     */
    fun processGps(latitude: Double, longitude: Double, altitude: Double, accuracy: Float) {
        val x = longitudeToMeters(longitude, latitude)
        val y = latitudeToMeters(latitude)

        val z = ArrayRealVector(doubleArrayOf(x, y, altitude))
        val H = Array2DRowRealMatrix(3, ExtendedKalmanFilter.STATE_DIM)
        H.setEntry(0, ExtendedKalmanFilter.IDX_X, 1.0)
        H.setEntry(1, ExtendedKalmanFilter.IDX_Y, 1.0)
        H.setEntry(2, ExtendedKalmanFilter.IDX_Z, 1.0)

        val gpsNoise = (accuracy * accuracy).toDouble().coerceAtLeast(1.0)
        val R = Array2DRowRealMatrix(arrayOf(
            doubleArrayOf(gpsNoise, 0.0, 0.0),
            doubleArrayOf(0.0, gpsNoise, 0.0),
            doubleArrayOf(0.0, 0.0, gpsNoise * 4) // altitude is less precise
        ))

        ekf.update(z, H, R)
    }

    fun getCurrentPosition(): Position = buildPosition()

    fun getMotionState(): MotionState = currentMotionState

    fun getFloorInfo(): FloorInfo? = currentFloorInfo

    fun setReferenceFloor(floor: Int, pressure: Float) {
        floorDetector.setReferenceFloor(floor, pressure)
    }

    fun reset() {
        stepDetector.reset()
        headingEstimator.reset()
        floorDetector.reset()
        lastUpdateTime = 0L
    }

    // --- Private helpers ---

    private fun buildPosition(): Position {
        val pos = ekf.getPosition()
        return Position(
            latitude = metersToLatitude(pos[1]),
            longitude = metersToLongitude(pos[0], metersToLatitude(pos[1])),
            altitude = pos[2],
            floor = ekf.getFloor(),
            accuracy = ekf.getPositionUncertainty().toFloat(),
            source = PositionSource.SENSOR_FUSION,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun classifyActivity(stepFrequency: Float): ActivityType {
        return when {
            stepFrequency < 0.5f -> ActivityType.STATIONARY
            stepFrequency < 1.5f -> ActivityType.WALKING
            stepFrequency < 2.5f -> ActivityType.RUNNING
            else -> ActivityType.RUNNING
        }
    }

    // --- Coordinate conversion (local meters ↔ lat/lon) ---
    // Using Mercator approximation for small areas

    companion object {
        private const val EARTH_RADIUS = 6371000.0 // meters

        private var refLat = 0.0
        private var refLon = 0.0

        fun setReferencePoint(lat: Double, lon: Double) {
            refLat = lat
            refLon = lon
        }

        fun latitudeToMeters(lat: Double): Double =
            (lat - refLat) * Math.PI / 180.0 * EARTH_RADIUS

        fun longitudeToMeters(lon: Double, lat: Double): Double =
            (lon - refLon) * Math.PI / 180.0 * EARTH_RADIUS * kotlin.math.cos(Math.toRadians(lat))

        fun metersToLatitude(meters: Double): Double =
            refLat + meters / (EARTH_RADIUS * Math.PI / 180.0)

        fun metersToLongitude(meters: Double, lat: Double): Double =
            refLon + meters / (EARTH_RADIUS * Math.PI / 180.0 * kotlin.math.cos(Math.toRadians(lat)))
    }
}
