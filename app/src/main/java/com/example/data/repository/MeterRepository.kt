package com.example.data.repository

import com.example.data.local.ConfigDao
import com.example.data.local.MeterDao
import com.example.data.model.MeterReading
import com.example.data.model.UtilityConfig
import com.example.data.model.UtilityType
import kotlinx.coroutines.flow.Flow

class MeterRepository(
    private val meterDao: MeterDao,
    private val configDao: ConfigDao
) {
    val allReadings: Flow<List<MeterReading>> = meterDao.getAllReadings()

    fun getReadingsByType(type: UtilityType): Flow<List<MeterReading>> =
        meterDao.getReadingsByType(type)

    fun getLatestReading(type: UtilityType): Flow<MeterReading?> =
        meterDao.getLatestReading(type)

    suspend fun getLatestReadingSync(type: UtilityType): MeterReading? =
        meterDao.getLatestReadingSync(type)

    suspend fun insertReading(reading: MeterReading): Long =
        meterDao.insertReading(reading)

    suspend fun deleteReading(reading: MeterReading) =
        meterDao.deleteReading(reading)

    suspend fun deleteById(id: Long) =
        meterDao.deleteById(id)

    suspend fun updateReading(reading: MeterReading) =
        meterDao.updateReading(reading)

    suspend fun getReadingsBetween(startTime: Long, endTime: Long): List<MeterReading> =
        meterDao.getReadingsBetween(startTime, endTime)

    fun getConfig(type: UtilityType): Flow<UtilityConfig?> =
        configDao.getConfig(type)

    suspend fun getConfigSync(type: UtilityType): UtilityConfig? =
        configDao.getConfigSync(type)

    fun getAllConfigs(): Flow<List<UtilityConfig>> =
        configDao.getAllConfigs()

    suspend fun saveConfig(config: UtilityConfig) =
        configDao.saveConfig(config)
}
