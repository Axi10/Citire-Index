package com.example.pdf

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.MeterReading
import com.example.data.model.UtilityType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports the reading history as a CSV file that opens correctly in Excel with a Romanian locale:
 * semicolon separator, decimal comma, UTF-8 with a byte order mark.
 */
object CsvExporter {

    private const val SEPARATOR = ";"
    private val ROMANIAN = Locale("ro", "RO")

    fun buildCsv(readings: List<MeterReading>): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val builder = StringBuilder()
        builder.append('﻿')
        builder.append(
            listOf("Data", "Utilitate", "Index", "Index anterior", "Consum", "Pret unitar", "Cost estimat", "Apel efectuat")
                .joinToString(SEPARATOR)
        )
        builder.append("\r\n")

        for (item in readings.sortedBy { it.timestamp }) {
            val utility = if (item.utilityType == UtilityType.GAS) "Gaz" else "Curent"
            val row = listOf(
                dateFormat.format(Date(item.timestamp)),
                utility,
                String.format(ROMANIAN, "%.0f", item.indexValue),
                item.previousIndexValue?.let { String.format(ROMANIAN, "%.0f", it) } ?: "",
                String.format(ROMANIAN, "%.1f", item.consumption),
                String.format(ROMANIAN, "%.2f", item.unitPrice),
                String.format(ROMANIAN, "%.2f", item.estimatedCost),
                if (item.isCallExecuted) "da" else "nu"
            )
            builder.append(row.joinToString(SEPARATOR))
            builder.append("\r\n")
        }
        return builder.toString()
    }

    fun generate(context: Context, readings: List<MeterReading>): File? {
        return try {
            val outputDir = File(context.cacheDir, "reports").apply { mkdirs() }
            outputDir.listFiles()?.forEach { if (it.name.endsWith(".csv")) it.delete() }

            val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val file = File(outputDir, "Istoric_Index_Utilitati_$stamp.csv")
            file.writeText(buildCsv(readings), Charsets.UTF_8)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun share(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Istoric Index Utilități")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Salvează sau trimite istoricul (CSV)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
