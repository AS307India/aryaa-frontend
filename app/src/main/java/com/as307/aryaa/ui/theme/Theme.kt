package com.as307.aryaa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = AryaaColors.Saffron,
    onPrimary = AryaaColors.White,
    secondary = AryaaColors.Emerald,
    background = AryaaColors.Navy,
    surface = AryaaColors.NavyCard,
    error = AryaaColors.Crimson,
    outline = AryaaColors.NavyBorder,
    onBackground = AryaaColors.White,
    onSurfaceVariant = AryaaColors.Slate
)

@Composable
fun AryaaTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAryaaMonoTextStyle provides AryaaMono
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = AryaaTypography,
            shapes = AryaaShapes,
            content = content
        )
    }
}
