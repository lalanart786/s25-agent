package com.agent.s25.domain.tools

import com.agent.s25.data.mcp.McpClient
import com.agent.s25.data.mcp.McpToolDefinition
import com.agent.s25.domain.memory.MemoryManager
import com.google.gson.Gson
import com.google.gson.JsonObject

class ToolRegistry(
    private val mcpClient: McpClient,
    private val memoryManager: MemoryManager
) {
    private val gson = Gson()

    fun getAllAvailableTools(): List<McpToolDefinition> {
        val memoryTools = listOf(
            McpToolDefinition(
                serverId = "internal_memory",
                name = "remember_fact",
                description = "Save an important personal fact, preference, or habit into persistent memory",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"category\":{\"type\":\"string\"},\"key\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}},\"required\":[\"category\",\"key\",\"value\"]}",
                isReadOnly = false
            ),
            McpToolDefinition(
                serverId = "internal_memory",
                name = "search_past_memory",
                description = "Search stored memories and past conversations across Gujarati, Hindi, and English",
                inputSchemaJson = "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}},\"required\":[\"query\"]}",
                isReadOnly = true
            )
        )
        return memoryTools + mcpClient.availableTools.value
    }

    /**
     * Multilingual dynamic tool selection (RAG for on-device tools).
     * Recognizes user intent in Gujarati, Hindi, and English.
     */
    fun getRelevantTools(userPrompt: String, maxTools: Int = 6): List<McpToolDefinition> {
        val all = getAllAvailableTools()
        val lowerPrompt = userPrompt.lowercase()

        return all.sortedByDescending { tool ->
            var score = 0
            val toolName = tool.name.lowercase()

            if (lowerPrompt.contains(toolName)) score += 10
            val descWords = tool.description.lowercase().split(" ")
            for (w in descWords) {
                if (w.length > 3 && lowerPrompt.contains(w)) score += 2
            }

            // Multilingual intent scoring
            if (toolName.contains("whatsapp") && (
                lowerPrompt.contains("whatsapp") || lowerPrompt.contains("વોટ્સએપ") ||
                lowerPrompt.contains("વાત") || lowerPrompt.contains("મેસેજ") || lowerPrompt.contains("संदेश")
            )) score += 20

            if ((toolName.contains("call") || toolName.contains("phone")) && (
                lowerPrompt.contains("call") || lowerPrompt.contains("phone") || lowerPrompt.contains("dial") ||
                lowerPrompt.contains("ફોન") || lowerPrompt.contains("કોલ") || lowerPrompt.contains("ડાયલ") ||
                lowerPrompt.contains("फोन") || lowerPrompt.contains("कॉल")
            )) score += 20

            if ((toolName.contains("maps") || toolName.contains("navigate")) && (
                lowerPrompt.contains("map") || lowerPrompt.contains("navigate") || lowerPrompt.contains("location") ||
                lowerPrompt.contains("નકશો") || lowerPrompt.contains("રસ્તો") || lowerPrompt.contains("જવું") ||
                lowerPrompt.contains("દિશા") || lowerPrompt.contains("नक्शा") || lowerPrompt.contains("रास्ता")
            )) score += 20

            if (toolName.contains("youtube") && (
                lowerPrompt.contains("youtube") || lowerPrompt.contains("video") ||
                lowerPrompt.contains("યૂટ્યુબ") || lowerPrompt.contains("યુટ્યુબ") || lowerPrompt.contains("વિડિયો") ||
                lowerPrompt.contains("वीडियो")
            )) score += 20

            if (toolName.contains("app") && (
                lowerPrompt.contains("app") || lowerPrompt.contains("launch") || lowerPrompt.contains("open") ||
                lowerPrompt.contains("ખોલ") || lowerPrompt.contains("ઓપન") || lowerPrompt.contains("એપ") ||
                lowerPrompt.contains("खोलो") || lowerPrompt.contains("ऐप")
            )) score += 15

            if (toolName.contains("screen") && (
                lowerPrompt.contains("screen") || lowerPrompt.contains("click") || lowerPrompt.contains("read") ||
                lowerPrompt.contains("સ્ક્રીન") || lowerPrompt.contains("વાંચ") || lowerPrompt.contains("ક્લિક") ||
                lowerPrompt.contains("स्क्रीन")
            )) score += 15

            if ((toolName.contains("memory") || toolName.contains("remember")) && (
                lowerPrompt.contains("remember") || lowerPrompt.contains("memory") ||
                lowerPrompt.contains("યાદ") || lowerPrompt.contains("સાચવ") ||
                lowerPrompt.contains("याद")
            )) score += 15

            if ((toolName.contains("calendar") || toolName.contains("event")) && (
                lowerPrompt.contains("calendar") || lowerPrompt.contains("event") || lowerPrompt.contains("meeting") ||
                lowerPrompt.contains("કેલેન્ડર") || lowerPrompt.contains("ઇવેન્ટ") || lowerPrompt.contains("તારીખ") ||
                lowerPrompt.contains("મીટિંગ") || lowerPrompt.contains("कैलेंडर") || lowerPrompt.contains("मीटिंग")
            )) score += 20

            if ((toolName.contains("alarm") || toolName.contains("timer")) && (
                lowerPrompt.contains("alarm") || lowerPrompt.contains("timer") || lowerPrompt.contains("wake") ||
                lowerPrompt.contains("એલાર્મ") || lowerPrompt.contains("જગાડજે") || lowerPrompt.contains("ટાઈમર") ||
                lowerPrompt.contains("अलार्म") || lowerPrompt.contains("टाइमर")
            )) score += 20

            if (toolName.contains("contact") && (
                lowerPrompt.contains("contact") || lowerPrompt.contains("phone book") ||
                lowerPrompt.contains("સંપર્ક") || lowerPrompt.contains("કોન્ટેક્ટ") ||
                lowerPrompt.contains("संपर्क")
            )) score += 15

            if (toolName.contains("clipboard") && (
                lowerPrompt.contains("clipboard") || lowerPrompt.contains("copy") ||
                lowerPrompt.contains("ક્લિપબોર્ડ") || lowerPrompt.contains("કોપી") ||
                lowerPrompt.contains("क्लिपबोर्ड")
            )) score += 20

            if ((toolName.contains("sms") || toolName.contains("otp")) && (
                lowerPrompt.contains("sms") || lowerPrompt.contains("otp") || lowerPrompt.contains("message") ||
                lowerPrompt.contains("એસએમએસ") || lowerPrompt.contains("ઓટીપી") || lowerPrompt.contains("કોડ") ||
                lowerPrompt.contains("ઓટીપી") || lowerPrompt.contains("ओटीपी") || lowerPrompt.contains("एसएमएस")
            )) score += 25

            if ((toolName.contains("email") || toolName.contains("mail")) && (
                lowerPrompt.contains("email") || lowerPrompt.contains("mail") || lowerPrompt.contains("gmail") ||
                lowerPrompt.contains("ઈમેલ") || lowerPrompt.contains("મેઇલ") || lowerPrompt.contains("જીમેલ") ||
                lowerPrompt.contains("ईमेल") || lowerPrompt.contains("मेल")
            )) score += 20

            if (toolName.contains("telegram") && (
                lowerPrompt.contains("telegram") || lowerPrompt.contains("ટેલિગ્રામ") || lowerPrompt.contains("टेलीग्राम")
            )) score += 20

            if (toolName.contains("screen") && (
                lowerPrompt.contains("screen") || lowerPrompt.contains("સ્ક્રીન") || lowerPrompt.contains("વાંચ") ||
                lowerPrompt.contains("દેખાય") || lowerPrompt.contains("આ શું છે") || lowerPrompt.contains("સ્ક્રીન પર")
            )) score += 30

            if (toolName.contains("briefing") && (
                lowerPrompt.contains("morning") || lowerPrompt.contains("સુપ્રભાત") || lowerPrompt.contains("બ્રીફિંગ") ||
                lowerPrompt.contains("આજનો દિવસ") || lowerPrompt.contains("આજનું પ્લાનિંગ") || lowerPrompt.contains("ગુડ મોર્નિંગ")
            )) score += 30

            if (toolName.contains("compound") || toolName.contains("workflow")) {
                if (lowerPrompt.contains("અને") || lowerPrompt.contains("પછી") || lowerPrompt.contains("ત્યારબાદ") || lowerPrompt.contains("workflow")) {
                    score += 25
                }
            }

            score
        }.take(maxTools)
    }

    suspend fun executeTool(name: String, argsJson: String): String {
        return when (name) {
            "remember_fact" -> {
                val obj = gson.fromJson(argsJson, JsonObject::class.java)
                val cat = obj.get("category")?.asString ?: "GENERAL"
                val key = obj.get("key")?.asString ?: "note"
                val value = obj.get("value")?.asString ?: ""
                val id = memoryManager.rememberFact(cat, key, value)
                "{\"status\": \"saved\", \"id\": $id}"
            }
            "search_past_memory" -> {
                val obj = gson.fromJson(argsJson, JsonObject::class.java)
                val query = obj.get("query")?.asString ?: ""
                val results = memoryManager.searchPastMemory(query)
                gson.toJson(results.map { mapOf("key" to it.key, "value" to it.value, "category" to it.category) })
            }
            else -> mcpClient.executeTool(name, argsJson)
        }
    }
}
