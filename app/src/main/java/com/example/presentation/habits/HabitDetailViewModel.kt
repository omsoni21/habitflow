package com.example.presentation.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.GamificationRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.Habit
import com.example.domain.model.HabitCompletion
import com.example.domain.model.HabitStreakInfo
import com.example.domain.usecase.LevelStatus
import com.example.domain.usecase.StreakCalculator
import com.example.notifications.ReminderScheduler
import com.example.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HabitDetailUiState(
    val habit: Habit? = null,
    val streakInfo: HabitStreakInfo = HabitStreakInfo(0, 0, 0, 0f),
    val recentCompletions: List<HabitCompletion> = emptyList(),
    val completedDatesSet: Set<LocalDate> = emptySet(),
    val isCompletedToday: Boolean = false,
    val todayProgressValue: Int = 0,
    val levelStatus: LevelStatus? = null
)

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    gamificationRepository: GamificationRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val habitId: Long = savedStateHandle.get<Long>("habitId") ?: 0L

    val uiState: StateFlow<HabitDetailUiState> = combine(
        habitRepository.observeHabitById(habitId),
        habitRepository.observeCompletionsForHabit(habitId),
        gamificationRepository.observeLevelStatus()
    ) { habit, completions, levelStatus ->
        if (habit == null) {
            HabitDetailUiState(levelStatus = levelStatus)
        } else {
            val today = DateUtils.today()
            val streak = StreakCalculator.calculateHabitStreak(habit, completions, today)
            val completedDates = completions
                .filter { it.value >= habit.target }
                .map { it.completionDate }
                .toSet()
            val todayCompletion = completions.firstOrNull { it.completionDate == today }
            val todayVal = todayCompletion?.value ?: 0

            HabitDetailUiState(
                habit = habit,
                streakInfo = streak,
                recentCompletions = completions.sortedByDescending { it.completionDate }.take(30),
                completedDatesSet = completedDates,
                isCompletedToday = todayVal >= habit.target,
                todayProgressValue = todayVal,
                levelStatus = levelStatus
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitDetailUiState()
    )

    fun toggleTodayCompletion() {
        val habit = uiState.value.habit ?: return
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habit, DateUtils.today())
        }
    }

    fun toggleDateCompletion(date: LocalDate) {
        val habit = uiState.value.habit ?: return
        if (date.isAfter(DateUtils.today())) return
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habit, date)
        }
    }

    fun togglePause() {
        val habit = uiState.value.habit ?: return
        viewModelScope.launch {
            val newActive = !habit.isActive
            habitRepository.setHabitActive(habit.id, newActive)
            if (newActive && habit.reminderEnabled) {
                reminderScheduler.scheduleHabitReminder(habit.copy(isActive = true))
            } else {
                reminderScheduler.cancelHabitReminder(habit.id)
            }
        }
    }

    fun toggleArchive() {
        val habit = uiState.value.habit ?: return
        viewModelScope.launch {
            val newArchived = !habit.isArchived
            habitRepository.setHabitArchived(habit.id, newArchived)
            if (newArchived) {
                reminderScheduler.cancelHabitReminder(habit.id)
            } else if (habit.isActive && habit.reminderEnabled) {
                reminderScheduler.scheduleHabitReminder(habit.copy(isArchived = false))
            }
        }
    }

    fun deleteHabit(onDeleted: () -> Unit) {
        val habit = uiState.value.habit ?: return
        viewModelScope.launch {
            reminderScheduler.cancelHabitReminder(habit.id)
            habitRepository.deleteHabit(habit.id)
            onDeleted()
        }
    }
}
