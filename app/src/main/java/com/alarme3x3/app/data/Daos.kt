package com.alarme3x3.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    suspend fun getAllOnce(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alarm: AlarmEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alarms: List<AlarmEntity>)

    @Delete
    suspend fun delete(alarm: AlarmEntity)
}

@Dao
interface ScheduleExceptionDao {
    @Query("SELECT * FROM schedule_exceptions ORDER BY dateIso ASC")
    fun observeAll(): Flow<List<ScheduleExceptionEntity>>

    @Query("SELECT * FROM schedule_exceptions")
    suspend fun getAllOnce(): List<ScheduleExceptionEntity>

    @Query("SELECT * FROM schedule_exceptions WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getByDate(dateIso: String): ScheduleExceptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScheduleExceptionEntity)

    @Query("DELETE FROM schedule_exceptions WHERE dateIso = :dateIso")
    suspend fun deleteByDate(dateIso: String)
}
