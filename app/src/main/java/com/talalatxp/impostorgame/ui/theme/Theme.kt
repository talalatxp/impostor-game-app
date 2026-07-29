package com.talalatxp.impostorgame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB), secondary = Color(0xFFE7BDB8), tertiary = Color(0xFFE6C38A),
    background = Color(0xFF171211), surface = Color(0xFF201A19), onPrimary = Color(0xFF690008),
)
private val LightScheme = lightColorScheme(
    primary = Color(0xFFB3261E), secondary = Color(0xFF775651), tertiary = Color(0xFF715B2E),
    background = Color(0xFFFFF8F6), surface = Color(0xFFFFF8F6),
)

@Composable fun ImpostorTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = DarkScheme, content = content)
