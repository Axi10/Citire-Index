package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meter_readings")
data class MeterReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val utilityType: UtilityType,
    val indexValue: Double,
    val previousIndexValue: Double? = null,
    val consumption: Double = 0.0,
    val unitPrice: Double = 0.0,
    val estimatedCost: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val callSequenceUsed: String = "",
    val isCallExecuted: Boolean = false,
    val notes: String = ""
)
