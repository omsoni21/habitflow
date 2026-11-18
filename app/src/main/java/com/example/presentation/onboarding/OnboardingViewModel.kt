package com.example.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.Habit
import com.example.domain.model.HabitSuggestion
import com.example.notifications.ReminderScheduler
import com.example.utils.DateUtils
import com.example.utils.HabitCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

private val initialGoals = setOf("Health & Hydration", "Deep Focus & Productivity")
private val initialRecommendations = HabitCatalog.recommendSuggestionsForGoals(initialGoals)

data class OnboardingUiState(
    val step: Int = 0, // 0: Welcome, 1: Goals, 2: Personalized Plan & Reminder
    val userName: String = "",
    val selectedGoals: Set<String> = initialGoals,
    val recommendedSuggestions: List<HabitSuggestion> = initialRecommendations,
    val selectedSuggestionTitles: Set<String> = initialRecommendations.map { it.title }.toSet(),
    val preferredReminderTime: LocalTime = LocalTime.of(8, 30),
    val enableReminders: Boolean = true,
    val isSaving: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextStep() {
        _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(2)) }
    }

    fun previousStep() {
        _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    }

    fun updateUserName(name: String) {
        _uiState.update { it.copy(userName = name) }
    }

    fun toggleGoal(goal: String) {
        _uiState.update { state ->
            val updated = state.selectedGoals.toMutableSet()
            if (!updated.add(goal)) updated.remove(goal)
            val recommendations = HabitCatalog.recommendSuggestionsForGoals(updated)
            state.copy(
                selectedGoals = updated,
                recommendedSuggestions = recommendations,
                selectedSuggestionTitles = recommendations.map { it.title }.toSet()
            )
        }
    }

    fun toggleSuggestionSelection(title: String) {
        _uiState.update { state ->
            val updated = state.selectedSuggestionTitles.toMutableSet()
            if (!updated.add(title)) updated.remove(title)
            state.copy(selectedSuggestionTitles = updated)
        }
    }

    fun updateReminderTime(time: LocalTime) {
        _uiState.update { it.copy(preferredReminderTime = time) }
    }

    fun setEnableReminders(enabled: Boolean) {
        _uiState.update { it.copy(enableReminders = enabled) }
    }

    fun completeOnboarding(onCompleted: () -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            runCatching {
                preferencesRepository.saveOnboardingPreferences(
                    userName = state.userName.ifBlank { "Alex" },
                    selectedGoals = state.selectedGoals,
                    preferredReminderTime = DateUtils.formatTime24(state.preferredReminderTime),
                    notificationsEnabled = state.enableReminders
                )

                if (habitRepository.getAllHabitEntitiesOnce().isEmpty()) {
                    val chosen = state.recommendedSuggestions.filter {
                        it.title in state.selectedSuggestionTitles
                    }
                    chosen.forEach { suggestion ->
                        val habit = Habit(
                            title = suggestion.title,
                            description = suggestion.description,
                            icon = suggestion.icon,
                            category = suggestion.category,
                            color = suggestion.color,
                            target = suggestion.target,
                            targetUnit = suggestion.targetUnit,
                            reminderTime = state.preferredReminderTime,
                            reminderEnabled = state.enableReminders,
                            startDate = DateUtils.today()
                        )
                        val newId = habitRepository.saveHabit(habit)
                        if (state.enableReminders) {
                            reminderScheduler.scheduleHabitReminder(habit.copy(id = newId))
                        }
                    }
                }
            }
            _uiState.update { it.copy(isSaving = false) }
            onCompleted()
        }
    }
}
