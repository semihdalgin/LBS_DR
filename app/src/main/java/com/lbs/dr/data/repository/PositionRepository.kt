package com.lbs.dr.data.repository

import com.lbs.dr.data.db.PositionDao
import com.lbs.dr.data.model.PositionEntity
import com.lbs.dr.domain.model.FloorInfo
import com.lbs.dr.domain.model.MotionState
import com.lbs.dr.domain.model.Position
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for position data persistence.
 * Abstracts the Room database and provides domain-level operations.
 */
@Singleton
class PositionRepository @Inject constructor(
    private val positionDao: PositionDao
) {
    private var currentSessionId: String = generateSessionId()

    fun startNewSession(): String {
        currentSessionId = generateSessionId()
        return currentSessionId
    }

    fun getCurrentSessionId(): String = currentSessionId

    suspend fun savePosition(
        position: Position,
        motionState: MotionState?,
        floorInfo: FloorInfo?,
        pressure: Float? = null,
        temperature: Float? = null,
        humidity: Float? = null,
        accel: FloatArray? = null
    ) {
        val entity = PositionEntity(
            latitude = position.latitude,
            longitude = position.longitude,
            altitude = position.altitude,
            floor = position.floor,
            accuracy = position.accuracy,
            source = position.source.name,
            heading = motionState?.heading ?: 0f,
            speed = motionState?.speed ?: 0f,
            activity = motionState?.activity?.name ?: "UNKNOWN",
            pressureHPa = pressure,
            temperatureC = temperature,
            humidityPercent = humidity,
            ax = accel?.getOrNull(0),
            ay = accel?.getOrNull(1),
            az = accel?.getOrNull(2),
            floorTransition = floorInfo?.transition?.name,
            floorConfidence = floorInfo?.confidence,
            timestamp = position.timestamp,
            sessionId = currentSessionId
        )
        positionDao.insert(entity)
    }

    fun getSessionPositions(sessionId: String): Flow<List<PositionEntity>> =
        positionDao.getBySession(sessionId)

    fun getRecentPositions(limit: Int = 100): Flow<List<PositionEntity>> =
        positionDao.getRecent(limit)

    fun getSessions(): Flow<List<String>> =
        positionDao.getSessions()

    suspend fun getPositionsByTimeRange(start: Long, end: Long): List<PositionEntity> =
        positionDao.getByTimeRange(start, end)

    suspend fun deleteSession(sessionId: String) =
        positionDao.deleteSession(sessionId)

    suspend fun exportSessionGpx(sessionId: String): String {
        val positions = positionDao.getByTimeRange(0, Long.MAX_VALUE)
            .filter { it.sessionId == sessionId }

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1" creator="LBS_DR Indoor Positioning">""")
            appendLine("  <trk>")
            appendLine("    <name>Session $sessionId</name>")
            appendLine("    <trkseg>")
            for (pos in positions) {
                appendLine("""      <trkpt lat="${pos.latitude}" lon="${pos.longitude}">""")
                appendLine("        <ele>${pos.altitude}</ele>")
                appendLine("        <time>${java.time.Instant.ofEpochMilli(pos.timestamp)}</time>")
                appendLine("        <extensions>")
                appendLine("          <floor>${pos.floor}</floor>")
                appendLine("          <accuracy>${pos.accuracy}</accuracy>")
                appendLine("          <source>${pos.source}</source>")
                appendLine("        </extensions>")
                appendLine("      </trkpt>")
            }
            appendLine("    </trkseg>")
            appendLine("  </trk>")
            appendLine("</gpx>")
        }
    }

    private fun generateSessionId(): String =
        "session_${java.time.LocalDateTime.now().toString().replace(":", "-")}_${UUID.randomUUID().toString().take(8)}"
}
