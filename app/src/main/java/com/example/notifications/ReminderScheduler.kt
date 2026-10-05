package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.model.UtilityType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

data class SubmissionPeriodStatus(
    val isSubmittedForCurrentCycle: Boolean,
    val displayBadge: String,
    val daysUntilNext: Int
)

object ReminderScheduler {

    private const val DAY_MS = 24L * 3600 * 1000

    // A reading counts for a due date if it was made in the 15 days before it (or later)
    private const val SUBMISSION_WINDOW_DAYS = 15L

    // How long after a missed due date we keep showing the "overdue" state
    private const val OVERDUE_DAYS = 14

    private val MONTH_NAMES = arrayOf("Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep", "Oct", "Noi", "Dec")

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

    /** Midnight of [day] in the given month (month may overflow, e.g. 12 = January next year). */
    private fun dueDate(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month, 1, 0, 0, 0)
            set(Calendar.DAY_OF_MONTH, day.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
        }

    private fun daysBetween(from: Calendar, to: Calendar): Int =
        Math.round((to.timeInMillis - from.timeInMillis) / DAY_MS.toDouble()).toInt()

    private fun label(date: Calendar): String =
        "${date.get(Calendar.DAY_OF_MONTH)} ${MONTH_NAMES[date.get(Calendar.MONTH)]}"

    /**
     * Tells whether the index was already sent for the current cycle.
     *
     * The due date is the target day of the month. A reading counts for a due date when it was
     * made at most 15 days before it, so an index sent on the 5th covers the 16th of the same
     * month, while one sent on the 28th of last month does not.
     */
    fun getSubmissionStatus(targetDayOfMonth: Int, latestReadingTimestamp: Long?): SubmissionPeriodStatus {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH)

        val dueThisMonth = dueDate(year, month, targetDayOfMonth)
        val previousDue =
            if (dueThisMonth.timeInMillis <= today.timeInMillis) dueThisMonth
            else dueDate(year, month - 1, targetDayOfMonth)
        val nextDue =
            if (dueThisMonth.timeInMillis >= today.timeInMillis) dueThisMonth
            else dueDate(year, month + 1, targetDayOfMonth)

        fun covers(due: Calendar): Boolean =
            latestReadingTimestamp != null && latestReadingTimestamp > 0 &&
                latestReadingTimestamp >= due.timeInMillis - SUBMISSION_WINDOW_DAYS * DAY_MS

        // Already sent for the upcoming due date: the next one is a month later
        if (covers(nextDue)) {
            val following = dueDate(nextDue.get(Calendar.YEAR), nextDue.get(Calendar.MONTH) + 1, targetDayOfMonth)
            val days = daysBetween(today, following)
            return SubmissionPeriodStatus(
                isSubmittedForCurrentCycle = true,
                displayBadge = "✅ Transmis pentru ${label(nextDue)} • Următorul: ${label(following)} (peste $days zile)",
                daysUntilNext = days
            )
        }

        val daysToNext = daysBetween(today, nextDue)

        // Sent for the most recent due date, nothing to do until the next one
        if (covers(previousDue)) {
            return SubmissionPeriodStatus(
                isSubmittedForCurrentCycle = true,
                displayBadge = "✅ Transmis luna aceasta • Următorul: ${label(nextDue)} (peste $daysToNext zile)",
                daysUntilNext = daysToNext
            )
        }

        val lateDays = daysBetween(previousDue, today)
        if (lateDays in 1..OVERDUE_DAYS) {
            return SubmissionPeriodStatus(
                isSubmittedForCurrentCycle = false,
                displayBadge = "⚠️ Întârziat cu $lateDays zile (termen: ${label(previousDue)}) • Transmite acum!",
                daysUntilNext = daysToNext
            )
        }

        val badge = if (daysToNext == 0) {
            "⚠️ Transmite azi ($targetDayOfMonth ale lunii)!"
        } else {
            "Peste $daysToNext zile ($targetDayOfMonth ale lunii)"
        }
        return SubmissionPeriodStatus(
            isSubmittedForCurrentCycle = false,
            displayBadge = badge,
            daysUntilNext = daysToNext
        )
    }

    private fun requestCodeFor(type: UtilityType): Int = if (type == UtilityType.GAS) 201 else 202

    private fun reminderIntent(context: Context, type: UtilityType): Intent =
        Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_UTILITY_TYPE, type.name)
        }

    /**
     * Next moment (this month or the following one) when the reminder should fire.
     * The day is clamped to the month length, so 31 in a 30-day month fires on the 30th
     * instead of rolling over into the next month.
     */
    private fun nextOccurrence(day: Int, hour: Int, minute: Int): Calendar {
        val now = Calendar.getInstance()

        fun candidate(monthOffset: Int): Calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, day.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val thisMonth = candidate(0)
        return if (thisMonth.timeInMillis <= now.timeInMillis) candidate(1) else thisMonth
    }

    fun scheduleNext(context: Context, type: UtilityType, targetDay: Int = type.defaultDay, targetHour: Int = 9, targetMinute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val nextDate = nextOccurrence(targetDay, targetHour, targetMinute)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(type),
            reminderIntent(context, type),
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

    /**
     * Removes the scheduled reminder for the given utility (used when reminders are disabled).
     */
    fun cancel(context: Context, type: UtilityType) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(type),
            reminderIntent(context, type),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Reschedules every reminder from the settings saved in the database.
     * Falls back to the default day for a utility that has no saved config yet.
     */
    suspend fun rescheduleAllNow(context: Context) {
        val appContext = context.applicationContext
        val configDao = AppDatabase.getDatabase(appContext).configDao()
        for (type in UtilityType.values()) {
            val config = configDao.getConfigSync(type)
            when {
                config == null -> scheduleNext(appContext, type, type.defaultDay, 9, 0)
                config.isReminderEnabled -> scheduleNext(
                    appContext,
                    type,
                    config.reminderDayOfMonth,
                    config.reminderHour,
                    config.reminderMinute
                )
                else -> cancel(appContext, type)
            }
        }
    }

    fun rescheduleAll(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            rescheduleAllNow(appContext)
        }
    }

    /**
     * Instantly triggers a test notification so the user can see how it looks and works.
     */
    fun sendImmediateTestNotification(context: Context, type: UtilityType) {
        context.sendBroadcast(reminderIntent(context, type))
    }
}
