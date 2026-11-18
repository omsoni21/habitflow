package com.example.widget

import com.example.data.repository.GamificationRepository
import com.example.data.repository.HabitRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HabitWidgetEntryPoint {
    fun habitRepository(): HabitRepository
    fun gamificationRepository(): GamificationRepository
}
