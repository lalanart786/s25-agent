package com.agent.s25

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

class S25AgentApplication : Application() {

    companion object {
        private const val TAG = "S25AgentApp"
        const val BACKGROUND_CHANNEL_ID = "s25_agent_background_channel"

        @Volatile
        var isNativeLoaded = false
            private set

        init {
            try {
                System.loadLibrary("c++_shared")
            } catch (t: Throwable) {
                Log.w(TAG, "c++_shared load: ${t.message}")
            }
            try {
                System.loadLibrary("s25_agent_native")
                isNativeLoaded = true
                Log.i(TAG, "Native library 's25_agent_native' loaded successfully.")
            } catch (t: Throwable) {
                isNativeLoaded = false
                Log.w(TAG, "Native library not available: ${t.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize SQLCipher encrypted database engine
        try {
            net.sqlcipher.database.SQLiteDatabase.loadLibs(this)
            Log.i(TAG, "SQLCipher native libraries loaded.")
        } catch (e: Exception) {
            Log.w(TAG, "SQLCipher init skipped: ${e.message}")
        }

        createNotificationChannels()
        Log.i(TAG, "S25 Agent Application initialized.")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BACKGROUND_CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
