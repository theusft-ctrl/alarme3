package com.alarme3x3.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alarme3x3.app.data.ExceptionSaveResult
import com.alarme3x3.app.domain.ExceptionType
import com.alarme3x3.app.ui.theme.ScaleColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExceptionsScreen(
    title: String,
    subtitle: String,
    exceptionType: ExceptionType,
    dates: List<LocalDate>,
    onAddDate: (LocalDate) -> Unit,
    onRemoveDate: (LocalDate) -> Unit,
    onBack: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val fmt = remember { DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("pt", "BR")) }
    val accent = if (exceptionType == ExceptionType.CUSTOM_OFF) ScaleColors.CustomOff else ScaleColors.ExtraWork

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onAddDate(date)
                    }
                    showDatePicker = false
                }) { Text("Adicionar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = pickerState) }
    }

    Scaffold(
        containerColor = ScaleColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScaleColors.Background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDatePicker = true }, containerColor = accent) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar data")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(subtitle, color = ScaleColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)

            if (dates.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma data cadastrada.", color = ScaleColors.TextSecondary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dates, key = { it.toString() }) { date ->
                        Card(colors = CardDefaults.cardColors(containerColor = ScaleColors.Surface)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = date.format(fmt).replaceFirstChar { it.uppercase() },
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(onClick = { onRemoveDate(date) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = ScaleColors.Danger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConflictResolutionDialog(
    conflict: ExceptionSaveResult.Conflict,
    onChoose: (ExceptionType) -> Unit,
    onDismiss: () -> Unit
) {
    val fmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val existingLabel = if (conflict.existingType == ExceptionType.CUSTOM_OFF) {
        "Folga Personalizada"
    } else {
        "Trabalho Extra"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conflito na data ${conflict.date.format(fmt)}") },
        text = {
            Text(
                "Esta data já está cadastrada como $existingLabel. Escolha qual regra deve prevalecer para este dia:"
            )
        },
        confirmButton = {
            TextButton(onClick = { onChoose(ExceptionType.CUSTOM_OFF) }) {
                Text("Folga Personalizada")
            }
        },
        dismissButton = {
            TextButton(onClick = { onChoose(ExceptionType.EXTRA_WORK) }) {
                Text("Trabalho Extra")
            }
        }
    )
}
