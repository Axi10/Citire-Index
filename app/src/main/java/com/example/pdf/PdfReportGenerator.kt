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

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val ROW_HEIGHT = 22f

    // Table rows must end above this line; the footer sits below it
    private const val CONTENT_BOTTOM = 790f
    private const val CONTINUATION_TOP = 40f

    private const val COL_DATE = 30f
    private const val COL_TYPE = 120f
    private const val COL_INDEX = 200f
    private const val COL_DELTA = 275f
    private const val COL_PRICE = 345f
    private const val COL_COST = 420f
    private const val COL_STATUS = 490f

    fun generateMonthlyReport(
        context: Context,
        periodName: String,
        readings: List<MeterReading>,
        gasConfig: UtilityConfig?,
        electricityConfig: UtilityConfig?
    ): File? {
        val pdfDocument = PdfDocument()
        return try {
            val sorted = readings.sortedByDescending { it.timestamp }

            var pageNumber = 1
            var page = pdfDocument.startPage(newPageInfo(pageNumber))
            var canvas: Canvas = page.canvas

            var y = drawFirstPageHeader(canvas, periodName, sorted)
            y = drawTableHeader(canvas, y)

            if (sorted.isEmpty()) {
                val paint = Paint().apply { isAntiAlias = true }
                paint.color = Color.rgb(148, 163, 184)
                paint.textSize = 11f
                canvas.drawText("Nu există înregistrări pentru perioada selectată.", 40f, y + 30f, paint)
                y += 60f
            }

            for ((index, item) in sorted.withIndex()) {
                if (y + ROW_HEIGHT > CONTENT_BOTTOM) {
                    drawFooter(canvas, pageNumber)
                    pdfDocument.finishPage(page)
                    pageNumber++
                    page = pdfDocument.startPage(newPageInfo(pageNumber))
                    canvas = page.canvas
                    y = drawTableHeader(canvas, CONTINUATION_TOP)
                }
                drawRow(canvas, item, y, index)
                y += ROW_HEIGHT
            }

            // Table bottom border
            val linePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1f
            }
            canvas.drawLine(30f, y, 565f, y, linePaint)

            // Contracts box needs 70 points; start a new page if it does not fit
            var infoTop = y + 30f
            if (infoTop + 70f > CONTENT_BOTTOM) {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)
                pageNumber++
                page = pdfDocument.startPage(newPageInfo(pageNumber))
                canvas = page.canvas
                infoTop = CONTINUATION_TOP
            }
            drawContractInfo(canvas, infoTop, gasConfig, electricityConfig)

            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)

            // Save file; reports from earlier runs are not needed any more
            val outputDir = File(context.cacheDir, "reports").apply { mkdirs() }
            outputDir.listFiles()?.forEach { if (it.name.endsWith(".pdf")) it.delete() }

            val sanitizedPeriod = periodName.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').lowercase(Locale.ROOT)
            val file = File(outputDir, "Raport_Consum_${sanitizedPeriod}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { pdfDocument.writeTo(it) }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            // close() throws if a page was left unfinished by an earlier failure
            try {
                pdfDocument.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun newPageInfo(pageNumber: Int): PdfDocument.PageInfo =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create() // A4 in points

    /**
     * Draws the dark header, the three summary cards and the section title.
     * Returns the Y position where the table header starts.
     */
    private fun drawFirstPageHeader(
        canvas: Canvas,
        periodName: String,
        readings: List<MeterReading>
    ): Float {
        val paint = Paint().apply { isAntiAlias = true }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        // 1. Header background
        paint.color = Color.rgb(15, 23, 42) // Dark Slate #0F172A
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

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

        drawSummaryCard(
            canvas = canvas,
            rect = RectF(30f, currentY, 205f, currentY + 80f),
            title = "Gaze Naturale",
            accentColor = Color.rgb(2, 132, 199), // Blue
            primaryMetric = String.format(Locale.US, "%.1f m³", totalGasConsumption),
            secondaryMetric = String.format(Locale.US, "Cost: %.2f LEI", totalGasCost),
            latestIndex = String.format(Locale.US, "Ultimul index: %.0f", latestGasIndex)
        )

        drawSummaryCard(
            canvas = canvas,
            rect = RectF(215f, currentY, 390f, currentY + 80f),
            title = "Curent Electric",
            accentColor = Color.rgb(245, 158, 11), // Amber
            primaryMetric = String.format(Locale.US, "%.1f kWh", totalElecConsumption),
            secondaryMetric = String.format(Locale.US, "Cost: %.2f LEI", totalElecCost),
            latestIndex = String.format(Locale.US, "Ultimul index: %.0f", latestElecIndex)
        )

        drawSummaryCard(
            canvas = canvas,
            rect = RectF(400f, currentY, 565f, currentY + 80f),
            title = "Total Estimativ",
            accentColor = Color.rgb(16, 185, 129), // Emerald
            primaryMetric = String.format(Locale.US, "%.2f LEI", totalGeneralCost),
            secondaryMetric = "Total de plată estimat",
            latestIndex = "${readings.size} citiri înregistrate"
        )

        currentY += 105f

        // 3. Section Title
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("DETALII TRANSMITERE ȘI CONSUM", 30f, currentY, paint)

        return currentY + 15f
    }

    /** Draws the table header at [y] and returns the Y of the first row. */
    private fun drawTableHeader(canvas: Canvas, y: Float): Float {
        val paint = Paint().apply { isAntiAlias = true }

        paint.color = Color.rgb(241, 245, 249) // Light grey table header
        canvas.drawRect(30f, y, 565f, y + 24f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9.5f
        paint.isFakeBoldText = true
        canvas.drawText("DATĂ", COL_DATE + 6f, y + 16f, paint)
        canvas.drawText("UTILITATE", COL_TYPE, y + 16f, paint)
        canvas.drawText("INDEX", COL_INDEX, y + 16f, paint)
        canvas.drawText("CONSUM", COL_DELTA, y + 16f, paint)
        canvas.drawText("PREȚ/UNIT", COL_PRICE, y + 16f, paint)
        canvas.drawText("COST (LEI)", COL_COST, y + 16f, paint)
        canvas.drawText("APEL IVR", COL_STATUS, y + 16f, paint)

        return y + 24f
    }

    private fun drawRow(canvas: Canvas, item: MeterReading, y: Float, index: Int) {
        val paint = Paint().apply { isAntiAlias = true }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        // alternating background
        if (index % 2 == 1) {
            paint.color = Color.rgb(248, 250, 252)
            canvas.drawRect(30f, y, 565f, y + ROW_HEIGHT, paint)
        }

        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 9.5f

        canvas.drawText(dateFormat.format(Date(item.timestamp)), COL_DATE + 6f, y + 15f, paint)

        val typeLabel = if (item.utilityType == UtilityType.GAS) "Gaz (m³)" else "Curent (kWh)"
        canvas.drawText(typeLabel, COL_TYPE, y + 15f, paint)

        canvas.drawText(String.format(Locale.US, "%.0f", item.indexValue), COL_INDEX, y + 15f, paint)

        val deltaText = if (item.consumption > 0) "+${String.format(Locale.US, "%.1f", item.consumption)}" else "-"
        canvas.drawText(deltaText, COL_DELTA, y + 15f, paint)

        canvas.drawText(String.format(Locale.US, "%.2f", item.unitPrice), COL_PRICE, y + 15f, paint)

        paint.isFakeBoldText = true
        canvas.drawText(String.format(Locale.US, "%.2f", item.estimatedCost), COL_COST, y + 15f, paint)
        paint.isFakeBoldText = false

        val statusText = if (item.isCallExecuted) "Efectuat" else "Salvat"
        paint.color = if (item.isCallExecuted) Color.rgb(22, 163, 74) else Color.rgb(100, 116, 139)
        canvas.drawText(statusText, COL_STATUS, y + 15f, paint)
    }

    private fun drawContractInfo(
        canvas: Canvas,
        top: Float,
        gasConfig: UtilityConfig?,
        electricityConfig: UtilityConfig?
    ) {
        val paint = Paint().apply { isAntiAlias = true }

        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(30f, top, 565f, top + 70f), 8f, 8f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 9.5f
        paint.isFakeBoldText = true
        canvas.drawText("Informații Contracte & Secvențe IVR:", 40f, top + 20f, paint)

        paint.isFakeBoldText = false
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        val gasDay = gasConfig?.reminderDayOfMonth ?: UtilityType.GAS.defaultDay
        val elecDay = electricityConfig?.reminderDayOfMonth ?: UtilityType.ELECTRICITY.defaultDay
        val gasInfo = "Gaz: TelVerde ${gasConfig?.phoneNumber ?: UtilityType.GAS.defaultPhone} | Cod Client: ${gasConfig?.clientCode ?: UtilityType.GAS.defaultClientCode} | Termen: $gasDay ale lunii"
        val elecInfo = "Curent: TelVerde ${electricityConfig?.phoneNumber ?: UtilityType.ELECTRICITY.defaultPhone} | Cod Client: ${electricityConfig?.clientCode ?: UtilityType.ELECTRICITY.defaultClientCode} | Termen: $elecDay ale lunii"
        canvas.drawText(gasInfo, 40f, top + 38f, paint)
        canvas.drawText(elecInfo, 40f, top + 54f, paint)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText(
            "Acest document a fost generat automat de aplicația 'Index Utilități'. Are caracter strict informativ pentru evidența consumului personal.",
            30f,
            810f,
            paint
        )
        canvas.drawText("Pagina $pageNumber", 30f, 824f, paint)
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
