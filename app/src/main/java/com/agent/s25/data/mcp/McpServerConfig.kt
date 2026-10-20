package com.agent.s25.data.mcp

data class McpServerConfig(
    val id: String,
    val name: String,
    val url: String, // e.g. "http://192.168.1.100:8000/sse"
    val isEnabled: Boolean = true,
    val headers: Map<String, String> = emptyMap()
)

data class McpToolDefinition(
    val serverId: String,
    val name: String,
    val description: String,
    val inputSchemaJson: String,
    val isReadOnly: Boolean = false,
    val isEnabled: Boolean = true
)
