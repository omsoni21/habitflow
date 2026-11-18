package com.example.domain.usecase

import com.example.data.entity.UserStatsEntity
import com.example.domain.model.AchievementType
import com.example.domain.model.Habit
import com.example.domain.model.HabitCompletion
import java.time.LocalDate

enum class AppAccentPalette(
    val label: String,
    val subtitle: String,
    val requiredLevel: Int,
    val previewColors: List<Long>
) {
    ELECTRIC_INDIGO(
        label = "Electric Indigo",
        subtitle = "Signature purple, indigo & sky palette",
        requiredLevel = 1,
        previewColors = listOf(0xFF4F46E5L, 0xFF7C3AEDL, 0xFF0284C7L)
    ),
    AURORA_EMERALD(
        label = "Aurora Emerald",
        subtitle = "Unlocked at Level 3 • Refreshing teal & mint",
        requiredLevel = 3,
        previewColors = listOf(0xFF059669L, 0xFF0D9488L, 0xFF34D399L)
    ),
    MIDNIGHT_GOLD(
        label = "Midnight Gold",
        subtitle = "Unlocked at Level 7 • Warm amber & obsidian crest",
        requiredLevel = 7,
        previewColors = listOf(0xFFB45309L, 0xFFD97706L, 0xFFFBBF24L)
    )
}

enum class AppCrestIcon(
    val label: String,
    val subtitle: String,
    val requiredLevel: Int,
    val accentColor: Long
) {
    CLASSIC_BOLT(
        label = "Electric Spark",
        subtitle = "Starter HabitFlow energy crest",
        requiredLevel = 1,
        accentColor = 0xFF4F46E5L
    ),
    EMBER_FLAME(
        label = "Ember Flame",
        subtitle = "Unlocked at Level 2 • Glowing streak emblem",
        requiredLevel = 2,
        accentColor = 0xFFF59E0BL
    ),
    STAR_CROWN(
        label = "Starlight Crest",
        subtitle = "Unlocked at Level 4 • Radiant focus badge",
        requiredLevel = 4,
        accentColor = 0xFF7C3AEDL
    ),
    DIAMOND_SHIELD(
        label = "Prism Diamond",
        subtitle = "Unlocked at Level 6 • Mastery crystal emblem",
        requiredLevel = 6,
        accentColor = 0xFF0284C7L
    ),
    GOLDEN_CREST(
        label = "Golden Crown",
        subtitle = "Unlocked at Level 7 • Prestige golden trophy crest",
        requiredLevel = 7,
        accentColor = 0xFFD97706L
    )
}

enum class GamificationReward(
    val title: String,
    val description: String,
    val requiredLevel: Int
) {
    AURORA_EMERALD_THEME(
        title = "Aurora Emerald Theme",
        description = "Refreshing teal-emerald & cyan Material 3 theme",
        requiredLevel = 3
    ),
    CONSISTENCY_HEATMAP(
        title = "90-Day Consistency Heatmap",
        description = "Multi-month daily intensity grid in Statistics",
        requiredLevel = 5
    ),
    MIDNIGHT_GOLD_THEME(
        title = "Midnight Gold & Golden Crest",
        description = "Luxury gold theme + Golden Crest emblem on your profile",
        requiredLevel = 7
    )
}

data class XpBreakdown(
    val completionXp: Int,
    val streakMilestoneXp: Int,
    val perfectDayXp: Int,
    val achievementXp: Int
) {
    val totalXp: Int
        get() = completionXp + streakMilestoneXp + perfectDayXp + achievementXp
}

data class XpRewardCalculation(
    val baseCompletionXp: Int,
    val multiTargetBonusXp: Int,
    val streakMilestoneXp: Int,
    val perfectDayBonusXp: Int,
    val achievementBonusXp: Int,
    val reachedMilestones: List<Int>,
    val totalXpDelta: Int
)

data class CelebrationEvent(
    val id: Long = System.nanoTime(),
    val habitTitle: String,
    val newLevel: Int? = null,
    val reachedStreakMilestone: Int? = null
) {
    val isLevelUp: Boolean get() = newLevel != null
    val isStreakMilestone: Boolean get() = reachedStreakMilestone != null
}

data class LevelStatus(
    val level: Int,
    val title: String,
    val totalXp: Int,
    val currentLevelFloorXp: Int,
    val nextLevelXp: Int,
    val progressToNextLevel: Float,
    val xpRemainingForNextLevel: Int,
    val breakdown: XpBreakdown,
    val unlockedRewards: Set<GamificationReward>
)

object GamificationEngine {

    const val XP_HABIT_COMPLETION = 10
    const val XP_MULTI_TARGET_BONUS = 5
    const val XP_PERFECT_DAY = 25
    const val XP_ACHIEVEMENT_UNLOCK = 40

    const val XP_STREAK_3 = 25
    const val XP_STREAK_7 = 50
    const val XP_STREAK_14 = 100
    const val XP_STREAK_30 = 250
    const val XP_STREAK_100 = 1000

    val SIGNIFICANT_STREAK_MILESTONES = listOf(3, 7, 14, 30, 100)

    private val milestoneRewards = listOf(
        3 to XP_STREAK_3,
        7 to XP_STREAK_7,
        14 to XP_STREAK_14,
        30 to XP_STREAK_30,
        100 to XP_STREAK_100
    )

    fun detectCelebrationTrigger(
        habitTitle: String,
        previousLevel: Int,
        newLevel: Int,
        previousStreak: Int,
        newStreak: Int
    ): CelebrationEvent? {
        val levelUpTo = if (newLevel > previousLevel) newLevel else null
        val crossedMilestone = if (newStreak > previousStreak) {
            SIGNIFICANT_STREAK_MILESTONES.lastOrNull { milestone ->
                previousStreak < milestone && newStreak >= milestone
            }
        } else {
            null
        }
        if (levelUpTo == null && crossedMilestone == null) return null
        return CelebrationEvent(
            habitTitle = habitTitle,
            newLevel = levelUpTo,
            reachedStreakMilestone = crossedMilestone
        )
    }

    private val levelThresholds = listOf(
        1 to 0,
        2 to 50,
        3 to 150,
        4 to 300,
        5 to 500,
        6 to 800,
        7 to 1200
    )

    fun xpThresholdForLevel(level: Int): Int {
        if (level <= 1) return 0
        levelThresholds.firstOrNull { it.first == level }?.let { return it.second }
        val extraLevels = level - 7
        return 1200 + (extraLevels * 500)
    }

    fun titleForLevel(level: Int): String = when (level) {
        1 -> "Seedling"
        2 -> "Momentum Builder"
        3 -> "Rhythm Keeper"
        4 -> "Streak Seeker"
        5 -> "Consistency Architect"
        6 -> "Flow Master"
        7 -> "Zenith Guardian"
        else -> "Grandmaster"
    }

    fun levelForXp(totalXp: Int): Int {
        val safeXp = totalXp.coerceAtLeast(0)
        var currentLevel = 1
        while (safeXp >= xpThresholdForLevel(currentLevel + 1)) {
            currentLevel++
        }
        return currentLevel
    }

    /**
     * Calculates the XP delta for a habit completion (or undo) event and any newly reached
     * streak milestones (3, 7, 14, 30, 100 days), perfect-day bonuses, and badge unlocks.
     */
    fun calculateEventXpReward(
        habitTarget: Int,
        previousStreak: Int,
        newStreak: Int,
        isUndo: Boolean = false,
        isPerfectDayReached: Boolean = false,
        newlyUnlockedAchievementsCount: Int = 0
    ): XpRewardCalculation {
        if (isUndo) {
            val baseUndo = -XP_HABIT_COMPLETION
            val multiUndo = if (habitTarget > 1) -XP_MULTI_TARGET_BONUS else 0
            return XpRewardCalculation(
                baseCompletionXp = baseUndo,
                multiTargetBonusXp = multiUndo,
                streakMilestoneXp = 0,
                perfectDayBonusXp = 0,
                achievementBonusXp = 0,
                reachedMilestones = emptyList(),
                totalXpDelta = baseUndo + multiUndo
            )
        }

        val baseXp = XP_HABIT_COMPLETION
        val multiTargetXp = if (habitTarget > 1) XP_MULTI_TARGET_BONUS else 0
        val (reachedMilestones, milestoneXp) = streakMilestoneXpForTransition(previousStreak, newStreak)
        val perfectDayXp = if (isPerfectDayReached) XP_PERFECT_DAY else 0
        val achievementXp = newlyUnlockedAchievementsCount.coerceAtLeast(0) * XP_ACHIEVEMENT_UNLOCK

        return XpRewardCalculation(
            baseCompletionXp = baseXp,
            multiTargetBonusXp = multiTargetXp,
            streakMilestoneXp = milestoneXp,
            perfectDayBonusXp = perfectDayXp,
            achievementBonusXp = achievementXp,
            reachedMilestones = reachedMilestones,
            totalXpDelta = baseXp + multiTargetXp + milestoneXp + perfectDayXp + achievementXp
        )
    }

    fun streakMilestoneXpForTransition(
        previousStreak: Int,
        newStreak: Int
    ): Pair<List<Int>, Int> {
        if (newStreak <= previousStreak) return emptyList<Int>() to 0
        val crossed = milestoneRewards.filter { (days, _) ->
            previousStreak < days && newStreak >= days
        }
        return crossed.map { it.first } to crossed.sumOf { it.second }
    }

    fun calculateXpBreakdown(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        unlockedAchievements: Set<AchievementType>,
        today: LocalDate = LocalDate.now()
    ): XpBreakdown {
        val habitMap = habits.associateBy { it.id }

        // 1. Daily completion XP (+10 XP, +5 bonus when target > 1)
        var completionXp = 0
        val completedRecords = completions.filter { completion ->
            val habit = habitMap[completion.habitId] ?: return@filter false
            completion.value >= habit.target
        }
        completedRecords.forEach { completion ->
            val habit = habitMap[completion.habitId]
            completionXp += XP_HABIT_COMPLETION
            if (habit != null && habit.target > 1) {
                completionXp += XP_MULTI_TARGET_BONUS
            }
        }

        // 2. Streak milestone XP per habit based on longest streak achieved
        var streakXp = 0
        habits.forEach { habit ->
            val longest = StreakCalculator.calculateHabitStreak(habit, completions, today).longestStreak
            milestoneRewards.forEach { (days, rewardXp) ->
                if (longest >= days) streakXp += rewardXp
            }
        }

        // 3. Perfect day bonus XP
        val candidateDates = completedRecords.map { it.completionDate }.distinct()
        val perfectDaysCount = candidateDates.count { date ->
            if (date.isAfter(today)) return@count false
            val point = StreakCalculator.calculateDailyCompletionRate(habits, completions, date)
            point.scheduledCount > 0 && point.completedCount >= point.scheduledCount
        }
        val perfectDayXp = perfectDaysCount * XP_PERFECT_DAY

        // 4. Achievement XP
        val achievementXp = unlockedAchievements.size * XP_ACHIEVEMENT_UNLOCK

        return XpBreakdown(
            completionXp = completionXp,
            streakMilestoneXp = streakXp,
            perfectDayXp = perfectDayXp,
            achievementXp = achievementXp
        )
    }

    fun computeLevelStatus(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        unlockedAchievements: Set<AchievementType>,
        today: LocalDate = LocalDate.now()
    ): LevelStatus {
        val breakdown = calculateXpBreakdown(habits, completions, unlockedAchievements, today)
        return buildLevelStatusFromXp(breakdown)
    }

    fun buildLevelStatusFromXp(breakdown: XpBreakdown): LevelStatus {
        val totalXp = breakdown.totalXp.coerceAtLeast(0)
        val level = levelForXp(totalXp)
        val floorXp = xpThresholdForLevel(level)
        val nextXp = xpThresholdForLevel(level + 1)
        val span = (nextXp - floorXp).coerceAtLeast(1)
        val progress = ((totalXp - floorXp).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        val remaining = (nextXp - totalXp).coerceAtLeast(0)
        val rewards = GamificationReward.entries.filter { level >= it.requiredLevel }.toSet()

        return LevelStatus(
            level = level,
            title = titleForLevel(level),
            totalXp = totalXp,
            currentLevelFloorXp = floorXp,
            nextLevelXp = nextXp,
            progressToNextLevel = progress,
            xpRemainingForNextLevel = remaining,
            breakdown = breakdown,
            unlockedRewards = rewards
        )
    }

    fun LevelStatus.toUserStatsEntity(updatedAt: Long = System.currentTimeMillis()): UserStatsEntity =
        UserStatsEntity(
            id = UserStatsEntity.SINGLETON_ID,
            totalXp = totalXp,
            currentLevel = level,
            currentLevelFloorXp = currentLevelFloorXp,
            nextLevelXp = nextLevelXp,
            progressToNextLevel = progressToNextLevel,
            completionXp = breakdown.completionXp,
            streakMilestoneXp = breakdown.streakMilestoneXp,
            perfectDayXp = breakdown.perfectDayXp,
            achievementXp = breakdown.achievementXp,
            updatedAt = updatedAt
        )

    fun UserStatsEntity.toLevelStatus(): LevelStatus = buildLevelStatusFromXp(
        XpBreakdown(
            completionXp = completionXp,
            streakMilestoneXp = streakMilestoneXp,
            perfectDayXp = perfectDayXp,
            achievementXp = achievementXp
        )
    )
}
