package com.agent.s25.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // SPATIAL, SCHEDULE, FINANCE, SECURITY
    val triggerInfo: String,
    val isActive: Boolean = true,
    val lastTriggered: String = "Active • Ready",
    val priority: String = "HIGH",
    val createdAt: Long = System.currentTimeMillis()
)
