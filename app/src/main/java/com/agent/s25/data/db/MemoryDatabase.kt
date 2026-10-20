package com.agent.s25.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.agent.s25.data.db.dao.MemoryDao
import com.agent.s25.data.db.dao.SpatialDao
import com.agent.s25.data.model.MemoryEntity
import com.agent.s25.data.model.SpatialEntity
import com.agent.s25.data.security.KeystoreManager
import net.sqlcipher.database.SupportFactory

@Database(entities = [MemoryEntity::class, SpatialEntity::class], version = 1, exportSchema = false)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun spatialDao(): SpatialDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getInstance(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = KeystoreManager.getOrCreateMasterPassphrase()
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "s25_knox_memory_vault.db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
