package com.example.presentation.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.Habit
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitFrequency
import com.example.domain.model.HabitSuggestion
import com.example.notifications.ReminderScheduler
import com.example.ui.theme.HabitColors
import com.example.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class AddEditHabitUiState(
    val habitId: Long? = null,
    val title: String = "",
    val description: String = "",
    val icon: String = "water_drop",
    val category: HabitCategory = HabitCategory.HEALTH,
    val color: Long = HabitColors.first(),
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val selectedDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val customIntervalDays: Int = 2,
    val target: Int = 1,
    val targetUnit: String = "times",
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(8, 30),
    val startDate: LocalDate = DateUtils.today(),
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val titleError: String? = null,
    val scheduleError: String? = null,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false
)

@HiltViewModel
class AddEditHabitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val editingHabitId: Long? = savedStateHandle.get<Long>("habitId")?.takeIf { it > 0L }

    private val _uiState = MutableStateFlow(
        AddEditHabitUiState(
            habitId = editingHabitId,
            isEditMode = editingHabitId != null
        )
    )
    val uiState: StateFlow<AddEditHabitUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (editingHabitId != null) {
                habitRepository.getHabitById(editingHabitId)?.let { habit ->
                    _uiState.update {
                        it.copy(
                            habitId = habit.id,
                            title = habit.title,
                            description = habit.description,
                            icon = habit.icon,
                            category = habit.category,
                            color = habit.color,
                            frequency = habit.frequency,
                            selectedDays = habit.selectedDays,
                            customIntervalDays = habit.customIntervalDays,
                            target = habit.target,
                            targetUnit = habit.targetUnit,
                            reminderEnabled = habit.reminderEnabled,
                            reminderTime = habit.reminderTime ?: LocalTime.of(8, 30),
                            startDate = habit.startDate,
                            isActive = habit.isActive,
                            isArchived = habit.isArchived,
                            createdAt = habit.createdAt,
                            isEditMode = true
                        )
                    }
                }
            } else {
                val prefs = preferencesRepository.getPreferencesOnce()
                val preferred = DateUtils.parseTime24(prefs.preferredReminderTime)
                if (preferred != null) {
                    _uiState.update { it.copy(reminderTime = preferred) }
                }
            }
        }
    }

    fun applySuggestion(suggestion: HabitSuggestion) {
        _uiState.update {
            it.copy(
                title = suggestion.title,
                description = suggestion.description,
                icon = suggestion.icon,
                category = suggestion.category,
                color = suggestion.color,
                target = suggestion.target,
                targetUnit = suggestion.targetUnit,
                reminderTime = suggestion.defaultReminderTime,
                titleError = null
            )
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, titleError = null) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateIcon(icon: String) {
        _uiState.update { it.copy(icon = icon) }
    }

    fun updateCategory(category: HabitCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun updateColor(color: Long) {
        _uiState.update { it.copy(color = color) }
    }

    fun updateFrequency(frequency: HabitFrequency) {
        _uiState.update { it.copy(frequency = frequency, scheduleError = null) }
    }

    fun toggleDay(day: DayOfWeek) {
        _uiState.update { state ->
            val updated = state.selectedDays.toMutableSet()
            if (!updated.add(day)) {
                updated.remove(day)
            }
            state.copy(selectedDays = updated, scheduleError = null)
        }
    }

    fun updateCustomIntervalDays(days: Int) {
        _uiState.update { it.copy(customIntervalDays = days.coerceIn(2, 30)) }
    }

    fun updateTarget(target: Int) {
        _uiState.update { it.copy(target = target.coerceIn(1, 999)) }
    }

    fun updateTargetUnit(unit: String) {
        _uiState.update { it.copy(targetUnit = unit) }
    }

    fun updateReminderEnabled(enabled: Boolean) {
        _uiState.update { it.copy(reminderEnabled = enabled) }
    }

    fun updateReminderTime(time: LocalTime) {
        _uiState.update { it.copy(reminderTime = time) }
    }

    fun updateStartDate(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun saveHabit(onSaved: () -> Unit) {
        val state = _uiState.value
        val trimmedTitle = state.title.trim()

        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(titleError = "Please enter a habit name") }
            return
        }
        if (state.frequency == HabitFrequency.WEEKDAYS && state.selectedDays.isEmpty()) {
            _uiState.update { it.copy(scheduleError = "Select at least one weekday") }
            return
        }
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val habit = Habit(
                id = state.habitId ?: 0L,
                title = trimmedTitle,
                description = state.description.trim(),
                icon = state.icon,
                category = state.category,
                color = state.color,
                frequency = state.frequency,
                selectedDays = if (state.frequency == HabitFrequency.DAILY) {
                    DayOfWeek.entries.toSet()
                } else {
                    state.selectedDays
                },
                customIntervalDays = if (state.frequency == HabitFrequency.CUSTOM) {
                    state.customIntervalDays
                } else {
                    1
                },
                target = state.target,
                targetUnit = state.targetUnit.ifBlank { "times" },
                reminderTime = if (state.reminderEnabled) state.reminderTime else null,
                reminderEnabled = state.reminderEnabled,
                startDate = state.startDate,
                isActive = state.isActive,
                isArchived = state.isArchived,
                createdAt = state.createdAt
            )

            val savedId = habitRepository.saveHabit(habit)
            val savedHabit = habit.copy(id = savedId)

            val prefs = preferencesRepository.getPreferencesOnce()
            if (prefs.notificationsEnabled && savedHabit.reminderEnabled && savedHabit.isActive && !savedHabit.isArchived) {
                reminderScheduler.scheduleHabitReminder(savedHabit)
            } else {
                reminderScheduler.cancelHabitReminder(savedId)
            }

            _uiState.update { it.copy(isSaving = false) }
            onSaved()
        }
    }
}
