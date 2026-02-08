package com.lbs.dr.sensor

import com.lbs.dr.domain.model.SensorReading
import kotlinx.coroutines.flow.Flow

/**
 * Common interface for all sensor collectors.
 * Each implementation wraps a hardware sensor and emits readings as a Flow.
 */
interface SensorCollector<T : SensorReading> {
    /** Whether this sensor is available on the device. */
    val isAvailable: Boolean

    /** Flow of sensor readings. Starts collecting when collected, stops when cancelled. */
    fun readings(): Flow<T>

    /** Start the sensor with the given sampling period (microseconds). */
    fun start(samplingPeriodUs: Int = 20_000) // 50 Hz default

    /** Stop the sensor and release resources. */
    fun stop()
}
