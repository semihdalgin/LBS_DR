package com.lbs.dr.sensor.imu

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.lbs.dr.domain.model.SensorReading
import com.lbs.dr.sensor.SensorCollector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collects IMU data: accelerometer, gyroscope, magnetometer.
 * Uses Android SensorManager to register listeners and emit fused IMU readings.
 */
@Singleton
class ImuCollector @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorCollector<SensorReading.Imu> {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    override val isAvailable: Boolean
        get() = accelerometer != null && gyroscope != null

    // Latest values from each sensor
    @Volatile private var accel = FloatArray(3)
    @Volatile private var gyro = FloatArray(3)
    @Volatile private var mag = FloatArray(3)

    private var samplingPeriod = SensorManager.SENSOR_DELAY_GAME

    override fun readings(): Flow<SensorReading.Imu> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        accel = event.values.copyOf()
                        // Emit fused reading on every accelerometer update (highest rate)
                        trySend(
                            SensorReading.Imu(
                                ax = accel[0], ay = accel[1], az = accel[2],
                                gx = gyro[0], gy = gyro[1], gz = gyro[2],
                                mx = mag[0], my = mag[1], mz = mag[2],
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    Sensor.TYPE_GYROSCOPE -> gyro = event.values.copyOf()
                    Sensor.TYPE_MAGNETIC_FIELD -> mag = event.values.copyOf()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, samplingPeriod)
        }
        gyroscope?.let {
            sensorManager.registerListener(listener, it, samplingPeriod)
        }
        magnetometer?.let {
            sensorManager.registerListener(listener, it, samplingPeriod)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    override fun start(samplingPeriodUs: Int) {
        samplingPeriod = samplingPeriodUs
    }

    override fun stop() {
        // Flow cancellation handles unregistration
    }
}
