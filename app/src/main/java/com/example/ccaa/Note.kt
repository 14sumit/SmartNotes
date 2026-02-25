package com.example.ccaa
data class Note(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val reminderTime: Long? = null,
    val isPinned: Boolean = false,
    val repeatType: String = "NONE"
)