package com.lbs.dr.domain.model

/**
 * 3D position in a building with floor information.
 */
data class Position(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val floor: Int,
    val accuracy: Float,
    val source: PositionSource,
    val timestamp: Long = System.currentTimeMillis()
)

enum class PositionSource {
    GPS,
    FUSED,              // Google Fused Location
    DEAD_RECKONING,
    WIFI_RTT,
    BLE_RSSI,
    PARTICLE_FILTER,
    SENSOR_FUSION       // Final fused result
}
