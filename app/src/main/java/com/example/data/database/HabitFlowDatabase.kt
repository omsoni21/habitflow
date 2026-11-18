package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.dao.AchievementDao
import com.example.data.dao.HabitCompletionDao
import com.example.data.dao.HabitDao
import com.example.data.dao.UserStatsDao
import com.example.data.entity.AchievementEntity
import com.example.data.entity.HabitCompletionEntity
import com.example.data.entity.HabitEntity
import com.example.data.entity.UserStatsEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitCompletionEntity::class,
        AchievementEntity::class,
        UserStatsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HabitFlowDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun userStatsDao(): UserStatsDao
}
