package com.example.ccaa

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ccaa.ai.ChatRequest
import com.example.ccaa.ai.ChatResponse
import com.example.ccaa.ai.Message
import com.example.ccaa.ai.OpenRouterClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

        // Get intent data
        noteId = intent.getStringExtra("noteId") ?: ""
        etTitle.setText(intent.getStringExtra("noteTitle") ?: "")
        etContent.setText(intent.getStringExtra("noteContent") ?: "")

        // ---------------- REMINDER PICKER ----------------

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

        // ---------------- UPDATE NOTE ----------------

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

        // ---------------- AI BUTTONS ----------------

        btnSummary.setOnClickListener {
            val prompt = "Summarize this note:\n${etContent.text}"
            callAI(prompt) { etContent.setText(it) }
        }

        btnRewrite.setOnClickListener {
            val prompt = "Rewrite professionally:\n${etContent.text}"
            callAI(prompt) { etContent.setText(it) }
        }

        btnTags.setOnClickListener {
            val prompt = "Generate 5 short tags:\n${etContent.text}"
            callAI(prompt) {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        btnSuggestTitle.setOnClickListener {
            val prompt = "Suggest a better short title:\n${etContent.text}"
            callAI(prompt) { etTitle.setText(it) }
        }
    }

    // ---------------- AI CALL ----------------

    private fun callAI(prompt: String, onResult: (String) -> Unit) {

        val request = ChatRequest(
            messages = listOf(
                Message("user", prompt)
            )
        )

        OpenRouterClient.service.chatCompletion(
            OpenRouterClient.API_KEY,
            "https://smartnotes.app",
            "SmartNotes AI",
            request
        ).enqueue(object : Callback<ChatResponse> {

            override fun onResponse(
                call: Call<ChatResponse>,
                response: Response<ChatResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {

                    val result = response.body()!!
                        .choices[0]
                        .message
                        .content

                    runOnUiThread {
                        onResult(result)
                    }

                } else {
                    Toast.makeText(
                        this@EditNoteActivity,
                        "AI Error: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                Toast.makeText(
                    this@EditNoteActivity,
                    "AI Failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // ---------------- REMINDER ----------------

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