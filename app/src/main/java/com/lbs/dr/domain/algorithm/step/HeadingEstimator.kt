package com.lbs.dr.domain.algorithm.step

import android.hardware.SensorManager
import com.lbs.dr.domain.model.SensorReading
import kotlin.math.atan2

/**
 * Heading (azimuth) estimator using accelerometer and magnetometer fusion.
 *
 * Uses Android's rotation matrix computation for tilt-compensated heading.
 * Falls back to simple magnetometer heading if rotation matrix cannot be computed.
 *
 * Applies complementary filter to blend gyroscope integration (short-term)
 * with magnetometer-based absolute heading (long-term).
 */
class HeadingEstimator(
    private val complementaryAlpha: Float = 0.98f  // gyro weight vs mag weight
) {
    private var currentHeading = 0f
    private var lastGyroTimestamp = 0L
    private var initialized = false

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    /**
     * Update heading estimate from IMU data.
     * Returns heading in radians, 0 = North, increasing clockwise.
     */
    fun update(imu: SensorReading.Imu): Float {
        val accel = floatArrayOf(imu.ax, imu.ay, imu.az)
        val mag = floatArrayOf(imu.mx, imu.my, imu.mz)

        // Compute tilt-compensated heading using rotation matrix
        val magHeading = computeMagneticHeading(accel, mag)

        if (!initialized) {
            currentHeading = magHeading
            lastGyroTimestamp = imu.timestamp
            initialized = true
            return currentHeading
        }

        // Integrate gyroscope for short-term heading
        val dt = (imu.timestamp - lastGyroTimestamp) / 1000.0f  // seconds
        lastGyroTimestamp = imu.timestamp

        if (dt > 0 && dt < 1.0f) {
            // Gyro z-axis rotation (yaw)
            val gyroHeading = currentHeading + imu.gz * dt

            // Complementary filter: blend gyro (fast, driftable) with mag (slow, stable)
            currentHeading = complementaryAlpha * gyroHeading +
                    (1 - complementaryAlpha) * magHeading
        } else {
            currentHeading = magHeading
        }

        // Normalize to [0, 2π)
        currentHeading = normalizeRadians(currentHeading)
        return currentHeading
    }

    fun getHeading(): Float = currentHeading

    fun reset() {
        initialized = false
        currentHeading = 0f
    }

    private fun computeMagneticHeading(accel: FloatArray, mag: FloatArray): Float {
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, accel, mag)
        return if (success) {
            SensorManager.getOrientation(rotationMatrix, orientation)
            orientation[0] // azimuth in radians
        } else {
            // Fallback: simple atan2 of magnetometer
            atan2(mag[1], mag[0])
        }
    }

    private fun normalizeRadians(angle: Float): Float {
        var a = angle % (2 * Math.PI).toFloat()
        if (a < 0) a += (2 * Math.PI).toFloat()
        return a
    }
}
