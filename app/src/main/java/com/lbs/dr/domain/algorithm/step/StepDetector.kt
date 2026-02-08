package com.lbs.dr.domain.algorithm.step

import com.lbs.dr.domain.model.SensorReading
import kotlin.math.sqrt

/**
 * Adaptive step detection using accelerometer data.
 *
 * Algorithm:
 * 1. Compute acceleration magnitude: |a| = sqrt(ax² + ay² + az²)
 * 2. Apply low-pass filter to remove high-frequency noise
 * 3. Detect peaks that exceed dynamic threshold
 * 4. Enforce minimum step period to prevent false positives
 * 5. Estimate stride length from step frequency (Weinberg model)
 *
 * Improvements over original:
 * - Adaptive threshold based on recent signal variance
 * - Weinberg stride length estimation: L = K * (amax - amin)^0.25
 * - Low-pass filtering for noise robustness
 */
class StepDetector(
    private val minStepPeriodMs: Long = 250,    // max ~4 steps/sec (running)
    private val maxStepPeriodMs: Long = 2000,   // min ~0.5 steps/sec
    private val weinbergK: Double = 0.52,        // calibration constant
    private val lowPassAlpha: Float = 0.2f       // low-pass filter smoothing
) {
    data class StepResult(
        val stepDetected: Boolean,
        val strideLength: Float,
        val stepFrequency: Float,
        val heading: Float,
        val timestamp: Long
    )

    // State
    private var filteredMagnitude = 0f
    private var lastStepTime = 0L
    private var stepCount = 0
    private var peakValue = 0f
    private var valleyValue = Float.MAX_VALUE
    private var lookingForPeak = true

    // Adaptive threshold
    private val recentMagnitudes = ArrayDeque<Float>(WINDOW_SIZE)
    private var dynamicThreshold = 10.5f   // ~gravity + step impulse

    // Step frequency estimation
    private val recentStepIntervals = ArrayDeque<Long>(10)

    companion object {
        private const val WINDOW_SIZE = 50
        private const val GRAVITY = 9.81f
    }

    /**
     * Process an IMU reading and detect if a step occurred.
     */
    fun process(imu: SensorReading.Imu): StepResult {
        val magnitude = sqrt(imu.ax * imu.ax + imu.ay * imu.ay + imu.az * imu.az)

        // Low-pass filter
        filteredMagnitude = lowPassAlpha * magnitude + (1 - lowPassAlpha) * filteredMagnitude

        // Update dynamic threshold
        recentMagnitudes.addLast(filteredMagnitude)
        if (recentMagnitudes.size > WINDOW_SIZE) recentMagnitudes.removeFirst()
        updateThreshold()

        // Peak-valley detection
        val stepDetected = detectStep(filteredMagnitude, imu.timestamp)

        val strideLength = if (stepDetected) estimateStrideLength() else 0f
        val stepFreq = estimateStepFrequency()

        return StepResult(
            stepDetected = stepDetected,
            strideLength = strideLength,
            stepFrequency = stepFreq,
            heading = computeHeading(imu),
            timestamp = imu.timestamp
        )
    }

    fun getStepCount(): Int = stepCount

    fun reset() {
        filteredMagnitude = 0f
        lastStepTime = 0L
        stepCount = 0
        peakValue = 0f
        valleyValue = Float.MAX_VALUE
        lookingForPeak = true
        recentMagnitudes.clear()
        recentStepIntervals.clear()
    }

    private fun detectStep(magnitude: Float, timestamp: Long): Boolean {
        if (lookingForPeak) {
            if (magnitude > peakValue) {
                peakValue = magnitude
            } else if (magnitude < peakValue - 0.5f && peakValue > dynamicThreshold) {
                // Found a peak above threshold
                lookingForPeak = false
                valleyValue = magnitude
            }
        } else {
            if (magnitude < valleyValue) {
                valleyValue = magnitude
            } else if (magnitude > valleyValue + 0.5f) {
                // Found valley after peak -> step complete
                lookingForPeak = true

                val timeSinceLastStep = timestamp - lastStepTime

                if (timeSinceLastStep >= minStepPeriodMs && timeSinceLastStep <= maxStepPeriodMs) {
                    stepCount++
                    recentStepIntervals.addLast(timeSinceLastStep)
                    if (recentStepIntervals.size > 10) recentStepIntervals.removeFirst()
                    lastStepTime = timestamp

                    // Reset for next step
                    peakValue = 0f
                    valleyValue = Float.MAX_VALUE
                    return true
                } else if (timeSinceLastStep > maxStepPeriodMs) {
                    // Too long since last step, still register but reset intervals
                    lastStepTime = timestamp
                    recentStepIntervals.clear()
                }

                peakValue = 0f
                valleyValue = Float.MAX_VALUE
            }
        }
        return false
    }

    /**
     * Weinberg stride length estimation: L = K * (amax - amin)^0.25
     */
    private fun estimateStrideLength(): Float {
        val amplitude = peakValue - valleyValue
        return if (amplitude > 0) {
            (weinbergK * Math.pow(amplitude.toDouble(), 0.25)).toFloat()
                .coerceIn(0.3f, 1.5f) // reasonable stride range
        } else 0.7f // default stride length
    }

    private fun estimateStepFrequency(): Float {
        if (recentStepIntervals.isEmpty()) return 0f
        val avgInterval = recentStepIntervals.average()
        return if (avgInterval > 0) (1000.0 / avgInterval).toFloat() else 0f
    }

    /**
     * Simple heading from magnetometer data in IMU reading.
     * Returns heading in radians from north.
     */
    private fun computeHeading(imu: SensorReading.Imu): Float {
        // Use atan2 of magnetometer x, y components
        // This is a simplified heading; full implementation uses rotation matrix
        return Math.atan2(imu.my.toDouble(), imu.mx.toDouble()).toFloat()
    }

    private fun updateThreshold() {
        if (recentMagnitudes.size < 10) return
        val mean = recentMagnitudes.average().toFloat()
        val variance = recentMagnitudes.map { (it - mean) * (it - mean) }.average().toFloat()
        // Threshold: mean + fraction of std dev
        dynamicThreshold = mean + sqrt(variance) * 0.8f
    }
}
