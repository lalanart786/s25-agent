package com.agent.s25.data.db.dao

import androidx.room.*
import com.agent.s25.data.model.MemoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class MemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMemoryRaw(memory: MemoryEntity): Long

    open suspend fun insertMemory(memory: MemoryEntity): Long {
        val encrypted = memory.toEncrypted()
        return insertMemoryRaw(encrypted)
    }

    @Query("SELECT * FROM memories WHERE category = 'CORE_PROFILE' ORDER BY id ASC")
    protected abstract fun getCoreProfileRaw(): Flow<List<MemoryEntity>>

    open fun getCoreProfile(): Flow<List<MemoryEntity>> {
        return getCoreProfileRaw().map { list ->
            list.map { it.toDecrypted() }
        }
    }

    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    protected abstract fun getAllMemoriesRaw(): Flow<List<MemoryEntity>>

    open fun getAllMemories(): Flow<List<MemoryEntity>> {
        return getAllMemoriesRaw().map { list ->
            list.map { it.toDecrypted() }
        }
    }

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY timestamp DESC")
    protected abstract fun getMemoriesByCategoryRaw(category: String): Flow<List<MemoryEntity>>

    open fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>> {
        return getMemoriesByCategoryRaw(category).map { list ->
            list.map { it.toDecrypted() }
        }
    }

    @Query("DELETE FROM memories WHERE id = :id")
    abstract suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM memories WHERE `key` LIKE '%' || :query || '%'")
    protected abstract suspend fun deleteByKeyLike(query: String): Int

    open suspend fun forgetByQuery(query: String): Int {
        var count = deleteByKeyLike(query)
        // Also check decrypted values in memory vault
        try {
            val all = getAllRawList()
            for (m in all) {
                if (m.value.contains(query, ignoreCase = true)) {
                    deleteMemory(m.id)
                    count++
                }
            }
        } catch (_: Exception) {}
        return count
    }

    @Query("SELECT * FROM memories")
    protected abstract suspend fun getAllRawListInternal(): List<MemoryEntity>

    open suspend fun getAllRawList(): List<MemoryEntity> {
        return getAllRawListInternal().map { it.toDecrypted() }
    }
}
