package com.lbs.dr.domain.model

/**
 * Unified sensor reading types for the indoor positioning system.
 */
sealed class SensorReading {
    abstract val timestamp: Long

    /** Raw IMU data: accelerometer + gyroscope + magnetometer */
    data class Imu(
        val ax: Float, val ay: Float, val az: Float,   // m/s² accelerometer
        val gx: Float, val gy: Float, val gz: Float,   // rad/s gyroscope
        val mx: Float, val my: Float, val mz: Float,   // µT magnetometer
        override val timestamp: Long = System.currentTimeMillis()
    ) : SensorReading()

    /** Barometric pressure reading */
    data class Pressure(
        val pressureHPa: Float,       // hPa (millibar)
        val temperatureC: Float?,     // optional ambient temp
        override val timestamp: Long = System.currentTimeMillis()
    ) : SensorReading()

    /** WiFi access point scan result */
    data class WifiScan(
        val accessPoints: List<AccessPoint>,
        override val timestamp: Long = System.currentTimeMillis()
    ) : SensorReading()

    /** BLE beacon scan result */
    data class BleScan(
        val beacons: List<BleBeacon>,
        override val timestamp: Long = System.currentTimeMillis()
    ) : SensorReading()

    /** Step event from pedometer / step detector */
    data class StepEvent(
        val stepCount: Int,
        val strideLength: Float,   // meters
        val heading: Float,        // radians from north
        override val timestamp: Long = System.currentTimeMillis()
    ) : SensorReading()
}

data class AccessPoint(
    val bssid: String,
    val ssid: String,
    val rssi: Int,           // dBm
    val frequency: Int,      // MHz
    val rttSupported: Boolean,
    val rttDistanceMm: Int?  // Wi-Fi RTT distance if available
)

data class BleBeacon(
    val address: String,
    val name: String?,
    val rssi: Int,
    val txPower: Int?,
    val uuid: String?,
    val major: Int?,
    val minor: Int?
)
