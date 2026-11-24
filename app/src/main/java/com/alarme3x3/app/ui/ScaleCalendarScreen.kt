package com.alarme3x3.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alarme3x3.app.domain.DayStatus
import com.alarme3x3.app.ui.theme.ScaleColors
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleCalendarScreen(state: AppUiState, onBack: () -> Unit) {
    val summary = state.summary ?: return
    val locale = remember { Locale("pt", "BR") }
    val dayFmt = remember { DateTimeFormatter.ofPattern("dd/MM", locale) }
    val dowFmt = remember { DateTimeFormatter.ofPattern("EEE", locale) }
    val alarmFmt = remember { DateTimeFormatter.ofPattern("dd/MM HH:mm", locale) }

    Scaffold(
        containerColor = ScaleColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Minha Escala (30 dias)") },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ScaleColors.Surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Próxima mudança de escala: em ${summary.daysUntilNextChange} dia(s)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Próximo dia de trabalho: ${summary.nextWorkDay?.format(dayFmt) ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Próxima folga: ${summary.nextOffDay?.format(dayFmt) ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Próximo alarme: ${summary.nextAlarm?.triggerDateTime?.format(alarmFmt) ?: "Nenhum"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LegendDot("Trabalho", ScaleColors.Work)
                LegendDot("Folga", ScaleColors.Off)
                LegendDot("F. Pers.", ScaleColors.CustomOff)
                LegendDot("Extra", ScaleColors.ExtraWork)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.preview30Days) { day ->
                    val color = ScaleColors.forStatus(day.effectiveStatus)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.18f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                day.date.format(dowFmt).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = ScaleColors.TextSecondary
                            )
                            Text(
                                day.date.format(dayFmt),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                            Text(
                                text = when (day.effectiveStatus) {
                                    DayStatus.WORK -> "TRAB"
                                    DayStatus.OFF -> "FOLGA"
                                    DayStatus.CUSTOM_OFF -> "F.PER"
                                    DayStatus.EXTRA_WORK -> "EXTRA"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = ScaleColors.TextSecondary)
    }
}
