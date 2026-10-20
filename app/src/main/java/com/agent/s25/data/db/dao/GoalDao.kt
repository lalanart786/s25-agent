package com.agent.s25.data.db.dao

import androidx.room.*
import com.agent.s25.data.model.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM agent_goals ORDER BY id ASC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM agent_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("UPDATE agent_goals SET isActive = :isActive WHERE id = :id")
    suspend fun toggleGoalActive(id: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM agent_goals")
    suspend fun count(): Int
}
