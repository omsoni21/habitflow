package com.example.presentation.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.GamificationRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.AchievementProgress
import com.example.domain.usecase.AchievementEvaluator
import com.example.domain.usecase.GamificationEngine
import com.example.domain.usecase.LevelStatus
import com.example.domain.usecase.StreakCalculator
import com.example.domain.usecase.XpBreakdown
import com.example.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AchievementsUiState(
    val achievements: List<AchievementProgress> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val userName: String = "Alex",
    val levelStatus: LevelStatus = GamificationEngine.buildLevelStatusFromXp(XpBreakdown(0, 0, 0, 0)),
    val bestStreak: Int = 0
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    habitRepository: HabitRepository,
    gamificationRepository: GamificationRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> = combine(
        habitRepository.observeAllHabits(),
        habitRepository.observeAllCompletions(),
        habitRepository.observeUnlockedAchievements(),
        gamificationRepository.observeLevelStatus(),
        preferencesRepository.userPreferences
    ) { habits, completions, unlockedMap, levelStatus, prefs ->
        val today = DateUtils.today()
        val items = AchievementEvaluator.buildAchievementProgressList(
            habits = habits,
            completions = completions,
            unlockedTimestamps = unlockedMap,
            today = today
        )
        val bestStreak = habits.maxOfOrNull {
            StreakCalculator.calculateHabitStreak(it, completions, today).longestStreak
        } ?: 0

        AchievementsUiState(
            achievements = items,
            unlockedCount = items.count { it.isUnlocked },
            totalCount = items.size,
            userName = prefs.userName,
            levelStatus = levelStatus,
            bestStreak = bestStreak
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AchievementsUiState()
    )
}
