package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.data.model.UtilityType
import java.util.Calendar

object ReminderScheduler {

    fun getDaysUntilNextSubmission(targetDayOfMonth: Int): Int {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_MONTH)

        return if (currentDay <= targetDayOfMonth) {
            targetDayOfMonth - currentDay
        } else {
            // Days remaining in this month + target day in next month
            val maxDayThisMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
            (maxDayThisMonth - currentDay) + targetDayOfMonth
        }
    }

    fun scheduleNext(context: Context, type: UtilityType, targetDay: Int = type.defaultDay, targetHour: Int = 9, targetMinute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = Calendar.getInstance()
        val nextDate = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, targetDay)
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If target time for this month has already passed, schedule for next month
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.MONTH, 1)
            }
        }

        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_UTILITY_TYPE, type.name)
        }

        val requestCode = if (type == UtilityType.GAS) 201 else 202
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextDate.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        nextDate.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextDate.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to non-exact alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                nextDate.timeInMillis,
                pendingIntent
            )
        }
    }

    fun rescheduleAll(context: Context) {
        scheduleNext(context, UtilityType.GAS, 16, 9, 0)
        scheduleNext(context, UtilityType.ELECTRICITY, 24, 9, 0)
    }

    /**
     * Instantly triggers a test notification so the user can see how it looks and works.
     */
    fun sendImmediateTestNotification(context: Context, type: UtilityType) {
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_UTILITY_TYPE, type.name)
        }
        context.sendBroadcast(intent)
    }
}
