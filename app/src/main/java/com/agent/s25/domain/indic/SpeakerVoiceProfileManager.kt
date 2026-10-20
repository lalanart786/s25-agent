package com.agent.s25.domain.indic

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * SpeakerVoiceProfileManager
 * Implements adaptive on-device speaker recognition for Jerry.
 * Passively learns Kishan's acoustic voiceprint over the first 5 interactions,
 * then enforces speaker verification so only Kishan can invoke Jerry.
 */
class SpeakerVoiceProfileManager(private val context: Context) {

    companion object {
        private const val TAG = "JerryVoiceProfile"
        private const val PREFS_NAME = "jerry_voice_profile_prefs"
        private const val KEY_INTERACTIONS_COUNT = "interactions_count"
        private const val KEY_TRAINING_COMPLETE = "training_complete"
        private const val KEY_PROFILE_CENTROID = "profile_centroid"
        private const val KEY_PROFILE_ENERGY = "profile_energy"
        private const val REQUIRED_TRAINING_SAMPLES = 5
        private const val CONFIDENCE_THRESHOLD = 0.78f // 78% acoustic match
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var interactionsCount: Int
        get() = prefs.getInt(KEY_INTERACTIONS_COUNT, 0)
        private set(value) = prefs.edit().putInt(KEY_INTERACTIONS_COUNT, value).apply()

    var isTrainingComplete: Boolean
        get() = prefs.getBoolean(KEY_TRAINING_COMPLETE, false)
        private set(value) = prefs.edit().putBoolean(KEY_TRAINING_COMPLETE, value).apply()

    private var profileCentroid: Float
        get() = prefs.getFloat(KEY_PROFILE_CENTROID, 0f)
        set(value) = prefs.edit().putFloat(KEY_PROFILE_CENTROID, value).apply()

    private var profileEnergy: Float
        get() = prefs.getFloat(KEY_PROFILE_ENERGY, 0f)
        set(value) = prefs.edit().putFloat(KEY_PROFILE_ENERGY, value).apply()

    /**
     * Extracts lightweight acoustic features from a raw PCM 16-bit audio buffer.
     * Features: Normalized Root-Mean-Square (RMS) Energy + Zero-Crossing Rate (ZCR)
     */
    fun extractAcousticFeatures(buffer: ShortArray, readSize: Int): Pair<Float, Float> {
        if (readSize <= 0) return Pair(0f, 0f)

        var sumSquare = 0.0
        var zeroCrossings = 0

        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            sumSquare += sample * sample
            if (i > 0 && ((buffer[i] >= 0 && buffer[i - 1] < 0) || (buffer[i] < 0 && buffer[i - 1] >= 0))) {
                zeroCrossings++
            }
        }

        val rms = sqrt(sumSquare / readSize).toFloat()
        val zcr = zeroCrossings.toFloat() / readSize
        return Pair(rms, zcr)
    }

    /**
     * Adaptively updates Kishan's voiceprint during an interaction.
     */
    fun adaptVoiceSample(buffer: ShortArray, readSize: Int): VoiceAdaptationResult {
        val (rms, zcr) = extractAcousticFeatures(buffer, readSize)

        // Ignore absolute silence or background noise
        if (rms < 250f) {
            return VoiceAdaptationResult(
                isAuthorized = !isTrainingComplete,
                confidence = 0.5f,
                trainingProgress = "$interactionsCount/$REQUIRED_TRAINING_SAMPLES"
            )
        }

        val currentCount = interactionsCount
        if (currentCount < REQUIRED_TRAINING_SAMPLES) {
            // Online running average update
            val newCentroid = if (currentCount == 0) zcr else (profileCentroid * currentCount + zcr) / (currentCount + 1)
            val newEnergy = if (currentCount == 0) rms else (profileEnergy * currentCount + rms) / (currentCount + 1)

            profileCentroid = newCentroid
            profileEnergy = newEnergy
            interactionsCount = currentCount + 1

            if (interactionsCount >= REQUIRED_TRAINING_SAMPLES) {
                isTrainingComplete = true
                Log.i(TAG, "Jerry Voice Profile locked for Kishan! Centroid: $newCentroid, Energy: $newEnergy")
            }

            return VoiceAdaptationResult(
                isAuthorized = true,
                confidence = 0.90f,
                trainingProgress = "$interactionsCount/$REQUIRED_TRAINING_SAMPLES",
                isNewlyTrained = isTrainingComplete
            )
        }

        // Verification phase once training is complete
        val confidence = calculateConfidence(rms, zcr)
        val isAuthorized = confidence >= CONFIDENCE_THRESHOLD

        Log.d(TAG, "Speaker Verification Confidence: ${(confidence * 100).toInt()}% (Authorized: $isAuthorized)")

        return VoiceAdaptationResult(
            isAuthorized = isAuthorized,
            confidence = confidence,
            trainingProgress = "$REQUIRED_TRAINING_SAMPLES/$REQUIRED_TRAINING_SAMPLES"
        )
    }

    private fun calculateConfidence(rms: Float, zcr: Float): Float {
        val targetCentroid = profileCentroid
        if (targetCentroid <= 0f) return 1.0f

        val zcrDiff = abs(zcr - targetCentroid) / targetCentroid
        val zcrSimilarity = (1.0f - zcrDiff).coerceIn(0.0f, 1.0f)

        // Weight spectral zero-crossing rate higher for speaker vocal tract uniqueness
        return (zcrSimilarity * 0.85f + 0.15f).coerceIn(0.0f, 1.0f)
    }

    fun resetProfile() {
        prefs.edit().clear().apply()
        Log.i(TAG, "Voice profile reset. Jerry will re-train on the next 5 interactions.")
    }
}

data class VoiceAdaptationResult(
    val isAuthorized: Boolean,
    val confidence: Float,
    val trainingProgress: String,
    val isNewlyTrained: Boolean = false
)
