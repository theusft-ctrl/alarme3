package com.alarme3x3.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alarme3x3.app.data.ExceptionSaveResult
import com.alarme3x3.app.data.ScheduleRepository
import com.alarme3x3.app.domain.AlarmConfig
import com.alarme3x3.app.domain.ExceptionType
import com.alarme3x3.app.domain.ScaleEngine
import com.alarme3x3.app.domain.ScaleSummary
import com.alarme3x3.app.domain.ScheduleDayInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

data class AppUiState(
    val isLoading: Boolean = true,
    val startDate: LocalDate? = null,
    val alarms: List<AlarmConfig> = emptyList(),
    val customOffDates: Set<LocalDate> = emptySet(),
    val extraWorkDates: Set<LocalDate> = emptySet(),
    val preview30Days: List<ScheduleDayInfo> = emptyList(),
    val summary: ScaleSummary? = null,
    val vibrateEnabled: Boolean = true,
    val pendingConflict: ExceptionSaveResult.Conflict? = null
)

class MainViewModel(private val repo: ScheduleRepository) : ViewModel() {

    private val conflictState = MutableStateFlow<ExceptionSaveResult.Conflict?>(null)

    val uiState: StateFlow<AppUiState> = combine(
        repo.startDateFlow,
        repo.alarmsFlow,
        repo.exceptionsFlow,
        repo.vibrateEnabledFlow,
        conflictState
    ) { startDate, alarms, exceptions, vibrate, conflict ->
        val customOff = exceptions
            .filter { it.exceptionType() == ExceptionType.CUSTOM_OFF }
            .map { LocalDate.parse(it.dateIso) }
            .toSet()

        val extraWork = exceptions
            .filter { it.exceptionType() == ExceptionType.EXTRA_WORK }
            .map { LocalDate.parse(it.dateIso) }
            .toSet()

        val now = ZonedDateTime.now()
        val today = now.toLocalDate()

        val preview = startDate?.let {
            ScaleEngine.buildPreview(it, today, 30, customOff, extraWork)
        } ?: emptyList()

        val summary = startDate?.let {
            ScaleEngine.buildSummary(it, alarms, customOff, extraWork, now)
        }

        AppUiState(
            isLoading = false,
            startDate = startDate,
            alarms = alarms,
            customOffDates = customOff,
            extraWorkDates = extraWork,
            preview30Days = preview,
            summary = summary,
            vibrateEnabled = vibrate,
            pendingConflict = conflict
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    fun completeOnboarding(firstWorkDate: LocalDate) = viewModelScope.launch {
        repo.initializeFirstUse(firstWorkDate)
    }

    fun updateStartDate(date: LocalDate) = viewModelScope.launch {
        repo.updateStartDate(date)
    }

    fun setVibrate(enabled: Boolean) = viewModelScope.launch {
        repo.setVibrateEnabled(enabled)
    }

    fun saveAlarm(alarm: AlarmConfig) = viewModelScope.launch {
        repo.saveAlarm(alarm)
    }

    fun deleteAlarm(alarm: AlarmConfig) = viewModelScope.launch {
        repo.deleteAlarm(alarm)
    }

    fun addException(date: LocalDate, type: ExceptionType) = viewModelScope.launch {
        val result = repo.addException(date, type, overwrite = false)
        if (result is ExceptionSaveResult.Conflict) {
            conflictState.value = result
        }
    }

    fun resolveConflict(chosenType: ExceptionType) = viewModelScope.launch {
        val conflict = conflictState.value ?: return@launch
        repo.addException(conflict.date, chosenType, overwrite = true)
        conflictState.value = null
    }

    fun dismissConflict() {
        conflictState.value = null
    }

    fun removeException(date: LocalDate) = viewModelScope.launch {
        repo.removeException(date)
    }

    fun syncAlarms() = viewModelScope.launch {
        repo.rescheduleAlarms()
    }
}

class MainViewModelFactory(private val repo: ScheduleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(repo) as T
    }
}
