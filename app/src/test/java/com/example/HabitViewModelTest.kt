package com.example

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.HabitFlowDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.GamificationRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.Habit
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitFrequency
import com.example.domain.usecase.AppAccentPalette
import com.example.domain.usecase.AppCrestIcon
import com.example.notifications.ReminderScheduler
import com.example.presentation.habits.AddEditHabitViewModel
import com.example.presentation.habits.HabitsViewModel
import com.example.presentation.home.HomeUiState
import com.example.presentation.home.HomeViewModel
import com.example.presentation.rewards.RewardsViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HabitViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: HabitFlowDatabase
    private lateinit var gamificationRepository: GamificationRepository
    private lateinit var repository: HabitRepository
    private lateinit var prefsRepository: UserPreferencesRepository
    private lateinit var reminderScheduler: ReminderScheduler

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
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
        prefsRepository = UserPreferencesRepository(context)
        reminderScheduler = ReminderScheduler(context, repository, prefsRepository)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `AddEditHabitViewModel validates blank title and saves valid habit`() = runBlocking {
        val vm = AddEditHabitViewModel(
            savedStateHandle = SavedStateHandle(),
            habitRepository = repository,
            preferencesRepository = prefsRepository,
            reminderScheduler = reminderScheduler
        )

        var savedFlag = false
        vm.updateTitle("   ")
        vm.saveHabit { savedFlag = true }
        assertNotNull(vm.uiState.value.titleError)
        assertEquals(false, savedFlag)

        val savedDeferred = CompletableDeferred<Unit>()
        vm.updateTitle("Daily Meditation")
        vm.updateFrequency(HabitFrequency.DAILY)
        vm.saveHabit { savedDeferred.complete(Unit) }
        withTimeout(5000) {
            savedDeferred.await()
        }

        val habits = repository.observeAllHabits().first { it.isNotEmpty() }
        assertEquals(1, habits.size)
        assertEquals("Daily Meditation", habits.first().title)
    }

    @Test
    fun `HomeViewModel reflects habit completion state immediately`() = runBlocking {
        val addVm = AddEditHabitViewModel(
            savedStateHandle = SavedStateHandle(),
            habitRepository = repository,
            preferencesRepository = prefsRepository,
            reminderScheduler = reminderScheduler
        )
        val savedDeferred = CompletableDeferred<Unit>()
        addVm.updateTitle("Evening Walk")
        addVm.saveHabit { savedDeferred.complete(Unit) }
        withTimeout(5000) {
            savedDeferred.await()
        }

        val homeVm = HomeViewModel(repository, prefsRepository, gamificationRepository)
        val collectorJob = launch { homeVm.uiState.collect {} }

        val stateBefore = withTimeout(5000) {
            homeVm.uiState.first { it is HomeUiState.Success && it.totalHabitsToday == 1 }
        } as HomeUiState.Success

        assertEquals(1, stateBefore.totalHabitsToday)
        assertEquals(0, stateBefore.completedHabitsToday)

        homeVm.toggleHabitCompletion(stateBefore.todayHabits.first().habit)

        val stateAfter = withTimeout(5000) {
            homeVm.uiState.first { it is HomeUiState.Success && it.completedHabitsToday == 1 }
        } as HomeUiState.Success

        assertEquals(1, stateAfter.completedHabitsToday)
        assertEquals(1f, stateAfter.completionPercentage, 0.001f)
        assertTrue(stateAfter.todayHabits.first().isCompletedToday)
        collectorJob.cancel()
    }

    @Test
    fun `RewardsViewModel gates locked rewards and equips unlocked themes and crests`() = runBlocking {
        val rewardsVm = RewardsViewModel(gamificationRepository, prefsRepository)
        val collectorJob = launch { rewardsVm.uiState.collect {} }

        // Initially Level 1: Aurora Emerald (Lv.3) & Ember Flame (Lv.2) are locked
        val initial = withTimeout(5000) { rewardsVm.uiState.first { it.totalCount > 0 } }
        assertEquals(1, initial.levelStatus.level)
        rewardsVm.equipPalette(AppAccentPalette.AURORA_EMERALD)
        assertEquals(AppAccentPalette.ELECTRIC_INDIGO, prefsRepository.getPreferencesOnce().accentPalette)

        // Earn 150+ XP (Level 3) by completing a habit 3 consecutive days
        val today = java.time.LocalDate.now()
        val habitId = repository.saveHabit(Habit(title = "Water", target = 1, startDate = today.minusDays(2)))
        val habit = repository.getHabitById(habitId)!!
        repository.toggleHabitCompletion(habit, today.minusDays(2))
        repository.toggleHabitCompletion(habit, today.minusDays(1))
        repository.toggleHabitCompletion(habit, today)

        val leveledUp = withTimeout(5000) {
            rewardsVm.uiState.first { it.levelStatus.level >= 3 }
        }
        assertTrue(leveledUp.levelStatus.level >= 3)

        rewardsVm.equipPalette(AppAccentPalette.AURORA_EMERALD)
        rewardsVm.equipCrest(AppCrestIcon.EMBER_FLAME)

        val equipped = withTimeout(5000) {
            rewardsVm.uiState.first {
                it.selectedPalette == AppAccentPalette.AURORA_EMERALD &&
                    it.selectedCrest == AppCrestIcon.EMBER_FLAME
            }
        }
        assertEquals(AppAccentPalette.AURORA_EMERALD, equipped.selectedPalette)
        assertEquals(AppCrestIcon.EMBER_FLAME, equipped.selectedCrest)
        collectorJob.cancel()
    }

    @Test
    fun `HabitsViewModel filters habits by title category and clearFilters`() = runBlocking {
        repository.saveHabit(Habit(title = "Morning Run", category = HabitCategory.FITNESS))
        repository.saveHabit(Habit(title = "Evening Meditation", category = HabitCategory.MINDFULNESS))
        repository.saveHabit(Habit(title = "Drink Water", category = HabitCategory.HEALTH))

        val habitsVm = HabitsViewModel(repository, reminderScheduler)
        val collectorJob = launch { habitsVm.uiState.collect {} }

        val initial = withTimeout(5000) { habitsVm.uiState.first { it.items.size == 3 } }
        assertEquals(3, initial.items.size)

        // Search by title
        habitsVm.updateSearchQuery("Run")
        val byTitle = withTimeout(5000) { habitsVm.uiState.first { it.searchQuery == "Run" && it.items.size == 1 } }
        assertEquals("Morning Run", byTitle.items.single().habit.title)

        // Search by category label (case-insensitive + trimmed)
        habitsVm.updateSearchQuery("  mindful ")
        val byCategory = withTimeout(5000) { habitsVm.uiState.first { it.items.size == 1 && it.items.first().habit.title == "Evening Meditation" } }
        assertEquals(HabitCategory.MINDFULNESS, byCategory.items.single().habit.category)

        // Unmatched query + clearFilters restores all 3
        habitsVm.updateSearchQuery("Nonexistent")
        val empty = withTimeout(5000) { habitsVm.uiState.first { it.items.isEmpty() } }
        assertTrue(empty.items.isEmpty())

        habitsVm.clearFilters()
        val restored = withTimeout(5000) { habitsVm.uiState.first { it.items.size == 3 } }
        assertEquals(3, restored.items.size)
        collectorJob.cancel()
    }
}
