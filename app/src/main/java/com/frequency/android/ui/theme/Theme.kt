package com.frequency.android.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

// Colors that match the iOS frequency theme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF39FF14),
    onPrimary = Color.Black,
    secondary = Color(0xFF8CA8FF),
    onSecondary = Color(0xFF12172A),
    background = Color(0xFF07110A),
    onBackground = Color(0xFF39FF14),
    surface = Color(0xCC102018),
    onSurface = Color(0xFFE8FFE3),
    surfaceVariant = Color(0x9917281E),
    outline = Color(0x6639FF14),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2A0D0D)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF008000),
    onPrimary = Color.White,
    secondary = Color(0xFF4056B4),
    onSecondary = Color.White,
    background = Color(0xFFF4FAF4),
    onBackground = Color(0xFF102A14),
    surface = Color(0xE6FFFFFF),
    onSurface = Color(0xFF102A14),
    surfaceVariant = Color(0xBBDFF1DF),
    outline = Color(0x66008000),
    error = Color(0xFFB42318),
    onError = Color.White
)

@Composable
fun FrequencyTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    // App-level override from ThemePreferenceManager
    val themePref by ThemePreferenceManager.themeFlow.collectAsState(initial = ThemePreference.System)
    val shouldUseDark = when (darkTheme) {
        true -> true
        false -> false
        null -> when (themePref) {
            ThemePreference.Dark -> true
            ThemePreference.Light -> false
            ThemePreference.System -> isSystemInDarkTheme()
        }
    }

    val colorScheme = if (shouldUseDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    if (!shouldUseDark) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = if (!shouldUseDark) {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else 0
            }
            window.navigationBarColor = colorScheme.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
