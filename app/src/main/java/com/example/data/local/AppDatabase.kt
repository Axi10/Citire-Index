package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.MeterReading
import com.example.data.model.UtilityConfig

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

                // Default settings for each utility. The reading history starts empty:
                // the user's real indexes are entered from the app.
                db.execSQL("""
                    INSERT OR REPLACE INTO utility_configs (utilityType, phoneNumber, clientCode, ivrTemplate, unitPrice, reminderDayOfMonth, reminderHour, reminderMinute, isReminderEnabled)
                    VALUES ('GAS', '0800800200', '3730081', '0800800200,3730081#,,,XXXX#,,1', 3.02, 16, 9, 0, 1)
                """.trimIndent())

                db.execSQL("""
                    INSERT OR REPLACE INTO utility_configs (utilityType, phoneNumber, clientCode, ivrTemplate, unitPrice, reminderDayOfMonth, reminderHour, reminderMinute, isReminderEnabled)
                    VALUES ('ELECTRICITY', '0800070701', '111192991', '0800070701,1,,111192991#,,,1,,,,,XXXX#,,1', 1.64, 24, 9, 0, 1)
                """.trimIndent())
            }
        }
    }
}
