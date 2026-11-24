package com.alarme3x3.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alarme3x3.app.alarm.ReliabilityChecker
import com.alarme3x3.app.ui.theme.ScaleColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: AppUiState,
    onUpdateStartDate: (LocalDate) -> Unit,
    onToggleVibrate: (Boolean) -> Unit,
    onPermissionsUpdated: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var reliability by remember { mutableStateOf(ReliabilityChecker.check(context)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val fmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        reliability = ReliabilityChecker.check(context)
        onPermissionsUpdated()
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reliability = ReliabilityChecker.check(context)
                onPermissionsUpdated()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    if (showDatePicker && state.startDate != null) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onUpdateStartDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = pickerState) }
    }

    Scaffold(
        containerColor = ScaleColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScaleColors.Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                onClick = { showDatePicker = true },
                colors = CardDefaults.cardColors(containerColor = ScaleColors.Surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Data inicial da escala 3x3", fontWeight = FontWeight.SemiBold)
                    Text(
                        state.startDate?.format(fmt) ?: "Não definida",
                        color = ScaleColors.TextSecondary
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = ScaleColors.Surface)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibrar ao disparar alarme", fontWeight = FontWeight.SemiBold)
                    Switch(checked = state.vibrateEnabled, onCheckedChange = onToggleVibrate)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = ScaleColors.Surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Diagnóstico de Confiabilidade Android", fontWeight = FontWeight.Bold)
                    Text("Notificações: ${if (reliability.hasNotificationPermission) "OK" else "Pendente"}")
                    Text("Alarmes Exatos: ${if (reliability.hasExactAlarmPermission) "OK" else "Pendente"}")
                    Text("Tela Cheia no Bloqueio: ${if (reliability.hasFullScreenIntentPermission) "OK" else "Pendente"}")
                    Text(
                        "Otimização de Bateria: ${if (reliability.isIgnoringBatteryOptimizations) "Sem restrição" else "Restrita (recomendado liberar)"}"
                    )

                    if (!reliability.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        OutlinedButton(onClick = {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) {
                            Text("Permitir notificações")
                        }
                    }
                    if (!reliability.hasExactAlarmPermission) {
                        OutlinedButton(onClick = {
                            context.startActivity(ReliabilityChecker.exactAlarmSettingsIntent(context))
                        }) {
                            Text("Permitir alarmes exatos")
                        }
                    }
                    if (!reliability.hasFullScreenIntentPermission) {
                        OutlinedButton(onClick = {
                            context.startActivity(ReliabilityChecker.fullScreenIntentSettings(context))
                        }) {
                            Text("Permitir tela cheia no bloqueio")
                        }
                    }
                    if (!reliability.isIgnoringBatteryOptimizations) {
                        OutlinedButton(onClick = {
                            context.startActivity(ReliabilityChecker.batteryOptimizationSettingsIntent())
                        }) {
                            Text("Abrir configurações de bateria")
                        }
                    }
                }
            }
        }
    }
}
