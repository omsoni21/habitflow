package com.example.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.GamificationRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.AchievementType
import com.example.domain.model.Habit
import com.example.domain.model.HabitWithProgress
import com.example.domain.usecase.AppCrestIcon
import com.example.domain.usecase.CelebrationEvent
import com.example.domain.usecase.HabitScheduleHelper
import com.example.domain.usecase.LevelStatus
import com.example.domain.usecase.StreakCalculator
import com.example.utils.DateUtils
import com.example.utils.HabitCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val greeting: String,
        val userName: String,
        val formattedDate: String,
        val totalHabitsToday: Int,
        val completedHabitsToday: Int,
        val pendingHabitsToday: Int,
        val completionPercentage: Float,
        val longestStreak: Int,
        val quote: Pair<String, String>,
        val todayHabits: List<HabitWithProgress>,
        val levelStatus: LevelStatus,
        val crestIcon: AppCrestIcon = AppCrestIcon.CLASSIC_BOLT,
        val unlockedAchievementsCount: Int = 0,
        val pendingLevelUpTo: Int? = null,
        val newlyUnlockedAchievement: AchievementType? = null,
        val activeCelebration: CelebrationEvent? = null
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val quoteIndex = MutableStateFlow(
        DateUtils.today().dayOfYear % HabitCatalog.motivationalQuotes.size
    )
    private val newlyUnlockedBanner = MutableStateFlow<AchievementType?>(null)
    private val activeCelebration = MutableStateFlow<CelebrationEvent?>(null)

    init {
        viewModelScope.launch {
            gamificationRepository.ensureInitialized(DateUtils.today())
        }
        viewModelScope.launch {
            habitRepository.celebrationEvents.collect { event ->
                activeCelebration.value = event
            }
        }
    }

    private val baseDataFlow = combine(
        habitRepository.observeAllHabits(),
        habitRepository.observeAllCompletions(),
        gamificationRepository.observeLevelStatus(),
        habitRepository.observeUnlockedAchievements(),
        preferencesRepository.userPreferences
    ) { habits, completions, levelStatus, unlockedMap, prefs ->
        QuintData(habits, completions, levelStatus, unlockedMap.size, prefs)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseDataFlow,
        quoteIndex,
        newlyUnlockedBanner,
        activeCelebration
    ) { data, qIdx, banner, celebration ->
        val habits = data.habits
        val completions = data.completions
        val levelStatus = data.levelStatus
        val unlockedCount = data.unlockedAchievementsCount
        val prefs = data.prefs

        val today = DateUtils.today()
        val todayCompletions = completions
            .filter { it.completionDate == today }
            .associateBy { it.habitId }

        val scheduledToday = habits.filter {
            HabitScheduleHelper.isScheduledForDate(it, today)
        }.map { habit ->
            val todayVal = todayCompletions[habit.id]?.value ?: 0
            HabitWithProgress(
                habit = habit,
                todayValue = todayVal,
                isCompletedToday = todayVal >= habit.target,
                streakInfo = StreakCalculator.calculateHabitStreak(habit, completions, today)
            )
        }.sortedWith(compareBy<HabitWithProgress> { it.isCompletedToday }.thenByDescending { it.streakInfo.currentStreak })

        val completedToday = scheduledToday.count { it.isCompletedToday }
        val totalToday = scheduledToday.size
        val pendingToday = (totalToday - completedToday).coerceAtLeast(0)
        val percentage = if (totalToday == 0) 0f else completedToday.toFloat() / totalToday.toFloat()

        val longestStreak = habits.filter { !it.isArchived }.maxOfOrNull {
            StreakCalculator.calculateHabitStreak(it, completions, today).longestStreak
        } ?: 0

        val pendingLevelUp = if (levelStatus.level > prefs.lastCelebratedLevel) {
            levelStatus.level
        } else {
            null
        }

        val quote = HabitCatalog.motivationalQuotes[qIdx % HabitCatalog.motivationalQuotes.size]

        HomeUiState.Success(
            greeting = DateUtils.greetingForHour(),
            userName = prefs.userName,
            formattedDate = DateUtils.formatFullDate(today),
            totalHabitsToday = totalToday,
            completedHabitsToday = completedToday,
            pendingHabitsToday = pendingToday,
            completionPercentage = percentage,
            longestStreak = longestStreak,
            quote = quote,
            todayHabits = scheduledToday,
            levelStatus = levelStatus,
            crestIcon = prefs.crestIcon,
            unlockedAchievementsCount = unlockedCount,
            pendingLevelUpTo = pendingLevelUp,
            newlyUnlockedAchievement = banner,
            activeCelebration = celebration
        ) as HomeUiState
    }.catch { error ->
        emit(HomeUiState.Error(error.message ?: "Unable to load your dashboard"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

    private data class QuintData(
        val habits: List<Habit>,
        val completions: List<com.example.domain.model.HabitCompletion>,
        val levelStatus: LevelStatus,
        val unlockedAchievementsCount: Int,
        val prefs: com.example.data.local.UserPreferences
    )

    fun toggleHabitCompletion(habit: Habit) {
        viewModelScope.launch {
            val unlocked = habitRepository.toggleHabitCompletion(habit, DateUtils.today())
            if (unlocked.isNotEmpty()) {
                newlyUnlockedBanner.value = unlocked.first()
            }
        }
    }

    fun incrementHabitProgress(habit: Habit) {
        viewModelScope.launch {
            val unlocked = habitRepository.incrementHabitProgress(habit, DateUtils.today())
            if (unlocked.isNotEmpty()) {
                newlyUnlockedBanner.value = unlocked.first()
            }
        }
    }

    fun cycleQuote() {
        quoteIndex.update { (it + 1) % HabitCatalog.motivationalQuotes.size }
    }

    fun dismissAchievementBanner() {
        newlyUnlockedBanner.value = null
    }

    fun dismissCelebration() {
        activeCelebration.value = null
    }

    fun dismissLevelUpDialog(level: Int) {
        viewModelScope.launch {
            preferencesRepository.updateLastCelebratedLevel(level)
        }
    }
}
