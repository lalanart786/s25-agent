package com.agent.s25.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return

        if (event.hasError()) {
            Log.e(TAG, "Geofencing error code: ${event.errorCode}")
            return
        }

        val transition = event.geofenceTransition
        val triggeringGeofences = event.triggeringGeofences ?: return

        for (geofence in triggeringGeofences) {
            val placeId = geofence.requestId
            when (transition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.i(TAG, "Entered geofence place ID: $placeId. Proactive location briefing triggered.")
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.i(TAG, "Exited geofence place ID: $placeId.")
                }
            }
        }
    }
}
