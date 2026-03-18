package com.example.ccaa

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ccaa.ai.GeminiHelper

import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.*

class EditNoteActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private var selectedReminderTime: Long? = null
    private var noteId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        firestore = FirebaseFirestore.getInstance()

        val etTitle = findViewById<TextInputEditText>(R.id.etTitle)
        val etContent = findViewById<TextInputEditText>(R.id.etContent)
        val btnUpdate = findViewById<MaterialButton>(R.id.btnUpdate)
        val btnPickDate = findViewById<MaterialButton>(R.id.btnPickDate)
        val btnRemoveReminder = findViewById<MaterialButton>(R.id.btnRemoveReminder)
        val tvSelectedDate = findViewById<TextView>(R.id.tvSelectedDate)

        val btnSummary = findViewById<MaterialButton>(R.id.btnSummary)
        val btnRewrite = findViewById<MaterialButton>(R.id.btnRewrite)
        val btnTags = findViewById<MaterialButton>(R.id.btnTags)
        val btnSuggestTitle = findViewById<MaterialButton>(R.id.btnSuggestTitle)

        // Get note data
        noteId = intent.getStringExtra("noteId") ?: ""
        etTitle.setText(intent.getStringExtra("noteTitle") ?: "")
        etContent.setText(intent.getStringExtra("noteContent") ?: "")

        // -------- DATE PICKER --------

        btnPickDate.setOnClickListener {

            val calendar = Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, day ->

                    TimePickerDialog(
                        this,
                        { _, hour, minute ->

                            val selectedCalendar = Calendar.getInstance()
                            selectedCalendar.set(year, month, day, hour, minute, 0)

                            selectedReminderTime = selectedCalendar.timeInMillis

                            tvSelectedDate.text = String.format(
                                Locale.getDefault(),
                                "Reminder: %02d/%02d/%04d %02d:%02d",
                                day,
                                month + 1,
                                year,
                                hour,
                                minute
                            )

                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnRemoveReminder.setOnClickListener {
            selectedReminderTime = null
            tvSelectedDate.text = "No Reminder"
            cancelReminder(noteId)
        }

        // -------- UPDATE NOTE --------

        btnUpdate.setOnClickListener {

            val updatedData = mapOf(
                "title" to etTitle.text.toString(),
                "content" to etContent.text.toString(),
                "reminderTime" to selectedReminderTime
            )

            firestore.collection("notes")
                .document(noteId)
                .update(updatedData)
                .addOnSuccessListener {

                    cancelReminder(noteId)

                    selectedReminderTime?.let {
                        scheduleReminder(it, etTitle.text.toString(), noteId)
                    }

                    Toast.makeText(this, "Note Updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
                }
        }

        // -------- AI BUTTONS --------

        btnSummary.setOnClickListener {
            callAI("Summarize this note in 3 lines:\n${etContent.text}") {
                etContent.setText(it)
            }
        }

        btnRewrite.setOnClickListener {
            callAI("Rewrite this note professionally:\n${etContent.text}") {
                etContent.setText(it)
            }
        }

        btnTags.setOnClickListener {
            callAI("Generate 5 short tags for this note:\n${etContent.text}") {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        btnSuggestTitle.setOnClickListener {
            callAI("Suggest a better short title for this note:\n${etContent.text}") {
                etTitle.setText(it)
            }
        }
    }

    // -------- GEMINI AI --------

    private fun callAI(prompt: String, onResult: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                // 1. USE YOUR ACTUAL KEY
                val apiKey = "AIzaSyCBmfWZAY3PosEEX8JWkNjvllyVfaGOO6U"

                // 2. Build JSON safely using JSONObject
                val contentJson = JSONObject().apply {
                    put("contents", org.json.JSONArray().put(
                        JSONObject().put("parts", org.json.JSONArray().put(
                            JSONObject().put("text", prompt)
                        ))
                    ))
                }
                val requestBody = contentJson.toString().toRequestBody("application/json".toMediaType())

                // 3. UPDATED URL: Changed to /v1/ and gemini-2.5-flash
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val responseBody = response.body?.string()

                // Logging remains vital for debugging
                android.util.Log.d("GEMINI_RAW", responseBody ?: "null")

                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)

                    // 4. Added safe navigation for the JSON response
                    val candidates = jsonObject.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val text = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")

                        withContext(Dispatchers.Main) { onResult(text) }
                    } else {
                        withContext(Dispatchers.Main) { onResult("No candidates returned.") }
                    }
                } else {
                    // If it fails, log the specific code (404, 403, etc.)
                    withContext(Dispatchers.Main) { onResult("Error: ${response.code}") }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditNoteActivity, "AI Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    // -------- REMINDER --------

    private fun scheduleReminder(time: Long, title: String, noteId: String) {

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    return
                }
            }

            val intent = Intent(this, ReminderReceiver::class.java)
            intent.putExtra("noteTitle", title)
            intent.putExtra("noteId", noteId)

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                noteId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                pendingIntent
            )

        } catch (e: SecurityException) {
            Toast.makeText(this, "Exact alarm permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelReminder(noteId: String) {

        val intent = Intent(this, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            noteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}