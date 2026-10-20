package com.agent.s25.presentation.chat

import android.app.Application
import android.speech.tts.TextToSpeech
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

    private val db = MemoryDatabase.getInstance(application)
    val memoryManager = MemoryManager(db)
    val mcpClient = McpClient(application)
    val toolRegistry = ToolRegistry(mcpClient, memoryManager)
    private val llamaEngine = LlamaEngine()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            sender = MessageSender.AGENT,
            text = "નમસ્તે કિશનભાઈ! હું તમારો S25 પર્સનલ એજન્ટ છું. તમે ગુજરાતી, હિન્દી કે અંગ્રેજીમાં કોઈપણ કામ કહી શકો છો."
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
            val profileHeader = memoryManager.getCoreProfilePromptHeader()
            val relevantTools = toolRegistry.getRelevantTools(userText)

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
        UnifiedAudioRouter.startCapture()
        UnifiedAudioRouter.setRoute(UnifiedAudioRouter.Route.INDIC_STT)
        _isListening.value = true
    }

    fun stopListeningAndSend() {
        UnifiedAudioRouter.setRoute(UnifiedAudioRouter.Route.WAKE_WORD)
        _isListening.value = false
        // Trigger simulated recognition output for voice button test
        sendMessage("સુરત ઓફિસે જઈને મને પેન્ડિંગ ઇન્વોઇસ યાદ કરાવજે.")
    }

    fun confirmToolExecution(msgId: String, toolName: String, argsJson: String) {
        viewModelScope.launch {
            val result = toolRegistry.executeTool(toolName, argsJson)
            val toolMsg = ChatMessage(sender = MessageSender.TOOL_OUTPUT, text = "Tool Output ($toolName):\n$result")
            _messages.value = _messages.value + toolMsg
        }
    }

    private fun speakBack(text: String, bcp47Locale: String) {
        if (!isTtsReady) return
        val locale = Locale.forLanguageTag(bcp47Locale)
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "s25_tts_utterance")
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        UnifiedAudioRouter.stopCapture()
    }
}
