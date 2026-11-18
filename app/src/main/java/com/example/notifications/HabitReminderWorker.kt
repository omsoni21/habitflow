package com.example.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.HabitRepository
import com.example.domain.usecase.HabitScheduleHelper
import com.example.utils.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class HabitReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationHelper: NotificationHelper,
    private val reminderScheduler: ReminderScheduler
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getLong(ReminderScheduler.KEY_HABIT_ID, -1L)
        if (habitId <= 0L) return Result.success()

        val prefs = userPreferencesRepository.getPreferencesOnce()
        val habit = habitRepository.getHabitById(habitId) ?: return Result.success()

        val today = DateUtils.today()
        if (prefs.notificationsEnabled &&
            habit.reminderEnabled &&
            habit.isActive &&
            !habit.isArchived &&
            HabitScheduleHelper.isScheduledForDate(habit, today)
        ) {
            val alreadyCompleted = habitRepository.isCompletedOnDate(habit.id, today)
            if (!alreadyCompleted) {
                notificationHelper.showHabitReminder(habit, today)
            }
        }

        // Schedule the next occurrence
        reminderScheduler.scheduleHabitReminder(habit)
        return Result.success()
    }
}
