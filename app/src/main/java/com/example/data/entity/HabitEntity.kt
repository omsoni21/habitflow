package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
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
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val createdAt: Long
)
