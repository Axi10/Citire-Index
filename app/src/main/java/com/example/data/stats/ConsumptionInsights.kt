package com.example.data.stats

import com.example.data.model.MeterReading

data class ConsumptionInsights(
    val readingsCounted: Int,
    val averageConsumption: Double,
    val lastConsumption: Double?,
    val changePercent: Double?,
    val costLast12Months: Double
)

object InsightsCalculator {

    private const val YEAR_MS = 365L * 24 * 3600 * 1000

    /**
     * Summarises the readings of ONE utility (do not mix gas and electricity).
     * The first reading ever has no previous index, so it carries no consumption and is not
     * counted in the average.
     */
    fun compute(readings: List<MeterReading>, nowMillis: Long = System.currentTimeMillis()): ConsumptionInsights {
        val sorted = readings.sortedBy { it.timestamp }
        val withConsumption = sorted.filter { it.previousIndexValue != null }

        val average = if (withConsumption.isEmpty()) 0.0 else withConsumption.map { it.consumption }.average()
        val last = withConsumption.lastOrNull()?.consumption
        val previous = if (withConsumption.size >= 2) withConsumption[withConsumption.size - 2].consumption else null

        val change = if (last != null && previous != null && previous > 0.0) {
            (last - previous) / previous * 100.0
        } else {
            null
        }

        val cost = sorted.filter { it.timestamp >= nowMillis - YEAR_MS }.sumOf { it.estimatedCost }

        return ConsumptionInsights(
            readingsCounted = withConsumption.size,
            averageConsumption = average,
            lastConsumption = last,
            changePercent = change,
            costLast12Months = cost
        )
    }
}
