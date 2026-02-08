package com.lbs.dr.sensor.ble

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.lbs.dr.domain.model.BleBeacon
import com.lbs.dr.domain.model.SensorReading
import com.lbs.dr.sensor.SensorCollector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans for BLE beacons (iBeacon, Eddystone, etc.) and emits RSSI readings.
 * Uses Android BLE scanning API with configurable scan modes.
 */
@Singleton
class BleBeaconCollector @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorCollector<SensorReading.BleScan> {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val scanner: BluetoothLeScanner?
        get() = bluetoothManager?.adapter?.bluetoothLeScanner

    override val isAvailable: Boolean
        get() = bluetoothManager?.adapter?.isEnabled == true &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    override fun readings(): Flow<SensorReading.BleScan> = callbackFlow {
        val beaconBuffer = mutableMapOf<String, BleBeacon>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED

                val deviceName = if (hasPermission) result.device.name else null
                val deviceAddress = result.device.address

                val beacon = BleBeacon(
                    address = deviceAddress,
                    name = deviceName,
                    rssi = result.rssi,
                    txPower = result.txPower.takeIf { it != ScanResult.TX_POWER_NOT_PRESENT },
                    uuid = parseIBeaconUuid(result),
                    major = parseIBeaconMajor(result),
                    minor = parseIBeaconMinor(result)
                )

                beaconBuffer[deviceAddress] = beacon
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                for (result in results) {
                    onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
                }
                // Emit batch
                if (beaconBuffer.isNotEmpty()) {
                    trySend(
                        SensorReading.BleScan(
                            beacons = beaconBuffer.values.toList(),
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    beaconBuffer.clear()
                }
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(1000) // batch every 1s
            .build()

        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            scanner?.startScan(emptyList<ScanFilter>(), settings, callback)
        }

        awaitClose {
            if (hasPermission) {
                scanner?.stopScan(callback)
            }
        }
    }

    override fun start(samplingPeriodUs: Int) {}
    override fun stop() {}

    // --- iBeacon parsing utilities ---

    private fun parseIBeaconUuid(result: ScanResult): String? {
        val bytes = result.scanRecord?.bytes ?: return null
        if (bytes.size < 30) return null
        // iBeacon: Company ID (0x004C) + Type (0x02) + Length (0x15) + UUID (16 bytes)
        val idx = findIBeaconPrefix(bytes) ?: return null
        val uuidBytes = bytes.sliceArray(idx until idx + 16)
        return formatUuid(uuidBytes)
    }

    private fun parseIBeaconMajor(result: ScanResult): Int? {
        val bytes = result.scanRecord?.bytes ?: return null
        val idx = findIBeaconPrefix(bytes) ?: return null
        if (idx + 18 > bytes.size) return null
        return ((bytes[idx + 16].toInt() and 0xFF) shl 8) or (bytes[idx + 17].toInt() and 0xFF)
    }

    private fun parseIBeaconMinor(result: ScanResult): Int? {
        val bytes = result.scanRecord?.bytes ?: return null
        val idx = findIBeaconPrefix(bytes) ?: return null
        if (idx + 20 > bytes.size) return null
        return ((bytes[idx + 18].toInt() and 0xFF) shl 8) or (bytes[idx + 19].toInt() and 0xFF)
    }

    private fun findIBeaconPrefix(bytes: ByteArray): Int? {
        for (i in 0 until bytes.size - 4) {
            if (bytes[i].toInt() and 0xFF == 0x4C &&
                bytes[i + 1].toInt() and 0xFF == 0x00 &&
                bytes[i + 2].toInt() and 0xFF == 0x02 &&
                bytes[i + 3].toInt() and 0xFF == 0x15
            ) {
                return i + 4
            }
        }
        return null
    }

    private fun formatUuid(bytes: ByteArray): String {
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-" +
                "${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
    }
}
