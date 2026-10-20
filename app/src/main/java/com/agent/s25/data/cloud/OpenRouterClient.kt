package com.agent.s25.data.cloud

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.agent.s25.data.security.KeystoreManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenRouterClient(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("openrouter_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    companion object {
        private const val TAG = "OpenRouterClient"
        private const val PREF_KEY_API_KEY = "encrypted_openrouter_api_key"
        private const val PREF_KEY_MODEL = "openrouter_model"
        const val DEFAULT_MODEL = "deepseek/deepseek-chat" // High quality, low latency & cheap/free
    }

    var selectedModel: String
        get() = prefs.getString(PREF_KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) {
            prefs.edit().putString(PREF_KEY_MODEL, value).apply()
        }

    fun getApiKey(): String? {
        val encrypted = prefs.getString(PREF_KEY_API_KEY, null) ?: return null
        return try {
            KeystoreManager.decrypt(encrypted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt OpenRouter API key: ${e.message}")
            null
        }
    }

    fun saveApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            prefs.edit().remove(PREF_KEY_API_KEY).apply()
        } else {
            val encrypted = KeystoreManager.encrypt(trimmed)
            prefs.edit().putString(PREF_KEY_API_KEY, encrypted).apply()
        }
    }

    fun hasApiKey(): Boolean {
        val key = getApiKey()
        return !key.isNullOrBlank()
    }

    /**
     * Stream responses from OpenRouter with bilingual Gujarati/Hindi/English system prompt
     */
    fun streamChatCompletion(
        userMessage: String,
        systemPrompt: String = "You are Jerry, an elite, highly intelligent personal AI agent for Kishan. Speak naturally and fluently in Gujarati, Hindi, or English based on user query."
    ): Flow<String> = callbackFlow {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            trySend("⚠️ OpenRouter API Key not set. Please tap OpenRouter Settings to add your key.")
            close()
            return@callbackFlow
        }

        val messagesArray = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", systemPrompt)
            })
            add(JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", userMessage)
            })
        }

        val requestBodyJson = JsonObject().apply {
            addProperty("model", selectedModel)
            add("messages", messagesArray)
            addProperty("stream", true)
            addProperty("temperature", 0.7)
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://github.com/lalanart786/s25-agent")
            .addHeader("X-Title", "Jerry My Agent")
            .post(gson.toJson(requestBodyJson).toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val eventSourceListener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    close()
                    return
                }
                try {
                    val json = gson.fromJson(data, JsonObject::class.java)
                    val choices = json.getAsJsonArray("choices")
                    if (choices != null && choices.size() > 0) {
                        val delta = choices[0].asJsonObject.getAsJsonObject("delta")
                        val content = delta?.get("content")?.asString
                        if (!content.isNullOrEmpty()) {
                            trySend(content)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing chunk: ${e.message}")
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e(TAG, "SSE Failure: ${t?.message}, code=${response?.code}")
                if (t != null) {
                    trySend("\n[Network Error: ${t.message}]")
                } else if (response != null) {
                    trySend("\n[API Error: HTTP ${response.code}]")
                }
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = EventSources.createFactory(client).newEventSource(request, eventSourceListener)

        awaitClose {
            eventSource.cancel()
        }
    }
}
