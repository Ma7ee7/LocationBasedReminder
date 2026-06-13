package com.ma7ee7.placereminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifications {
    private const val SERVICE_CHANNEL_ID = "location_checker_status"
    private const val REMINDER_CHANNEL_ID = "place_reminder_alerts"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Location checker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows that Place Reminder is checking location every 10 minutes."
            }

            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Place reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you enter a saved 100 meter reminder zone."
            }

            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }

    fun serviceNotification(context: Context): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Place Reminder is running")
            .setContentText("Checking your location only at 10-minute marks: :00, :10, :20, :30, :40, :50.")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showReminder(context: Context, reminder: Reminder, distanceMeters: Float) {
        val openIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(reminder.title)
            .setContentText("You are inside the 100 meter zone. Distance: ${distanceMeters.toInt()}m.")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(reminder.id.hashCode(), notification)
    }
}
