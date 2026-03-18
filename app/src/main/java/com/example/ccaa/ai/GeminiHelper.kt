package com.example.ccaa.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.GenerativeModel

object GeminiHelper {
    // gemini-1.5-flash is retired. Use 2.5 or 3.
    val model: GenerativeModel = Firebase.ai.generativeModel(
        modelName = "gemini-2.5-flash"
    )
}

