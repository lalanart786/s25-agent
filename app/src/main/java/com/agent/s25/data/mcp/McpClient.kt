package com.agent.s25.data.mcp

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class McpClient(private val context: Context) {
    private val client = OkHttpClient.Builder().build()
    private val gson = Gson()
    private val embeddedServer = EmbeddedAndroidMcpServer(context)

    private val _connectedServers = MutableStateFlow<List<McpServerConfig>>(emptyList())
    val connectedServers = _connectedServers.asStateFlow()

    private val _availableTools = MutableStateFlow<List<McpToolDefinition>>(emptyList())
    val availableTools = _availableTools.asStateFlow()

    init {
        // Register embedded tools by default
        _availableTools.value = embeddedServer.listEmbeddedTools()
    }

    suspend fun executeTool(toolName: String, argsJson: String): String = withContext(Dispatchers.IO) {
        val tool = _availableTools.value.find { it.name == toolName }
            ?: return@withContext "{\"error\": \"Tool $toolName not registered.\"}"

        val argsObj = try {
            gson.fromJson(argsJson, JsonObject::class.java) ?: JsonObject()
        } catch (e: Exception) {
            JsonObject()
        }

        if (tool.serverId == "embedded_android") {
            return@withContext embeddedServer.executeTool(toolName, argsObj)
        }

        // Remote SSE / HTTP MCP Server Execution
        val server = _connectedServers.value.find { it.id == tool.serverId }
            ?: return@withContext "{\"error\": \"Server ${tool.serverId} offline or unreachable.\"}"

        try {
            val payload = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("method", "tools/call")
                addProperty("id", System.currentTimeMillis())
                add("params", JsonObject().apply {
                    addProperty("name", toolName)
                    add("arguments", argsObj)
                })
            }

            val request = Request.Builder()
                .url(server.url.replace("/sse", "/call"))
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            response.body?.string() ?: "{\"error\": \"Empty response from remote MCP server.\"}"
        } catch (e: IOException) {
            Log.w("McpClient", "Remote tool execution failed (Graceful degradation): ${e.message}")
            "{\"status\": \"offline\", \"message\": \"Remote server is temporarily unreachable. Using local phone fallback.\"}"
        }
    }
}
