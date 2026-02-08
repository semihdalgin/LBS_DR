package com.lbs.dr.sensor.barometer

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
 * Collects barometric pressure and ambient temperature from built-in sensors.
 * Used for altitude estimation and floor detection.
 */
@Singleton
class BarometerCollector @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorCollector<SensorReading.Pressure> {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

    override val isAvailable: Boolean
        get() = pressureSensor != null

    @Volatile private var lastTemp: Float? = null

    override fun readings(): Flow<SensorReading.Pressure> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_PRESSURE -> {
                        trySend(
                            SensorReading.Pressure(
                                pressureHPa = event.values[0],
                                temperatureC = lastTemp,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                        lastTemp = event.values[0]
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        pressureSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        tempSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    override fun start(samplingPeriodUs: Int) {}
    override fun stop() {}
}
