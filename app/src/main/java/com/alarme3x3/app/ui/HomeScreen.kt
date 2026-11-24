package com.alarme3x3.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alarme3x3.app.alarm.ReliabilityChecker
import com.alarme3x3.app.ui.theme.ScaleColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: AppUiState,
    onNavigate: (String) -> Unit,
    onPermissionsUpdated: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var reliability by remember { mutableStateOf(ReliabilityChecker.check(context)) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
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

    val summary = state.summary ?: return
    val locale = remember { Locale("pt", "BR") }
    val fullDateFmt = remember { DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", locale) }
    val shortDateFmt = remember { DateTimeFormatter.ofPattern("dd/MM (EEE)", locale) }
    val alarmFmt = remember { DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm", locale) }
    val statusColor = ScaleColors.forStatus(summary.todayStatus)

    Scaffold(
        containerColor = ScaleColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Alarme 3x3", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScaleColors.Background),
                actions = {
                    IconButton(onClick = { onNavigate(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (!reliability.allCriticalOk) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScaleColors.Danger.copy(alpha = 0.16f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Atenção: Permissões de Alarme",
                                fontWeight = FontWeight.Bold,
                                color = ScaleColors.Danger
                            )
                            Text(
                                "Para garantir que os alarmes toquem com o app fechado, conceda as permissões nativas do Android.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!reliability.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    TextButton(onClick = {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }) {
                                        Text("Notificações")
                                    }
                                }
                                if (!reliability.hasExactAlarmPermission) {
                                    TextButton(onClick = {
                                        context.startActivity(ReliabilityChecker.exactAlarmSettingsIntent(context))
                                    }) {
                                        Text("Alarmes Exatos")
                                    }
                                }
                                if (!reliability.hasFullScreenIntentPermission) {
                                    TextButton(onClick = {
                                        context.startActivity(ReliabilityChecker.fullScreenIntentSettings(context))
                                    }) {
                                        Text("Tela Cheia")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ScaleColors.Surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = LocalDate.now().format(fullDateFmt).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ScaleColors.TextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = summary.todayStatus.label,
                                color = statusColor,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        HorizontalDivider(color = ScaleColors.SurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricColumn(
                                title = "Próximo alarme",
                                value = summary.nextAlarm?.triggerDateTime?.format(alarmFmt) ?: "Nenhum ativo"
                            )
                            MetricColumn(
                                title = "Mudança de escala",
                                value = if (summary.daysUntilNextChange == 1L) "Amanhã" else "Em ${summary.daysUntilNextChange} dias"
                            )
                        }
                        MetricColumn(
                            title = "Próximo dia de trabalho",
                            value = summary.nextWorkDay?.format(shortDateFmt) ?: "—"
                        )
                    }
                }
            }

            item {
                Text(
                    "Acesso rápido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionCard(
                            title = "Minha Escala",
                            subtitle = "Próximos 30 dias",
                            icon = Icons.Default.CalendarMonth,
                            accent = ScaleColors.Work,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Routes.SCALE) }
                        )
                        QuickActionCard(
                            title = "Meus Alarmes",
                            subtitle = "${state.alarms.count { it.enabled }} ativos",
                            icon = Icons.Default.Alarm,
                            accent = ScaleColors.Off,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Routes.ALARMS) }
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionCard(
                            title = "Minhas Folgas",
                            subtitle = "${state.customOffDates.size} cadastradas",
                            icon = Icons.Default.BeachAccess,
                            accent = ScaleColors.CustomOff,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Routes.CUSTOM_OFF) }
                        )
                        QuickActionCard(
                            title = "Trabalho Extra",
                            subtitle = "${state.extraWorkDates.size} cadastrados",
                            icon = Icons.Default.WorkHistory,
                            accent = ScaleColors.ExtraWork,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Routes.EXTRA_WORK) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelMedium, color = ScaleColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ScaleColors.Surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = title, tint = accent)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ScaleColors.TextSecondary)
        }
    }
}
