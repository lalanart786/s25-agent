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

    // Semantic Dynamic Tool Selection (RAG for Tools)
    fun getRelevantTools(userPrompt: String, maxTools: Int = 4): List<McpToolDefinition> {
        val all = getAllAvailableTools()
        val lowerPrompt = userPrompt.lowercase()

        // Prioritize tools matching keywords in the prompt
        return all.sortedByDescending { tool ->
            var score = 0
            if (lowerPrompt.contains(tool.name.lowercase())) score += 10
            val descWords = tool.description.lowercase().split(" ")
            for (w in descWords) {
                if (w.length > 3 && lowerPrompt.contains(w)) score += 2
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
