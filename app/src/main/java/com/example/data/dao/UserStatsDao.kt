package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.UserStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = :id LIMIT 1")
    fun observeUserStats(id: Int = UserStatsEntity.SINGLETON_ID): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = :id LIMIT 1")
    suspend fun getUserStatsOnce(id: Int = UserStatsEntity.SINGLETON_ID): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserStats(stats: UserStatsEntity)

    @Query("DELETE FROM user_stats")
    suspend fun deleteUserStats()
}
