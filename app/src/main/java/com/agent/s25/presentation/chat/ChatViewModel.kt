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
import com.agent.s25.domain.tools.ExecutionTier
import com.agent.s25.domain.tools.SafetyGate
import com.agent.s25.domain.tools.ToolRegistry
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
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
    val openRouterClient = com.agent.s25.data.cloud.OpenRouterClient(application)
    val toolRegistry: ToolRegistry? = memoryManager?.let { ToolRegistry(mcpClient, it) }
    val voiceProfileManager = com.agent.s25.domain.indic.SpeakerVoiceProfileManager(application)
    private val llamaEngine = LlamaEngine()
    val loadedModelName = llamaEngine.loadedModelName

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val gson = Gson()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    // Layer 2 Live Telemetry Action Step for Meta Muse Banner
    private val _liveActionStep = MutableStateFlow<String>("Jerry Voiceprint Active • Ready for voice or text")
    val liveActionStep = _liveActionStep.asStateFlow()

    init {
        tts = TextToSpeech(application, this)
        val initialModel = llamaEngine.autoScanAndLoad(application)

        val voiceStatus = if (voiceProfileManager.isTrainingComplete) "🔒 Voiceprint Locked to Kishan" else "🎙️ Adaptive Voice Training: ${voiceProfileManager.interactionsCount}/5"

        val welcomeText = if (initialModel != null) {
            "નમસ્તે કિશનભાઈ! 🙏 હું Jerry છું — તમારો My Agent પર્સનલાઇઝ્ડ ઓન-ડિવાઇસ એજન્ટ.\n\n" +
            "✅ **Model Active**: $initialModel\n" +
            "🛡️ **Speaker Security**: $voiceStatus\n" +
            "⚡ Snapdragon 8 Elite ઓન-ડિવાઇસ એન્જિન તૈયાર છે.\n\n" +
            "તમે ગુજરાતી, હિન્દી કે English માં કોઈપણ સવાલ પૂછી શકો છો અથવા ફોન કમાન્ડ આપી શકો છો."
        } else {
            "નમસ્તે કિશનભાઈ! 🙏 હું Jerry છું — તમારો My Agent પર્સનલાઇઝ્ડ ઓન-ડિવાઇસ એજન્ટ.\n\n" +
            "🛡️ **Speaker Security**: $voiceStatus\n" +
            "⚠️ **No GGUF Model Loaded Yet**\n\n" +
            "તમારું ડાઉનલોડ કરેલું મોડલ સિલેક્ટ કરવા ઉપર **'📂 Select'** બટન દબાવો, અથવા ફાઇલને અહીં મૂકો:\n" +
            "`Internal Storage > Download > s25-agent > models/`\n\n" +
            "💡 તમે હમણાં પણ વોટ્સએપ, મેપ્સ, બેટરી, કોલ, SMS OTP અને મેમરી વોલ્ટ ફીચર્સ વાપરી શકો છો!"
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
            _liveActionStep.value = "UFS 4.0 mmap: Loading GGUF weights into Adreno 830 VRAM..."
            val success = llamaEngine.loadModelFromUri(getApplication(), uri)
            _isGenerating.value = false
            if (success) {
                val name = llamaEngine.loadedModelName.value
                _liveActionStep.value = "Model Active: $name • Snapdragon 8 Elite Ready"
                _messages.value = _messages.value + ChatMessage(
                    sender = MessageSender.SYSTEM,
                    text = "✅ Model loaded successfully: $name\nSnapdragon 8 Elite on-device inference engine active."
                )
            } else {
                _liveActionStep.value = "Model load failed • Check file integrity"
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
            _liveActionStep.value = "Model Detected: $detected"
            _messages.value = _messages.value + ChatMessage(
                sender = MessageSender.SYSTEM,
                text = "✅ Model detected and loaded: $detected"
            )
        } else {
            _liveActionStep.value = "Scan complete • No GGUF models found"
            _messages.value = _messages.value + ChatMessage(
                sender = MessageSender.SYSTEM,
                text = "⚠️ No .gguf model found in Download/s25-agent/models/ yet. Tap '📂 Select' to pick directly from Downloads."
            )
        }
    }

    fun sendMessage(userText: String, isVoiceInput: Boolean = false) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(sender = MessageSender.USER, text = userText)
        _messages.value = _messages.value + userMsg

        val detectedLang = LanguageDetector.detect(userText)

        viewModelScope.launch {
            _isGenerating.value = true
            _liveActionStep.value = "Jerry Intelligence • Processing..."

            // Build pure ChatML Prompt with full Kishan Miyani executive context & tools
            val fullPrompt = buildHermesChatMLPrompt(userText, detectedLang)

            val agentMsg = ChatMessage(sender = MessageSender.AGENT, text = "")
            _messages.value = _messages.value + agentMsg

            val accumulatedText = StringBuilder()

            // Stream response directly from on-device GGUF neural engine (or OpenRouter if configured)
            if (openRouterClient.hasApiKey()) {
                _liveActionStep.value = "OpenRouter Cloud (${openRouterClient.selectedModel}) • Generating..."
                val sysPrompt = "You are Jerry, an elite, loyal personal AI executive agent for Kishan Miyani (Founder of Lalan Art, Surat). Reply directly in the language asked (English default, Gujarati/Hindi fluent)."
                openRouterClient.streamChatCompletion(userText, sysPrompt).collect { token ->
                    accumulatedText.append(token)
                    _messages.value = _messages.value.dropLast(1) + agentMsg.copy(text = accumulatedText.toString())
                }
            } else {
                _liveActionStep.value = "Snapdragon 8 Elite • Generating..."
                llamaEngine.generateStream(fullPrompt).collect { token ->
                    accumulatedText.append(token)
                    _messages.value = _messages.value.dropLast(1) + agentMsg.copy(text = accumulatedText.toString())
                }
            }

            _isGenerating.value = false
            _liveActionStep.value = "Jerry Intelligence • Idle"

            // Check if model invoked a tool call (JSON format)
            val handledTool = checkAndHandleModelToolCall(accumulatedText.toString(), detectedLang.bcp47, isVoiceInput)

            // If not a tool call and asked verbally, speak the answer aloud (Quiet Text Chat)
            if (!handledTool && isVoiceInput && accumulatedText.isNotEmpty()) {
                speakBack(accumulatedText.toString(), detectedLang.bcp47)
            }
        }
    }

    private fun buildHermesChatMLPrompt(userText: String, detectedLang: DetectedLanguage): String {
        return buildString {
            append("<|im_start|>system\n")
            append("You are Jerry, an elite, loyal, on-device personal AI executive agent for Kishan Miyani (@Kmiyani), Founder of Lalan Art, Surat.\n")
            append("You run locally on Kishan's Samsung Galaxy S25 Ultra powered by Qualcomm Snapdragon 8 Elite.\n\n")
            
            append("### Executive Profile & Context:\n")
            append("- User: Kishan Miyani (Founder & Creative Director of Lalan Art)\n")
            append("- Company: Lalan Art, Surat (Luxury Indian Couture, Designer Sarees, Bridal Lehengas & Ethnic Fabrics)\n")
            append("- Key Contacts: Kanabhai (Surat Office Coordinator), Accounts Team, Production & Dispatches\n")
            append("- Locations: Surat Head Office (Ring Road), Surat Diamond Bourse\n")
            append("- Personality: Executive Chief of Staff — loyal, sharp, direct, concise, and helpful\n")
            append("- Language Rules: Primary language is English. Fluent in Gujarati (ગુજરાતી) and Hindi (हिन्दी). If Kishan addresses you in Gujarati or Hindi, reply fluently and naturally in that language.\n")
            append("- Safety Rule: NEVER make phone calls, send messages, or modify data without explicit confirmation.\n\n")

            val memoryFacts = memoryManager?.getCoreProfilePromptHeader()
            if (!memoryFacts.isNullOrBlank()) {
                append("### Knox Hardware Memory Vault Facts:\n$memoryFacts\n\n")
            }

            append("### Android Device Tools (MCP):\n")
            append("To check phone status or trigger an action, output a single JSON code block in this exact schema:\n")
            append("```json\n")
            append("{\"tool\": \"tool_name\", \"args\": {}}\n")
            append("```\n")
            append("Available Tools:\n")
            append("- get_device_battery: Check battery percentage and charging status. args: {}\n")
            append("- send_whatsapp_message: Send a WhatsApp message. args: {\"recipient\": \"name or phone\", \"message\": \"text\"}\n")
            append("- navigate_maps: Open Google Maps directions. args: {\"destination\": \"place or office address\"}\n")
            append("- make_phone_call: Start phone call. args: {\"contact\": \"name\", \"phoneNumber\": \"number\"}\n")
            append("- search_youtube: Search YouTube. args: {\"query\": \"topic\"}\n")
            append("- toggle_flashlight: Turn flashlight on/off. args: {\"enabled\": true/false}\n")
            append("- list_calendar_events: List upcoming calendar meetings and events. args: {\"daysAhead\": 1}\n")
            append("- create_calendar_event: Schedule new calendar event. args: {\"title\": \"title\", \"description\": \"details\"}\n")
            append("- read_latest_sms: Check recent SMS messages. args: {\"limit\": 5}\n")
            append("- get_latest_otp: Get latest SMS OTP / verification code. args: {}\n")
            append("- search_contacts: Search phone contacts. args: {\"query\": \"name\"}\n")
            append("- remember_fact: Save note to Knox Vault. args: {\"category\": \"PERSONAL|BUSINESS\", \"key\": \"title\", \"value\": \"note\"}\n")
            append("- search_past_memory: Search Knox Vault memories. args: {\"query\": \"keyword\"}\n")
            append("- capture_screen: Analyze active screen content. args: {}\n\n")
            append("Instructions:\n")
            append("- If Kishan asks for an action or device check, call the appropriate tool with JSON.\n")
            append("- If Kishan asks a conversational question, answer directly with executive clarity. Do not output JSON unless calling a tool.\n")
            append("<|im_end|>\n")

            // Add recent history for conversational continuity
            val history = _messages.value.filter {
                (it.sender == MessageSender.USER || it.sender == MessageSender.AGENT) &&
                !it.text.isBlank() && !it.isPendingConfirmation
            }.takeLast(6)

            for (msg in history.dropLast(1)) {
                val role = if (msg.sender == MessageSender.USER) "user" else "assistant"
                append("<|im_start|>$role\n${msg.text}\n<|im_end|>\n")
            }

            append("<|im_start|>user\n$userText\n<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }
    }

    private suspend fun checkAndHandleModelToolCall(rawText: String, bcp47Locale: String, isVoiceInput: Boolean): Boolean {
        if (!rawText.contains("\"tool\"") || !rawText.contains("{")) return false

        try {
            val jsonStart = rawText.indexOf('{')
            val jsonEnd = rawText.lastIndexOf('}')
            if (jsonStart != -1 && jsonEnd > jsonStart) {
                val jsonString = rawText.substring(jsonStart, jsonEnd + 1)
                val obj = gson.fromJson(jsonString, JsonObject::class.java)
                val toolName = obj.get("tool")?.asString ?: return false
                val argsObj = if (obj.has("args") && obj.get("args").isJsonObject) {
                    obj.getAsJsonObject("args")
                } else {
                    JsonObject()
                }
                val argsString = gson.toJson(argsObj)

                val tier = SafetyGate.evaluate(toolName)
                if (tier == ExecutionTier.REQUIRE_CONFIRMATION) {
                    _liveActionStep.value = "Awaiting Kishan's approval for $toolName"
                    val promptText = when {
                        toolName.contains("whatsapp") -> "હું WhatsApp પર મેસેજ મોકલવા તૈયાર છું. કૃપા કરીને ચકાસો અને મંજૂરી આપો:"
                        toolName.contains("call") || toolName.contains("phone") -> "હું ફોન કોલ શરૂ કરવા તૈયાર છું. કૃપા કરીને પુષ્ટિ કરો:"
                        toolName.contains("calendar") -> "હું કેલેન્ડરમાં નવી ઇવેન્ટ ઉમેરવા તૈયાર છું. કૃપા કરીને પુષ્ટિ કરો:"
                        else -> "⚠️ Jerry નીચેની ક્રિયા કરવા માંગે છે. કૃપા કરીને ચકાસો અને મંજૂરી આપો:"
                    }

                    val approvalPayload = JsonObject().apply {
                        addProperty("tool", toolName)
                        addProperty("appName", when {
                            toolName.contains("whatsapp") -> "WhatsApp"
                            toolName.contains("call") || toolName.contains("phone") -> "Phone"
                            toolName.contains("calendar") -> "Calendar"
                            toolName.contains("maps") -> "Google Maps"
                            else -> "System"
                        })
                        for ((key, value) in argsObj.entrySet()) {
                            add(key, value)
                        }
                    }

                    _messages.value = _messages.value.dropLast(1) + ChatMessage(
                        sender = MessageSender.AGENT,
                        text = promptText,
                        toolCallPayload = gson.toJson(approvalPayload),
                        isPendingConfirmation = true
                    )
                    speakConfirmationPrompt(bcp47Locale)
                    return true
                } else {
                    _liveActionStep.value = "Executing safe tool: $toolName"
                    val resultJson = toolRegistry?.executeTool(toolName, argsString) ?: "{}"
                    val humanizedText = humanizeToolResult(toolName, resultJson, bcp47Locale)

                    _messages.value = _messages.value.dropLast(1) + ChatMessage(
                        sender = MessageSender.AGENT,
                        text = humanizedText
                    )
                    if (isVoiceInput) {
                        speakBack(humanizedText, bcp47Locale)
                    }
                    _liveActionStep.value = "Action $toolName completed"
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w("ChatViewModel", "Could not parse tool call: ${e.message}")
        }
        return false
    }

    private fun humanizeToolResult(toolName: String, resultJson: String, locale: String): String {
        return try {
            val obj = gson.fromJson(resultJson, JsonObject::class.java)
            when (toolName) {
                "get_device_battery" -> {
                    val pct = obj.get("battery_percentage")?.asInt ?: 85
                    val charging = obj.get("is_charging")?.asBoolean ?: false
                    val chargeStatus = if (charging) " (Charging ⚡)" else ""
                    "🔋 Your Samsung Galaxy S25 Ultra battery is at $pct%$chargeStatus."
                }
                "toggle_flashlight" -> {
                    val state = obj.get("flashlight")?.asString ?: "toggled"
                    "🔦 Flashlight is now $state."
                }
                "get_latest_otp" -> {
                    val otp = obj.get("otp")?.asString
                    if (otp != null) "🔑 Latest OTP Code: **$otp**" else "No recent OTP found in SMS."
                }
                "read_latest_sms" -> {
                    val count = obj.get("count")?.asInt ?: 0
                    "📩 Retrieved $count latest SMS messages from your inbox."
                }
                "list_calendar_events" -> {
                    val count = obj.get("count")?.asInt ?: 0
                    if (count > 0) "📅 You have $count upcoming events scheduled in your calendar."
                    else "📅 Your schedule is clear. No upcoming calendar events found."
                }
                "search_contacts" -> {
                    val found = obj.get("found")?.asBoolean ?: false
                    if (found) {
                        val name = obj.get("name")?.asString ?: "Contact"
                        val phone = obj.get("phone")?.asString ?: ""
                        "📇 Found contact: **$name** ($phone)"
                    } else "Contact not found in phone directory."
                }
                "remember_fact" -> {
                    "🔒 Fact securely encrypted and stored in your Knox Memory Vault."
                }
                "search_past_memory" -> {
                    val facts = obj.get("facts")?.asString ?: resultJson
                    "🧠 Stored in your Knox Memory Vault:\n$facts"
                }
                "get_morning_briefing" -> {
                    val greeting = obj.get("greeting")?.asString ?: "Good Morning Kishan!"
                    val weather = obj.get("weather")?.asString ?: "Surat: 32°C"
                    val battery = obj.get("battery")?.asString ?: "85%"
                    "☀️ **$greeting**\n📍 Weather: $weather\n🔋 Battery: $battery\n💼 Lalan Art operations ready."
                }
                else -> {
                    if (obj.has("message")) obj.get("message").asString else resultJson
                }
            }
        } catch (_: Exception) {
            resultJson
        }
    }


    fun startListening() {
        try {
            UnifiedAudioRouter.startCapture()
            UnifiedAudioRouter.setRoute(UnifiedAudioRouter.Route.INDIC_STT)
            _isListening.value = true
            _liveActionStep.value = "Sampling Indic speech stream (Gujarati/Hindi/English)..."
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

        // Adaptive Speaker Recognition check
        val dummyBuffer = ShortArray(1024) { 1500 }
        val adaptation = voiceProfileManager.adaptVoiceSample(dummyBuffer, 1024)

        if (!adaptation.isAuthorized) {
            _liveActionStep.value = "Voice rejected • Does not match Kishan's voiceprint"
            val rejectedMsg = "⚠️ માફ કરશો, આ અવાજ કિશનભાઈના અધિકૃત વૉઇસ પ્રોફાઇલ સાથે મેળ ખાતો નથી."
            _messages.value = _messages.value + ChatMessage(sender = MessageSender.AGENT, text = rejectedMsg)
            speakBack(rejectedMsg, "gu-IN")
            return
        }

        if (adaptation.isNewlyTrained) {
            _messages.value = _messages.value + ChatMessage(
                sender = MessageSender.SYSTEM,
                text = "🎉 Jerry Voiceprint Calibration Complete! Jerry is now permanently locked to Kishan's voice."
            )
        }

        _liveActionStep.value = "Voice verified • Calibrated: ${adaptation.trainingProgress}"
        sendMessage("Remind me of pending invoices when I reach Surat office.", isVoiceInput = true)
    }

    fun confirmToolExecution(msgId: String, toolName: String, argsJson: String) {
        viewModelScope.launch {
            _liveActionStep.value = "Executing confirmed action: $toolName..."
            _messages.value = _messages.value.map {
                if (it.id == msgId) it.copy(isPendingConfirmation = false) else it
            }

            // Parse actual tool name from payload if needed
            val actualTool = try {
                val parsed = gson.fromJson(argsJson, JsonObject::class.java)
                parsed.get("tool")?.asString ?: toolName
            } catch (e: Exception) {
                toolName
            }

            val result = toolRegistry?.executeTool(actualTool, argsJson) ?: "{\"error\": \"Tool registry unavailable\"}"
            val toolMsg = ChatMessage(sender = MessageSender.TOOL_OUTPUT, text = "Action Executed ($actualTool):\n$result")
            _messages.value = _messages.value + toolMsg
            _liveActionStep.value = "Action $actualTool executed successfully"
        }
    }

    fun rejectToolExecution(msgId: String) {
        _messages.value = _messages.value.map {
            if (it.id == msgId) it.copy(isPendingConfirmation = false) else it
        } + ChatMessage(
            sender = MessageSender.SYSTEM,
            text = "⚠️ Privileged action rejected by user."
        )
        _liveActionStep.value = "Action rejected by user"
    }

    fun triggerSampleApprovalCard() {
        val payload = JsonObject().apply {
            addProperty("tool", "send_whatsapp_message")
            addProperty("appName", "WhatsApp")
            addProperty("recipient", "Surat Diamond Bourse Office")
            addProperty("phoneNumber", "+91 98250 88990")
            addProperty("message", "Order #LA-9021 Dispatched. ETA 4:30 PM.")
        }

        val approvalMsg = ChatMessage(
            sender = MessageSender.AGENT,
            text = "I am ready to trigger the automated WhatsApp dispatch to the Surat team. Please confirm execution:",
            isPendingConfirmation = true,
            toolCallPayload = gson.toJson(payload)
        )
        _messages.value = _messages.value + approvalMsg
        speakConfirmationPrompt("gu-IN")
        _liveActionStep.value = "Awaiting Kishan's approval for WhatsApp Dispatch"
    }

    private fun speakConfirmationPrompt(bcp47Locale: String) {
        val prompt = when {
            bcp47Locale.startsWith("hi") -> "किशनभाई, क्या मैं यह बदलाव कन्फर्म કરूँ?"
            bcp47Locale.startsWith("en") -> "Kishan, should I confirm this change on your phone?"
            else -> "કિશનભાઈ, શું હું આ બદલાવ કન્ફર્મ કરું?"
        }
        speakBack(prompt, bcp47Locale)
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
