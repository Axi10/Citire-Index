package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.MeterReading
import com.example.data.model.UtilityConfig
import com.example.data.model.UtilityType

@Database(
    entities = [MeterReading::class, UtilityConfig::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun meterDao(): MeterDao
    abstract fun configDao(): ConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meter_readings_v6.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                // 1. Inserare Configurații
                db.execSQL("""
                    INSERT OR REPLACE INTO utility_configs (utilityType, phoneNumber, clientCode, ivrTemplate, unitPrice, reminderDayOfMonth, reminderHour, reminderMinute, isReminderEnabled)
                    VALUES ('GAS', '0800800200', '3730081', '0800800200,3730081#,,,XXXX#,,1', 3.02, 16, 9, 0, 1)
                """.trimIndent())

                db.execSQL("""
                    INSERT OR REPLACE INTO utility_configs (utilityType, phoneNumber, clientCode, ivrTemplate, unitPrice, reminderDayOfMonth, reminderHour, reminderMinute, isReminderEnabled)
                    VALUES ('ELECTRICITY', '0800070701', '111192991', '0800070701,1,,111192991#,,,1,,,,,XXXX#,,1', 1.64, 24, 9, 0, 1)
                """.trimIndent())

                val now = System.currentTimeMillis()
                val dayMs = 24L * 3600 * 1000
                val fourMonthsAgo = now - 120L * dayMs
                val threeMonthsAgo = now - 90L * dayMs
                val twoMonthsAgo = now - 60L * dayMs
                val oneMonthAgo = now - 30L * dayMs
                val sevenDaysAgo = now - 7L * dayMs

                // 2. Inserare Curent (5174, 5215, 5270, 5350, 5397)
                db.execSQL("""
                    INSERT INTO meter_readings (utilityType, indexValue, previousIndexValue, consumption, unitPrice, estimatedCost, timestamp, callSequenceUsed, isCallExecuted, notes)
                    VALUES ('ELECTRICITY', 5174.0, NULL, 0.0, 1.64, 0.0, $fourMonthsAgo, '', 1, '')
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO meter_readings (utilityType, indexValue, previousIndexValue, consumption, unitPrice, estimatedCost, timestamp, callSequenceUsed, isCallExecuted, notes)
                    VALUES ('ELECTRICITY', 5215.0, 5174.0, 41.0, 1.64, 67.24, $threeMonthsAgo, '', 1, '')
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO meter_readings (utilityType, indexValue, previousIndexValue, consumption, unitPrice, estimatedCost, timestamp, callSequenceUsed, isCallExecuted, notes)
                    VALUES ('ELECTRICITY', 5270.0, 5215.0, 55.0, 1.64, 90.20, $twoMonthsAgo, '', 1, '')
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO meter_readings (utilityType, indexValue, previousIndexValue, consumption, unitPrice, estimatedCost, timestamp, callSequenceUsed, isCallExecuted, notes)
                    VALUES ('ELECTRICITY', 5350.0, 5270.0, 80.0, 1.64, 131.20, $oneMonthAgo, '', 1, '')
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO meter_readings (utilityType, indexValue, previousIndexValue, consumption, unitPrice, estimatedCost, timestamp, callSequenceUsed, isCallExecuted, notes)
                    VALUES ('ELECTRICITY', 5397.0, 5350.0, 47.0, 1.64, 77.08, $now, '', 1, 'Transmis astăzi la robot')
                """.trimIndent())

                // 3. Inserare Gaz (2886, 2889, 2892, 2896, 2899)
                db.execSQL("""
                    INSERT INTO meter_readings (utilityType, indexValue, previousIndexValue, consumption, unitPrice, estimatedCost, timestamp, callSequenceUsed, isCallExecuted, notes)
                    VALUES ('GAS', 2886.0, NULL, 0.0, 3.02, 0.0, $fourMonthsAgo, '', 1, '')
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO meter_readings (utilityType, indexValue, previousIndexValue, consumption, unitPrice, estimatedCost, timestamp, callSequenceUsed, isCallExecuted, notes)
                    VALUES ('GAS', 2889.0, 2886.0, 3.0, 3.02, 9.06, $threeMonthsAgo, '', 1, '')
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO meter_readings (utilityType, indexValue, previousIndexValue, consumption, unitPrice, estimatedCost, timestamp, callSequenceUsed, isCallExecuted, notes)
                    VALUES ('GAS', 2892.0, 2889.0, 3.0, 3.02, 9.06, $twoMonthsAgo, '', 1, '')
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO meter_readings (utilityType, indexValue, previousIndexValue, consumption, unitPrice, estimatedCost, timestamp, callSequenceUsed, isCallExecuted, notes)
                    VALUES ('GAS', 2896.0, 2892.0, 4.0, 3.02, 12.08, $oneMonthAgo, '', 1, '')
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO meter_readings (utilityType, indexValue, previousIndexValue, consumption, unitPrice, estimatedCost, timestamp, callSequenceUsed, isCallExecuted, notes)
                    VALUES ('GAS', 2899.0, 2896.0, 3.0, 3.02, 9.06, $sevenDaysAgo, '', 1, 'Transmis la robot')
                """.trimIndent())
            }
        }
    }
}
