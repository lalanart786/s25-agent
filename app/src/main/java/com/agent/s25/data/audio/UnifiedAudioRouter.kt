package com.agent.s25.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.agent.s25.domain.indic.IndicSpeechBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object UnifiedAudioRouter {
    private const val TAG = "UnifiedAudioRouter"
    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var isRunning = false
        private set

    enum class Route(val code: Int) {
        WAKE_WORD(0),
        INDIC_STT(1),
        BOTH(2),
        MUTED(3)
    }

    @SuppressLint("MissingPermission")
    fun startCapture() {
        if (isRunning) return

        val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = (minBufSize * 2).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            audioRecord?.startRecording()
            isRunning = true
            Log.i(TAG, "Unified Audio stream started (16kHz PCM).")

            recordingJob = scope.launch {
                val buffer = ShortArray(1024)
                while (isActive && isRunning) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        IndicSpeechBridge.feedAudioSamples(buffer, read)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord: ${e.message}")
            isRunning = false
        }
    }

    fun setRoute(route: Route) {
        IndicSpeechBridge.setAudioRoute(route.code)
        Log.i(TAG, "Audio stream redirected to: $route")
    }

    fun stopCapture() {
        isRunning = false
        recordingJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio stream: ${e.message}")
        }
        audioRecord = null
        Log.i(TAG, "Unified Audio stream closed.")
    }
}
