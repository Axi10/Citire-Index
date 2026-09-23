package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.UtilityType

class Converters {
    @TypeConverter
    fun fromUtilityType(value: UtilityType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toUtilityType(value: String?): UtilityType? {
        return value?.let {
            try {
                UtilityType.valueOf(it)
            } catch (e: Exception) {
                UtilityType.GAS
            }
        }
    }
}
