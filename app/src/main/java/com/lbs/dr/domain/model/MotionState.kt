package com.lbs.dr.domain.model

/**
 * Current motion state of the user, inferred from sensor data.
 */
data class MotionState(
    val activity: ActivityType,
    val heading: Float,          // radians from magnetic north
    val speed: Float,            // m/s estimated
    val stepFrequency: Float,    // Hz
    val strideLength: Float,     // meters
    val isStationary: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ActivityType {
    STATIONARY,
    WALKING,
    RUNNING,
    CLIMBING_STAIRS,
    DESCENDING_STAIRS,
    ELEVATOR,
    UNKNOWN
}
