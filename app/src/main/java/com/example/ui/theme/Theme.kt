package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val RetroColorScheme = darkColorScheme(
    primary = RetroNeonCyan,
    onPrimary = RetroDark,
    primaryContainer = RetroPurple,
    secondary = RetroNeonPink,
    tertiary = RetroNeonGreen,
    background = RetroDark,
    surface = RetroSurface,
    surfaceVariant = BlockyGray,
    onBackground = RetroNeonCyan,
    onSurface = RetroNeonGreen,
    error = ArcadeRed
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamicColor to force Retro Theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = RetroColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
