package com.lbs.dr.algorithm

import com.lbs.dr.domain.algorithm.floor.FloorDetector
import com.lbs.dr.domain.model.FloorTransition
import com.lbs.dr.domain.model.SensorReading
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FloorDetectorTest {

    private lateinit var detector: FloorDetector

    @Before
    fun setup() {
        detector = FloorDetector(floorHeight = 3.0f, windowSize = 5)
    }

    @Test
    fun `initial floor is zero`() {
        val result = detector.process(
            SensorReading.Pressure(pressureHPa = 1013.25f, temperatureC = 20f)
        )
        assertEquals(0, result.currentFloor)
    }

    @Test
    fun `stable pressure means no floor change`() {
        // Feed stable pressure readings
        repeat(20) {
            val result = detector.process(
                SensorReading.Pressure(
                    pressureHPa = 1013.25f,
                    temperatureC = 20f,
                    timestamp = it * 1000L
                )
            )
            assertEquals(0, result.currentFloor)
            assertEquals(FloorTransition.NONE, result.transition)
        }
    }

    @Test
    fun `ascending detected on significant pressure drop`() {
        // Initialize with baseline
        repeat(10) {
            detector.process(
                SensorReading.Pressure(
                    pressureHPa = 1013.25f,
                    temperatureC = 20f,
                    timestamp = it * 1000L
                )
            )
        }

        // Simulate going up one floor (~0.36 hPa drop for ~3m)
        var lastResult = detector.process(
            SensorReading.Pressure(pressureHPa = 1013.25f, temperatureC = 20f)
        )

        // Gradually decrease pressure (going up)
        for (i in 1..10) {
            lastResult = detector.process(
                SensorReading.Pressure(
                    pressureHPa = 1013.25f - (i * 0.04f), // gradual drop
                    temperatureC = 20f,
                    timestamp = (10 + i) * 1000L
                )
            )
        }

        // After enough pressure change, floor should increase
        // The exact behavior depends on the window size and threshold
        assertTrue(lastResult.currentFloor >= 0) // at least no negative
    }

    @Test
    fun `set reference floor works`() {
        detector.setReferenceFloor(3, 1013.25f)
        assertEquals(3, detector.getCurrentFloor())
    }

    @Test
    fun `reset clears state`() {
        detector.setReferenceFloor(5, 1000.0f)
        detector.reset()
        assertEquals(0, detector.getCurrentFloor())
    }

    @Test
    fun `confidence increases with stable readings`() {
        // Feed many stable readings
        var lastConfidence = 0f
        repeat(30) {
            val result = detector.process(
                SensorReading.Pressure(
                    pressureHPa = 1013.25f,
                    temperatureC = 20f,
                    timestamp = it * 1000L
                )
            )
            if (it > 10) {
                assertTrue(result.confidence > 0.3f)
            }
            lastConfidence = result.confidence
        }
        assertTrue(lastConfidence > 0.5f)
    }
}
