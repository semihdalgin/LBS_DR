package com.lbs.dr.sensor.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.lbs.dr.domain.model.AccessPoint
import com.lbs.dr.domain.model.SensorReading
import com.lbs.dr.sensor.SensorCollector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Collects WiFi scan results including Wi-Fi RTT (802.11mc) ranging when available.
 * Wi-Fi RTT provides sub-meter distance estimation to compatible access points.
 */
@Singleton
class WifiCollector @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorCollector<SensorReading.WifiScan> {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val rttManager: WifiRttManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as? WifiRttManager
        } else null

    override val isAvailable: Boolean
        get() = wifiManager.isWifiEnabled

    val isRttAvailable: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT) &&
                rttManager?.isAvailable == true

    override fun readings(): Flow<SensorReading.WifiScan> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPermission) return

                    val results = wifiManager.scanResults
                    launch {
                        val accessPoints = buildAccessPoints(results)
                        trySend(
                            SensorReading.WifiScan(
                                accessPoints = accessPoints,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )

        // Trigger initial scan
        @Suppress("DEPRECATION")
        wifiManager.startScan()

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    /**
     * Build access point list, enriching RTT-capable APs with distance measurements.
     */
    private suspend fun buildAccessPoints(scanResults: List<ScanResult>): List<AccessPoint> {
        val rttCapable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            scanResults.filter { it.is80211mcResponder }
        } else emptyList()

        val rttDistances = if (isRttAvailable && rttCapable.isNotEmpty()) {
            performRttRanging(rttCapable)
        } else emptyMap()

        return scanResults.map { sr ->
            val isRtt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && sr.is80211mcResponder
            AccessPoint(
                bssid = sr.BSSID,
                ssid = sr.SSID,
                rssi = sr.level,
                frequency = sr.frequency,
                rttSupported = isRtt,
                rttDistanceMm = rttDistances[sr.BSSID]
            )
        }
    }

    /**
     * Perform Wi-Fi RTT ranging to get precise distances to compatible APs.
     */
    private suspend fun performRttRanging(
        rttCapable: List<ScanResult>
    ): Map<String, Int> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return emptyMap()

        return suspendCancellableCoroutine { cont ->
            val request = RangingRequest.Builder()
                .addAccessPoints(rttCapable)
                .build()

            val executor = Executors.newSingleThreadExecutor()

            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                cont.resume(emptyMap())
                return@suspendCancellableCoroutine
            }

            rttManager?.startRanging(request, executor, object : RangingResultCallback() {
                override fun onRangingResults(results: MutableList<RangingResult>) {
                    val distances = mutableMapOf<String, Int>()
                    for (result in results) {
                        if (result.status == RangingResult.STATUS_SUCCESS) {
                            distances[result.macAddress.toString()] = result.distanceMm
                        }
                    }
                    cont.resume(distances)
                }

                override fun onRangingFailure(code: Int) {
                    cont.resume(emptyMap())
                }
            })
        }
    }

    override fun start(samplingPeriodUs: Int) {}
    override fun stop() {}
}
