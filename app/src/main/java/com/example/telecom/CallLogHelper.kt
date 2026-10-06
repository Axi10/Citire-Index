package com.example.telecom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.example.data.model.UtilityType

data class CallLogEntry(
    val number: String,
    val date: Long,
    val durationSeconds: Long
)

enum class CallStatusEvaluation {
    CONFIRMED_SUCCESS,
    TOO_SHORT,
    CANCELLED_OR_MISSED,
    NOT_FOUND
}

data class CallVerificationResult(
    val status: CallStatusEvaluation,
    val durationSeconds: Long,
    val message: String
)

object CallLogHelper {

    fun hasCallLogPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks the device call log for the latest outgoing call made around callStartTime.
     */
    fun checkLatestOutgoingCall(
        context: Context,
        callStartTimeMs: Long,
        expectedPhone: String? = null
    ): CallLogEntry? {
        if (!hasCallLogPermission(context)) return null

        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )

        // Search for outgoing calls starting from 30 seconds before callStartTimeMs
        val minTime = (callStartTimeMs - 30_000L).coerceAtLeast(0L)
        val selection = "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} >= ?"
        val selectionArgs = arrayOf(
            CallLog.Calls.OUTGOING_TYPE.toString(),
            minTime.toString()
        )
        // No "LIMIT 1" here: Android 11+ rejects it in sortOrder ("Invalid token LIMIT"), so we
        // sort newest first and read just the first row.
        val sortOrder = "${CallLog.Calls.DATE} DESC"

        return try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                    val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                    val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                    val number = if (numberIdx != -1) cursor.getString(numberIdx) ?: "" else ""
                    val date = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L
                    val duration = if (durationIdx != -1) cursor.getLong(durationIdx) else 0L

                    CallLogEntry(number = number, date = date, durationSeconds = duration)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Evaluates whether the call lasted long enough for the IVR robot to complete.
     * The call log only knows the duration: it cannot tell whether the robot accepted
     * the index, so a long call is reported as "probably transmitted".
     */
    fun evaluateCall(entry: CallLogEntry?, utilityType: UtilityType): CallVerificationResult {
        if (entry == null) {
            return CallVerificationResult(
                status = CallStatusEvaluation.NOT_FOUND,
                durationSeconds = 0,
                message = "Nu s-a putut citi istoricul apelului. Verifică permisiunea de acces la apeluri."
            )
        }

        val minExpectedSeconds = if (utilityType == UtilityType.GAS) 20 else 30

        return when {
            entry.durationSeconds == 0L -> {
                CallVerificationResult(
                    status = CallStatusEvaluation.CANCELLED_OR_MISSED,
                    durationSeconds = 0,
                    message = "Apelul a fost închis imediat (0 secunde). Indexul NU a fost transmis."
                )
            }
            entry.durationSeconds < minExpectedSeconds -> {
                CallVerificationResult(
                    status = CallStatusEvaluation.TOO_SHORT,
                    durationSeconds = entry.durationSeconds,
                    message = "Apelul a durat doar ${entry.durationSeconds}s (sub timpul necesar de ${minExpectedSeconds}s). Este posibil ca robotul să nu fi salvat indexul."
                )
            }
            else -> {
                CallVerificationResult(
                    status = CallStatusEvaluation.CONFIRMED_SUCCESS,
                    durationSeconds = entry.durationSeconds,
                    message = "Apel de ${entry.durationSeconds} secunde, suficient ca robotul să fi preluat indexul. Aplicația nu poate confirma sigur; verifică mesajul vocal sau factura."
                )
            }
        }
    }
}
