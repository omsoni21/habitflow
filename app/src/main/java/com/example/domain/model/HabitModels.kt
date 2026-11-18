package com.example.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class HabitFrequency(val label: String) {
    DAILY("Every day"),
    WEEKDAYS("Selected weekdays"),
    CUSTOM("Custom schedule")
}

enum class HabitCategory(val label: String) {
    HEALTH("Health"),
    FITNESS("Fitness"),
    MINDFULNESS("Mindfulness"),
    PRODUCTIVITY("Productivity"),
    LEARNING("Learning"),
    LIFESTYLE("Lifestyle")
}

enum class StartOfWeek(val label: String) {
    MONDAY("Monday"),
    SUNDAY("Sunday")
}

enum class AchievementType(
    val title: String,
    val description: String,
    val requiredCount: Int
) {
    FIRST_HABIT_COMPLETED(
        title = "First Step",
        description = "Complete your very first habit",
        requiredCount = 1
    ),
    STREAK_3_DAYS(
        title = "Spark Ignited",
        description = "Reach a 3-day consistency streak",
        requiredCount = 3
    ),
    STREAK_7_DAYS(
        title = "One Week Champion",
        description = "Maintain a 7-day habit streak",
        requiredCount = 7
    ),
    STREAK_14_DAYS(
        title = "Fortnight Focus",
        description = "Keep a 14-day streak going strong",
        requiredCount = 14
    ),
    STREAK_30_DAYS(
        title = "30-Day Consistency",
        description = "Build an unstoppable 30-day streak",
        requiredCount = 30
    ),
    STREAK_100_DAYS(
        title = "Century Master",
        description = "Achieve a legendary 100-day streak",
        requiredCount = 100
    ),
    PERFECT_DAY(
        title = "Perfect Day",
        description = "Complete 100% of your scheduled habits in a day",
        requiredCount = 1
    ),
    TOTAL_100_COMPLETIONS(
        title = "100 Habits Completed",
        description = "Log 100 total habit completions",
        requiredCount = 100
    )
}

data class Habit(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val icon: String = "check_circle",
    val category: HabitCategory = HabitCategory.PRODUCTIVITY,
    val color: Long = 0xFF4F46E5L,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val selectedDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val customIntervalDays: Int = 1,
    val target: Int = 1,
    val targetUnit: String = "times",
    val reminderTime: LocalTime? = null,
    val reminderEnabled: Boolean = false,
    val startDate: LocalDate = LocalDate.now(),
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class HabitCompletion(
    val id: Long = 0L,
    val habitId: Long,
    val completionDate: LocalDate,
    val completedAt: Long,
    val value: Int
)

data class HabitStreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletedDays: Int,
    val completionRate: Float
)

data class HabitWithProgress(
    val habit: Habit,
    val todayValue: Int,
    val isCompletedToday: Boolean,
    val streakInfo: HabitStreakInfo
)

data class AchievementProgress(
    val type: AchievementType,
    val isUnlocked: Boolean,
    val unlockedAt: Long?,
    val currentProgress: Int,
    val targetProgress: Int
)

data class DailyCompletionPoint(
    val date: LocalDate,
    val label: String,
    val completedCount: Int,
    val scheduledCount: Int,
    val percentage: Float
)

data class HabitConsistencyStat(
    val habit: Habit,
    val completionRate: Float,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletedDays: Int
)

data class HabitSuggestion(
    val title: String,
    val description: String,
    val icon: String,
    val category: HabitCategory,
    val color: Long,
    val target: Int,
    val targetUnit: String,
    val defaultReminderTime: LocalTime
)
