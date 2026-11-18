package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.HabitFlowDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.GamificationRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.Habit
import com.example.domain.model.HabitCategory
import com.example.notifications.ReminderScheduler
import com.example.domain.usecase.GamificationEngine
import com.example.domain.usecase.XpBreakdown
import com.example.presentation.achievements.AchievementsScreen
import com.example.presentation.achievements.AchievementsViewModel
import com.example.presentation.components.LevelingInfo
import com.example.presentation.habits.AddEditHabitScreen
import com.example.presentation.habits.AddEditHabitViewModel
import com.example.presentation.habits.HabitDetailScreen
import com.example.presentation.habits.HabitDetailViewModel
import com.example.presentation.habits.HabitsScreen
import com.example.presentation.habits.HabitsViewModel
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.onboarding.OnboardingViewModel
import com.example.presentation.rewards.RewardsScreen
import com.example.presentation.rewards.RewardsViewModel
import com.example.ui.theme.HabitFlowTheme
import androidx.lifecycle.SavedStateHandle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HabitUiFlowRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: HabitFlowDatabase
    private lateinit var gamificationRepository: GamificationRepository
    private lateinit var repository: HabitRepository
    private lateinit var prefsRepository: UserPreferencesRepository
    private lateinit var reminderScheduler: ReminderScheduler

    @Before
    fun setup() {
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
    }

    @Test
    fun `onboarding goal step renders FlowRow chips without runtime crash`() {
        val vm = OnboardingViewModel(prefsRepository, repository, reminderScheduler)

        composeRule.setContent {
            HabitFlowTheme {
                OnboardingScreen(onFinished = {}, viewModel = vm)
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("onboarding_primary_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("What are your main goals?").assertIsDisplayed()
        composeRule.onNodeWithText("Health & Hydration").assertIsDisplayed()
        composeRule.onNodeWithText("Continuous Learning").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("onboarding_primary_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Set your daily rhythm").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_suggestion_Drink Water").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_suggestion_Read Books").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `add habit screen renders FlowRow icon and category pickers without crash`() {
        val vm = AddEditHabitViewModel(
            savedStateHandle = SavedStateHandle(),
            habitRepository = repository,
            preferencesRepository = prefsRepository,
            reminderScheduler = reminderScheduler
        )

        composeRule.setContent {
            HabitFlowTheme {
                AddEditHabitScreen(onBack = {}, viewModel = vm)
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create Habit").assertIsDisplayed()
        composeRule.onNodeWithText("Icon").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Category").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `leveling info renders current level progress bar and next level milestone`() {
        val status = GamificationEngine.buildLevelStatusFromXp(
            XpBreakdown(
                completionXp = 80,
                streakMilestoneXp = 25,
                perfectDayXp = 0,
                achievementXp = 0
            )
        )

        composeRule.setContent {
            HabitFlowTheme {
                LevelingInfo(levelStatus = status, showXpBreakdown = true)
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("leveling_info_current_level").assertIsDisplayed()
        composeRule.onNodeWithTag("leveling_info_progress_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("leveling_info_next_milestone").assertIsDisplayed()
        composeRule.onNodeWithText("Level 2 • Momentum Builder").assertIsDisplayed()
        composeRule.onNodeWithText("105 / 150 XP").assertIsDisplayed()
        composeRule.onNodeWithText("45 XP left").assertIsDisplayed()
    }

    @Test
    fun `dashboard displays LevelingInfo component`() {
        val homeVm = HomeViewModel(repository, prefsRepository, gamificationRepository)

        composeRule.setContent {
            HabitFlowTheme {
                HomeScreen(
                    onNavigateToAddHabit = {},
                    onNavigateToHabitDetail = {},
                    onNavigateToAchievements = {},
                    viewModel = homeVm
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeRule.onNodeWithTag("home_xp_card").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("leveling_info_current_level", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("leveling_info_progress_bar", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("leveling_info_next_milestone", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `rewards screen lists unlocked and locked cosmetic rewards and filters cleanly`() {
        val rewardsVm = RewardsViewModel(gamificationRepository, prefsRepository)

        composeRule.setContent {
            HabitFlowTheme {
                RewardsScreen(onBack = {}, viewModel = rewardsVm)
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeRule.onNodeWithTag("rewards_summary_title").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("rewards_list").performScrollToNode(hasTestTag("reward_palette_ELECTRIC_INDIGO"))
        composeRule.onNodeWithTag("reward_palette_ELECTRIC_INDIGO").assertIsDisplayed()
        composeRule.onNodeWithTag("rewards_list").performScrollToNode(hasTestTag("reward_palette_AURORA_EMERALD"))
        composeRule.onNodeWithTag("reward_palette_AURORA_EMERALD").assertIsDisplayed()
        composeRule.onNodeWithTag("rewards_list").performScrollToNode(hasTestTag("reward_crest_CLASSIC_BOLT"))
        composeRule.onNodeWithTag("reward_crest_CLASSIC_BOLT").assertIsDisplayed()
        composeRule.onNodeWithTag("rewards_list").performScrollToNode(hasTestTag("reward_crest_GOLDEN_CREST"))
        composeRule.onNodeWithTag("reward_crest_GOLDEN_CREST").assertIsDisplayed()

        // Filter to Locked only -> Electric Indigo (Lv.1) disappears, Aurora Emerald (Lv.3) remains
        composeRule.onNodeWithTag("rewards_list").performScrollToNode(hasTestTag("rewards_filter_LOCKED"))
        composeRule.onNodeWithTag("rewards_filter_LOCKED").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("rewards_list").performScrollToNode(hasTestTag("reward_palette_AURORA_EMERALD"))
        composeRule.onNodeWithTag("reward_palette_AURORA_EMERALD").assertIsDisplayed()
        composeRule.onNodeWithTag("reward_palette_ELECTRIC_INDIGO").assertDoesNotExist()
    }

    @Test
    fun `achievements screen launches Android ShareSheet with level and achievements`() {
        val achievementsVm = AchievementsViewModel(repository, gamificationRepository, prefsRepository)

        composeRule.setContent {
            HabitFlowTheme {
                AchievementsScreen(onBack = {}, viewModel = achievementsVm)
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeRule.onNodeWithTag("achievements_share_button").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("achievements_share_button").performClick()

        val startedIntent = Shadows.shadowOf(context as Application).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)
        val innerIntent = startedIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertNotNull(innerIntent)
        assertEquals(Intent.ACTION_SEND, innerIntent?.action)
        val sharedText = innerIntent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue(sharedText.contains("Level"))
        assertTrue(sharedText.contains("Achievements Unlocked"))
    }

    @Test
    fun `habit detail screen launches Android ShareSheet with current habit streak`() = kotlinx.coroutines.runBlocking {
        val today = java.time.LocalDate.now()
        val habitId = repository.saveHabit(Habit(title = "Read Books", target = 1, startDate = today))
        val habit = repository.getHabitById(habitId)!!
        repository.toggleHabitCompletion(habit, today)

        val detailVm = HabitDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("habitId" to habitId)),
            habitRepository = repository,
            gamificationRepository = gamificationRepository,
            reminderScheduler = reminderScheduler
        )

        composeRule.setContent {
            HabitFlowTheme {
                HabitDetailScreen(onBack = {}, onEdit = {}, viewModel = detailVm)
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeRule.onNodeWithTag("habit_detail_share_button").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("habit_detail_share_button").performClick()

        val startedIntent = Shadows.shadowOf(context as Application).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)
        val innerIntent = startedIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertNotNull(innerIntent)
        assertEquals(Intent.ACTION_SEND, innerIntent?.action)
        val sharedText = innerIntent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue(sharedText.contains("1-day streak"))
        assertTrue(sharedText.contains("Read Books"))
    }

    @Test
    fun `completing habit for streak milestone and level up displays confetti particle overlay`() = kotlinx.coroutines.runBlocking {
        val today = java.time.LocalDate.now()
        val habitId = repository.saveHabit(
            Habit(
                title = "Morning Run",
                target = 1,
                startDate = today.minusDays(2)
            )
        )
        val habit = repository.getHabitById(habitId)!!
        // Complete yesterday and 2 days ago so completing today hits 3-day streak milestone AND levels up
        repository.toggleHabitCompletion(habit, today.minusDays(2))
        repository.toggleHabitCompletion(habit, today.minusDays(1))
        prefsRepository.updateLastCelebratedLevel(10)

        val homeVm = HomeViewModel(repository, prefsRepository, gamificationRepository)
        composeRule.setContent {
            HabitFlowTheme {
                HomeScreen(
                    onNavigateToAddHabit = {},
                    onNavigateToHabitDetail = {},
                    onNavigateToAchievements = {},
                    viewModel = homeVm
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeRule.onNodeWithTag("home_habit_list").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("home_habit_list").performScrollToNode(hasTestTag("habit_card_$habitId"))
        composeRule.onNodeWithTag("habit_complete_$habitId").assertIsDisplayed()

        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("habit_complete_$habitId").performClick()

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.mainClock.advanceTimeByFrame()
            runCatching {
                composeRule.onNodeWithTag("confetti_particle_canvas").assertIsDisplayed()
                composeRule.onNodeWithTag("confetti_milestone_banner").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // Tapping the milestone banner dismisses the celebration overlay cleanly
        composeRule.onNodeWithTag("confetti_milestone_banner").performClick()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confetti_particle_canvas").assertDoesNotExist()
    }

    @Test
    fun `habits screen search filters by title and category and supports clearing filters`() = kotlinx.coroutines.runBlocking {
        repository.saveHabit(Habit(title = "Morning Run", category = HabitCategory.FITNESS))
        repository.saveHabit(Habit(title = "Read Kotlin", category = HabitCategory.LEARNING))

        val habitsVm = HabitsViewModel(repository, reminderScheduler)
        composeRule.setContent {
            HabitFlowTheme {
                HabitsScreen(
                    onNavigateToAddHabit = {},
                    onNavigateToEditHabit = {},
                    onNavigateToHabitDetail = {},
                    viewModel = habitsVm
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeRule.onNodeWithText("Morning Run").assertIsDisplayed()
                composeRule.onNodeWithText("Read Kotlin").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // Filter by category name in search field
        composeRule.onNodeWithTag("habits_search_input").performTextReplacement("Fitness")
        composeRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeRule.onNodeWithText("Morning Run").assertIsDisplayed()
                composeRule.onNodeWithText("Read Kotlin").assertDoesNotExist()
                composeRule.onNodeWithTag("habits_search_result_count").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // Clear search via trailing clear button
        composeRule.onNodeWithTag("habits_search_clear").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeRule.onNodeWithTag("habits_search_clear").assertDoesNotExist()
                composeRule.onNodeWithTag("habits_list").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("habits_list")
            .performScrollToNode(androidx.compose.ui.test.hasText("Read Kotlin"))
        composeRule.onNodeWithText("Read Kotlin").assertIsDisplayed()

        // Filter by title with no match -> shows Clear Filters empty state
        composeRule.onNodeWithTag("habits_search_input").performTextReplacement("Swimming")
        composeRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeRule.onNodeWithTag("habits_no_matches_state").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("No matching habits").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("empty_state_action_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeRule.onNodeWithText("Morning Run").assertIsDisplayed()
                composeRule.onNodeWithText("Read Kotlin").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }
}
