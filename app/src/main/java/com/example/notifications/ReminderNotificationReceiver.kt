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
        const val EXTRA_IS_TEST = "extra_is_test"
        const val ACTION_REMINDER = "com.example.ACTION_REMINDER"
        const val ACTION_FOLLOW_UP = "com.example.ACTION_FOLLOW_UP"

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

                val database = AppDatabase.getDatabase(appContext)
                val config = database.configDao().getConfigSync(utilityType)
                val day = config?.reminderDayOfMonth ?: utilityType.defaultDay
                val hour = config?.reminderHour ?: 9
                val minute = config?.reminderMinute ?: 0
                val remindersOn = config == null || config.isReminderEnabled

                // Second reminder: only if the index still was not transmitted for this cycle
                if (intent.action == ACTION_FOLLOW_UP) {
                    if (remindersOn) {
                        val lastTransmitted = database.meterDao().getReadingsByTypeAsc(utilityType)
                            .filter { it.isCallExecuted }
                            .maxOfOrNull { it.timestamp }
                        val status = ReminderScheduler.getSubmissionStatus(day, lastTransmitted)
                        if (!status.isSubmittedForCurrentCycle) {
                            showFollowUpNotification(appContext, utilityType, day)
                        }
                    }
                    return@launch
                }

                // Test button in Settings: just show the notification
                if (intent.getBooleanExtra(EXTRA_IS_TEST, false)) {
                    showReminderNotification(appContext, utilityType, day)
                    return@launch
                }

                if (remindersOn) {
                    showReminderNotification(appContext, utilityType, day)
                    // Reschedule for next month with the configured day / time
                    ReminderScheduler.scheduleNext(appContext, utilityType, day, hour, minute)
                    ReminderScheduler.scheduleFollowUp(appContext, utilityType)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun openAppIntent(context: Context, type: UtilityType): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_UTILITY_TYPE, type.name)
        }

        return PendingIntent.getActivity(
            context,
            if (type == UtilityType.GAS) 101 else 102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showReminderNotification(context: Context, type: UtilityType, day: Int) {
        ensureChannel(context)

        val pendingIntent = openAppIntent(context, type)

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
        notify(context, notificationId, title, message, pendingIntent)
    }

    private fun showFollowUpNotification(context: Context, type: UtilityType, day: Int) {
        ensureChannel(context)

        val pendingIntent = openAppIntent(context, type)
        val utility = if (type == UtilityType.GAS) "gaz" else "curent"

        val title = "⏰ Încă nu ai transmis indexul la ${type.title}"
        val message = "Termenul (ziua $day) a trecut, iar indexul pentru $utility nu apare transmis. " +
            "Durează un minut: deschide aplicația și apasă Transmite."

        val notificationId = if (type == UtilityType.GAS) 1003 else 1004
        notify(context, notificationId, title, message, pendingIntent)
    }

    private fun notify(context: Context, id: Int, title: String, message: String, pendingIntent: PendingIntent) {
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
        notificationManager.notify(id, notification)
    }
}
