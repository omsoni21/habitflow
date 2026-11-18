package com.example.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.GamificationRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.DailyCompletionPoint
import com.example.domain.model.HabitConsistencyStat
import com.example.domain.usecase.GamificationReward
import com.example.domain.usecase.LevelStatus
import com.example.domain.usecase.StreakCalculator
import com.example.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatsTimeRange(val label: String) {
    DAILY("7 Days"),
    WEEKLY("4 Weeks"),
    MONTHLY("6 Months")
}

data class StatisticsUiState(
    val selectedRange: StatsTimeRange = StatsTimeRange.DAILY,
    val dailyPoints: List<DailyCompletionPoint> = emptyList(),
    val weeklyPoints: List<Pair<String, Float>> = emptyList(),
    val monthlyPoints: List<Pair<String, Float>> = emptyList(),
    val heatmap90Days: List<DailyCompletionPoint> = emptyList(),
    val isHeatmapUnlocked: Boolean = false,
    val currentLevel: Int = 1,
    val habitStats: List<HabitConsistencyStat> = emptyList(),
    val mostConsistentHabit: HabitConsistencyStat? = null,
    val leastConsistentHabit: HabitConsistencyStat? = null,
    val totalCompletedHabits: Int = 0,
    val bestStreak: Int = 0,
    val productivityScore: Int = 0,
    val hasHabits: Boolean = false
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    habitRepository: HabitRepository,
    preferencesRepository: UserPreferencesRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val selectedRange = MutableStateFlow(StatsTimeRange.DAILY)

    init {
        viewModelScope.launch {
            gamificationRepository.ensureInitialized(DateUtils.today())
        }
    }

    val uiState: StateFlow<StatisticsUiState> = combine(
        habitRepository.observeAllHabits(),
        habitRepository.observeAllCompletions(),
        gamificationRepository.observeLevelStatus(),
        preferencesRepository.userPreferences,
        selectedRange
    ) { habits, completions, levelStatus: LevelStatus, prefs, range ->
        val today = DateUtils.today()
        val dailyPoints = StreakCalculator.calculateLast7Days(habits, completions, today)
        val weeklyPoints = StreakCalculator.calculateWeeklyTrend(habits, completions, prefs.startOfWeek, 4, today)
        val monthlyPoints = StreakCalculator.calculateMonthlyTrend(habits, completions, 6, today)
        val habitStats = StreakCalculator.calculateHabitConsistencyStats(habits, completions, today)
        val heatmap90 = (89 downTo 0).map { offset ->
            StreakCalculator.calculateDailyCompletionRate(habits, completions, today.minusDays(offset.toLong()))
        }

        val habitMap = habits.associateBy { it.id }
        val totalCompleted = completions.count { completion ->
            val target = habitMap[completion.habitId]?.target ?: 1
            completion.value >= target
        }

        val bestStreak = habits.maxOfOrNull {
            StreakCalculator.calculateHabitStreak(it, completions, today).longestStreak
        } ?: 0

        val score = StreakCalculator.calculateOverallProductivityScore(habits, completions, today)

        val mostConsistent = habitStats.firstOrNull()
        val leastConsistent = if (habitStats.size > 1) habitStats.lastOrNull() else null

        StatisticsUiState(
            selectedRange = range,
            dailyPoints = dailyPoints,
            weeklyPoints = weeklyPoints,
            monthlyPoints = monthlyPoints,
            heatmap90Days = heatmap90,
            isHeatmapUnlocked = GamificationReward.CONSISTENCY_HEATMAP in levelStatus.unlockedRewards,
            currentLevel = levelStatus.level,
            habitStats = habitStats,
            mostConsistentHabit = mostConsistent,
            leastConsistentHabit = leastConsistent,
            totalCompletedHabits = totalCompleted,
            bestStreak = bestStreak,
            productivityScore = score,
            hasHabits = habits.any { !it.isArchived }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState()
    )

    fun selectRange(range: StatsTimeRange) {
        selectedRange.value = range
    }
}
