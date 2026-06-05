package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgColor = Color(0xFF0F172A)
val PanelBg = Color(0xFF1E293B)
val TextMain = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)
val AccentRun = Color(0xFF06B6D4)
val AccentRest = Color(0xFFF59E0B)
val AccentSpeed = Color(0xFFF43F5E)
val AccentDist = Color(0xFF10B981)

private val YoYoColorScheme =
  darkColorScheme(
    primary = AccentRun,
    secondary = AccentDist,
    tertiary = AccentSpeed,
    background = BgColor,
    surface = PanelBg,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextMain,
    onSurface = TextMain,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = TextMain,
    error = AccentSpeed,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Forcing our custom dark theme for this tracker dashboard app
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = YoYoColorScheme, typography = Typography, content = content)
}
