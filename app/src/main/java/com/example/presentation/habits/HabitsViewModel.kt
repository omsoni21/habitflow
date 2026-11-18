package com.example.presentation.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.HabitRepository
import com.example.domain.model.Habit
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitStreakInfo
import com.example.domain.usecase.StreakCalculator
import com.example.notifications.ReminderScheduler
import com.example.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HabitFilterTab(val label: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    ARCHIVED("Archived")
}

data class HabitListItem(
    val habit: Habit,
    val streakInfo: HabitStreakInfo
)

data class HabitsUiState(
    val selectedTab: HabitFilterTab = HabitFilterTab.ACTIVE,
    val selectedCategory: HabitCategory? = null,
    val searchQuery: String = "",
    val items: List<HabitListItem> = emptyList(),
    val totalActiveCount: Int = 0,
    val totalPausedCount: Int = 0,
    val totalArchivedCount: Int = 0
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val selectedTab = MutableStateFlow(HabitFilterTab.ACTIVE)
    private val selectedCategory = MutableStateFlow<HabitCategory?>(null)
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HabitsUiState> = combine(
        habitRepository.observeAllHabits(),
        habitRepository.observeAllCompletions(),
        selectedTab,
        selectedCategory,
        searchQuery
    ) { habits, completions, tab, category, query ->
        val today = DateUtils.today()
        val activeHabits = habits.filter { !it.isArchived && it.isActive }
        val pausedHabits = habits.filter { !it.isArchived && !it.isActive }
        val archivedHabits = habits.filter { it.isArchived }

        val tabFiltered = when (tab) {
            HabitFilterTab.ACTIVE -> activeHabits
            HabitFilterTab.PAUSED -> pausedHabits
            HabitFilterTab.ARCHIVED -> archivedHabits
        }

        val trimmedQuery = query.trim()
        val filtered = tabFiltered.filter { habit ->
            val matchesCategory = category == null || habit.category == category
            val matchesQuery = trimmedQuery.isBlank() ||
                habit.title.contains(trimmedQuery, ignoreCase = true) ||
                habit.category.label.contains(trimmedQuery, ignoreCase = true) ||
                habit.description.contains(trimmedQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }.map { habit ->
            HabitListItem(
                habit = habit,
                streakInfo = StreakCalculator.calculateHabitStreak(habit, completions, today)
            )
        }

        HabitsUiState(
            selectedTab = tab,
            selectedCategory = category,
            searchQuery = query,
            items = filtered,
            totalActiveCount = activeHabits.size,
            totalPausedCount = pausedHabits.size,
            totalArchivedCount = archivedHabits.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsUiState()
    )

    fun selectTab(tab: HabitFilterTab) {
        selectedTab.value = tab
    }

    fun selectCategory(category: HabitCategory?) {
        selectedCategory.value = if (category == null || selectedCategory.value == category) null else category
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun clearFilters() {
        searchQuery.value = ""
        selectedCategory.value = null
    }

    fun togglePause(habit: Habit) {
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

    fun toggleArchive(habit: Habit) {
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

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            reminderScheduler.cancelHabitReminder(habitId)
            habitRepository.deleteHabit(habitId)
        }
    }
}
