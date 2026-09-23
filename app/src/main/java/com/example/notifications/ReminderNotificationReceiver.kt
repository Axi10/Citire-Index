package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.UtilityType

class ReminderNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "meter_reading_reminders"
        const val EXTRA_UTILITY_TYPE = "extra_utility_type"
        const val ACTION_REMINDER = "com.example.ACTION_REMINDER"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Notificări Transmitere Index"
                val descriptionText = "Alerte lunare pentru transmiterea indexului la Gaz și Curent"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule after reboot
            ReminderScheduler.rescheduleAll(context)
            return
        }

        val typeString = intent.getStringExtra(EXTRA_UTILITY_TYPE)
        val utilityType = if (typeString == UtilityType.ELECTRICITY.name) {
            UtilityType.ELECTRICITY
        } else {
            UtilityType.GAS
        }

        showReminderNotification(context, utilityType)

        // Reschedule for next month
        ReminderScheduler.scheduleNext(context, utilityType)
    }

    private fun showReminderNotification(context: Context, type: UtilityType) {
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_UTILITY_TYPE, type.name)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            if (type == UtilityType.GAS) 101 else 102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (type == UtilityType.GAS) {
            "🔥 Transmite indexul la Gaz (16 ale lunii)"
        } else {
            "⚡ Transmite indexul la Curent (24 ale lunii)"
        }

        val message = if (type == UtilityType.GAS) {
            "Este perioada de autocitire pentru gaz! Deschide aplicația pentru apelul automat."
        } else {
            "Este perioada de autocitire energie electrică! Deschide aplicația pentru apelul automat."
        }

        val notificationId = if (type == UtilityType.GAS) 1001 else 1002

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Apelează & Transmite",
                pendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
