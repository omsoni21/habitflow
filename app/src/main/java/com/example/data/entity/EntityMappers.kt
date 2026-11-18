package com.example.data.entity

import com.example.domain.model.Habit
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitCompletion
import com.example.domain.model.HabitFrequency
import com.example.utils.DateUtils

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    title = title,
    description = description,
    icon = icon,
    category = runCatching { HabitCategory.valueOf(category) }.getOrDefault(HabitCategory.PRODUCTIVITY),
    color = color,
    frequency = runCatching { HabitFrequency.valueOf(frequency) }.getOrDefault(HabitFrequency.DAILY),
    selectedDays = DateUtils.decodeDays(selectedDays),
    customIntervalDays = customIntervalDays.coerceAtLeast(1),
    target = target.coerceAtLeast(1),
    targetUnit = targetUnit.ifBlank { "times" },
    reminderTime = DateUtils.parseTime24(reminderTime),
    reminderEnabled = reminderEnabled,
    startDate = DateUtils.parseIsoDate(startDate),
    isActive = isActive,
    isArchived = isArchived,
    createdAt = createdAt
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    title = title.trim(),
    description = description.trim(),
    icon = icon,
    category = category.name,
    color = color,
    frequency = frequency.name,
    selectedDays = DateUtils.encodeDays(selectedDays),
    customIntervalDays = customIntervalDays.coerceAtLeast(1),
    target = target.coerceAtLeast(1),
    targetUnit = targetUnit.ifBlank { "times" },
    reminderTime = reminderTime?.let { DateUtils.formatTime24(it) },
    reminderEnabled = reminderEnabled && reminderTime != null,
    startDate = DateUtils.formatIsoDate(startDate),
    isActive = isActive,
    isArchived = isArchived,
    createdAt = createdAt
)

fun HabitCompletionEntity.toDomain(): HabitCompletion = HabitCompletion(
    id = id,
    habitId = habitId,
    completionDate = DateUtils.parseIsoDate(completionDate),
    completedAt = completedAt,
    value = value
)
