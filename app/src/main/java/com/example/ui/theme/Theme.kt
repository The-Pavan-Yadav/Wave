package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.services.AppSettings

private val BaseDarkColorScheme =
  darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Color(0xFF070B14), // Modern pitch AMOLED dark
    surface = Color(0xFF101726),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF9FAFB),
    onSurface = Color(0xFFF9FAFB),
    onSurfaceVariant = Color(0xFF9CA3AF)
  )

private val BaseLightColorScheme =
  lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Color(0xFFF3F4F6),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val primaryColor = Color(0xFF8B5CF6)
  val secondaryColor = Color(0xFF3B82F6)

  val colorScheme = BaseDarkColorScheme.copy(
    primary = primaryColor,
    secondary = secondaryColor
  )

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
