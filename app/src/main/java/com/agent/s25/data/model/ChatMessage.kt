package com.agent.s25.data.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val toolCallPayload: String? = null,
    val isPendingConfirmation: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    AGENT,
    TOOL_OUTPUT,
    SYSTEM
}
