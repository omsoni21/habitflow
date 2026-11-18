package com.example.widget

import com.example.data.repository.GamificationRepository
import com.example.data.repository.HabitRepository
import com.example.domain.usecase.HabitScheduleHelper
import com.example.domain.usecase.StreakCalculator
import com.example.utils.DateUtils
import java.time.LocalDate

data class HabitWidgetItem(
    val habitId: Long,
    val title: String,
    val categoryLabel: String,
    val todayValue: Int,
    val target: Int,
    val targetUnit: String,
    val isCompleted: Boolean,
    val currentStreak: Int
)

data class HabitWidgetSnapshot(
    val dateFormatted: String,
    val totalToday: Int,
    val completedToday: Int,
    val progressPercent: Int,
    val userLevel: Int,
    val bestCurrentStreak: Int,
    val items: List<HabitWidgetItem>
)

object HabitWidgetDataLoader {

    suspend fun loadSnapshot(
        habitRepository: HabitRepository,
        gamificationRepository: GamificationRepository,
        today: LocalDate = DateUtils.today()
    ): HabitWidgetSnapshot {
        val habits = habitRepository.getAllHabitEntitiesOnce()
        val completions = habitRepository.getAllCompletionEntitiesOnce()
        val domainHabits = habits.map { entity ->
            com.example.domain.model.Habit(
                id = entity.id,
                title = entity.title,
                description = entity.description,
                icon = entity.icon,
                category = runCatching {
                    com.example.domain.model.HabitCategory.valueOf(entity.category)
                }.getOrDefault(com.example.domain.model.HabitCategory.PRODUCTIVITY),
                color = entity.color,
                frequency = runCatching {
                    com.example.domain.model.HabitFrequency.valueOf(entity.frequency)
                }.getOrDefault(com.example.domain.model.HabitFrequency.DAILY),
                selectedDays = DateUtils.decodeDays(entity.selectedDays),
                customIntervalDays = entity.customIntervalDays,
                target = entity.target,
                targetUnit = entity.targetUnit,
                reminderTime = DateUtils.parseTime24(entity.reminderTime),
                reminderEnabled = entity.reminderEnabled,
                startDate = DateUtils.parseIsoDate(entity.startDate),
                isActive = entity.isActive,
                isArchived = entity.isArchived,
                createdAt = entity.createdAt
            )
        }
        val domainCompletions = completions.map { entity ->
            com.example.domain.model.HabitCompletion(
                id = entity.id,
                habitId = entity.habitId,
                completionDate = DateUtils.parseIsoDate(entity.completionDate),
                completedAt = entity.completedAt,
                value = entity.value
            )
        }

        val todayCompletions = domainCompletions
            .filter { it.completionDate == today }
            .associateBy { it.habitId }

        val scheduledItems = domainHabits
            .filter { HabitScheduleHelper.isScheduledForDate(it, today) }
            .map { habit ->
                val todayValue = todayCompletions[habit.id]?.value ?: 0
                val streak = StreakCalculator.calculateHabitStreak(habit, domainCompletions, today)
                HabitWidgetItem(
                    habitId = habit.id,
                    title = habit.title,
                    categoryLabel = habit.category.label,
                    todayValue = todayValue,
                    target = habit.target.coerceAtLeast(1),
                    targetUnit = habit.targetUnit,
                    isCompleted = todayValue >= habit.target.coerceAtLeast(1),
                    currentStreak = streak.currentStreak
                )
            }
            .sortedWith(
                compareBy<HabitWidgetItem> { it.isCompleted }
                    .thenByDescending { it.currentStreak }
                    .thenBy { it.title.lowercase() }
            )

        val totalToday = scheduledItems.size
        val completedToday = scheduledItems.count { it.isCompleted }
        val progressPercent = if (totalToday == 0) 0 else ((completedToday * 100f) / totalToday).toInt().coerceIn(0, 100)
        val bestCurrentStreak = scheduledItems.maxOfOrNull { it.currentStreak } ?: 0

        gamificationRepository.ensureInitialized(today)
        val userStats = gamificationRepository.getUserStatsOnce()

        return HabitWidgetSnapshot(
            dateFormatted = DateUtils.formatFullDate(today),
            totalToday = totalToday,
            completedToday = completedToday,
            progressPercent = progressPercent,
            userLevel = userStats.currentLevel.coerceAtLeast(1),
            bestCurrentStreak = bestCurrentStreak,
            items = scheduledItems
        )
    }
}
