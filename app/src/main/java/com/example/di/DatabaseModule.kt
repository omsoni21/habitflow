package com.example.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AchievementDao
import com.example.data.dao.HabitCompletionDao
import com.example.data.dao.HabitDao
import com.example.data.dao.UserStatsDao
import com.example.data.database.HabitFlowDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_stats` (
                    `id` INTEGER NOT NULL,
                    `totalXp` INTEGER NOT NULL,
                    `currentLevel` INTEGER NOT NULL,
                    `currentLevelFloorXp` INTEGER NOT NULL,
                    `nextLevelXp` INTEGER NOT NULL,
                    `progressToNextLevel` REAL NOT NULL,
                    `completionXp` INTEGER NOT NULL,
                    `streakMilestoneXp` INTEGER NOT NULL,
                    `perfectDayXp` INTEGER NOT NULL,
                    `achievementXp` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideHabitFlowDatabase(
        @ApplicationContext context: Context
    ): HabitFlowDatabase {
        return Room.databaseBuilder(
            context,
            HabitFlowDatabase::class.java,
            "habitflow_database.db"
        ).addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    @Provides
    fun provideHabitDao(database: HabitFlowDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideHabitCompletionDao(database: HabitFlowDatabase): HabitCompletionDao =
        database.habitCompletionDao()

    @Provides
    fun provideAchievementDao(database: HabitFlowDatabase): AchievementDao =
        database.achievementDao()

    @Provides
    fun provideUserStatsDao(database: HabitFlowDatabase): UserStatsDao =
        database.userStatsDao()

    @Provides
    @Singleton
    fun provideWidgetUpdateNotifier(
        @ApplicationContext context: Context
    ): com.example.widget.WidgetUpdateNotifier =
        com.example.widget.AndroidWidgetUpdateNotifier(context)
}
