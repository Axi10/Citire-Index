package com.example.unit

import com.example.data.model.MeterReading
import com.example.data.model.UtilityType
import com.example.pdf.CsvExporter
import com.example.pdf.CsvImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CsvRoundTripTest {

    private fun at(month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, month, day, 10, 30, 0)
        }.timeInMillis

    @Test
    fun exportedFile_isReadBackWithTheSameValues() {
        val original = listOf(
            MeterReading(
                utilityType = UtilityType.ELECTRICITY,
                indexValue = 5350.0,
                previousIndexValue = 5270.0,
                consumption = 80.0,
                unitPrice = 1.64,
                estimatedCost = 131.2,
                timestamp = at(Calendar.AUGUST, 24),
                isCallExecuted = true
            ),
            MeterReading(
                utilityType = UtilityType.GAS,
                indexValue = 2899.0,
                previousIndexValue = null,
                consumption = 0.0,
                unitPrice = 3.02,
                estimatedCost = 0.0,
                timestamp = at(Calendar.SEPTEMBER, 16),
                isCallExecuted = false
            )
        )

        val parsed = CsvImporter.parse(CsvExporter.buildCsv(original))

        assertEquals(0, parsed.skippedLines)
        assertEquals(2, parsed.readings.size)

        // The export is sorted by date: electricity (24 Aug) first, then gas (16 Sep)
        val electricity = parsed.readings[0]
        assertEquals(UtilityType.ELECTRICITY, electricity.utilityType)
        assertEquals(5350.0, electricity.indexValue, 0.0001)
        assertEquals(5270.0, electricity.previousIndexValue ?: -1.0, 0.0001)
        assertEquals(80.0, electricity.consumption, 0.0001)
        assertEquals(1.64, electricity.unitPrice, 0.0001)
        assertEquals(131.2, electricity.estimatedCost, 0.0001)
        assertEquals(at(Calendar.AUGUST, 24), electricity.timestamp)
        assertTrue(electricity.isCallExecuted)

        val gas = parsed.readings[1]
        assertEquals(UtilityType.GAS, gas.utilityType)
        assertEquals(2899.0, gas.indexValue, 0.0001)
        assertEquals(null, gas.previousIndexValue)
        assertFalse(gas.isCallExecuted)
    }

    @Test
    fun invalidLines_areSkippedNotFatal() {
        val text = "Data;Utilitate;Index;Index anterior;Consum;Pret unitar;Cost estimat;Apel efectuat\r\n" +
            "24.08.2026 10:30;Curent;5350;5270;80,0;1,64;131,20;da\r\n" +
            "not a date;Curent;5400;5350;50,0;1,64;82,00;da\r\n" +
            "25.08.2026 10:30;Apa;5400;5350;50,0;1,64;82,00;da\r\n" +
            "26.08.2026 10:30;Gaz;abc;;;;;nu\r\n" +
            "too;few;columns\r\n"

        val parsed = CsvImporter.parse(text)

        assertEquals(1, parsed.readings.size)
        assertEquals(4, parsed.skippedLines)
    }

    @Test
    fun quotedFieldsAndByteOrderMark_areAccepted() {
        val text = "﻿\"Data\";\"Utilitate\";\"Index\";\"Index anterior\";\"Consum\";\"Pret unitar\";\"Cost estimat\";\"Apel efectuat\"\r\n" +
            "\"24.08.2026 10:30\";\"Gaz\";\"2899\";\"2896\";\"3,0\";\"3,02\";\"9,06\";\"da\"\r\n"

        val parsed = CsvImporter.parse(text)

        assertEquals(1, parsed.readings.size)
        assertEquals(0, parsed.skippedLines)
        assertEquals(2899.0, parsed.readings[0].indexValue, 0.0001)
        assertEquals(9.06, parsed.readings[0].estimatedCost, 0.0001)
    }
}
