package com.agent.s25.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // "CORE_PROFILE", "PREFERENCE", "HABIT", "EPISODIC", "SPATIAL"
    val key: String,
    val value: String,
    val languageCode: String = "gu", // "gu", "hi", "en"
    val embeddingJson: String = "[]", // 384-dimensional vector serialized as JSON
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)
