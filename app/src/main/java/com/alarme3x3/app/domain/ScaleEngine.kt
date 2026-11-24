package com.alarme3x3.app.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class DayStatus(val label: String, val alarmsActive: Boolean) {
    WORK("TRABALHO", true),
    OFF("FOLGA", false),
    CUSTOM_OFF("FOLGA PERSONALIZADA", false),
    EXTRA_WORK("TRABALHO EXTRA", true)
}

enum class ExceptionType {
    CUSTOM_OFF,
    EXTRA_WORK
}

data class AlarmConfig(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val soundUri: String? = null,
    val label: String = ""
) {
    val time: LocalTime get() = LocalTime.of(hour, minute)
    fun formattedTime(): String = String.format(Locale.US, "%02d:%02d", hour, minute)
}

data class ScheduleDayInfo(
    val date: LocalDate,
    val baseStatus: DayStatus,
    val effectiveStatus: DayStatus
)

data class ScheduledAlarmOccurrence(
    val alarm: AlarmConfig,
    val triggerDateTime: ZonedDateTime
)

data class ScaleSummary(
    val todayStatus: DayStatus,
    val daysUntilNextChange: Long,
    val nextChangeTargetStatus: DayStatus,
    val nextWorkDay: LocalDate?,
    val nextOffDay: LocalDate?,
    val nextAlarm: ScheduledAlarmOccurrence?
)

object ScaleEngine {

    /**
     * Ciclo contínuo 3x3 (6 dias):
     * 0, 1, 2 -> TRABALHO
     * 3, 4, 5 -> FOLGA
     */
    fun getBaseStatus(startDate: LocalDate, targetDate: LocalDate): DayStatus {
        val diffDays = ChronoUnit.DAYS.between(startDate, targetDate)
        val cycleIndex = Math.floorMod(diffDays, 6L).toInt()
        return if (cycleIndex in 0..2) DayStatus.WORK else DayStatus.OFF
    }

    /**
     * Regra estrita de prioridade:
     * 1. Folga personalizada -> desativa alarmes
     * 2. Trabalho extra -> ativa alarmes
     * 3. Ciclo original 3x3
     */
    fun getEffectiveStatus(
        startDate: LocalDate,
        targetDate: LocalDate,
        customOffDates: Set<LocalDate>,
        extraWorkDates: Set<LocalDate>
    ): DayStatus {
        if (targetDate in customOffDates) return DayStatus.CUSTOM_OFF
        if (targetDate in extraWorkDates) return DayStatus.EXTRA_WORK
        return getBaseStatus(startDate, targetDate)
    }

    fun buildPreview(
        startDate: LocalDate,
        fromDate: LocalDate,
        daysCount: Int = 30,
        customOffDates: Set<LocalDate>,
        extraWorkDates: Set<LocalDate>
    ): List<ScheduleDayInfo> {
        return (0 until daysCount).map { offset ->
            val date = fromDate.plusDays(offset.toLong())
            ScheduleDayInfo(
                date = date,
                baseStatus = getBaseStatus(startDate, date),
                effectiveStatus = getEffectiveStatus(startDate, date, customOffDates, extraWorkDates)
            )
        }
    }

    /**
     * Calcula a próxima ocorrência exata para um alarme específico, respeitando fuso horário atual.
     * Busca até 365 dias à frente (cobrindo sequências extensas de folgas personalizadas).
     */
    fun findNextOccurrenceForAlarm(
        alarm: AlarmConfig,
        startDate: LocalDate,
        customOffDates: Set<LocalDate>,
        extraWorkDates: Set<LocalDate>,
        now: ZonedDateTime = ZonedDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ScheduledAlarmOccurrence? {
        if (!alarm.enabled) return null

        val today = now.toLocalDate()
        for (offset in 0L..365L) {
            val candidateDate = today.plusDays(offset)
            val status = getEffectiveStatus(startDate, candidateDate, customOffDates, extraWorkDates)
            if (!status.alarmsActive) continue

            val candidateDateTime = ZonedDateTime.of(candidateDate, alarm.time, zoneId)
            if (candidateDateTime.isAfter(now)) {
                return ScheduledAlarmOccurrence(alarm, candidateDateTime)
            }
        }
        return null
    }

    fun findNextGlobalAlarm(
        alarms: List<AlarmConfig>,
        startDate: LocalDate,
        customOffDates: Set<LocalDate>,
        extraWorkDates: Set<LocalDate>,
        now: ZonedDateTime = ZonedDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ScheduledAlarmOccurrence? {
        return alarms
            .filter { it.enabled }
            .mapNotNull { findNextOccurrenceForAlarm(it, startDate, customOffDates, extraWorkDates, now, zoneId) }
            .minByOrNull { it.triggerDateTime.toInstant().toEpochMilli() }
    }

    fun buildSummary(
        startDate: LocalDate,
        alarms: List<AlarmConfig>,
        customOffDates: Set<LocalDate>,
        extraWorkDates: Set<LocalDate>,
        now: ZonedDateTime = ZonedDateTime.now()
    ): ScaleSummary {
        val today = now.toLocalDate()
        val todayStatus = getEffectiveStatus(startDate, today, customOffDates, extraWorkDates)
        val todayWorking = todayStatus.alarmsActive

        var daysUntilChange = 0L
        var nextChangeStatus = todayStatus
        var nextWorkDay: LocalDate? = null
        var nextOffDay: LocalDate? = null

        for (offset in 1L..365L) {
            val d = today.plusDays(offset)
            val status = getEffectiveStatus(startDate, d, customOffDates, extraWorkDates)

            if (daysUntilChange == 0L && status.alarmsActive != todayWorking) {
                daysUntilChange = offset
                nextChangeStatus = status
            }
            if (nextWorkDay == null && status.alarmsActive) {
                nextWorkDay = d
            }
            if (nextOffDay == null && !status.alarmsActive) {
                nextOffDay = d
            }
            if (daysUntilChange > 0L && nextWorkDay != null && nextOffDay != null) break
        }

        val nextAlarm = findNextGlobalAlarm(alarms, startDate, customOffDates, extraWorkDates, now)

        return ScaleSummary(
            todayStatus = todayStatus,
            daysUntilNextChange = daysUntilChange,
            nextChangeTargetStatus = nextChangeStatus,
            nextWorkDay = nextWorkDay,
            nextOffDay = nextOffDay,
            nextAlarm = nextAlarm
        )
    }
}
