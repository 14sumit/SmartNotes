package com.example.ccaa.ai

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterService {

    @POST("api/v1/chat/completions")
    fun chatCompletion(
        @Header("Authorization") token: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") title: String,
        @Body request: ChatRequest
    ): Call<ChatResponse>
}