package com.example.domain.usecase

import com.example.domain.model.DailyCompletionPoint
import com.example.domain.model.Habit
import com.example.domain.model.HabitCompletion
import com.example.domain.model.HabitConsistencyStat
import com.example.domain.model.HabitStreakInfo
import com.example.domain.model.StartOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

object StreakCalculator {

    fun calculateHabitStreak(
        habit: Habit,
        completions: List<HabitCompletion>,
        today: LocalDate = LocalDate.now()
    ): HabitStreakInfo {
        val completedDates = completions
            .asSequence()
            .filter { it.habitId == habit.id && it.value >= habit.target }
            .map { it.completionDate }
            .toSet()

        val totalCompletedDays = completedDates.size

        if (habit.startDate.isAfter(today)) {
            return HabitStreakInfo(
                currentStreak = 0,
                longestStreak = 0,
                totalCompletedDays = totalCompletedDays,
                completionRate = 0f
            )
        }

        // Gather all scheduled dates from startDate up to today
        val scheduledDates = mutableListOf<LocalDate>()
        var cursor = habit.startDate
        while (!cursor.isAfter(today)) {
            if (HabitScheduleHelper.isScheduledForDate(habit, cursor, ignoreActiveFlag = true)) {
                scheduledDates.add(cursor)
            }
            cursor = cursor.plusDays(1)
        }

        // Longest streak across scheduled dates (plus any extra completed dates if startDate shifted)
        var longestStreak = 0
        var runningStreak = 0
        for (date in scheduledDates) {
            if (date in completedDates) {
                runningStreak++
                if (runningStreak > longestStreak) longestStreak = runningStreak
            } else {
                runningStreak = 0
            }
        }

        // Current streak: walk backwards from most recent scheduled date
        var currentStreak = 0
        for (i in scheduledDates.indices.reversed()) {
            val date = scheduledDates[i]
            if (date in completedDates) {
                currentStreak++
            } else if (date == today) {
                // Today is still in progress; don't break streak until day passes
                continue
            } else {
                break
            }
        }

        val scheduledCompletedCount = scheduledDates.count { it in completedDates }
        val completionRate = if (scheduledDates.isEmpty()) {
            0f
        } else {
            (scheduledCompletedCount.toFloat() / scheduledDates.size.toFloat()).coerceIn(0f, 1f)
        }

        return HabitStreakInfo(
            currentStreak = currentStreak,
            longestStreak = maxOf(longestStreak, currentStreak),
            totalCompletedDays = totalCompletedDays,
            completionRate = completionRate
        )
    }

    fun calculateDailyCompletionRate(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        date: LocalDate
    ): DailyCompletionPoint {
        val scheduledHabits = habits.filter {
            HabitScheduleHelper.isScheduledForDate(it, date, ignoreActiveFlag = it.isArchived.not() && date.isBefore(LocalDate.now()))
        }
        val completionMap = completions
            .filter { it.completionDate == date }
            .associateBy { it.habitId }

        val completedCount = scheduledHabits.count { habit ->
            (completionMap[habit.id]?.value ?: 0) >= habit.target
        }

        val percentage = if (scheduledHabits.isEmpty()) {
            0f
        } else {
            (completedCount.toFloat() / scheduledHabits.size.toFloat()).coerceIn(0f, 1f)
        }

        return DailyCompletionPoint(
            date = date,
            label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            completedCount = completedCount,
            scheduledCount = scheduledHabits.size,
            percentage = percentage
        )
    }

    fun calculateLast7Days(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        today: LocalDate = LocalDate.now()
    ): List<DailyCompletionPoint> {
        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            calculateDailyCompletionRate(habits, completions, date)
        }
    }

    fun calculateWeeklyTrend(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        startOfWeek: StartOfWeek,
        weeksCount: Int = 4,
        today: LocalDate = LocalDate.now()
    ): List<Pair<String, Float>> {
        val firstDayOfWeek = if (startOfWeek == StartOfWeek.SUNDAY) DayOfWeek.SUNDAY else DayOfWeek.MONDAY
        var currentWeekStart = today
        while (currentWeekStart.dayOfWeek != firstDayOfWeek) {
            currentWeekStart = currentWeekStart.minusDays(1)
        }

        val formatter = DateTimeFormatter.ofPattern("MMM d")
        return ((weeksCount - 1) downTo 0).map { weekOffset ->
            val weekStart = currentWeekStart.minusWeeks(weekOffset.toLong())
            val weekEnd = minOf(weekStart.plusDays(6), today)
            var totalScheduled = 0
            var totalCompleted = 0
            var cursor = weekStart
            while (!cursor.isAfter(weekEnd)) {
                val point = calculateDailyCompletionRate(habits, completions, cursor)
                totalScheduled += point.scheduledCount
                totalCompleted += point.completedCount
                cursor = cursor.plusDays(1)
            }
            val rate = if (totalScheduled == 0) 0f else (totalCompleted.toFloat() / totalScheduled) * 100f
            weekStart.format(formatter) to rate.coerceIn(0f, 100f)
        }
    }

    fun calculateMonthlyTrend(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        monthsCount: Int = 6,
        today: LocalDate = LocalDate.now()
    ): List<Pair<String, Float>> {
        val formatter = DateTimeFormatter.ofPattern("MMM")
        val currentMonth = YearMonth.from(today)
        return ((monthsCount - 1) downTo 0).map { monthOffset ->
            val month = currentMonth.minusMonths(monthOffset.toLong())
            val start = month.atDay(1)
            val end = minOf(month.atEndOfMonth(), today)
            var totalScheduled = 0
            var totalCompleted = 0
            var cursor = start
            while (!cursor.isAfter(end)) {
                val point = calculateDailyCompletionRate(habits, completions, cursor)
                totalScheduled += point.scheduledCount
                totalCompleted += point.completedCount
                cursor = cursor.plusDays(1)
            }
            val rate = if (totalScheduled == 0) 0f else (totalCompleted.toFloat() / totalScheduled) * 100f
            month.atDay(1).format(formatter) to rate.coerceIn(0f, 100f)
        }
    }

    fun calculateHabitConsistencyStats(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        today: LocalDate = LocalDate.now()
    ): List<HabitConsistencyStat> {
        return habits.filter { !it.isArchived }.map { habit ->
            val streak = calculateHabitStreak(habit, completions, today)
            HabitConsistencyStat(
                habit = habit,
                completionRate = streak.completionRate,
                currentStreak = streak.currentStreak,
                longestStreak = streak.longestStreak,
                totalCompletedDays = streak.totalCompletedDays
            )
        }.sortedByDescending { it.completionRate }
    }

    fun calculateOverallProductivityScore(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        today: LocalDate = LocalDate.now()
    ): Int {
        val activeHabits = habits.filter { it.isActive && !it.isArchived }
        if (activeHabits.isEmpty() || completions.isEmpty()) return 0

        val last7Days = calculateLast7Days(activeHabits, completions, today)
        val scheduledDays = last7Days.filter { it.scheduledCount > 0 }
        val avgCompletion = if (scheduledDays.isEmpty()) 0f else {
            scheduledDays.map { it.percentage }.average().toFloat()
        }

        val bestCurrentStreak = activeHabits.maxOfOrNull {
            calculateHabitStreak(it, completions, today).currentStreak
        } ?: 0
        val streakMomentum = (bestCurrentStreak.coerceAtMost(14) / 14f)

        val score = ((avgCompletion * 0.75f) + (streakMomentum * 0.25f)) * 100f
        return score.roundToInt().coerceIn(0, 100)
    }
}
