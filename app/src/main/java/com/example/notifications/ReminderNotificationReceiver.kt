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
import com.example.data.local.AppDatabase
import com.example.data.model.UtilityType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Alarms are lost on reboot, and an app update can clear them too:
                // rebuild them from the saved settings
                if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
                    intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
                ) {
                    ReminderScheduler.rescheduleAllNow(appContext)
                    return@launch
                }

                val typeString = intent.getStringExtra(EXTRA_UTILITY_TYPE)
                val utilityType = if (typeString == UtilityType.ELECTRICITY.name) {
                    UtilityType.ELECTRICITY
                } else {
                    UtilityType.GAS
                }

                val config = AppDatabase.getDatabase(appContext).configDao().getConfigSync(utilityType)
                val day = config?.reminderDayOfMonth ?: utilityType.defaultDay
                val hour = config?.reminderHour ?: 9
                val minute = config?.reminderMinute ?: 0

                if (config == null || config.isReminderEnabled) {
                    showReminderNotification(appContext, utilityType, day)
                    // Reschedule for next month with the configured day / time
                    ReminderScheduler.scheduleNext(appContext, utilityType, day, hour, minute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showReminderNotification(context: Context, type: UtilityType, day: Int) {
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
            "🔥 Transmite indexul la Gaz (ziua $day a lunii)"
        } else {
            "⚡ Transmite indexul la Curent (ziua $day a lunii)"
        }

        val message = if (type == UtilityType.GAS) {
            "Este perioada de autocitire pentru gaz! Deschide aplicația pentru apelul automat."
        } else {
            "Este perioada de autocitire energie electrică! Deschide aplicația pentru apelul automat."
        }

        val notificationId = if (type == UtilityType.GAS) 1001 else 1002

        // The status bar needs a monochrome icon: the launcher foreground shows up as a white square
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_notification,
                "Apelează & Transmite",
                pendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
