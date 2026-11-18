package com.example.domain.usecase

import com.example.domain.model.Habit
import com.example.domain.model.HabitFrequency
import com.example.utils.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object HabitScheduleHelper {

    fun isScheduledForDate(
        habit: Habit,
        date: LocalDate,
        ignoreActiveFlag: Boolean = false
    ): Boolean {
        if (!ignoreActiveFlag && (!habit.isActive || habit.isArchived)) return false
        if (date.isBefore(habit.startDate)) return false

        return when (habit.frequency) {
            HabitFrequency.DAILY -> true
            HabitFrequency.WEEKDAYS -> date.dayOfWeek in habit.selectedDays
            HabitFrequency.CUSTOM -> {
                if (habit.customIntervalDays > 1) {
                    val daysBetween = ChronoUnit.DAYS.between(habit.startDate, date)
                    daysBetween >= 0 && daysBetween % habit.customIntervalDays == 0L
                } else {
                    date.dayOfWeek in habit.selectedDays
                }
            }
        }
    }

    fun formatScheduleLabel(habit: Habit): String = when (habit.frequency) {
        HabitFrequency.DAILY -> "Every day"
        HabitFrequency.WEEKDAYS -> {
            val sorted = habit.selectedDays.sortedBy { it.value }
            if (sorted.size == 7) {
                "Every day"
            } else if (sorted.size == 5 &&
                sorted.containsAll(
                    listOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    )
                )
            ) {
                "Mon - Fri"
            } else if (sorted.size == 2 &&
                sorted.containsAll(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
            ) {
                "Weekends"
            } else {
                sorted.joinToString(", ") { DateUtils.shortDayName(it) }
            }
        }
        HabitFrequency.CUSTOM -> {
            if (habit.customIntervalDays > 1) {
                "Every ${habit.customIntervalDays} days"
            } else {
                habit.selectedDays.sortedBy { it.value }
                    .joinToString(", ") { DateUtils.shortDayName(it) }
            }
        }
    }
}
