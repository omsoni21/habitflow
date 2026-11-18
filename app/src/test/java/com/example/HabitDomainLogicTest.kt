package com.example

import com.example.domain.model.AchievementType
import com.example.domain.model.Habit
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitCompletion
import com.example.domain.model.HabitFrequency
import com.example.domain.model.HabitStreakInfo
import com.example.domain.model.StartOfWeek
import com.example.domain.usecase.AchievementEvaluator
import com.example.domain.usecase.BackupAndExportManager
import com.example.domain.usecase.GamificationEngine
import com.example.domain.usecase.GamificationReward
import com.example.domain.usecase.HabitScheduleHelper
import com.example.domain.usecase.StreakCalculator
import com.example.notifications.ReminderScheduler
import com.example.utils.HabitCatalog
import com.example.utils.ShareMilestoneHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class HabitDomainLogicTest {

    @Test
    fun `streak calculation counts consecutive completed days and keeps streak open today`() {
        val today = LocalDate.of(2026, 11, 18)
        val habit = Habit(
            id = 1L,
            title = "Drink Water",
            frequency = HabitFrequency.DAILY,
            target = 8,
            startDate = today.minusDays(5)
        )

        val completions = listOf(
            HabitCompletion(1, 1L, today.minusDays(4), 100L, 8),
            HabitCompletion(2, 1L, today.minusDays(3), 200L, 8),
            HabitCompletion(3, 1L, today.minusDays(2), 300L, 8),
            HabitCompletion(4, 1L, today.minusDays(1), 400L, 8)
            // Today not completed yet -> streak remains 4
        )

        val streak = StreakCalculator.calculateHabitStreak(habit, completions, today)
        assertEquals(4, streak.currentStreak)
        assertEquals(4, streak.longestStreak)
        assertEquals(4, streak.totalCompletedDays)
    }

    @Test
    fun `streak calculation resets current streak when yesterday was missed`() {
        val today = LocalDate.of(2026, 11, 18)
        val habit = Habit(
            id = 2L,
            title = "Exercise",
            frequency = HabitFrequency.DAILY,
            target = 1,
            startDate = today.minusDays(6)
        )

        val completions = listOf(
            HabitCompletion(1, 2L, today.minusDays(6), 100L, 1),
            HabitCompletion(2, 2L, today.minusDays(5), 200L, 1),
            HabitCompletion(3, 2L, today.minusDays(4), 300L, 1),
            // Missed day -3, -2, -1
            HabitCompletion(4, 2L, today, 400L, 1)
        )

        val streak = StreakCalculator.calculateHabitStreak(habit, completions, today)
        assertEquals(1, streak.currentStreak)
        assertEquals(3, streak.longestStreak)
        assertEquals(4, streak.totalCompletedDays)
    }

    @Test
    fun `weekday schedule skips non-scheduled days without breaking streak`() {
        val monday = LocalDate.of(2026, 11, 16) // Monday
        val wednesday = monday.plusDays(2)
        val friday = monday.plusDays(4)

        val habit = Habit(
            id = 3L,
            title = "Gym",
            frequency = HabitFrequency.WEEKDAYS,
            selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            target = 1,
            startDate = monday
        )

        assertFalse(HabitScheduleHelper.isScheduledForDate(habit, monday.plusDays(1))) // Tuesday
        assertTrue(HabitScheduleHelper.isScheduledForDate(habit, wednesday))

        val completions = listOf(
            HabitCompletion(1, 3L, monday, 100L, 1),
            HabitCompletion(2, 3L, wednesday, 200L, 1),
            HabitCompletion(3, 3L, friday, 300L, 1)
        )

        val streak = StreakCalculator.calculateHabitStreak(habit, completions, friday)
        assertEquals(3, streak.currentStreak)
        assertEquals(3, streak.longestStreak)
        assertEquals(1f, streak.completionRate, 0.001f)
    }

    @Test
    fun `weekly and monthly statistics compute accurate completion rates`() {
        val today = LocalDate.of(2026, 11, 18)
        val habit = Habit(
            id = 4L,
            title = "Read Books",
            category = HabitCategory.LEARNING,
            frequency = HabitFrequency.DAILY,
            target = 1,
            startDate = today.minusDays(13)
        )

        val completions = (0..6).map { offset ->
            HabitCompletion(offset.toLong(), 4L, today.minusDays(offset.toLong()), 100L, 1)
        }

        val daily7 = StreakCalculator.calculateLast7Days(listOf(habit), completions, today)
        assertEquals(7, daily7.size)
        assertTrue(daily7.all { it.percentage == 1f })

        val weekly = StreakCalculator.calculateWeeklyTrend(
            habits = listOf(habit),
            completions = completions,
            startOfWeek = StartOfWeek.MONDAY,
            weeksCount = 4,
            today = today
        )
        assertEquals(4, weekly.size)
        assertEquals(100f, weekly.last().second, 0.5f)

        val monthly = StreakCalculator.calculateMonthlyTrend(
            habits = listOf(habit),
            completions = completions,
            monthsCount = 6,
            today = today
        )
        assertEquals(6, monthly.size)
        assertTrue(monthly.last().second > 0f)
    }

    @Test
    fun `achievement evaluator unlocks expected milestones`() {
        val today = LocalDate.of(2026, 11, 18)
        val habit = Habit(
            id = 5L,
            title = "Meditation",
            frequency = HabitFrequency.DAILY,
            target = 1,
            startDate = today.minusDays(6)
        )
        val completions = (0..6).map { offset ->
            HabitCompletion(offset.toLong(), 5L, today.minusDays(offset.toLong()), 100L, 1)
        }

        val unlocked = AchievementEvaluator.evaluateNewUnlocks(
            habits = listOf(habit),
            completions = completions,
            alreadyUnlocked = emptySet(),
            today = today
        )

        assertTrue(AchievementType.FIRST_HABIT_COMPLETED in unlocked)
        assertTrue(AchievementType.STREAK_3_DAYS in unlocked)
        assertTrue(AchievementType.STREAK_7_DAYS in unlocked)
        assertTrue(AchievementType.PERFECT_DAY in unlocked)
    }

    @Test
    fun `reminder scheduler computes next trigger delay accurately`() {
        val now = LocalDateTime.of(2026, 11, 18, 9, 0) // Wednesday 9:00 AM
        val habitLaterToday = Habit(
            id = 10L,
            title = "Water",
            frequency = HabitFrequency.DAILY,
            reminderEnabled = true,
            reminderTime = LocalTime.of(10, 0),
            startDate = LocalDate.of(2026, 11, 1)
        )
        val delayLater = ReminderScheduler.calculateNextTriggerDelayMillis(habitLaterToday, now)
        assertNotNull(delayLater)
        assertEquals(TimeUnit.HOURS.toMillis(1), delayLater)

        val disabledHabit = habitLaterToday.copy(reminderEnabled = false)
        assertNull(ReminderScheduler.calculateNextTriggerDelayMillis(disabledHabit, now))
    }

    @Test
    fun `csv export escapes quotes and reflects completion status`() {
        val habit = Habit(id = 1L, title = "Read \"Deep Work\"", target = 20, targetUnit = "pages")
        val completion = HabitCompletion(1L, 1L, LocalDate.of(2026, 11, 18), 12345L, 20)

        val csv = BackupAndExportManager.buildCsvExport(listOf(habit), listOf(completion))
        assertTrue(csv.contains("\"Read \"\"Deep Work\"\"\""))
        assertTrue(csv.contains(",20,20,pages,true,12345"))
    }

    @Test
    fun `gamification engine computes XP breakdown levels and rewards accurately`() {
        val today = LocalDate.of(2026, 11, 18)
        val habit = Habit(
            id = 20L,
            title = "Drink Water",
            frequency = HabitFrequency.DAILY,
            target = 8,
            targetUnit = "glasses",
            startDate = today.minusDays(6)
        )
        // 7 consecutive days completed:
        // Completion XP = 7 * (10 + 5 multi-target bonus) = 105 XP
        // Streak XP = 25 (3d) + 50 (7d) = 75 XP
        // Perfect days = 7 * 25 = 175 XP
        // Achievements (3 badges) = 3 * 40 = 120 XP
        // Total = 475 XP -> Level 4 (Streak Seeker; Level 5 requires 500 XP)
        val completions = (0..6).map { offset ->
            HabitCompletion(offset.toLong(), 20L, today.minusDays(offset.toLong()), 100L, 8)
        }
        val badges = setOf(
            AchievementType.FIRST_HABIT_COMPLETED,
            AchievementType.STREAK_3_DAYS,
            AchievementType.STREAK_7_DAYS
        )

        val status = GamificationEngine.computeLevelStatus(listOf(habit), completions, badges, today)
        assertEquals(105, status.breakdown.completionXp)
        assertEquals(75, status.breakdown.streakMilestoneXp)
        assertEquals(175, status.breakdown.perfectDayXp)
        assertEquals(120, status.breakdown.achievementXp)
        assertEquals(475, status.totalXp)
        assertEquals(4, status.level)
        assertEquals("Streak Seeker", status.title)
        assertTrue(GamificationReward.AURORA_EMERALD_THEME in status.unlockedRewards)
        assertFalse(GamificationReward.CONSISTENCY_HEATMAP in status.unlockedRewards)
    }

    @Test
    fun `onboarding recommendation engine returns 3 to 5 relevant habit suggestions`() {
        val healthLearning = HabitCatalog.recommendSuggestionsForGoals(
            setOf("Health & Hydration", "Continuous Learning")
        )
        assertTrue(healthLearning.size in 3..5)
        assertTrue(healthLearning.any { it.title == "Drink Water" })
        assertTrue(healthLearning.any { it.title == "Read Books" })

        val emptyGoals = HabitCatalog.recommendSuggestionsForGoals(emptySet())
        assertTrue(emptyGoals.size in 3..5)
    }

    @Test
    fun `event XP reward calculator handles completion milestones and undo`() {
        val multiMilestone = GamificationEngine.calculateEventXpReward(
            habitTarget = 5,
            previousStreak = 2,
            newStreak = 7,
            isPerfectDayReached = false,
            newlyUnlockedAchievementsCount = 2
        )
        assertEquals(10, multiMilestone.baseCompletionXp)
        assertEquals(5, multiMilestone.multiTargetBonusXp)
        assertEquals(listOf(3, 7), multiMilestone.reachedMilestones)
        assertEquals(75, multiMilestone.streakMilestoneXp)
        assertEquals(80, multiMilestone.achievementBonusXp)
        assertEquals(170, multiMilestone.totalXpDelta)

        val undo = GamificationEngine.calculateEventXpReward(
            habitTarget = 5,
            previousStreak = 3,
            newStreak = 2,
            isUndo = true
        )
        assertEquals(-15, undo.totalXpDelta)
        assertTrue(undo.reachedMilestones.isEmpty())
    }

    @Test
    fun `share milestone helper formats habit streak and level achievement posts`() {
        val habit = Habit(id = 1L, title = "Morning Meditation")
        val streakInfo = HabitStreakInfo(
            currentStreak = 14,
            longestStreak = 21,
            totalCompletedDays = 30,
            completionRate = 0.92f
        )
        val levelStatus = GamificationEngine.buildLevelStatusFromXp(
            com.example.domain.usecase.XpBreakdown(100, 175, 50, 120)
        )

        val habitShareText = ShareMilestoneHelper.buildHabitStreakShareText(
            habit = habit,
            streakInfo = streakInfo,
            levelStatus = levelStatus
        )
        assertTrue(habitShareText.contains("14-day streak"))
        assertTrue(habitShareText.contains("Morning Meditation"))
        assertTrue(habitShareText.contains("Best Streak: 21 days"))
        assertTrue(habitShareText.contains("Level ${levelStatus.level}"))

        val levelShareText = ShareMilestoneHelper.buildLevelAndAchievementsShareText(
            userName = "Alex",
            levelStatus = levelStatus,
            bestStreak = 21,
            unlockedAchievementsCount = 4,
            totalAchievementsCount = 8
        )
        assertTrue(levelShareText.contains("Alex"))
        assertTrue(levelShareText.contains("Level ${levelStatus.level}"))
        assertTrue(levelShareText.contains("${levelStatus.totalXp} Total XP"))
        assertTrue(levelShareText.contains("4/8 Achievements Unlocked"))
    }

    @Test
    fun `detectCelebrationTrigger fires for level ups and significant streak milestones`() {
        val levelOnly = GamificationEngine.detectCelebrationTrigger(
            habitTitle = "Drink Water",
            previousLevel = 1,
            newLevel = 2,
            previousStreak = 1,
            newStreak = 2
        )
        assertNotNull(levelOnly)
        assertEquals(2, levelOnly?.newLevel)
        assertNull(levelOnly?.reachedStreakMilestone)

        val streakOnly = GamificationEngine.detectCelebrationTrigger(
            habitTitle = "Read Books",
            previousLevel = 2,
            newLevel = 2,
            previousStreak = 6,
            newStreak = 7
        )
        assertNotNull(streakOnly)
        assertNull(streakOnly?.newLevel)
        assertEquals(7, streakOnly?.reachedStreakMilestone)

        val combined = GamificationEngine.detectCelebrationTrigger(
            habitTitle = "Meditation",
            previousLevel = 2,
            newLevel = 3,
            previousStreak = 2,
            newStreak = 3
        )
        assertNotNull(combined)
        assertEquals(3, combined?.newLevel)
        assertEquals(3, combined?.reachedStreakMilestone)

        val none = GamificationEngine.detectCelebrationTrigger(
            habitTitle = "Walk",
            previousLevel = 2,
            newLevel = 2,
            previousStreak = 3,
            newStreak = 4
        )
        assertNull(none)
    }
}
