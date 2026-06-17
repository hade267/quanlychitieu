package com.example.ui.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DeepOrangePrimary,
    onPrimary = Color.Black,
    primaryContainer = SoftOrangeContainer,
    onPrimaryContainer = OrangeHighlight,
    secondary = IncomeBlue,
    onSecondary = Color.White,
    tertiary = SuccessGreen,
    background = SpaceSlateDark,
    onBackground = Color.White,
    surface = SolidCardBg,
    onSurface = Color.White,
    surfaceVariant = GlassCardBg,
    onSurfaceVariant = WhiteOpacity70,
    outline = GlassBorder,
    error = ErrorRed
  )

private val LightColorScheme = DarkColorScheme // Always default to the elegant dark neon aesthetic

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force the beautiful dark orange theme as per prototype
  dynamicColor: Boolean = false, // Keep colors cohesive and custom
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
