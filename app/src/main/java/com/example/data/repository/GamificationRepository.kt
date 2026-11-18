package com.example.data.repository

import com.example.data.dao.AchievementDao
import com.example.data.dao.HabitCompletionDao
import com.example.data.dao.HabitDao
import com.example.data.dao.UserStatsDao
import com.example.data.entity.UserStatsEntity
import com.example.data.entity.toDomain
import com.example.domain.model.AchievementType
import com.example.domain.usecase.GamificationEngine
import com.example.domain.usecase.GamificationEngine.toLevelStatus
import com.example.domain.usecase.GamificationEngine.toUserStatsEntity
import com.example.domain.usecase.LevelStatus
import com.example.domain.usecase.XpBreakdown
import com.example.domain.usecase.XpRewardCalculation
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationRepository @Inject constructor(
    private val userStatsDao: UserStatsDao,
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao,
    private val achievementDao: AchievementDao
) {

    fun observeUserStats(): Flow<UserStatsEntity> =
        userStatsDao.observeUserStats().map { it ?: UserStatsEntity() }

    fun observeLevelStatus(): Flow<LevelStatus> =
        observeUserStats().map { it.toLevelStatus() }

    suspend fun getUserStatsOnce(): UserStatsEntity =
        userStatsDao.getUserStatsOnce() ?: UserStatsEntity()

    /**
     * Calculates XP rewards for a habit completion (or undo) event and any crossed
     * streak milestones (3, 7, 14, 30, 100 days).
     */
    fun calculateXpReward(
        habitTarget: Int,
        previousStreak: Int,
        newStreak: Int,
        isUndo: Boolean = false,
        isPerfectDayReached: Boolean = false,
        newlyUnlockedAchievementsCount: Int = 0
    ): XpRewardCalculation = GamificationEngine.calculateEventXpReward(
        habitTarget = habitTarget,
        previousStreak = previousStreak,
        newStreak = newStreak,
        isUndo = isUndo,
        isPerfectDayReached = isPerfectDayReached,
        newlyUnlockedAchievementsCount = newlyUnlockedAchievementsCount
    )

    /**
     * Applies an incremental XP reward event to the persisted [UserStatsEntity] row,
     * recalculating level and progress towards the next level.
     */
    suspend fun applyCompletionEventReward(
        habitTarget: Int,
        previousStreak: Int,
        newStreak: Int,
        isUndo: Boolean = false,
        isPerfectDayReached: Boolean = false,
        newlyUnlockedAchievementsCount: Int = 0
    ): UserStatsEntity {
        val current = getUserStatsOnce()
        val reward = calculateXpReward(
            habitTarget = habitTarget,
            previousStreak = previousStreak,
            newStreak = newStreak,
            isUndo = isUndo,
            isPerfectDayReached = isPerfectDayReached,
            newlyUnlockedAchievementsCount = newlyUnlockedAchievementsCount
        )

        val updatedBreakdown = XpBreakdown(
            completionXp = (current.completionXp + reward.baseCompletionXp + reward.multiTargetBonusXp).coerceAtLeast(0),
            streakMilestoneXp = (current.streakMilestoneXp + reward.streakMilestoneXp).coerceAtLeast(0),
            perfectDayXp = (current.perfectDayXp + reward.perfectDayBonusXp).coerceAtLeast(0),
            achievementXp = (current.achievementXp + reward.achievementBonusXp).coerceAtLeast(0)
        )
        val updatedEntity = GamificationEngine
            .buildLevelStatusFromXp(updatedBreakdown)
            .toUserStatsEntity()
        userStatsDao.upsertUserStats(updatedEntity)
        return updatedEntity
    }

    /**
     * Recomputes the authoritative XP breakdown, level, and next-level progress from
     * persisted habits, completions, and achievements, and stores the snapshot in [user_stats].
     */
    suspend fun syncUserStatsFromDatabase(today: LocalDate = DateUtils.today()): LevelStatus {
        val habits = habitDao.getAllHabitsOnce().map { it.toDomain() }
        val completions = completionDao.getAllCompletionsOnce().map { it.toDomain() }
        val unlockedAchievements = achievementDao.getUnlockedAchievementsOnce()
            .mapNotNull { runCatching { AchievementType.valueOf(it.achievementType) }.getOrNull() }
            .toSet()

        val status = GamificationEngine.computeLevelStatus(
            habits = habits,
            completions = completions,
            unlockedAchievements = unlockedAchievements,
            today = today
        )
        userStatsDao.upsertUserStats(status.toUserStatsEntity())
        return status
    }

    suspend fun ensureInitialized(today: LocalDate = DateUtils.today()) {
        if (userStatsDao.getUserStatsOnce() == null) {
            syncUserStatsFromDatabase(today)
        }
    }

    suspend fun resetStats() {
        userStatsDao.deleteUserStats()
    }
}
