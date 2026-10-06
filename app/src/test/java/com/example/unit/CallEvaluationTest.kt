package com.example.unit

import com.example.data.model.UtilityType
import com.example.telecom.CallLogEntry
import com.example.telecom.CallLogHelper
import com.example.telecom.CallStatusEvaluation
import org.junit.Assert.assertEquals
import org.junit.Test

class CallEvaluationTest {

    private fun entry(seconds: Long) = CallLogEntry(number = "0800800200", date = 0L, durationSeconds = seconds)

    @Test
    fun missingEntry_isNotFound() {
        val result = CallLogHelper.evaluateCall(null, UtilityType.GAS)
        assertEquals(CallStatusEvaluation.NOT_FOUND, result.status)
    }

    @Test
    fun zeroSeconds_isCancelled() {
        val result = CallLogHelper.evaluateCall(entry(0), UtilityType.GAS)
        assertEquals(CallStatusEvaluation.CANCELLED_OR_MISSED, result.status)
    }

    @Test
    fun shortCall_isTooShort() {
        val result = CallLogHelper.evaluateCall(entry(10), UtilityType.GAS)
        assertEquals(CallStatusEvaluation.TOO_SHORT, result.status)
        assertEquals(10L, result.durationSeconds)
    }

    @Test
    fun gasNeedsTwentySeconds_electricityThirty() {
        assertEquals(
            CallStatusEvaluation.CONFIRMED_SUCCESS,
            CallLogHelper.evaluateCall(entry(25), UtilityType.GAS).status
        )
        assertEquals(
            CallStatusEvaluation.TOO_SHORT,
            CallLogHelper.evaluateCall(entry(25), UtilityType.ELECTRICITY).status
        )
        assertEquals(
            CallStatusEvaluation.CONFIRMED_SUCCESS,
            CallLogHelper.evaluateCall(entry(30), UtilityType.ELECTRICITY).status
        )
    }
}
