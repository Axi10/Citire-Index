package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.MeterReading
import com.example.data.model.UtilityConfig
import com.example.data.model.UtilityType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateMonthlyReport(
        context: Context,
        periodName: String,
        readings: List<MeterReading>,
        gasConfig: UtilityConfig?,
        electricityConfig: UtilityConfig?
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (points)
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            drawReportContent(canvas, periodName, readings, gasConfig, electricityConfig)

            pdfDocument.finishPage(page)

            // Save file
            val outputDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val sanitizedPeriod = periodName.replace(" ", "_").lowercase(Locale.ROOT)
            val file = File(outputDir, "Raport_Consum_${sanitizedPeriod}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawReportContent(
        canvas: Canvas,
        periodName: String,
        readings: List<MeterReading>,
        gasConfig: UtilityConfig?,
        electricityConfig: UtilityConfig?
    ) {
        val paint = Paint().apply { isAntiAlias = true }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        // 1. Header background
        paint.color = Color.rgb(15, 23, 42) // Dark Slate #0F172A
        canvas.drawRect(0f, 0f, 595f, 95f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("INDEX UTILITĂȚI — RAPORT CONSUM", 30f, 45f, paint)

        // Subtitle & period
        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("Perioadă raportată: $periodName  |  Generat: ${dateFormat.format(Date())}", 30f, 70f, paint)

        // 2. Summary KPI Cards
        val gasReadings = readings.filter { it.utilityType == UtilityType.GAS }
        val electricityReadings = readings.filter { it.utilityType == UtilityType.ELECTRICITY }

        val totalGasConsumption = gasReadings.sumOf { it.consumption }
        val totalGasCost = gasReadings.sumOf { it.estimatedCost }
        val latestGasIndex = gasReadings.maxByOrNull { it.timestamp }?.indexValue ?: 0.0

        val totalElecConsumption = electricityReadings.sumOf { it.consumption }
        val totalElecCost = electricityReadings.sumOf { it.estimatedCost }
        val latestElecIndex = electricityReadings.maxByOrNull { it.timestamp }?.indexValue ?: 0.0

        val totalGeneralCost = totalGasCost + totalElecCost

        var currentY = 115f

        // Card 1: Gaz
        drawSummaryCard(
            canvas = canvas,
            rect = RectF(30f, currentY, 205f, currentY + 80f),
            title = "🔥 Gaze Naturale",
            accentColor = Color.rgb(2, 132, 199), // Blue
            primaryMetric = String.format(Locale.US, "%.1f m³", totalGasConsumption),
            secondaryMetric = String.format(Locale.US, "Cost: %.2f LEI", totalGasCost),
            latestIndex = String.format(Locale.US, "Ultimul index: %.0f", latestGasIndex)
        )

        // Card 2: Curent Electric
        drawSummaryCard(
            canvas = canvas,
            rect = RectF(215f, currentY, 390f, currentY + 80f),
            title = "⚡ Curent Electric",
            accentColor = Color.rgb(245, 158, 11), // Amber
            primaryMetric = String.format(Locale.US, "%.1f kWh", totalElecConsumption),
            secondaryMetric = String.format(Locale.US, "Cost: %.2f LEI", totalElecCost),
            latestIndex = String.format(Locale.US, "Ultimul index: %.0f", latestElecIndex)
        )

        // Card 3: Total Estimat
        drawSummaryCard(
            canvas = canvas,
            rect = RectF(400f, currentY, 565f, currentY + 80f),
            title = "💰 Total Estimativ",
            accentColor = Color.rgb(16, 185, 129), // Emerald
            primaryMetric = String.format(Locale.US, "%.2f LEI", totalGeneralCost),
            secondaryMetric = "Total de plată estimat",
            latestIndex = "${readings.size} citiri înregistrate"
        )

        currentY += 105f

        // 3. Section Title: Istoric & Detalii
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("DETALII TRANSMITERE ȘI CONSUM", 30f, currentY, paint)

        currentY += 15f

        // 4. Table Header
        val colDate = 30f
        val colType = 120f
        val colIndex = 200f
        val colDelta = 275f
        val colPrice = 345f
        val colCost = 420f
        val colStatus = 490f

        paint.color = Color.rgb(241, 245, 249) // Light grey table header
        canvas.drawRect(30f, currentY, 565f, currentY + 24f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9.5f
        paint.isFakeBoldText = true
        canvas.drawText("DATĂ", colDate + 6f, currentY + 16f, paint)
        canvas.drawText("UTILITATE", colType, currentY + 16f, paint)
        canvas.drawText("INDEX", colIndex, currentY + 16f, paint)
        canvas.drawText("CONSUM", colDelta, currentY + 16f, paint)
        canvas.drawText("PREȚ/UNIT", colPrice, currentY + 16f, paint)
        canvas.drawText("COST (LEI)", colCost, currentY + 16f, paint)
        canvas.drawText("APEL IVR", colStatus, currentY + 16f, paint)

        currentY += 24f

        // 5. Table Rows
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        paint.isFakeBoldText = false

        if (readings.isEmpty()) {
            paint.color = Color.rgb(148, 163, 184)
            paint.textSize = 11f
            canvas.drawText("Nu există înregistrări pentru perioada selectată.", 40f, currentY + 30f, paint)
            currentY += 60f
        } else {
            readings.forEachIndexed { index, item ->
                if (currentY > 750f) return@forEachIndexed // avoid overflowing A4 page

                // alternating background
                if (index % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(30f, currentY, 565f, currentY + 22f, paint)
                }

                paint.color = Color.rgb(30, 41, 59)
                paint.textSize = 9.5f

                // Date
                canvas.drawText(simpleDateFormat.format(Date(item.timestamp)), colDate + 6f, currentY + 15f, paint)

                // Type
                val typeLabel = if (item.utilityType == UtilityType.GAS) "Gaz (m³)" else "Curent (kWh)"
                canvas.drawText(typeLabel, colType, currentY + 15f, paint)

                // Index
                canvas.drawText(String.format(Locale.US, "%.0f", item.indexValue), colIndex, currentY + 15f, paint)

                // Consumption Delta
                val deltaText = if (item.consumption > 0) "+${String.format(Locale.US, "%.1f", item.consumption)}" else "-"
                canvas.drawText(deltaText, colDelta, currentY + 15f, paint)

                // Price
                canvas.drawText(String.format(Locale.US, "%.2f", item.unitPrice), colPrice, currentY + 15f, paint)

                // Cost
                paint.isFakeBoldText = true
                canvas.drawText(String.format(Locale.US, "%.2f", item.estimatedCost), colCost, currentY + 15f, paint)
                paint.isFakeBoldText = false

                // Status
                val statusText = if (item.isCallExecuted) "Efectuat" else "Salvat"
                paint.color = if (item.isCallExecuted) Color.rgb(22, 163, 74) else Color.rgb(100, 116, 139)
                canvas.drawText(statusText, colStatus, currentY + 15f, paint)

                currentY += 22f
            }
        }

        // Table Bottom Border
        paint.color = Color.rgb(203, 213, 225)
        paint.strokeWidth = 1f
        canvas.drawLine(30f, currentY, 565f, currentY, paint)

        // 6. IVR Configurations footnote box
        currentY += 30f
        paint.color = Color.rgb(248, 250, 252)
        val infoRect = RectF(30f, currentY, 565f, currentY + 70f)
        canvas.drawRoundRect(infoRect, 8f, 8f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 9.5f
        paint.isFakeBoldText = true
        canvas.drawText("Informații Contracte & Secvențe IVR:", 40f, currentY + 20f, paint)

        paint.isFakeBoldText = false
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        val gasInfo = "Gaz: TelVerde ${gasConfig?.phoneNumber ?: UtilityType.GAS.defaultPhone} | Cod Client: ${gasConfig?.clientCode ?: UtilityType.GAS.defaultClientCode} | Termen: 16 ale lunii"
        val elecInfo = "Curent: TelVerde ${electricityConfig?.phoneNumber ?: UtilityType.ELECTRICITY.defaultPhone} | Cod Client: ${electricityConfig?.clientCode ?: UtilityType.ELECTRICITY.defaultClientCode} | Termen: 24 ale lunii"
        canvas.drawText(gasInfo, 40f, currentY + 38f, paint)
        canvas.drawText(elecInfo, 40f, currentY + 54f, paint)

        // 7. Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText(
            "Acest document a fost generat automat de aplicația 'Index Utilități'. Are caracter strict informativ pentru evidența consumului personal.",
            30f,
            810f,
            paint
        )
    }

    private fun drawSummaryCard(
        canvas: Canvas,
        rect: RectF,
        title: String,
        accentColor: Int,
        primaryMetric: String,
        secondaryMetric: String,
        latestIndex: String
    ) {
        val paint = Paint().apply { isAntiAlias = true }

        // Background
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        // Top Accent line
        paint.color = accentColor
        canvas.drawRoundRect(RectF(rect.left, rect.top, rect.right, rect.top + 4f), 2f, 2f, paint)

        // Title
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 10.5f
        paint.isFakeBoldText = true
        canvas.drawText(title, rect.left + 10f, rect.top + 20f, paint)

        // Primary Metric
        paint.color = accentColor
        paint.textSize = 15f
        paint.isFakeBoldText = true
        canvas.drawText(primaryMetric, rect.left + 10f, rect.top + 42f, paint)

        // Secondary Metric
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText(secondaryMetric, rect.left + 10f, rect.top + 58f, paint)

        // Latest index
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText(latestIndex, rect.left + 10f, rect.top + 72f, paint)
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Raport Index Consum - ${file.name}")
                putExtra(Intent.EXTRA_TEXT, "Atașat găsiți raportul de consum și transmitere a indexului de utilități.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Trimite sau Salvează Raportul PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
