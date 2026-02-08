package com.lbs.dr.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lbs.dr.data.model.PositionEntity

@Database(
    entities = [PositionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun positionDao(): PositionDao
}
