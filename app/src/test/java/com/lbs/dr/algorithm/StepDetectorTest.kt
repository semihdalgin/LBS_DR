package com.lbs.dr.algorithm

import com.lbs.dr.domain.algorithm.step.StepDetector
import com.lbs.dr.domain.model.SensorReading
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.sin

class StepDetectorTest {

    private lateinit var detector: StepDetector

    @Before
    fun setup() {
        detector = StepDetector()
    }

    @Test
    fun `no steps detected from stationary data`() {
        // Simulate stationary accelerometer (only gravity)
        var steps = 0
        repeat(100) { i ->
            val result = detector.process(
                SensorReading.Imu(
                    ax = 0f, ay = 0f, az = 9.81f,
                    gx = 0f, gy = 0f, gz = 0f,
                    mx = 20f, my = 0f, mz = 40f,
                    timestamp = i * 20L // 50 Hz
                )
            )
            if (result.stepDetected) steps++
        }
        assertEquals(0, steps)
    }

    @Test
    fun `steps detected from walking pattern`() {
        // Simulate walking: sinusoidal acceleration pattern
        var steps = 0
        val stepFreq = 2.0 // 2 Hz = walking
        val sampleRate = 50.0 // Hz

        repeat(500) { i -> // 10 seconds
            val t = i / sampleRate
            // Vertical acceleration: gravity + walking oscillation
            val walkingAccel = 9.81f + (2.0 * sin(2 * Math.PI * stepFreq * t)).toFloat()

            val result = detector.process(
                SensorReading.Imu(
                    ax = 0.5f, ay = 0.3f, az = walkingAccel,
                    gx = 0f, gy = 0f, gz = 0f,
                    mx = 20f, my = 0f, mz = 40f,
                    timestamp = (i * 20L) + 300 // offset to avoid 0 interval issues
                )
            )
            if (result.stepDetected) steps++
        }

        // Should detect roughly 20 steps in 10 seconds at 2 Hz
        assertTrue("Expected ~20 steps, got $steps", steps in 5..40)
    }

    @Test
    fun `stride length is within reasonable range`() {
        val stepFreq = 1.8
        val sampleRate = 50.0

        val strides = mutableListOf<Float>()
        repeat(500) { i ->
            val t = i / sampleRate
            val walkingAccel = 9.81f + (2.5 * sin(2 * Math.PI * stepFreq * t)).toFloat()

            val result = detector.process(
                SensorReading.Imu(
                    ax = 0.3f, ay = 0.2f, az = walkingAccel,
                    gx = 0f, gy = 0f, gz = 0f,
                    mx = 20f, my = 0f, mz = 40f,
                    timestamp = (i * 20L) + 300
                )
            )
            if (result.stepDetected) strides.add(result.strideLength)
        }

        // Stride length should be between 0.3m and 1.5m
        for (stride in strides) {
            assertTrue("Stride $stride out of range", stride in 0.3f..1.5f)
        }
    }

    @Test
    fun `reset clears step count`() {
        // Generate some steps
        val stepFreq = 2.0
        val sampleRate = 50.0
        repeat(200) { i ->
            val t = i / sampleRate
            val walkingAccel = 9.81f + (2.0 * sin(2 * Math.PI * stepFreq * t)).toFloat()
            detector.process(
                SensorReading.Imu(
                    ax = 0f, ay = 0f, az = walkingAccel,
                    gx = 0f, gy = 0f, gz = 0f,
                    mx = 20f, my = 0f, mz = 40f,
                    timestamp = (i * 20L) + 300
                )
            )
        }

        detector.reset()
        assertEquals(0, detector.getStepCount())
    }
}
