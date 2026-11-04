package com.example.data.model

enum class ChatSender {
    USER,
    CLIENT_AGENT,
    WORKER_AGENT,
    SYSTEM
}

data class ChatMessage(
    val id: String,
    val sender: ChatSender,
    val senderTitle: String,
    val message: String,
    val timestamp: String = "Just now",
    val recommendedWorker: GigWorker? = null,
    val createdTask: ClientTask? = null,
    val actionSuggestions: List<String> = emptyList()
)
