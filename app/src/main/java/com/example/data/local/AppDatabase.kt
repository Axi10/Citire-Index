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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [MeterReading::class, UtilityConfig::class],
    version = 2,
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
                    "meter_reading_database"
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
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialConfigs(database.configDao(), database.meterDao())
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialConfigs(database.configDao(), database.meterDao())
                    }
                }
            }

            private suspend fun populateInitialConfigs(configDao: ConfigDao, meterDao: MeterDao) {
                // Initial configs for Gas and Electricity with user's exact rates
                configDao.saveConfig(
                    UtilityConfig(
                        utilityType = UtilityType.GAS,
                        phoneNumber = UtilityType.GAS.defaultPhone,
                        clientCode = UtilityType.GAS.defaultClientCode,
                        ivrTemplate = UtilityType.GAS.defaultIvrTemplate,
                        unitPrice = 3.02,
                        reminderDayOfMonth = 16,
                        reminderHour = 9,
                        reminderMinute = 0,
                        isReminderEnabled = true
                    )
                )

                configDao.saveConfig(
                    UtilityConfig(
                        utilityType = UtilityType.ELECTRICITY,
                        phoneNumber = UtilityType.ELECTRICITY.defaultPhone,
                        clientCode = UtilityType.ELECTRICITY.defaultClientCode,
                        ivrTemplate = UtilityType.ELECTRICITY.defaultIvrTemplate,
                        unitPrice = 1.64,
                        reminderDayOfMonth = 24,
                        reminderHour = 9,
                        reminderMinute = 0,
                        isReminderEnabled = true
                    )
                )

                val dayMs = 24L * 3600 * 1000
                val now = System.currentTimeMillis()
                val fourMonthsAgo = now - 120L * dayMs
                val threeMonthsAgo = now - 90L * dayMs
                val twoMonthsAgo = now - 60L * dayMs
                val oneMonthAgo = now - 30L * dayMs

                // ============================================
                // CURENT (Indexuri: 5174, 5215, 5270, 5350, 5397 | Preț: 1.64 lei/kWh)
                // ============================================
                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.ELECTRICITY,
                        indexValue = 5174.0,
                        previousIndexValue = null,
                        consumption = 0.0,
                        unitPrice = 1.64,
                        estimatedCost = 0.0,
                        timestamp = fourMonthsAgo,
                        isCallExecuted = true,
                        notes = "Citire inițială"
                    )
                )

                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.ELECTRICITY,
                        indexValue = 5215.0,
                        previousIndexValue = 5174.0,
                        consumption = 41.0,
                        unitPrice = 1.64,
                        estimatedCost = 41.0 * 1.64,
                        timestamp = threeMonthsAgo,
                        isCallExecuted = true,
                        notes = "Transmitere lunară curent"
                    )
                )

                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.ELECTRICITY,
                        indexValue = 5270.0,
                        previousIndexValue = 5215.0,
                        consumption = 55.0,
                        unitPrice = 1.64,
                        estimatedCost = 55.0 * 1.64,
                        timestamp = twoMonthsAgo,
                        isCallExecuted = true,
                        notes = "Transmitere lunară curent"
                    )
                )

                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.ELECTRICITY,
                        indexValue = 5350.0,
                        previousIndexValue = 5270.0,
                        consumption = 80.0,
                        unitPrice = 1.64,
                        estimatedCost = 80.0 * 1.64,
                        timestamp = oneMonthAgo,
                        isCallExecuted = true,
                        notes = "Transmitere lunară curent"
                    )
                )

                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.ELECTRICITY,
                        indexValue = 5397.0,
                        previousIndexValue = 5350.0,
                        consumption = 47.0,
                        unitPrice = 1.64,
                        estimatedCost = 47.0 * 1.64,
                        timestamp = now,
                        isCallExecuted = true,
                        notes = "Transmis astăzi la robot (23 Septembrie)"
                    )
                )

                // ============================================
                // GAZ (Indexuri: 2886, 2889, 2892, 2896, 2899 | Preț: 3.02 lei/m³)
                // ============================================
                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.GAS,
                        indexValue = 2886.0,
                        previousIndexValue = null,
                        consumption = 0.0,
                        unitPrice = 3.02,
                        estimatedCost = 0.0,
                        timestamp = fourMonthsAgo,
                        isCallExecuted = true,
                        notes = "Citire inițială"
                    )
                )

                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.GAS,
                        indexValue = 2889.0,
                        previousIndexValue = 2886.0,
                        consumption = 3.0,
                        unitPrice = 3.02,
                        estimatedCost = 3.0 * 3.02,
                        timestamp = threeMonthsAgo,
                        isCallExecuted = true,
                        notes = "Transmitere lunară gaz"
                    )
                )

                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.GAS,
                        indexValue = 2892.0,
                        previousIndexValue = 2889.0,
                        consumption = 3.0,
                        unitPrice = 3.02,
                        estimatedCost = 3.0 * 3.02,
                        timestamp = twoMonthsAgo,
                        isCallExecuted = true,
                        notes = "Transmitere lunară gaz"
                    )
                )

                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.GAS,
                        indexValue = 2896.0,
                        previousIndexValue = 2892.0,
                        consumption = 4.0,
                        unitPrice = 3.02,
                        estimatedCost = 4.0 * 3.02,
                        timestamp = oneMonthAgo,
                        isCallExecuted = true,
                        notes = "Transmitere lunară gaz"
                    )
                )

                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.GAS,
                        indexValue = 2899.0,
                        previousIndexValue = 2896.0,
                        consumption = 3.0,
                        unitPrice = 3.02,
                        estimatedCost = 3.0 * 3.02,
                        timestamp = now - 5L * dayMs,
                        isCallExecuted = true,
                        notes = "Ultima transmitere gaz"
                    )
                )
            }
        }
    }
}
