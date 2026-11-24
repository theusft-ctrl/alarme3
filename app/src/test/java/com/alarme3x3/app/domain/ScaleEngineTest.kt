package com.alarme3x3.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ScaleEngineTest {

    @Test
    fun baseCycleFollowsContinuous3x3PatternAcrossMonthsAndLeapYear() {
        val start = LocalDate.of(2024, 2, 27) // 2024 é ano bissexto (29/02)
        assertEquals(DayStatus.WORK, ScaleEngine.getBaseStatus(start, LocalDate.of(2024, 2, 27)))
        assertEquals(DayStatus.WORK, ScaleEngine.getBaseStatus(start, LocalDate.of(2024, 2, 28)))
        assertEquals(DayStatus.WORK, ScaleEngine.getBaseStatus(start, LocalDate.of(2024, 2, 29)))
        assertEquals(DayStatus.OFF, ScaleEngine.getBaseStatus(start, LocalDate.of(2024, 3, 1)))
        assertEquals(DayStatus.OFF, ScaleEngine.getBaseStatus(start, LocalDate.of(2024, 3, 2)))
        assertEquals(DayStatus.OFF, ScaleEngine.getBaseStatus(start, LocalDate.of(2024, 3, 3)))
        assertEquals(DayStatus.WORK, ScaleEngine.getBaseStatus(start, LocalDate.of(2024, 3, 4)))
    }

    @Test
    fun customOffDoesNotShiftOriginalCycle() {
        val start = LocalDate.of(2026, 11, 2) // Seg: trab, Ter: trab, Qua: trab, Qui-Sab: folga
        val customOff = setOf(LocalDate.of(2026, 11, 2))
        assertEquals(
            DayStatus.CUSTOM_OFF,
            ScaleEngine.getEffectiveStatus(start, LocalDate.of(2026, 11, 2), customOff, emptySet())
        )
        assertEquals(
            DayStatus.WORK,
            ScaleEngine.getEffectiveStatus(start, LocalDate.of(2026, 11, 3), customOff, emptySet())
        )
        assertEquals(
            DayStatus.OFF,
            ScaleEngine.getEffectiveStatus(start, LocalDate.of(2026, 11, 5), customOff, emptySet())
        )
    }

    @Test
    fun extraWorkActivatesAlarmsOnlyOnSelectedDateWithoutShiftingCycle() {
        val start = LocalDate.of(2026, 11, 2) // Seg-Qua trab, Qui-Sab folga, Dom trab
        val extraWork = setOf(LocalDate.of(2026, 11, 6)) // Sexta (folga original)
        assertEquals(
            DayStatus.OFF,
            ScaleEngine.getEffectiveStatus(start, LocalDate.of(2026, 11, 5), emptySet(), extraWork)
        )
        assertEquals(
            DayStatus.EXTRA_WORK,
            ScaleEngine.getEffectiveStatus(start, LocalDate.of(2026, 11, 6), emptySet(), extraWork)
        )
        assertEquals(
            DayStatus.OFF,
            ScaleEngine.getEffectiveStatus(start, LocalDate.of(2026, 11, 7), emptySet(), extraWork)
        )
        assertEquals(
            DayStatus.WORK,
            ScaleEngine.getEffectiveStatus(start, LocalDate.of(2026, 11, 8), emptySet(), extraWork)
        )
    }

    @Test
    fun customOffTakesPriorityOverExtraWorkIfBothPresent() {
        val date = LocalDate.of(2026, 11, 6)
        assertEquals(
            DayStatus.CUSTOM_OFF,
            ScaleEngine.getEffectiveStatus(
                startDate = LocalDate.of(2026, 11, 2),
                targetDate = date,
                customOffDates = setOf(date),
                extraWorkDates = setOf(date)
            )
        )
    }

    @Test
    fun nextAlarmSkipsOffDaysAndTriggersOnExtraWorkDay() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val start = LocalDate.of(2026, 11, 2) // Seg-Qua trab, Qui-Sab folga
        val extraWork = setOf(LocalDate.of(2026, 11, 6)) // Sexta
        val alarm = AlarmConfig(id = 1, hour = 6, minute = 0, enabled = true)
        val now = ZonedDateTime.of(2026, 11, 5, 10, 0, 0, 0, zone) // Quinta 10:00

        val next = ScaleEngine.findNextOccurrenceForAlarm(
            alarm = alarm,
            startDate = start,
            customOffDates = emptySet(),
            extraWorkDates = extraWork,
            now = now,
            zoneId = zone
        )

        assertNotNull(next)
        assertEquals(LocalDate.of(2026, 11, 6), next!!.triggerDateTime.toLocalDate())
    }
}
