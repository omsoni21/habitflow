package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.HabitFlowDatabase
import com.example.data.repository.GamificationRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.Habit
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitFrequency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HabitRepositoryRobolectricTest {

    private lateinit var database: HabitFlowDatabase
    private lateinit var gamificationRepository: GamificationRepository
    private lateinit var repository: HabitRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HabitFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        gamificationRepository = GamificationRepository(
            userStatsDao = database.userStatsDao(),
            habitDao = database.habitDao(),
            completionDao = database.habitCompletionDao(),
            achievementDao = database.achievementDao()
        )
        repository = HabitRepository(
            database = database,
            habitDao = database.habitDao(),
            completionDao = database.habitCompletionDao(),
            achievementDao = database.achievementDao(),
            gamificationRepository = gamificationRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `create edit pause archive and delete habit persists in Room`() = runTest {
        val habit = Habit(
            title = "Morning Run",
            description = "Run 3km",
            icon = "directions_run",
            category = HabitCategory.FITNESS,
            frequency = HabitFrequency.DAILY,
            target = 1,
            startDate = LocalDate.of(2026, 11, 1)
        )

        val id = repository.saveHabit(habit)
        assertTrue(id > 0)

        val loaded = repository.getHabitById(id)
        assertNotNull(loaded)
        assertEquals("Morning Run", loaded?.title)

        repository.saveHabit(loaded!!.copy(title = "Evening Run", target = 2))
        assertEquals("Evening Run", repository.getHabitById(id)?.title)
        assertEquals(2, repository.getHabitById(id)?.target)

        repository.setHabitActive(id, false)
        assertFalse(repository.getHabitById(id)!!.isActive)

        repository.setHabitArchived(id, true)
        assertTrue(repository.getHabitById(id)!!.isArchived)

        repository.deleteHabit(id)
        assertNull(repository.getHabitById(id))
    }

    @Test
    fun `toggle and increment completions prevent duplicates and support undo`() = runTest {
        val today = LocalDate.of(2026, 11, 18)
        val id = repository.saveHabit(
            Habit(
                title = "Drink Water",
                target = 2,
                targetUnit = "glasses",
                startDate = today
            )
        )
        val habit = repository.getHabitById(id)!!

        // Step 1: 1 of 2
        repository.incrementHabitProgress(habit, today)
        assertFalse(repository.isCompletedOnDate(id, today))
        assertEquals(1, repository.observeCompletionsForHabit(id).first().size)

        // Step 2: 2 of 2 -> Completed & unlocks FIRST_HABIT_COMPLETED
        val unlocked = repository.incrementHabitProgress(habit, today)
        assertTrue(repository.isCompletedOnDate(id, today))
        assertEquals(1, repository.observeCompletionsForHabit(id).first().size)
        assertTrue(unlocked.isNotEmpty())

        // Verify UserStatsEntity persisted XP, level, and next-level progress
        val stats = gamificationRepository.getUserStatsOnce()
        assertTrue(stats.totalXp >= 15)
        assertTrue(stats.currentLevel >= 1)
        assertTrue(stats.nextLevelXp > stats.currentLevelFloorXp)

        // Step 3: Toggle again -> Undo completion cleanly & updates persisted UserStatsEntity
        repository.toggleHabitCompletion(habit, today)
        assertFalse(repository.isCompletedOnDate(id, today))
        assertEquals(0, repository.observeCompletionsForHabit(id).first().size)
        assertEquals(0, gamificationRepository.getUserStatsOnce().completionXp)
    }

    @Test
    fun `gamification repository persists user stats and calculates event XP rewards`() = runTest {
        val today = LocalDate.now()
        val id = repository.saveHabit(
            Habit(
                title = "Deep Focus",
                target = 1,
                startDate = today.minusDays(2)
            )
        )
        val habit = repository.getHabitById(id)!!

        // Complete 3 consecutive days up to today -> triggers 3-day streak milestone & Level 2+
        repository.toggleHabitCompletion(habit, today.minusDays(2))
        repository.toggleHabitCompletion(habit, today.minusDays(1))
        repository.toggleHabitCompletion(habit, today)

        val persisted = gamificationRepository.getUserStatsOnce()
        // 3 completions (30) + 3-day streak milestone (25) + 3 perfect days (75) + 3 achievements (120) = 250 XP -> Level 3
        assertEquals(30, persisted.completionXp)
        assertEquals(25, persisted.streakMilestoneXp)
        assertEquals(75, persisted.perfectDayXp)
        assertEquals(250, persisted.totalXp)
        assertEquals(3, persisted.currentLevel)
        assertEquals(150, persisted.currentLevelFloorXp)
        assertEquals(300, persisted.nextLevelXp)
        assertEquals((250f - 150f) / 150f, persisted.progressToNextLevel, 0.001f)

        // Verify direct event XP calculation for a 7-day streak milestone transition
        val eventReward = gamificationRepository.calculateXpReward(
            habitTarget = 8,
            previousStreak = 6,
            newStreak = 7,
            isPerfectDayReached = true,
            newlyUnlockedAchievementsCount = 1
        )
        assertEquals(10, eventReward.baseCompletionXp)
        assertEquals(5, eventReward.multiTargetBonusXp)
        assertEquals(50, eventReward.streakMilestoneXp)
        assertEquals(25, eventReward.perfectDayBonusXp)
        assertEquals(40, eventReward.achievementBonusXp)
        assertEquals(listOf(7), eventReward.reachedMilestones)
        assertEquals(130, eventReward.totalXpDelta)

        // Verify applyCompletionEventReward updates and persists UserStatsEntity
        gamificationRepository.resetStats()
        val applied = gamificationRepository.applyCompletionEventReward(
            habitTarget = 8,
            previousStreak = 6,
            newStreak = 7,
            isPerfectDayReached = true,
            newlyUnlockedAchievementsCount = 1
        )
        assertEquals(130, applied.totalXp)
        assertEquals(2, applied.currentLevel)
        assertEquals(50, applied.currentLevelFloorXp)
        assertEquals(150, applied.nextLevelXp)
        assertEquals(0.8f, applied.progressToNextLevel, 0.001f)
    }
}
