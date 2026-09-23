package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "utility_configs")
data class UtilityConfig(
    @PrimaryKey
    val utilityType: UtilityType,
    val phoneNumber: String,
    val clientCode: String,
    val ivrTemplate: String,
    val unitPrice: Double,
    val reminderDayOfMonth: Int,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val isReminderEnabled: Boolean = true
)
