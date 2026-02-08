package com.lbs.dr.domain.model

/**
 * Floor detection state and transition info.
 */
data class FloorInfo(
    val currentFloor: Int,
    val floorHeight: Float = DEFAULT_FLOOR_HEIGHT,
    val pressureAtReference: Float,
    val altitudeEstimate: Float,
    val transition: FloorTransition = FloorTransition.NONE,
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_FLOOR_HEIGHT = 3.0f   // meters
        const val PA_PER_METER = 12.0f          // Pa per meter altitude change
    }
}

enum class FloorTransition {
    NONE,
    ASCENDING,
    DESCENDING
}
