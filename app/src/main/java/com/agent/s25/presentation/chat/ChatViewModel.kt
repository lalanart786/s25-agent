package com.agent.s25.presentation.chat

import android.app.Application
import android.net.Uri
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
    val loadedModelName = llamaEngine.loadedModelName

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    init {
        tts = TextToSpeech(application, this)
        val initialModel = llamaEngine.autoScanAndLoad(application)

        val welcomeText = if (initialModel != null) {
            "નમસ્તે કિશનભાઈ! 🙏 હું તમારો S25 Personal Agent છું.\n\n" +
            "✅ **Model Active**: $initialModel\n" +
            "Snapdragon 8 Elite ઓન-ડિવાઇસ એન્જિન તૈયાર છે.\n\n" +
            "તમે ગુજરાતી, હિન્દી કે English માં કોઈપણ સવાલ પૂછી શકો છો."
        } else {
            "નમસ્તે કિશનભાઈ! 🙏 હું તમારો S25 Personal Agent છું.\n\n" +
            "⚠️ **No GGUF Model Loaded Yet**\n\n" +
            "તમારું ડાઉનલોડ કરેલું મોડલ સિલેક્ટ કરવા ઉપર **'📂 Select Model File'** બટન દબાવો, અથવા ફાઇલને અહીં મૂકો:\n" +
            "`Internal Storage > Download > s25-agent > models/`"
        }

        _messages.value = listOf(
            ChatMessage(sender = MessageSender.AGENT, text = welcomeText)
        )
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
        }
    }

    fun selectModelUri(uri: Uri) {
        viewModelScope.launch {
            _isGenerating.value = true
            val success = llamaEngine.loadModelFromUri(getApplication(), uri)
            _isGenerating.value = false
            if (success) {
                val name = llamaEngine.loadedModelName.value
                _messages.value = _messages.value + ChatMessage(
                    sender = MessageSender.SYSTEM,
                    text = "✅ Model loaded successfully: $name\nSnapdragon 8 Elite inference engine active."
                )
            } else {
                _messages.value = _messages.value + ChatMessage(
                    sender = MessageSender.SYSTEM,
                    text = "❌ Failed to load the selected model file."
                )
            }
        }
    }

    fun refreshScan() {
        val detected = llamaEngine.autoScanAndLoad(getApplication())
        if (detected != null) {
            _messages.value = _messages.value + ChatMessage(
                sender = MessageSender.SYSTEM,
                text = "✅ Model detected and loaded: $detected"
            )
        } else {
            _messages.value = _messages.value + ChatMessage(
                sender = MessageSender.SYSTEM,
                text = "⚠️ No .gguf model found in Download/s25-agent/models/ yet. Tap '📂 Select Model' to pick directly from Downloads."
            )
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
