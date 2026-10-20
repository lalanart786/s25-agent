package com.agent.s25.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agent.s25.data.security.KeystoreManager

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
) {
    /**
     * Returns a copy of this entity with sensitive fields encrypted using Knox AES-256 GCM.
     */
    fun toEncrypted(): MemoryEntity {
        return this.copy(
            value = KeystoreManager.encrypt(this.value),
            embeddingJson = KeystoreManager.encrypt(this.embeddingJson)
        )
    }

    /**
     * Returns a copy of this entity with sensitive fields decrypted to plaintext.
     */
    fun toDecrypted(): MemoryEntity {
        return this.copy(
            value = KeystoreManager.decrypt(this.value),
            embeddingJson = KeystoreManager.decrypt(this.embeddingJson)
        )
    }
}
