package com.example.ccaa.ai

data class ChatRequest(
    val model: String = "mistralai/mistral-7b-instruct",
    val messages: List<Message>,
    val max_tokens: Int = 300
)

data class Message(
    val role: String,
    val content: String
)