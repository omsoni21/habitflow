package com.example.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.Habit
import com.example.domain.model.StartOfWeek
import com.example.domain.usecase.HabitScheduleHelper
import com.example.domain.usecase.StreakCalculator
import com.example.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class CalendarDayStatus {
    NONE_SCHEDULED,
    COMPLETED_ALL,
    PARTIAL,
    MISSED,
    UPCOMING
}

data class CalendarDayCell(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val status: CalendarDayStatus,
    val completionPercentage: Float,
    val completedCount: Int,
    val scheduledCount: Int
)

data class SelectedDayHabitItem(
    val habit: Habit,
    val completedValue: Int,
    val isCompleted: Boolean
)

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val startOfWeek: StartOfWeek = StartOfWeek.MONDAY,
    val monthCells: List<CalendarDayCell> = emptyList(),
    val selectedDateCompletionPercentage: Float = 0f,
    val selectedDateHabits: List<SelectedDayHabitItem> = emptyList()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val currentMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(DateUtils.today())

    val uiState: StateFlow<CalendarUiState> = combine(
        habitRepository.observeAllHabits(),
        habitRepository.observeAllCompletions(),
        preferencesRepository.userPreferences,
        currentMonth,
        selectedDate
    ) { habits, completions, prefs, month, selected ->
        val today = DateUtils.today()
        val cells = buildMonthGrid(month, prefs.startOfWeek, habits, completions, today)

        val scheduledOnSelected = habits.filter {
            !it.isArchived && HabitScheduleHelper.isScheduledForDate(it, selected, ignoreActiveFlag = selected.isBefore(today))
        }
        val selectedDayMap = completions
            .filter { it.completionDate == selected }
            .associateBy { it.habitId }

        val selectedItems = scheduledOnSelected.map { habit ->
            val value = selectedDayMap[habit.id]?.value ?: 0
            SelectedDayHabitItem(
                habit = habit,
                completedValue = value,
                isCompleted = value >= habit.target
            )
        }

        val completedCount = selectedItems.count { it.isCompleted }
        val pct = if (selectedItems.isEmpty()) 0f else completedCount.toFloat() / selectedItems.size

        CalendarUiState(
            currentMonth = month,
            selectedDate = selected,
            startOfWeek = prefs.startOfWeek,
            monthCells = cells,
            selectedDateCompletionPercentage = pct,
            selectedDateHabits = selectedItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun previousMonth() {
        currentMonth.value = currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        currentMonth.value = currentMonth.value.plusMonths(1)
    }

    fun jumpToToday() {
        val today = DateUtils.today()
        currentMonth.value = YearMonth.from(today)
        selectedDate.value = today
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        if (YearMonth.from(date) != currentMonth.value) {
            currentMonth.value = YearMonth.from(date)
        }
    }

    fun toggleHabitOnSelectedDate(habit: Habit) {
        val date = selectedDate.value
        if (date.isAfter(DateUtils.today())) return
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habit, date)
        }
    }

    private fun buildMonthGrid(
        month: YearMonth,
        startOfWeek: StartOfWeek,
        habits: List<Habit>,
        completions: List<com.example.domain.model.HabitCompletion>,
        today: LocalDate
    ): List<CalendarDayCell> {
        val firstOfMonth = month.atDay(1)
        val firstDayValue = if (startOfWeek == StartOfWeek.SUNDAY) 7 else 1
        val offset = ((firstOfMonth.dayOfWeek.value - firstDayValue) + 7) % 7
        val gridStart = firstOfMonth.minusDays(offset.toLong())

        return (0 until 42).map { index ->
            val date = gridStart.plusDays(index.toLong())
            val inMonth = YearMonth.from(date) == month
            val point = StreakCalculator.calculateDailyCompletionRate(habits, completions, date)

            val status = when {
                point.scheduledCount == 0 -> CalendarDayStatus.NONE_SCHEDULED
                date.isAfter(today) -> CalendarDayStatus.UPCOMING
                point.completedCount >= point.scheduledCount -> CalendarDayStatus.COMPLETED_ALL
                point.completedCount > 0 -> CalendarDayStatus.PARTIAL
                date.isBefore(today) -> CalendarDayStatus.MISSED
                else -> CalendarDayStatus.NONE_SCHEDULED
            }

            CalendarDayCell(
                date = date,
                inCurrentMonth = inMonth,
                status = status,
                completionPercentage = point.percentage,
                completedCount = point.completedCount,
                scheduledCount = point.scheduledCount
            )
        }
    }
}
