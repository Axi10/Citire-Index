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

            private suspend fun populateInitialConfigs(configDao: ConfigDao, meterDao: MeterDao) {
                // Initial configs for Gas and Electricity
                configDao.saveConfig(
                    UtilityConfig(
                        utilityType = UtilityType.GAS,
                        phoneNumber = UtilityType.GAS.defaultPhone,
                        clientCode = UtilityType.GAS.defaultClientCode,
                        ivrTemplate = UtilityType.GAS.defaultIvrTemplate,
                        unitPrice = UtilityType.GAS.defaultPrice,
                        reminderDayOfMonth = UtilityType.GAS.defaultDay,
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
                        unitPrice = UtilityType.ELECTRICITY.defaultPrice,
                        reminderDayOfMonth = UtilityType.ELECTRICITY.defaultDay,
                        reminderHour = 9,
                        reminderMinute = 0,
                        isReminderEnabled = true
                    )
                )

                // Populate a couple of realistic previous readings so history and charts have rich initial context
                val now = System.currentTimeMillis()
                val oneMonthAgo = now - 30L * 24 * 3600 * 1000
                val twoMonthsAgo = now - 60L * 24 * 3600 * 1000

                // Gas historical readings
                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.GAS,
                        indexValue = 1420.0,
                        previousIndexValue = 1380.0,
                        consumption = 40.0,
                        unitPrice = 0.31,
                        estimatedCost = 40.0 * 0.31,
                        timestamp = twoMonthsAgo,
                        callSequenceUsed = "0800800200,3730081#,,,1420#,,1",
                        isCallExecuted = true,
                        notes = "Citire index vară"
                    )
                )
                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.GAS,
                        indexValue = 1465.0,
                        previousIndexValue = 1420.0,
                        consumption = 45.0,
                        unitPrice = 0.31,
                        estimatedCost = 45.0 * 0.31,
                        timestamp = oneMonthAgo,
                        callSequenceUsed = "0800800200,3730081#,,,1465#,,1",
                        isCallExecuted = true,
                        notes = "Transmitere automată"
                    )
                )

                // Electricity historical readings
                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.ELECTRICITY,
                        indexValue = 5120.0,
                        previousIndexValue = 4980.0,
                        consumption = 140.0,
                        unitPrice = 0.80,
                        estimatedCost = 140.0 * 0.80,
                        timestamp = twoMonthsAgo,
                        callSequenceUsed = "0800070701,1,,111192991#,,,1,,,,,5120#,,1",
                        isCallExecuted = true,
                        notes = "Citire precedentă"
                    )
                )
                meterDao.insertReading(
                    MeterReading(
                        utilityType = UtilityType.ELECTRICITY,
                        indexValue = 5275.0,
                        previousIndexValue = 5120.0,
                        consumption = 155.0,
                        unitPrice = 0.80,
                        estimatedCost = 155.0 * 0.80,
                        timestamp = oneMonthAgo,
                        callSequenceUsed = "0800070701,1,,111192991#,,,1,,,,,5275#,,1",
                        isCallExecuted = true,
                        notes = "Transmitere automată Curent"
                    )
                )
            }
        }
    }
}
