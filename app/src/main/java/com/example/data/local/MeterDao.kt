package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MeterReading
import com.example.data.model.UtilityType
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterDao {
    @Query("SELECT * FROM meter_readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<MeterReading>>

    @Query("SELECT * FROM meter_readings WHERE utilityType = :type ORDER BY timestamp DESC")
    fun getReadingsByType(type: UtilityType): Flow<List<MeterReading>>

    @Query("SELECT * FROM meter_readings WHERE utilityType = :type ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReading(type: UtilityType): Flow<MeterReading?>

    @Query("SELECT * FROM meter_readings WHERE utilityType = :type ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestReadingSync(type: UtilityType): MeterReading?

    @Query("SELECT * FROM meter_readings WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getReadingsBetween(startTime: Long, endTime: Long): List<MeterReading>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReading): Long

    @Update
    suspend fun updateReading(reading: MeterReading)

    @Delete
    suspend fun deleteReading(reading: MeterReading)

    @Query("DELETE FROM meter_readings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM meter_readings")
    suspend fun deleteAllReadings()
}
