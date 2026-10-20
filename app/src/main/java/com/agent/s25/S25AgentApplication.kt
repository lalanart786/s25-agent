package com.agent.s25

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import net.sqlcipher.database.SQLiteDatabase

class S25AgentApplication : Application() {

    companion object {
        private const val TAG = "S25AgentApp"
        const val BACKGROUND_CHANNEL_ID = "s25_agent_background_channel"
        
        init {
            try {
                System.loadLibrary("s25_agent_native")
                Log.i(TAG, "Native library 's25_agent_native' loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize SQLCipher encrypted database engine
        SQLiteDatabase.loadLibs(this)
        
        createNotificationChannels()
        Log.i(TAG, "S25 Agent Application initialized on Snapdragon 8 Elite runtime.")
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
