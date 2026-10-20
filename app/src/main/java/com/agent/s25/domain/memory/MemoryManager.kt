package com.agent.s25.domain.memory

import com.agent.s25.data.db.MemoryDatabase
import com.agent.s25.data.model.MemoryEntity
import com.agent.s25.domain.engine.LlamaBridge
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

class MemoryManager(private val db: MemoryDatabase) {
    private val gson = Gson()

    suspend fun getCoreProfilePromptHeader(): String {
        val profileItems = db.memoryDao().getCoreProfile().first()
        if (profileItems.isEmpty()) return ""

        val sb = StringBuilder("### Persistent User Profile:\n")
        profileItems.forEach { item ->
            sb.append("- ${item.key}: ${item.value}\n")
        }
        return sb.toString()
    }

    suspend fun rememberFact(category: String, key: String, value: String, lang: String = "gu"): Long {
        val queryForEmbedding = "$key: $value"
        val embedding = LlamaBridge.nativeCalculateEmbedding(queryForEmbedding)
        val embeddingJson = gson.toJson(embedding)

        val entity = MemoryEntity(
            category = category,
            key = key,
            value = value,
            languageCode = lang,
            embeddingJson = embeddingJson
        )
        return db.memoryDao().insertMemory(entity)
    }

    suspend fun searchPastMemory(query: String, topK: Int = 3): List<MemoryEntity> {
        val queryEmbedding = LlamaBridge.nativeCalculateEmbedding(query)
        val allMemories = db.memoryDao().getAllRawList()

        return allMemories
            .map { entity ->
                val vec = try {
                    gson.fromJson(entity.embeddingJson, Array<Float>::class.java)
                } catch (e: Exception) {
                    null
                }
                val score = if (vec != null) cosineSimilarity(queryEmbedding, vec) else 0.0f
                Pair(entity, score)
            }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    suspend fun forget(query: String): Int {
        return db.memoryDao().forgetByQuery(query)
    }

    private fun cosineSimilarity(a: FloatArray, b: Array<Float>): Float {
        var dot = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        val len = minOf(a.size, b.size)
        for (i in 0 until len) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        if (normA == 0.0f || normB == 0.0f) return 0.0f
        return (dot / (Math.sqrt(normA.toDouble()) * Math.sqrt(normB.toDouble()))).toFloat()
    }
}
