package com.lbs.dr.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting position records.
 * Replaces the original 46+ column SQLite schema with a cleaner structure.
 */
@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // Position
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val floor: Int,
    val accuracy: Float,
    val source: String,

    // Motion
    val heading: Float,
    val speed: Float,
    val activity: String,

    // Sensors
    val pressureHPa: Float?,
    val temperatureC: Float?,
    val humidityPercent: Float?,
    val ax: Float?, val ay: Float?, val az: Float?,

    // Floor detection
    val floorTransition: String?,
    val floorConfidence: Float?,

    // Metadata
    val timestamp: Long,
    val sessionId: String
)
