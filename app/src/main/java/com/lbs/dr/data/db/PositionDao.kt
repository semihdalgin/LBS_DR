package com.lbs.dr.data.db

import androidx.room.*
import com.lbs.dr.data.model.PositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {
    @Insert
    suspend fun insert(position: PositionEntity): Long

    @Insert
    suspend fun insertAll(positions: List<PositionEntity>)

    @Query("SELECT * FROM positions WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getBySession(sessionId: String): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    suspend fun getByTimeRange(start: Long, end: Long): List<PositionEntity>

    @Query("SELECT DISTINCT sessionId FROM positions ORDER BY timestamp DESC")
    fun getSessions(): Flow<List<String>>

    @Query("DELETE FROM positions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM positions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM positions WHERE sessionId = :sessionId")
    suspend fun countBySession(sessionId: String): Int
}
