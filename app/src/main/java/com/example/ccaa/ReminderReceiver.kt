package com.example.ccaa

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val title = intent.getStringExtra("noteTitle") ?: "Reminder"
        val noteId = intent.getStringExtra("noteId")
        val repeatType = intent.getStringExtra("repeatType")
        val originalTime = intent.getLongExtra("originalTime", 0L)

        val channelId = "note_reminder_channel"

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Note Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, DashboardActivity::class.java)

        val pendingIntentOpen = PendingIntent.getActivity(
            context,
            noteId?.hashCode() ?: 0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Reminder 📒")
            .setContentText(title)
            .setContentIntent(pendingIntentOpen)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)

        // 🔁 Repeat logic
        if (repeatType == "DAILY" || repeatType == "WEEKLY") {

            val interval = if (repeatType == "DAILY")
                24 * 60 * 60 * 1000L
            else
                7 * 24 * 60 * 60 * 1000L

            val nextTime = originalTime + interval

            val newIntent = Intent(context, ReminderReceiver::class.java)
            newIntent.putExtra("noteTitle", title)
            newIntent.putExtra("noteId", noteId)
            newIntent.putExtra("repeatType", repeatType)
            newIntent.putExtra("originalTime", nextTime)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                noteId?.hashCode() ?: 0,
                newIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager =
                context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTime,
                pendingIntent
            )
        }
    }
}