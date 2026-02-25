package com.example.ccaa.ai

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenRouterClient {

    const val API_KEY = "Bearer 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"

    val service: OpenRouterService by lazy {

        Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenRouterService::class.java)
    }
}