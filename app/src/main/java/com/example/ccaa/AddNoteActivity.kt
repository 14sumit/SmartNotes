package com.example.ccaa

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddNoteActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var selectedReminderTime: Long? = null
    private var selectedRepeat = "NONE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        requestNotificationPermission()

        val etTitle = findViewById<TextInputEditText>(R.id.etTitle)
        val etContent = findViewById<TextInputEditText>(R.id.etContent)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val btnPickDate = findViewById<MaterialButton>(R.id.btnPickDate)
        val btnRepeat = findViewById<MaterialButton>(R.id.btnRepeat)
        val tvSelectedDate = findViewById<TextView>(R.id.tvSelectedDate)

        // ---------------- REPEAT TYPE ----------------

        btnRepeat.setOnClickListener {

            val options = arrayOf("None", "Daily", "Weekly")

            AlertDialog.Builder(this)
                .setTitle("Select Repeat Type")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            selectedRepeat = "NONE"
                            btnRepeat.text = "None"
                        }
                        1 -> {
                            selectedRepeat = "DAILY"
                            btnRepeat.text = "Daily"
                        }
                        2 -> {
                            selectedRepeat = "WEEKLY"
                            btnRepeat.text = "Weekly"
                        }
                    }
                }
                .show()
        }

        // ---------------- DATE PICKER ----------------

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

        // ---------------- SAVE NOTE ----------------

        btnSave.setOnClickListener {

            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser ?: run {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val noteId = firestore.collection("notes").document().id

            val note = hashMapOf(
                "id" to noteId,
                "userId" to user.uid,
                "title" to title,
                "content" to content,
                "timestamp" to System.currentTimeMillis(),
                "reminderTime" to selectedReminderTime,
                "isPinned" to false,
                "repeatType" to selectedRepeat
            )

            firestore.collection("notes")
                .document(noteId)
                .set(note)
                .addOnSuccessListener {

                    selectedReminderTime?.let {
                        scheduleReminder(it, title, noteId)
                    }

                    Toast.makeText(this, "Note Saved Successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to Save Note", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // ---------------- PERMISSION ----------------

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    // ---------------- SAFE REMINDER ----------------

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

            when (selectedRepeat) {

                "DAILY" -> alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    time,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )

                "WEEKLY" -> alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    time,
                    AlarmManager.INTERVAL_DAY * 7,
                    pendingIntent
                )

                else -> alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    time,
                    pendingIntent
                )
            }

        } catch (e: SecurityException) {
            Toast.makeText(this, "Exact alarm permission required", Toast.LENGTH_SHORT).show()
        }
    }
}