package com.example.unit

import com.example.data.model.MeterReading
import com.example.data.model.UtilityType
import com.example.data.stats.InsightsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InsightsCalculatorTest {

    private val now = 1_800_000_000_000L
    private val dayMs = 24L * 3600 * 1000

    private fun reading(daysAgo: Int, index: Double, previous: Double?, consumption: Double, cost: Double) = MeterReading(
        utilityType = UtilityType.ELECTRICITY,
        indexValue = index,
        previousIndexValue = previous,
        consumption = consumption,
        unitPrice = 1.5,
        estimatedCost = cost,
        timestamp = now - daysAgo * dayMs
    )

    @Test
    fun noReadings_givesEmptyInsights() {
        val result = InsightsCalculator.compute(emptyList(), now)
        assertEquals(0, result.readingsCounted)
        assertEquals(0.0, result.averageConsumption, 0.0001)
        assertNull(result.lastConsumption)
        assertNull(result.changePercent)
    }

    @Test
    fun firstReadingEver_isNotCountedInTheAverage() {
        val result = InsightsCalculator.compute(listOf(reading(0, 100.0, null, 0.0, 0.0)), now)
        assertEquals(0, result.readingsCounted)
        assertNull(result.lastConsumption)
    }

    @Test
    fun averageAndChange_areComputedFromConsumptionReadings() {
        val readings = listOf(
            reading(90, 100.0, null, 0.0, 0.0),
            reading(60, 140.0, 100.0, 40.0, 60.0),
            reading(30, 190.0, 140.0, 50.0, 75.0),
            reading(0, 250.0, 190.0, 60.0, 90.0)
        )
        val result = InsightsCalculator.compute(readings, now)

        assertEquals(3, result.readingsCounted)
        assertEquals(50.0, result.averageConsumption, 0.0001)
        assertEquals(60.0, result.lastConsumption ?: -1.0, 0.0001)
        assertNotNull(result.changePercent)
        assertEquals(20.0, result.changePercent ?: -1.0, 0.0001)
        assertEquals(225.0, result.costLast12Months, 0.0001)
    }

    @Test
    fun order_ofTheInputList_doesNotMatter() {
        val readings = listOf(
            reading(0, 250.0, 190.0, 60.0, 90.0),
            reading(60, 140.0, 100.0, 40.0, 60.0),
            reading(30, 190.0, 140.0, 50.0, 75.0)
        )
        val result = InsightsCalculator.compute(readings, now)
        assertEquals(60.0, result.lastConsumption ?: -1.0, 0.0001)
        assertEquals(20.0, result.changePercent ?: -1.0, 0.0001)
    }

    @Test
    fun costOlderThanAYear_isNotCounted() {
        val readings = listOf(
            reading(400, 100.0, 50.0, 50.0, 500.0),
            reading(10, 150.0, 100.0, 50.0, 75.0)
        )
        val result = InsightsCalculator.compute(readings, now)
        assertEquals(75.0, result.costLast12Months, 0.0001)
    }

    @Test
    fun changeIsUnknown_whenThePreviousConsumptionWasZero() {
        val readings = listOf(
            reading(60, 100.0, 100.0, 0.0, 0.0),
            reading(30, 150.0, 100.0, 50.0, 75.0)
        )
        val result = InsightsCalculator.compute(readings, now)
        assertNull(result.changePercent)
    }
}
