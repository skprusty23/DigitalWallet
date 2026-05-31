package com.digitalwallet.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary          = WalletNavy700,
    onPrimary        = WalletCard,
    primaryContainer = Color(0xFFD8E6FF),
    onPrimaryContainer = WalletNavy900,
    secondary        = WalletTeal500,
    onSecondary      = WalletCard,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF004D40),
    tertiary         = WalletGold500,
    onTertiary       = WalletNavy900,
    error            = WalletRed500,
    background       = WalletSurface,
    onBackground     = WalletNavy900,
    surface          = WalletCard,
    onSurface        = WalletNavy900,
    surfaceVariant   = Color(0xFFEEF2FF),
    onSurfaceVariant = TextSecondary,
    outline          = Divider
)

private val DarkColorScheme = darkColorScheme(
    primary          = WalletNavy400,
    onPrimary        = WalletNavy900,
    primaryContainer = WalletNavy800,
    onPrimaryContainer = WalletNavy400,
    secondary        = WalletTeal300,
    onSecondary      = Color(0xFF004D40),
    tertiary         = WalletGold300,
    onTertiary       = WalletNavy900,
    error            = WalletRed400,
    background       = DarkBackground,
    onBackground     = Color(0xFFE8EEFF),
    surface          = DarkSurface,
    onSurface        = Color(0xFFE8EEFF),
    surfaceVariant   = DarkCard,
    onSurfaceVariant = Color(0xFFB0BEC5)
)

@Composable
fun DigitalWalletTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = WalletTypography, content = content)
}
