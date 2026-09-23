package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UtilityConfig
import com.example.data.model.UtilityType
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM utility_configs WHERE utilityType = :type")
    fun getConfig(type: UtilityType): Flow<UtilityConfig?>

    @Query("SELECT * FROM utility_configs WHERE utilityType = :type")
    suspend fun getConfigSync(type: UtilityType): UtilityConfig?

    @Query("SELECT * FROM utility_configs")
    fun getAllConfigs(): Flow<List<UtilityConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: UtilityConfig)
}
