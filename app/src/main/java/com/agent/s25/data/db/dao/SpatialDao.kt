package com.agent.s25.data.db.dao

import androidx.room.*
import com.agent.s25.data.model.SpatialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpatialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: SpatialEntity): Long

    @Query("SELECT * FROM saved_places ORDER BY name ASC")
    fun getAllPlaces(): Flow<List<SpatialEntity>>

    @Query("SELECT * FROM saved_places WHERE isGeofenceActive = 1")
    suspend fun getActiveGeofences(): List<SpatialEntity>

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deletePlace(id: Long)

    @Update
    suspend fun updatePlace(place: SpatialEntity)
}
