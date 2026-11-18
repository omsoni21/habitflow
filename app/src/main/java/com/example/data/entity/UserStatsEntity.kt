package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val currentLevelFloorXp: Int = 0,
    val nextLevelXp: Int = 50,
    val progressToNextLevel: Float = 0f,
    val completionXp: Int = 0,
    val streakMilestoneXp: Int = 0,
    val perfectDayXp: Int = 0,
    val achievementXp: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
