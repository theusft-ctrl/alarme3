package com.alarme3x3.app.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
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
import com.alarme3x3.app.alarm.AlarmSoundTester
import com.alarme3x3.app.domain.AlarmConfig
import com.alarme3x3.app.ui.theme.ScaleColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    alarms: List<AlarmConfig>,
    onSaveAlarm: (AlarmConfig) -> Unit,
    onDeleteAlarm: (AlarmConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tester = remember { AlarmSoundTester(context) }
    var testingAlarmId by remember { mutableStateOf<Long?>(null) }
    var editingAlarm by remember { mutableStateOf<AlarmConfig?>(null) }
    var soundTargetAlarm by remember { mutableStateOf<AlarmConfig?>(null) }

    DisposableEffect(Unit) {
        onDispose { tester.stop() }
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            soundTargetAlarm?.let { target ->
                onSaveAlarm(target.copy(soundUri = uri?.toString()))
            }
        }
        soundTargetAlarm = null
    }

    editingAlarm?.let { current ->
        AlarmTimeDialog(
            initial = current,
            onDismiss = { editingAlarm = null },
            onConfirm = { updated ->
                onSaveAlarm(updated)
                editingAlarm = null
            }
        )
    }

    Scaffold(
        containerColor = ScaleColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Meus Alarmes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScaleColors.Background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingAlarm = AlarmConfig(hour = 7, minute = 0, label = "Novo alarme")
            }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar alarme")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(alarms, key = { it.id }) { alarm ->
                val soundName = remember(alarm.soundUri) {
                    if (alarm.soundUri == null) {
                        "Som padrão do alarme"
                    } else {
                        runCatching {
                            RingtoneManager.getRingtone(context, Uri.parse(alarm.soundUri))?.getTitle(context)
                        }.getOrNull() ?: "Som personalizado"
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = ScaleColors.Surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    alarm.formattedTime(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    alarm.label.ifBlank { "Alarme da escala" },
                                    color = ScaleColors.TextSecondary
                                )
                                Text(
                                    soundName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ScaleColors.TextSecondary
                                )
                            }
                            Switch(
                                checked = alarm.enabled,
                                onCheckedChange = { onSaveAlarm(alarm.copy(enabled = it)) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = { editingAlarm = alarm },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Horário")
                            }
                            TextButton(
                                onClick = {
                                    soundTargetAlarm = alarm
                                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                        alarm.soundUri?.let {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                        }
                                    }
                                    ringtoneLauncher.launch(intent)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Som")
                            }
                            TextButton(
                                onClick = {
                                    if (testingAlarmId == alarm.id) {
                                        tester.stop()
                                        testingAlarmId = null
                                    } else {
                                        tester.play(alarm.soundUri)
                                        testingAlarmId = alarm.id
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    if (testingAlarmId == alarm.id) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    null,
                                    Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (testingAlarmId == alarm.id) "Parar" else "Testar")
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { onDeleteAlarm(alarm) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = ScaleColors.Danger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmTimeDialog(
    initial: AlarmConfig,
    onDismiss: () -> Unit,
    onConfirm: (AlarmConfig) -> Unit
) {
    val timeState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true
    )
    var label by remember { mutableStateOf(initial.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Novo alarme" else "Editar alarme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TimePicker(state = timeState)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nome do alarme") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(initial.copy(hour = timeState.hour, minute = timeState.minute, label = label))
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
