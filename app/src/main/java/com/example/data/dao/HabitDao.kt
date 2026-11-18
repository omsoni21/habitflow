package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun observeAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isArchived = 0 AND isActive = 1 ORDER BY createdAt DESC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    suspend fun getAllHabitsOnce(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE isArchived = 0 AND isActive = 1 AND reminderEnabled = 1 AND reminderTime IS NOT NULL")
    suspend fun getActiveHabitsWithReminders(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :habitId")
    fun observeHabitById(habitId: Long): Flow<HabitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Long)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("UPDATE habits SET isActive = :isActive WHERE id = :habitId")
    suspend fun setActive(habitId: Long, isActive: Boolean)

    @Query("UPDATE habits SET isArchived = :isArchived WHERE id = :habitId")
    suspend fun setArchived(habitId: Long, isArchived: Boolean)
}
