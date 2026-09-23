package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.data.model.UtilityType
import java.util.Calendar

data class SubmissionPeriodStatus(
    val isSubmittedForCurrentCycle: Boolean,
    val displayBadge: String,
    val daysUntilNext: Int
)

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

    /**
     * Determines whether the reading was already transmitted for the current month/cycle.
     * If already submitted (e.g. today or recently this month), the countdown automatically
     * resets to the target day of NEXT month!
     */
    fun getSubmissionStatus(targetDayOfMonth: Int, latestReadingTimestamp: Long?): SubmissionPeriodStatus {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)
        val currentDay = now.get(Calendar.DAY_OF_MONTH)

        var isSubmittedThisCycle = false
        if (latestReadingTimestamp != null && latestReadingTimestamp > 0) {
            val readingCal = Calendar.getInstance().apply { timeInMillis = latestReadingTimestamp }
            val rYear = readingCal.get(Calendar.YEAR)
            val rMonth = readingCal.get(Calendar.MONTH)
            val diffDays = ((now.timeInMillis - latestReadingTimestamp) / (24L * 3600 * 1000)).toInt()

            if ((rYear == currentYear && rMonth == currentMonth) || diffDays <= 25) {
                isSubmittedThisCycle = true
            }
        }

        if (isSubmittedThisCycle) {
            val nextMonthCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, 1)
                val maxDaysInNext = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, targetDayOfMonth.coerceAtMost(maxDaysInNext))
            }
            val daysUntilNextMonthTarget = ((nextMonthCal.timeInMillis - now.timeInMillis) / (24L * 3600 * 1000)).toInt().coerceAtLeast(1)
            val monthNames = arrayOf("Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep", "Oct", "Noi", "Dec")
            val nextMonthName = monthNames[nextMonthCal.get(Calendar.MONTH)]

            return SubmissionPeriodStatus(
                isSubmittedForCurrentCycle = true,
                displayBadge = "✅ Transmis luna aceasta • Următorul: $targetDayOfMonth $nextMonthName (peste $daysUntilNextMonthTarget zile)",
                daysUntilNext = daysUntilNextMonthTarget
            )
        } else {
            val daysUntil = if (currentDay <= targetDayOfMonth) {
                targetDayOfMonth - currentDay
            } else {
                val maxDayThisMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                (maxDayThisMonth - currentDay) + targetDayOfMonth
            }

            val badge = if (daysUntil == 0) {
                "⚠️ Transmite azi ($targetDayOfMonth ale lunii)!"
            } else {
                "Peste $daysUntil zile ($targetDayOfMonth ale lunii)"
            }

            return SubmissionPeriodStatus(
                isSubmittedForCurrentCycle = false,
                displayBadge = badge,
                daysUntilNext = daysUntil
            )
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
