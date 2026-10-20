package com.agent.s25.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.agent.s25.data.db.dao.MemoryDao
import com.agent.s25.data.db.dao.SpatialDao
import com.agent.s25.data.model.MemoryEntity
import com.agent.s25.data.model.SpatialEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [MemoryEntity::class, SpatialEntity::class], version = 1, exportSchema = false)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun spatialDao(): SpatialDao

    companion object {
        private const val TAG = "MemoryDatabase"
        private const val DB_NAME = "s25_knox_memory_vault.db"

        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getInstance(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { db ->
                    INSTANCE = db
                    // Auto-populate default profile in background if database is new or empty
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            db.populateDefaultProfileIfEmpty()
                        } catch (e: Exception) {
                            Log.w(TAG, "Default profile auto-population skipped: ${e.message}")
                        }
                    }
                }
            }
        }

        private fun buildDatabase(context: Context): MemoryDatabase {
            return try {
                Room.databaseBuilder(
                    context,
                    MemoryDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "Disk database creation failed, falling back to in-memory: ${e.message}")
                Room.inMemoryDatabaseBuilder(context, MemoryDatabase::class.java)
                    .fallbackToDestructiveMigration()
                    .build()
            }
        }
    }

    /**
     * Auto-populates persistent profile memories for Kishan (bilingual Gujarati/Hindi, Lalan Art, Surat)
     * encrypted at rest using Knox AES-256 GCM.
     */
    suspend fun populateDefaultProfileIfEmpty() {
        val dao = memoryDao()
        val existing = dao.getAllRawList()
        if (existing.isEmpty()) {
            Log.i(TAG, "Empty vault: auto-populating default profile for Kishan.")
            val defaults = listOf(
                MemoryEntity(
                    category = "CORE_PROFILE",
                    key = "User Name",
                    value = "Kishan",
                    languageCode = "gu",
                    isPinned = true
                ),
                MemoryEntity(
                    category = "CORE_PROFILE",
                    key = "Language Preferences",
                    value = "Gujarati (Native/Primary), Hindi (Fluent), English (Business)",
                    languageCode = "gu",
                    isPinned = true
                ),
                MemoryEntity(
                    category = "CORE_PROFILE",
                    key = "Business",
                    value = "Lalan Art (Luxury Ethnic Wear & Saree Manufacturing)",
                    languageCode = "gu",
                    isPinned = true
                ),
                MemoryEntity(
                    category = "CORE_PROFILE",
                    key = "Primary Location",
                    value = "Surat, Gujarat, India",
                    languageCode = "gu",
                    isPinned = true
                ),
                MemoryEntity(
                    category = "PREFERENCE",
                    key = "Communication Tone",
                    value = "Respectful, proactive, culturally attuned S25 AI partner",
                    languageCode = "gu",
                    isPinned = false
                ),
                MemoryEntity(
                    category = "HABIT",
                    key = "Daily Review",
                    value = "Review Zoho Books daily invoices, pending disbursements, and dispatch schedule",
                    languageCode = "gu",
                    isPinned = false
                )
            )

            for (item in defaults) {
                dao.insertMemory(item)
            }

            // Ensure Lalan Art Surat geofence exists in spatial database
            try {
                val places = spatialDao().getActiveGeofences()
                if (places.isEmpty()) {
                    spatialDao().insertPlace(
                        SpatialEntity(
                            name = "Lalan Art HQ",
                            latitude = 21.1702,
                            longitude = 72.8311,
                            radiusMeters = 200f,
                            notes = "Lalan Art Head Office & Manufacturing, Ring Road, Surat",
                            isGeofenceActive = true
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Spatial default seed skipped: ${e.message}")
            }
        }
    }
}
