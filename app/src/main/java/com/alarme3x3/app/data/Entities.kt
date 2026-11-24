package com.alarme3x3.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alarme3x3.app.domain.AlarmConfig
import com.alarme3x3.app.domain.ExceptionType

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val soundUri: String? = null,
    val label: String = ""
) {
    fun toDomain() = AlarmConfig(id, hour, minute, enabled, soundUri, label)
}

@Entity(tableName = "schedule_exceptions")
data class ScheduleExceptionEntity(
    @PrimaryKey val dateIso: String,
    val type: String,
    val note: String = ""
) {
    fun exceptionType(): ExceptionType = ExceptionType.valueOf(type)
}
