package com.example.domain.usecase

import com.example.domain.model.AchievementProgress
import com.example.domain.model.AchievementType
import com.example.domain.model.Habit
import com.example.domain.model.HabitCompletion
import java.time.LocalDate

object AchievementEvaluator {

    fun evaluateNewUnlocks(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        alreadyUnlocked: Set<AchievementType>,
        today: LocalDate = LocalDate.now()
    ): List<AchievementType> {
        if (habits.isEmpty() || completions.isEmpty()) return emptyList()

        val habitMap = habits.associateBy { it.id }
        val completedRecords = completions.filter { completion ->
            val target = habitMap[completion.habitId]?.target ?: 1
            completion.value >= target
        }

        val totalCompletions = completedRecords.size
        val maxLongestStreak = habits.maxOfOrNull {
            StreakCalculator.calculateHabitStreak(it, completions, today).longestStreak
        } ?: 0

        val hasPerfectDay = hasAnyPerfectDay(habits, completions, today)

        val newlyUnlocked = mutableListOf<AchievementType>()

        fun check(type: AchievementType, condition: Boolean) {
            if (condition && type !in alreadyUnlocked) {
                newlyUnlocked.add(type)
            }
        }

        check(AchievementType.FIRST_HABIT_COMPLETED, totalCompletions >= 1)
        check(AchievementType.STREAK_3_DAYS, maxLongestStreak >= 3)
        check(AchievementType.STREAK_7_DAYS, maxLongestStreak >= 7)
        check(AchievementType.STREAK_14_DAYS, maxLongestStreak >= 14)
        check(AchievementType.STREAK_30_DAYS, maxLongestStreak >= 30)
        check(AchievementType.STREAK_100_DAYS, maxLongestStreak >= 100)
        check(AchievementType.PERFECT_DAY, hasPerfectDay)
        check(AchievementType.TOTAL_100_COMPLETIONS, totalCompletions >= 100)

        return newlyUnlocked
    }

    fun buildAchievementProgressList(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        unlockedTimestamps: Map<AchievementType, Long>,
        today: LocalDate = LocalDate.now()
    ): List<AchievementProgress> {
        val habitMap = habits.associateBy { it.id }
        val totalCompletions = completions.count { completion ->
            val target = habitMap[completion.habitId]?.target ?: 1
            completion.value >= target
        }
        val maxStreak = habits.maxOfOrNull {
            StreakCalculator.calculateHabitStreak(it, completions, today).longestStreak
        } ?: 0
        val hasPerfectDay = unlockedTimestamps.containsKey(AchievementType.PERFECT_DAY) ||
            hasAnyPerfectDay(habits, completions, today)

        return AchievementType.entries.map { type ->
            val unlockedAt = unlockedTimestamps[type]
            val current = when (type) {
                AchievementType.FIRST_HABIT_COMPLETED -> totalCompletions.coerceAtMost(1)
                AchievementType.STREAK_3_DAYS -> maxStreak.coerceAtMost(3)
                AchievementType.STREAK_7_DAYS -> maxStreak.coerceAtMost(7)
                AchievementType.STREAK_14_DAYS -> maxStreak.coerceAtMost(14)
                AchievementType.STREAK_30_DAYS -> maxStreak.coerceAtMost(30)
                AchievementType.STREAK_100_DAYS -> maxStreak.coerceAtMost(100)
                AchievementType.PERFECT_DAY -> if (hasPerfectDay) 1 else 0
                AchievementType.TOTAL_100_COMPLETIONS -> totalCompletions.coerceAtMost(100)
            }

            AchievementProgress(
                type = type,
                isUnlocked = unlockedAt != null,
                unlockedAt = unlockedAt,
                currentProgress = if (unlockedAt != null) type.requiredCount else current,
                targetProgress = type.requiredCount
            )
        }
    }

    private fun hasAnyPerfectDay(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        today: LocalDate
    ): Boolean {
        val candidateDates = completions.map { it.completionDate }.distinct()
        return candidateDates.any { date ->
            if (date.isAfter(today)) return@any false
            val point = StreakCalculator.calculateDailyCompletionRate(habits, completions, date)
            point.scheduledCount > 0 && point.completedCount >= point.scheduledCount
        }
    }
}
