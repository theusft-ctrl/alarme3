package com.alarme3x3.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alarme3x3.app.domain.ExceptionType
import com.alarme3x3.app.ui.theme.ScaleColors

object Routes {
    const val HOME = "home"
    const val SCALE = "scale"
    const val ALARMS = "alarms"
    const val CUSTOM_OFF = "custom_off"
    const val EXTRA_WORK = "extra_work"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScaleColors.Background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.startDate == null) {
        OnboardingScreen(onConfirmStartDate = viewModel::completeOnboarding)
        return
    }

    val navController = rememberNavController()

    state.pendingConflict?.let { conflict ->
        ConflictResolutionDialog(
            conflict = conflict,
            onChoose = viewModel::resolveConflict,
            onDismiss = viewModel::dismissConflict
        )
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                state = state,
                onNavigate = { route -> navController.navigate(route) },
                onPermissionsUpdated = viewModel::syncAlarms
            )
        }
        composable(Routes.SCALE) {
            ScaleCalendarScreen(
                state = state,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ALARMS) {
            AlarmsScreen(
                alarms = state.alarms,
                onSaveAlarm = viewModel::saveAlarm,
                onDeleteAlarm = viewModel::deleteAlarm,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CUSTOM_OFF) {
            ExceptionsScreen(
                title = "Minhas Folgas",
                subtitle = "Desative todos os alarmes em datas específicas sem alterar seu ciclo 3x3.",
                exceptionType = ExceptionType.CUSTOM_OFF,
                dates = state.customOffDates.sorted(),
                onAddDate = { viewModel.addException(it, ExceptionType.CUSTOM_OFF) },
                onRemoveDate = viewModel::removeException,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.EXTRA_WORK) {
            ExceptionsScreen(
                title = "Trabalho Extra",
                subtitle = "Ative todos os alarmes em um dia de folga sem deslocar a escala original.",
                exceptionType = ExceptionType.EXTRA_WORK,
                dates = state.extraWorkDates.sorted(),
                onAddDate = { viewModel.addException(it, ExceptionType.EXTRA_WORK) },
                onRemoveDate = viewModel::removeException,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                state = state,
                onUpdateStartDate = viewModel::updateStartDate,
                onToggleVibrate = viewModel::setVibrate,
                onPermissionsUpdated = viewModel::syncAlarms,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
