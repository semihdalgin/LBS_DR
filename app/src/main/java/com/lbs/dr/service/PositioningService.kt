package com.lbs.dr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.lbs.dr.R
import com.lbs.dr.data.repository.PositionRepository
import com.lbs.dr.domain.algorithm.fusion.IndoorPositioningEngine
import com.lbs.dr.sensor.barometer.BarometerCollector
import com.lbs.dr.sensor.ble.BleBeaconCollector
import com.lbs.dr.sensor.imu.ImuCollector
import com.lbs.dr.sensor.wifi.WifiCollector
import com.lbs.dr.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for continuous indoor positioning.
 * Keeps sensor collection alive when the app is in the background.
 */
@AndroidEntryPoint
class PositioningService : LifecycleService() {

    @Inject lateinit var positioningEngine: IndoorPositioningEngine
    @Inject lateinit var imuCollector: ImuCollector
    @Inject lateinit var barometerCollector: BarometerCollector
    @Inject lateinit var wifiCollector: WifiCollector
    @Inject lateinit var bleCollector: BleBeaconCollector
    @Inject lateinit var repository: PositionRepository

    private var collectionJob: Job? = null

    companion object {
        const val CHANNEL_ID = "lbs_dr_positioning"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.lbs.dr.START"
        const val ACTION_STOP = "com.lbs.dr.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> startPositioning()
            ACTION_STOP -> stopPositioning()
        }

        return START_STICKY
    }

    private fun startPositioning() {
        startForeground(NOTIFICATION_ID, createNotification("Positioning active"))

        collectionJob = lifecycleScope.launch {
            // IMU collection
            launch {
                imuCollector.readings().collect { imu ->
                    positioningEngine.processImu(imu)
                }
            }

            // Barometer collection
            launch {
                barometerCollector.readings().collect { pressure ->
                    positioningEngine.processPressure(pressure)
                }
            }

            // WiFi collection
            launch {
                wifiCollector.readings().collect { wifi ->
                    positioningEngine.processWifi(wifi, emptyMap())
                }
            }

            // BLE collection
            launch {
                bleCollector.readings().collect { ble ->
                    positioningEngine.processBle(ble, emptyMap())
                }
            }
        }
    }

    private fun stopPositioning() {
        collectionJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Indoor Positioning",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when indoor positioning is active"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LBS/DR Indoor Positioning")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        collectionJob?.cancel()
        super.onDestroy()
    }
}
