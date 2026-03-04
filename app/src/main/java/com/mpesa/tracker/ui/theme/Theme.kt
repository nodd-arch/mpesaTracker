package com.mpesa.tracker.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MpesaGreen = Color(0xFF00963D)
val MpesaGreenDark = Color(0xFF006B2B)
val MpesaGreenLight = Color(0xFF4CAF78)
val MpesaBackground = Color(0xFFF5FFF8)
val MpesaSurface = Color(0xFFFFFFFF)
val MpesaOnGreen = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF666666)
val CardBg = Color(0xFFFFFFFF)
val DividerColor = Color(0xFFE8F5EE)

private val LightColorScheme = lightColorScheme(
    primary = MpesaGreen,
    onPrimary = MpesaOnGreen,
    primaryContainer = Color(0xFFD0F5E0),
    onPrimaryContainer = MpesaGreenDark,
    secondary = Color(0xFF4CAF78),
    onSecondary = Color.White,
    background = MpesaBackground,
    surface = MpesaSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Color(0xFFCCE5D5),
    surfaceVariant = Color(0xFFE8F5EE),
)

@Composable
fun MpesaTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}
