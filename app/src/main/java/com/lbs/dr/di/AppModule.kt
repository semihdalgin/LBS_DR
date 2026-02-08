package com.lbs.dr.di

import android.content.Context
import androidx.room.Room
import com.lbs.dr.data.db.AppDatabase
import com.lbs.dr.data.db.PositionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lbs_dr_database"
        ).build()
    }

    @Provides
    @Singleton
    fun providePositionDao(database: AppDatabase): PositionDao {
        return database.positionDao()
    }
}
