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

                // Default settings for each utility. The client code starts empty (the user types it
                // in Settings) and the reading history starts empty too.
                for (type in UtilityType.values()) {
                    db.execSQL(
                        "INSERT OR REPLACE INTO utility_configs " +
                            "(utilityType, phoneNumber, clientCode, ivrTemplate, unitPrice, reminderDayOfMonth, reminderHour, reminderMinute, isReminderEnabled) " +
                            "VALUES ('${type.name}', '${type.defaultPhone}', '${type.defaultClientCode}', " +
                            "'${type.defaultIvrTemplate}', ${type.defaultPrice}, ${type.defaultDay}, 9, 0, 1)"
                    )
                }
            }
        }
    }
}
