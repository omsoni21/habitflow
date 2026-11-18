package com.example.domain.usecase

import com.example.data.entity.AchievementEntity
import com.example.data.entity.HabitCompletionEntity
import com.example.data.entity.HabitEntity
import com.example.domain.model.Habit
import com.example.domain.model.HabitCompletion
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupHabitDto(
    val id: Long,
    val title: String,
    val description: String,
    val icon: String,
    val category: String,
    val color: Long,
    val frequency: String,
    val selectedDays: String,
    val customIntervalDays: Int = 1,
    val target: Int,
    val targetUnit: String = "times",
    val reminderTime: String?,
    val reminderEnabled: Boolean = false,
    val startDate: String,
    val isActive: Boolean,
    val isArchived: Boolean = false,
    val createdAt: Long
)

@Serializable
data class BackupCompletionDto(
    val habitId: Long,
    val completionDate: String,
    val completedAt: Long,
    val value: Int
)

@Serializable
data class BackupAchievementDto(
    val achievementType: String,
    val unlockedAt: Long
)

@Serializable
data class HabitFlowBackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val userName: String = "HabitFlow User",
    val habits: List<BackupHabitDto>,
    val completions: List<BackupCompletionDto>,
    val achievements: List<BackupAchievementDto>
)

object BackupAndExportManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun encodeBackupJson(
        userName: String,
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        achievements: List<AchievementEntity>
    ): String {
        val payload = HabitFlowBackupPayload(
            userName = userName,
            habits = habits.map {
                BackupHabitDto(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    icon = it.icon,
                    category = it.category,
                    color = it.color,
                    frequency = it.frequency,
                    selectedDays = it.selectedDays,
                    customIntervalDays = it.customIntervalDays,
                    target = it.target,
                    targetUnit = it.targetUnit,
                    reminderTime = it.reminderTime,
                    reminderEnabled = it.reminderEnabled,
                    startDate = it.startDate,
                    isActive = it.isActive,
                    isArchived = it.isArchived,
                    createdAt = it.createdAt
                )
            },
            completions = completions.map {
                BackupCompletionDto(
                    habitId = it.habitId,
                    completionDate = it.completionDate,
                    completedAt = it.completedAt,
                    value = it.value
                )
            },
            achievements = achievements.map {
                BackupAchievementDto(
                    achievementType = it.achievementType,
                    unlockedAt = it.unlockedAt
                )
            }
        )
        return json.encodeToString(payload)
    }

    fun decodeBackupJson(rawJson: String): Result<HabitFlowBackupPayload> = runCatching {
        json.decodeFromString<HabitFlowBackupPayload>(rawJson)
    }

    fun buildCsvExport(
        habits: List<Habit>,
        completions: List<HabitCompletion>
    ): String {
        val habitMap = habits.associateBy { it.id }
        val sb = StringBuilder()
        sb.appendLine("Habit,Category,Date,Value,Target,Unit,Completed,CompletedAtMillis")
        completions.sortedWith(compareByDescending<HabitCompletion> { it.completionDate }.thenBy { it.habitId })
            .forEach { completion ->
                val habit = habitMap[completion.habitId] ?: return@forEach
                val escapedTitle = "\"${habit.title.replace("\"", "\"\"")}\""
                val isCompleted = completion.value >= habit.target
                sb.appendLine(
                    "$escapedTitle,${habit.category.label},${completion.completionDate},${completion.value},${habit.target},${habit.targetUnit},$isCompleted,${completion.completedAt}"
                )
            }
        return sb.toString()
    }
}
