package com.example.pdf

import com.example.data.model.MeterReading
import com.example.data.model.UtilityType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Reads back the file written by [CsvExporter]: semicolon separated, decimal comma, optional
 * quotes (Excel adds them when it re-saves the file) and an optional byte order mark.
 */
object CsvImporter {

    data class Result(val readings: List<MeterReading>, val skippedLines: Int)

    private val BYTE_ORDER_MARK = Char(0xFEFF)

    fun parse(text: String): Result {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).apply { isLenient = false }

        val readings = mutableListOf<MeterReading>()
        var skipped = 0

        val lines = text.trimStart(BYTE_ORDER_MARK).lines().filter { it.isNotBlank() }
        for ((position, line) in lines.withIndex()) {
            val columns = line.split(';').map { it.trim().trim('"').trim() }

            // The first line is the header
            if (position == 0 && columns.firstOrNull().equals("Data", ignoreCase = true)) continue

            val reading = parseRow(columns, dateFormat)
            if (reading == null) skipped++ else readings.add(reading)
        }
        return Result(readings, skipped)
    }

    private fun parseRow(columns: List<String>, dateFormat: SimpleDateFormat): MeterReading? {
        if (columns.size < 8) return null

        val timestamp = try {
            dateFormat.parse(columns[0])?.time
        } catch (e: Exception) {
            null
        } ?: return null

        val type = when (columns[1].lowercase(Locale.ROOT)) {
            "gaz" -> UtilityType.GAS
            "curent" -> UtilityType.ELECTRICITY
            else -> return null
        }

        val index = number(columns[2]) ?: return null
        if (index <= 0.0) return null

        val previous = number(columns[3])
        val consumption = number(columns[4]) ?: 0.0
        val price = number(columns[5]) ?: type.defaultPrice
        val cost = number(columns[6]) ?: (consumption * price)
        val called = columns[7].lowercase(Locale.ROOT) in setOf("da", "yes", "true", "1")

        return MeterReading(
            utilityType = type,
            indexValue = index,
            previousIndexValue = previous,
            consumption = consumption,
            unitPrice = price,
            estimatedCost = cost,
            timestamp = timestamp,
            callSequenceUsed = "",
            isCallExecuted = called,
            notes = ""
        )
    }

    private fun number(value: String): Double? = value.replace(',', '.').toDoubleOrNull()
}
