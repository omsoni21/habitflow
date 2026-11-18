package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.dao.AchievementDao
import com.example.data.dao.HabitCompletionDao
import com.example.data.dao.HabitDao
import com.example.data.database.HabitFlowDatabase
import com.example.data.entity.AchievementEntity
import com.example.data.entity.HabitCompletionEntity
import com.example.data.entity.HabitEntity
import com.example.data.entity.toDomain
import com.example.data.entity.toEntity
import com.example.domain.model.AchievementType
import com.example.domain.model.Habit
import com.example.domain.model.HabitCompletion
import com.example.domain.usecase.AchievementEvaluator
import com.example.domain.usecase.CelebrationEvent
import com.example.domain.usecase.GamificationEngine
import com.example.domain.usecase.HabitFlowBackupPayload
import com.example.domain.usecase.MilestoneCelebrationBus
import com.example.domain.usecase.StreakCalculator
import com.example.utils.DateUtils
import com.example.widget.WidgetUpdateNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val database: HabitFlowDatabase,
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao,
    private val achievementDao: AchievementDao,
    private val gamificationRepository: GamificationRepository,
    private val widgetUpdateNotifier: WidgetUpdateNotifier = WidgetUpdateNotifier.NO_OP,
    private val celebrationBus: MilestoneCelebrationBus = MilestoneCelebrationBus()
) {
    val celebrationEvents: SharedFlow<CelebrationEvent> = celebrationBus.events
    fun observeAllHabits(): Flow<List<Habit>> =
        habitDao.observeAllHabits().map { list -> list.map { it.toDomain() } }

    fun observeHabitById(habitId: Long): Flow<Habit?> =
        habitDao.observeHabitById(habitId).map { it?.toDomain() }

    fun observeAllCompletions(): Flow<List<HabitCompletion>> =
        completionDao.observeAllCompletions().map { list -> list.map { it.toDomain() } }

    fun observeCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> =
        completionDao.observeCompletionsForHabit(habitId).map { list -> list.map { it.toDomain() } }

    fun observeUnlockedAchievements(): Flow<Map<AchievementType, Long>> =
        achievementDao.observeUnlockedAchievements().map { list ->
            list.mapNotNull { entity ->
                runCatching { AchievementType.valueOf(entity.achievementType) }
                    .getOrNull()
                    ?.let { it to entity.unlockedAt }
            }.toMap()
        }

    suspend fun getHabitById(habitId: Long): Habit? =
        habitDao.getHabitById(habitId)?.toDomain()

    suspend fun getActiveHabitsWithReminders(): List<Habit> =
        habitDao.getActiveHabitsWithReminders().map { it.toDomain() }

    suspend fun getAllHabitEntitiesOnce(): List<HabitEntity> =
        habitDao.getAllHabitsOnce()

    suspend fun getAllCompletionEntitiesOnce(): List<HabitCompletionEntity> =
        completionDao.getAllCompletionsOnce()

    suspend fun getAllAchievementEntitiesOnce(): List<AchievementEntity> =
        achievementDao.getUnlockedAchievementsOnce()

    suspend fun saveHabit(habit: Habit): Long {
        val entity = habit.toEntity()
        val savedId = database.withTransaction {
            val id = if (habit.id == 0L) {
                habitDao.insertHabit(entity)
            } else {
                habitDao.updateHabit(entity)
                habit.id
            }
            gamificationRepository.syncUserStatsFromDatabase(DateUtils.today())
            id
        }
        widgetUpdateNotifier.notifyHabitsUpdated()
        return savedId
    }

    suspend fun deleteHabit(habitId: Long) {
        database.withTransaction {
            habitDao.deleteHabitById(habitId)
            gamificationRepository.syncUserStatsFromDatabase(DateUtils.today())
        }
        widgetUpdateNotifier.notifyHabitsUpdated()
    }

    suspend fun setHabitActive(habitId: Long, isActive: Boolean) {
        habitDao.setActive(habitId, isActive)
        widgetUpdateNotifier.notifyHabitsUpdated()
    }

    suspend fun setHabitArchived(habitId: Long, isArchived: Boolean) {
        habitDao.setArchived(habitId, isArchived)
        widgetUpdateNotifier.notifyHabitsUpdated()
    }

    suspend fun isCompletedOnDate(habitId: Long, date: LocalDate): Boolean {
        val habit = habitDao.getHabitById(habitId) ?: return false
        val completion = completionDao.getCompletion(habitId, DateUtils.formatIsoDate(date))
        return (completion?.value ?: 0) >= habit.target.coerceAtLeast(1)
    }

    /**
     * Toggles full completion for [habit] on [date].
     * If already completed (>= target), removes the completion (undo).
     * Otherwise sets progress to [habit.target] and evaluates achievements.
     * Returns newly unlocked achievements.
     */
    suspend fun toggleHabitCompletion(
        habit: Habit,
        date: LocalDate = DateUtils.today()
    ): List<AchievementType> {
        val dateStr = DateUtils.formatIsoDate(date)
        val referenceDate = maxOf(date, DateUtils.today())
        var pendingCelebration: CelebrationEvent? = null

        val unlocked = database.withTransaction {
            val existing = completionDao.getCompletion(habit.id, dateStr)
            val isCompleting = existing == null || existing.value < habit.target
            val previousLevel = if (isCompleting) {
                gamificationRepository.ensureInitialized(referenceDate)
                gamificationRepository.getUserStatsOnce().currentLevel
            } else {
                1
            }
            val previousStreak = if (isCompleting) {
                val beforeCompletions = completionDao.getAllCompletionsOnce()
                    .filter { it.habitId == habit.id }
                    .map { it.toDomain() }
                StreakCalculator.calculateHabitStreak(habit, beforeCompletions, referenceDate).currentStreak
            } else {
                0
            }

            val result = if (!isCompleting) {
                completionDao.deleteCompletion(habit.id, dateStr)
                emptyList()
            } else {
                completionDao.upsertCompletion(
                    HabitCompletionEntity(
                        id = existing?.id ?: 0L,
                        habitId = habit.id,
                        completionDate = dateStr,
                        completedAt = System.currentTimeMillis(),
                        value = habit.target
                    )
                )
                evaluateAndPersistAchievements(date)
            }
            val updatedLevelStatus = gamificationRepository.syncUserStatsFromDatabase(referenceDate)
            if (isCompleting) {
                val afterCompletions = completionDao.getAllCompletionsOnce()
                    .filter { it.habitId == habit.id }
                    .map { it.toDomain() }
                val newStreak = StreakCalculator.calculateHabitStreak(habit, afterCompletions, referenceDate).currentStreak
                pendingCelebration = GamificationEngine.detectCelebrationTrigger(
                    habitTitle = habit.title,
                    previousLevel = previousLevel,
                    newLevel = updatedLevelStatus.level,
                    previousStreak = previousStreak,
                    newStreak = newStreak
                )
            }
            result
        }
        pendingCelebration?.let { celebrationBus.emit(it) }
        widgetUpdateNotifier.notifyHabitsUpdated()
        return unlocked
    }

    /**
     * Increments progress by 1 towards [habit.target], wrapping back to 0 if already completed.
     * Returns newly unlocked achievements if target was just reached.
     */
    suspend fun incrementHabitProgress(
        habit: Habit,
        date: LocalDate = DateUtils.today()
    ): List<AchievementType> {
        val dateStr = DateUtils.formatIsoDate(date)
        val referenceDate = maxOf(date, DateUtils.today())
        var pendingCelebration: CelebrationEvent? = null

        val unlocked = database.withTransaction {
            val existing = completionDao.getCompletion(habit.id, dateStr)
            val currentValue = existing?.value ?: 0
            val nextValue = if (currentValue >= habit.target) 0 else (currentValue + 1).coerceAtMost(habit.target)
            val isJustCompleted = currentValue < habit.target && nextValue >= habit.target

            val previousLevel = if (isJustCompleted) {
                gamificationRepository.ensureInitialized(referenceDate)
                gamificationRepository.getUserStatsOnce().currentLevel
            } else {
                1
            }
            val previousStreak = if (isJustCompleted) {
                val beforeCompletions = completionDao.getAllCompletionsOnce()
                    .filter { it.habitId == habit.id }
                    .map { it.toDomain() }
                StreakCalculator.calculateHabitStreak(habit, beforeCompletions, referenceDate).currentStreak
            } else {
                0
            }

            val result = if (currentValue >= habit.target) {
                completionDao.deleteCompletion(habit.id, dateStr)
                emptyList()
            } else {
                completionDao.upsertCompletion(
                    HabitCompletionEntity(
                        id = existing?.id ?: 0L,
                        habitId = habit.id,
                        completionDate = dateStr,
                        completedAt = System.currentTimeMillis(),
                        value = nextValue
                    )
                )
                if (nextValue >= habit.target) {
                    evaluateAndPersistAchievements(date)
                } else {
                    emptyList()
                }
            }
            val updatedLevelStatus = gamificationRepository.syncUserStatsFromDatabase(referenceDate)
            if (isJustCompleted) {
                val afterCompletions = completionDao.getAllCompletionsOnce()
                    .filter { it.habitId == habit.id }
                    .map { it.toDomain() }
                val newStreak = StreakCalculator.calculateHabitStreak(habit, afterCompletions, referenceDate).currentStreak
                pendingCelebration = GamificationEngine.detectCelebrationTrigger(
                    habitTitle = habit.title,
                    previousLevel = previousLevel,
                    newLevel = updatedLevelStatus.level,
                    previousStreak = previousStreak,
                    newStreak = newStreak
                )
            }
            result
        }
        pendingCelebration?.let { celebrationBus.emit(it) }
        widgetUpdateNotifier.notifyHabitsUpdated()
        return unlocked
    }

    suspend fun restoreFromBackup(payload: HabitFlowBackupPayload) {
        database.withTransaction {
            completionDao.deleteAllCompletions()
            habitDao.deleteAllHabits()
            achievementDao.deleteAllAchievements()

            val idMapping = mutableMapOf<Long, Long>()
            payload.habits.forEach { dto ->
                val insertedId = habitDao.insertHabit(
                    HabitEntity(
                        id = 0L,
                        title = dto.title,
                        description = dto.description,
                        icon = dto.icon,
                        category = dto.category,
                        color = dto.color,
                        frequency = dto.frequency,
                        selectedDays = dto.selectedDays,
                        customIntervalDays = dto.customIntervalDays,
                        target = dto.target,
                        targetUnit = dto.targetUnit,
                        reminderTime = dto.reminderTime,
                        reminderEnabled = dto.reminderEnabled,
                        startDate = dto.startDate,
                        isActive = dto.isActive,
                        isArchived = dto.isArchived,
                        createdAt = dto.createdAt
                    )
                )
                idMapping[dto.id] = insertedId
            }

            payload.completions.forEach { dto ->
                val newHabitId = idMapping[dto.habitId] ?: return@forEach
                completionDao.upsertCompletion(
                    HabitCompletionEntity(
                        habitId = newHabitId,
                        completionDate = dto.completionDate,
                        completedAt = dto.completedAt,
                        value = dto.value
                    )
                )
            }

            payload.achievements.forEach { dto ->
                achievementDao.insertAchievement(
                    AchievementEntity(
                        achievementType = dto.achievementType,
                        unlockedAt = dto.unlockedAt
                    )
                )
            }

            gamificationRepository.syncUserStatsFromDatabase(DateUtils.today())
        }
        widgetUpdateNotifier.notifyHabitsUpdated()
    }

    private suspend fun evaluateAndPersistAchievements(referenceDate: LocalDate): List<AchievementType> {
        val habits = habitDao.getAllHabitsOnce().map { it.toDomain() }
        val completions = completionDao.getAllCompletionsOnce().map { it.toDomain() }
        val unlocked = achievementDao.getUnlockedAchievementsOnce()
            .mapNotNull { runCatching { AchievementType.valueOf(it.achievementType) }.getOrNull() }
            .toSet()

        val newlyUnlocked = AchievementEvaluator.evaluateNewUnlocks(
            habits = habits,
            completions = completions,
            alreadyUnlocked = unlocked,
            today = maxOf(referenceDate, DateUtils.today())
        )

        val now = System.currentTimeMillis()
        newlyUnlocked.forEach { type ->
            achievementDao.insertAchievement(
                AchievementEntity(
                    achievementType = type.name,
                    unlockedAt = now
                )
            )
        }
        return newlyUnlocked
    }
}
