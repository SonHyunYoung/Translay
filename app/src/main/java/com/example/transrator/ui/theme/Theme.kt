package com.example.transrator.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.transrator.data.local.ThemeManager

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlue,
    secondary = BrandBlue,
    tertiary = BrandBlue
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    secondary = BrandBlue,
    tertiary = BrandBlue
)

@Composable
fun TransratorTheme(
    darkTheme: Boolean = when (ThemeManager.mode.collectAsState().value) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}