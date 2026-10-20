package com.agent.s25.data.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.agent.s25.data.model.SpatialEntity
import com.agent.s25.service.GeofenceBroadcastReceiver
import com.google.android.gms.location.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCancellableCoroutine

class LocationProvider(private val context: Context) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    companion object {
        private const val TAG = "LocationProvider"
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc ->
                continuation.resume(loc)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get current location: ${e.message}")
                continuation.resume(null)
            }
    }

    @SuppressLint("MissingPermission")
    fun registerGeofences(places: List<SpatialEntity>) {
        if (places.isEmpty()) return

        val geofenceList = places.map { place ->
            Geofence.Builder()
                .setRequestId(place.id.toString())
                .setCircularRegion(place.latitude, place.longitude, place.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        geofencingClient.addGeofences(request, pendingIntent)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully registered ${places.size} geofences on S25 Ultra.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to register geofences: ${e.message}")
            }
    }
}
