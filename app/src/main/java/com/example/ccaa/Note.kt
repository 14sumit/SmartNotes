package com.example.ccaa

data class Note(

    var id: String = "",

    var userId: String = "",

    var title: String = "",

    var content: String = "",

    var timestamp: Long = 0,

    var reminderTime: Long? = null,

    var isPinned: Boolean = false,

    var repeatType: String = "NONE"
)