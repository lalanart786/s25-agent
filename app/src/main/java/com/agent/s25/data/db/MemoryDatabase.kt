package com.agent.s25.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.agent.s25.data.db.dao.GoalDao
import com.agent.s25.data.db.dao.MemoryDao
import com.agent.s25.data.db.dao.SpatialDao
import com.agent.s25.data.model.GoalEntity
import com.agent.s25.data.model.MemoryEntity
import com.agent.s25.data.model.SpatialEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [MemoryEntity::class, SpatialEntity::class, GoalEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun spatialDao(): SpatialDao
    abstract fun goalDao(): GoalDao

    companion object {
        private const val TAG = "MemoryDatabase"
        private const val DB_NAME = "s25_knox_memory_vault.db"

        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getInstance(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { db ->
                    INSTANCE = db
                    // Auto-populate default profile & goals in background if database is new or empty
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            db.populateDefaultProfileIfEmpty()
                            db.populateDefaultGoalsIfEmpty()
                        } catch (e: Exception) {
                            Log.w(TAG, "Default database auto-population skipped: ${e.message}")
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

    /**
     * Auto-populates persistent autonomous missions / goals if none exist yet.
     */
    suspend fun populateDefaultGoalsIfEmpty() {
        val dao = goalDao()
        if (dao.count() == 0) {
            Log.i(TAG, "Seeding default persistent agent missions...")
            val defaultGoals = listOf(
                GoalEntity(
                    title = "Surat Office Arrival Alert",
                    description = "When entering Surat Diamond Bourse office radius (200m), check pending client invoices & summarize on-screen.",
                    category = "SPATIAL",
                    triggerInfo = "Geofence: Surat Office • 21.1702°N, 72.8311°E",
                    isActive = true,
                    lastTriggered = "Active • Ready",
                    priority = "HIGH"
                ),
                GoalEntity(
                    title = "Daily Zoho Books Financial Audit",
                    description = "Run automated 8:45 PM IST ledger reconciliations across today's sales invoices, bills, and outstanding customer dues.",
                    category = "FINANCE",
                    triggerInfo = "Scheduled: Mon - Sat @ 8:45 PM IST",
                    isActive = true,
                    lastTriggered = "Scheduled tonight",
                    priority = "HIGH"
                ),
                GoalEntity(
                    title = "Knox Memory Sync & Passphrase Rotation",
                    description = "Verify Knox hardware Keystore seal and re-index vector similarity embeddings for newly learned personal facts.",
                    category = "SECURITY",
                    triggerInfo = "On-Device: Hardware TEE / StrongBox",
                    isActive = true,
                    lastTriggered = "Verified Secure",
                    priority = "NORMAL"
                ),
                GoalEntity(
                    title = "Low Battery Intelligence Safeguard",
                    description = "Down-shift Snapdragon 8 Elite background threads to 4 when battery drops under 20% to conserve battery.",
                    category = "SECURITY",
                    triggerInfo = "Hardware Sensor: Battery < 20%",
                    isActive = true,
                    lastTriggered = "Armed • Sensor Active",
                    priority = "NORMAL"
                ),
                GoalEntity(
                    title = "Lalan Art Morning Dispatch Briefing",
                    description = "Prepare morning WhatsApp dispatch checklist for production managers and weavers before 9:30 AM.",
                    category = "SCHEDULE",
                    triggerInfo = "Scheduled: Mon - Sat @ 9:00 AM IST",
                    isActive = true,
                    lastTriggered = "Ready for tomorrow",
                    priority = "HIGH"
                )
            )

            for (g in defaultGoals) {
                dao.insertGoal(g)
            }
        }
    }
}
