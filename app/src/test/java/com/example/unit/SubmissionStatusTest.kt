package com.example.unit

import com.example.notifications.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SubmissionStatusTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis

    @Test
    fun readingFromLastMonth_doesNotCoverTheUpcomingDueDate() {
        // Sent on 28 Sep, today is 5 Oct, due day is the 16th
        val status = ReminderScheduler.getSubmissionStatus(
            targetDayOfMonth = 16,
            latestReadingTimestamp = at(2026, Calendar.SEPTEMBER, 28),
            nowMillis = at(2026, Calendar.OCTOBER, 5)
        )
        // It covers the September cycle, and the next due date is still 16 Oct
        assertTrue(status.isSubmittedForCurrentCycle)
        assertEquals(11, status.daysUntilNext)
        assertTrue(status.displayBadge.contains("16 Oct"))
    }

    @Test
    fun noReading_farFromTheDueDate_isSimplyUpcoming() {
        val status = ReminderScheduler.getSubmissionStatus(16, null, at(2026, Calendar.OCTOBER, 5))
        assertFalse(status.isSubmittedForCurrentCycle)
        assertEquals(11, status.daysUntilNext)
        assertTrue(status.displayBadge.contains("Peste 11 zile"))
    }

    @Test
    fun noReading_justAfterTheDueDate_isOverdue() {
        val status = ReminderScheduler.getSubmissionStatus(16, null, at(2026, Calendar.SEPTEMBER, 20))
        assertFalse(status.isSubmittedForCurrentCycle)
        assertTrue(status.displayBadge.contains("Întârziat cu 4 zile"))
        assertEquals(26, status.daysUntilNext)
    }

    @Test
    fun noReading_onTheDueDate_saysSubmitToday() {
        val status = ReminderScheduler.getSubmissionStatus(16, null, at(2026, Calendar.OCTOBER, 16))
        assertFalse(status.isSubmittedForCurrentCycle)
        assertEquals(0, status.daysUntilNext)
        assertTrue(status.displayBadge.contains("Transmite azi"))
    }

    @Test
    fun earlySubmission_coversTheComingDueDate() {
        // Sent on 5 Oct for the 16 Oct due date: the next one is 16 Nov, 42 days away
        val status = ReminderScheduler.getSubmissionStatus(
            16,
            at(2026, Calendar.OCTOBER, 5),
            at(2026, Calendar.OCTOBER, 5)
        )
        assertTrue(status.isSubmittedForCurrentCycle)
        assertEquals(42, status.daysUntilNext)
        assertTrue(status.displayBadge.contains("16 Noi"))
    }

    @Test
    fun dueDay31_inAMonthWithThirtyDays_fallsOnThe30th() {
        val status = ReminderScheduler.getSubmissionStatus(31, null, at(2026, Calendar.NOVEMBER, 20))
        assertFalse(status.isSubmittedForCurrentCycle)
        assertEquals(10, status.daysUntilNext)
    }
}
