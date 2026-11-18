package com.example.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.HabitRepository
import com.example.domain.model.Habit
import com.example.domain.usecase.HabitScheduleHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitRepository: HabitRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        const val KEY_HABIT_ID = "habit_id"
        private const val WORK_PREFIX = "habit_reminder_"

        fun calculateNextTriggerDelayMillis(
            habit: Habit,
            now: LocalDateTime = LocalDateTime.now()
        ): Long? {
            val reminderTime = habit.reminderTime ?: return null
            if (!habit.reminderEnabled || !habit.isActive || habit.isArchived) return null

            var candidateDate = now.toLocalDate()
            if (!now.toLocalTime().isBefore(reminderTime)) {
                candidateDate = candidateDate.plusDays(1)
            }

            // Look ahead up to 31 days for the next scheduled occurrence
            for (offset in 0..31) {
                val checkDate = candidateDate.plusDays(offset.toLong())
                if (HabitScheduleHelper.isScheduledForDate(habit, checkDate)) {
                    val triggerDateTime = LocalDateTime.of(checkDate, reminderTime)
                    val millis = Duration.between(now, triggerDateTime).toMillis()
                    return millis.coerceAtLeast(TimeUnit.SECONDS.toMillis(5))
                }
            }
            return null
        }
    }

    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    fun scheduleHabitReminder(habit: Habit) {
        if (!habit.reminderEnabled || !habit.isActive || habit.isArchived || habit.reminderTime == null) {
            cancelHabitReminder(habit.id)
            return
        }

        val delayMillis = calculateNextTriggerDelayMillis(habit) ?: run {
            cancelHabitReminder(habit.id)
            return
        }

        val inputData = Data.Builder()
            .putLong(KEY_HABIT_ID, habit.id)
            .build()

        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(WORK_PREFIX + habit.id)
            .build()

        workManager.enqueueUniqueWork(
            WORK_PREFIX + habit.id,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelHabitReminder(habitId: Long) {
        workManager.cancelUniqueWork(WORK_PREFIX + habitId)
    }

    suspend fun rescheduleAllActiveReminders() {
        val prefs = userPreferencesRepository.getPreferencesOnce()
        val habits = habitRepository.getAllHabitEntitiesOnce()
        if (!prefs.notificationsEnabled) {
            habits.forEach { cancelHabitReminder(it.id) }
            return
        }
        val activeReminderHabits = habitRepository.getActiveHabitsWithReminders()
        activeReminderHabits.forEach { scheduleHabitReminder(it) }
    }
}
