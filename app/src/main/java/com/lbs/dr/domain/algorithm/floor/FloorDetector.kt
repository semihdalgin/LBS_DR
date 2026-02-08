package com.lbs.dr.domain.algorithm.floor

import com.lbs.dr.domain.model.ActivityType
import com.lbs.dr.domain.model.FloorInfo
import com.lbs.dr.domain.model.FloorTransition
import com.lbs.dr.domain.model.SensorReading
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Barometric floor detection using pressure sensor data.
 *
 * Algorithm:
 * 1. Convert pressure to altitude using hypsometric formula
 * 2. Track relative altitude changes (avoids absolute calibration issues)
 * 3. Apply moving average to reduce noise
 * 4. Detect floor transitions when altitude change exceeds threshold
 * 5. Use activity recognition to distinguish stairs vs elevator
 *
 * Improvements over original ZamanServisi:
 * - Hypsometric formula instead of linear approximation
 * - Moving average with configurable window
 * - Activity-aware transition detection
 * - Handles weather-related pressure drift via relative measurements
 */
class FloorDetector(
    private val floorHeight: Float = FloorInfo.DEFAULT_FLOOR_HEIGHT,
    private val windowSize: Int = 30,
    private val transitionThresholdMeters: Float = 2.0f
) {
    // Pressure/altitude history
    private val pressureHistory = ArrayDeque<Float>(windowSize)
    private val altitudeHistory = ArrayDeque<Float>(windowSize)

    // State
    private var referencePressure: Float? = null
    private var referenceAltitude = 0f
    private var currentFloor = 0
    private var lastFloorAltitude = 0f
    private var smoothedAltitude = 0f
    private var initialized = false

    companion object {
        // Standard atmosphere constants
        private const val SEA_LEVEL_PRESSURE = 1013.25f  // hPa
        private const val TEMPERATURE_LAPSE_RATE = 0.0065f // K/m
        private const val SEA_LEVEL_TEMP = 288.15f  // K (15°C)
        private const val GRAVITY = 9.80665f  // m/s²
        private const val MOLAR_MASS_AIR = 0.0289644f  // kg/mol
        private const val GAS_CONSTANT = 8.31447f  // J/(mol·K)
    }

    /**
     * Process a pressure reading and detect floor changes.
     */
    fun process(pressure: SensorReading.Pressure, activity: ActivityType = ActivityType.UNKNOWN): FloorInfo {
        val pressureHPa = pressure.pressureHPa

        if (!initialized || referencePressure == null) {
            referencePressure = pressureHPa
            referenceAltitude = pressureToAltitude(pressureHPa)
            lastFloorAltitude = referenceAltitude
            smoothedAltitude = referenceAltitude
            initialized = true
        }

        // Convert to altitude
        val altitude = pressureToAltitude(pressureHPa)

        // Moving average smoothing
        altitudeHistory.addLast(altitude)
        if (altitudeHistory.size > windowSize) altitudeHistory.removeFirst()
        smoothedAltitude = altitudeHistory.average().toFloat()

        pressureHistory.addLast(pressureHPa)
        if (pressureHistory.size > windowSize) pressureHistory.removeFirst()

        // Detect floor transition
        val altitudeChange = smoothedAltitude - lastFloorAltitude
        val transition = detectTransition(altitudeChange, activity)

        if (transition != FloorTransition.NONE) {
            val floorsChanged = (altitudeChange / floorHeight).roundToInt()
            currentFloor += floorsChanged
            lastFloorAltitude = smoothedAltitude
        }

        return FloorInfo(
            currentFloor = currentFloor,
            floorHeight = floorHeight,
            pressureAtReference = referencePressure!!,
            altitudeEstimate = smoothedAltitude,
            transition = transition,
            confidence = computeConfidence(),
            timestamp = pressure.timestamp
        )
    }

    /**
     * Set reference floor (e.g., from user input or known entry point).
     */
    fun setReferenceFloor(floor: Int, currentPressure: Float) {
        currentFloor = floor
        referencePressure = currentPressure
        referenceAltitude = pressureToAltitude(currentPressure)
        lastFloorAltitude = referenceAltitude
    }

    fun getCurrentFloor(): Int = currentFloor

    fun reset() {
        pressureHistory.clear()
        altitudeHistory.clear()
        referencePressure = null
        currentFloor = 0
        lastFloorAltitude = 0f
        smoothedAltitude = 0f
        initialized = false
    }

    /**
     * Hypsometric formula: converts pressure to altitude.
     * More accurate than the linear 12 Pa/m approximation used in the original.
     *
     * h = (T0 / L) * [1 - (P/P0)^(R*L / (g*M))]
     */
    private fun pressureToAltitude(pressureHPa: Float): Float {
        val exponent = (GAS_CONSTANT * TEMPERATURE_LAPSE_RATE) / (GRAVITY * MOLAR_MASS_AIR)
        return (SEA_LEVEL_TEMP / TEMPERATURE_LAPSE_RATE) *
                (1 - Math.pow(
                    (pressureHPa / SEA_LEVEL_PRESSURE).toDouble(),
                    exponent.toDouble()
                ).toFloat())
    }

    /**
     * Alternative: relative altitude from reference pressure.
     * h = (R * T) / (g * M) * ln(P0 / P)
     */
    @Suppress("unused")
    private fun relativeAltitude(pressureHPa: Float, referencePressure: Float): Float {
        return ((GAS_CONSTANT * SEA_LEVEL_TEMP) / (GRAVITY * MOLAR_MASS_AIR) *
                ln((referencePressure / pressureHPa).toDouble())).toFloat()
    }

    private fun detectTransition(altitudeChange: Float, activity: ActivityType): FloorTransition {
        val absChange = abs(altitudeChange)

        // Threshold depends on activity
        val threshold = when (activity) {
            ActivityType.CLIMBING_STAIRS, ActivityType.DESCENDING_STAIRS -> transitionThresholdMeters * 0.8f
            ActivityType.ELEVATOR -> transitionThresholdMeters * 0.6f
            else -> transitionThresholdMeters
        }

        return when {
            absChange >= threshold && altitudeChange > 0 -> FloorTransition.ASCENDING
            absChange >= threshold && altitudeChange < 0 -> FloorTransition.DESCENDING
            else -> FloorTransition.NONE
        }
    }

    private fun computeConfidence(): Float {
        if (altitudeHistory.size < 5) return 0.3f

        // Confidence based on pressure stability (low variance = high confidence)
        val mean = pressureHistory.average().toFloat()
        val variance = pressureHistory.map { (it - mean) * (it - mean) }.average().toFloat()

        // Low variance (<0.1 hPa²) → high confidence, high variance → low confidence
        return (1.0f - (variance / 1.0f).coerceAtMost(1.0f)).coerceIn(0.1f, 1.0f)
    }
}
