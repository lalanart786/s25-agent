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
            val libs = listOf(
                "c++_shared",
                "ggml-base",
                "ggml",
                "ggml-cpu",
                "ggml-opencl",
                "llama",
                "s25_agent_native"
            )
            var nativeLoaded = false
            for (lib in libs) {
                try {
                    System.loadLibrary(lib)
                    Log.i(TAG, "Native library '$lib' loaded successfully.")
                    if (lib == "s25_agent_native") {
                        nativeLoaded = true
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Could not load library '$lib': ${t.message}")
                }
            }
            isNativeLoaded = nativeLoaded
        }
    }

    override fun onCreate() {
        super.onCreate()
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
