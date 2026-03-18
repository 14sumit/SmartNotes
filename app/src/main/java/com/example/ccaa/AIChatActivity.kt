package com.example.ccaa

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AIChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etQuestion: EditText
    private lateinit var btnSend: MaterialButton

    private val chatList = ArrayList<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aichat)

        recyclerView = findViewById(R.id.chatRecyclerView)
        etQuestion = findViewById(R.id.etQuestion)
        btnSend = findViewById(R.id.btnSend)

        recyclerView.layoutManager = LinearLayoutManager(this)

        chatAdapter = ChatAdapter(chatList)
        recyclerView.adapter = chatAdapter

        btnSend.setOnClickListener {

            val question = etQuestion.text.toString().trim()
            if (question.isEmpty()) return@setOnClickListener

            btnSend.isEnabled = false

            sendUserMessage(question)
            etQuestion.setText("")

            askAI(question)
        }
    }

    private fun sendUserMessage(message: String) {
        chatList.add(ChatMessage(message, true))
        chatAdapter.notifyItemInserted(chatList.size - 1)
        recyclerView.scrollToPosition(chatList.size - 1)
    }

    private fun askAI(prompt: String) {

        val typingMessage = ChatMessage("AI is typing...", false)
        chatList.add(typingMessage)
        chatAdapter.notifyItemInserted(chatList.size - 1)

        lifecycleScope.launch {

            val resultText = withContext(Dispatchers.IO) {

                try {
                    val apiKey = "Api Key" // 🔥 PUT YOUR REAL KEY

                    val json = """
                    {
                      "contents": [
                        {
                          "parts": [
                            {"text": "$prompt"}
                          ]
                        }
                      ]
                    }
                    """

                    val body = json.toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=$apiKey")
                        .post(body)
                        .build()

                    val response = client.newCall(request).execute()

                    if (!response.isSuccessful) {
                        return@withContext "API Error: ${response.code}"
                    }

                    val responseBody = response.body?.string()

                    Log.d("AI_RESPONSE", responseBody ?: "null")

                    val jsonObject = JSONObject(responseBody ?: "{}")

                    val candidates = jsonObject.optJSONArray("candidates")

                    if (candidates != null && candidates.length() > 0) {

                        val content = candidates.getJSONObject(0)
                            .optJSONObject("content")

                        val parts = content?.optJSONArray("parts")

                        if (parts != null && parts.length() > 0) {
                            parts.getJSONObject(0)
                                .optString("text", "No AI text found")
                        } else {
                            "No response from AI"
                        }

                    } else {
                        "No candidates returned"
                    }

                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }

            // Remove typing safely
            if (chatList.isNotEmpty()) {
                chatList.removeAt(chatList.size - 1)
                chatAdapter.notifyItemRemoved(chatList.size)
            }

            // Add AI response
            chatList.add(ChatMessage(resultText, false))
            chatAdapter.notifyItemInserted(chatList.size - 1)

            recyclerView.scrollToPosition(chatList.size - 1)

            btnSend.isEnabled = true
        }
    }
}