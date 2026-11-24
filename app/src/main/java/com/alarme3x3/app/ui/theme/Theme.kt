package com.alarme3x3.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.alarme3x3.app.domain.DayStatus

object ScaleColors {
    val Background = Color(0xFF0B0F17)
    val Surface = Color(0xFF141B29)
    val SurfaceVariant = Color(0xFF1E293B)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)

    val Work = Color(0xFF3B82F6)
    val Off = Color(0xFF10B981)
    val CustomOff = Color(0xFFF59E0B)
    val ExtraWork = Color(0xFFA855F7)
    val Danger = Color(0xFFEF4444)

    fun forStatus(status: DayStatus): Color = when (status) {
        DayStatus.WORK -> Work
        DayStatus.OFF -> Off
        DayStatus.CUSTOM_OFF -> CustomOff
        DayStatus.EXTRA_WORK -> ExtraWork
    }
}

private val AppDarkScheme = darkColorScheme(
    primary = ScaleColors.Work,
    secondary = ScaleColors.Off,
    tertiary = ScaleColors.ExtraWork,
    background = ScaleColors.Background,
    surface = ScaleColors.Surface,
    surfaceVariant = ScaleColors.SurfaceVariant,
    onBackground = ScaleColors.TextPrimary,
    onSurface = ScaleColors.TextPrimary
)

@Composable
fun Alarme3x3Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppDarkScheme, content = content)
}
