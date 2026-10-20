package com.agent.s25.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_places")
data class SpatialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // e.g. "Lalan Art Office", "Home", "Surat Ring Road Vendor"
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 150f,
    val notes: String = "",
    val isGeofenceActive: Boolean = true,
    val lastVisitedTime: Long = 0
)
