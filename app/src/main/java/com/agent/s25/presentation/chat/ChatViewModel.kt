package com.agent.s25.presentation.chat

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agent.s25.data.audio.UnifiedAudioRouter
import com.agent.s25.data.db.MemoryDatabase
import com.agent.s25.data.mcp.McpClient
import com.agent.s25.data.model.ChatMessage
import com.agent.s25.data.model.MessageSender
import com.agent.s25.domain.engine.LlamaEngine
import com.agent.s25.domain.indic.LanguageDetector
import com.agent.s25.domain.memory.MemoryManager
import com.agent.s25.domain.tools.SafetyGate
import com.agent.s25.domain.tools.ToolRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db: MemoryDatabase? = try {
        MemoryDatabase.getInstance(application)
    } catch (e: Exception) {
        Log.e("ChatViewModel", "Database init failed: ${e.message}")
        null
    }

    val memoryManager: MemoryManager? = db?.let { MemoryManager(it) }
    val mcpClient = McpClient(application)
    val toolRegistry: ToolRegistry? = memoryManager?.let { ToolRegistry(mcpClient, it) }
    private val llamaEngine = LlamaEngine()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            sender = MessageSender.AGENT,
            text = "નમસ્તે કિશનભાઈ! 🙏 હું તમારો S25 Personal Agent છું.\n\n" +
                "તમે ગુજરાતી, હિન્દી કે English માં કોઈપણ સવાલ પૂછી શકો છો.\n\n" +
                if (!llamaEngine.isNativeAvailable) {
                    "⚠️ AI model not loaded yet. Place a .gguf file in:\n" +
                    "`/sdcard/Download/s25-agent/models/`\n" +
                    "and restart the app."
                } else {
                    "✅ On-device AI engine ready!"
                }
        )
    ))
    val messages = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    init {
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(sender = MessageSender.USER, text = userText)
        _messages.value = _messages.value + userMsg

        val detectedLang = LanguageDetector.detect(userText)

        viewModelScope.launch {
            _isGenerating.value = true

            // 1. Build prompt with Core Profile + Relevant Tools
            val profileHeader = memoryManager?.getCoreProfilePromptHeader() ?: ""
            val relevantTools = toolRegistry?.getRelevantTools(userText) ?: emptyList()

            val toolsHeader = if (relevantTools.isNotEmpty()) {
                val toolDefs = relevantTools.joinToString("\n") { "- ${it.name}: ${it.description}" }
                "\n### Available Tools:\n$toolDefs\nTo call a tool, respond with strict JSON: {\"tool\": \"name\", \"args\": {}}\n"
            } else ""

            val fullPrompt = "$profileHeader$toolsHeader\nUser: $userText\nAssistant:"

            val agentMsg = ChatMessage(sender = MessageSender.AGENT, text = "")
            _messages.value = _messages.value + agentMsg

            val accumulatedText = StringBuilder()
            llamaEngine.generateStream(fullPrompt).collect { token ->
                accumulatedText.append(token)
                _messages.value = _messages.value.dropLast(1) + agentMsg.copy(text = accumulatedText.toString())
            }

            _isGenerating.value = false

            // Speak back in native tongue if speech is enabled
            speakBack(accumulatedText.toString(), detectedLang.bcp47)
        }
    }

    fun startListening() {
        try {
            UnifiedAudioRouter.startCapture()
            UnifiedAudioRouter.setRoute(UnifiedAudioRouter.Route.INDIC_STT)
            _isListening.value = true
        } catch (e: Exception) {
            Log.w("ChatViewModel", "Audio capture failed: ${e.message}")
            _isListening.value = false
        }
    }

    fun stopListeningAndSend() {
        try {
            UnifiedAudioRouter.setRoute(UnifiedAudioRouter.Route.WAKE_WORD)
        } catch (_: Exception) {}
        _isListening.value = false
        // Trigger simulated recognition output for voice button test
        sendMessage("સુરત ઓફિસે જઈને મને પેન્ડિંગ ઇન્વોઇસ યાદ કરાવજે.")
    }

    fun confirmToolExecution(msgId: String, toolName: String, argsJson: String) {
        viewModelScope.launch {
            val result = toolRegistry?.executeTool(toolName, argsJson) ?: "{\"error\": \"Tool registry unavailable\"}"
            val toolMsg = ChatMessage(sender = MessageSender.TOOL_OUTPUT, text = "Tool Output ($toolName):\n$result")
            _messages.value = _messages.value + toolMsg
        }
    }

    private fun speakBack(text: String, bcp47Locale: String) {
        if (!isTtsReady) return
        try {
            val locale = Locale.forLanguageTag(bcp47Locale)
            tts?.language = locale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "s25_tts_utterance")
        } catch (e: Exception) {
            Log.w("ChatViewModel", "TTS speak failed: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        try {
            UnifiedAudioRouter.stopCapture()
        } catch (_: Exception) {}
    }
}
