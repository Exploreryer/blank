package com.example.blank.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BlankScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = PureWhite,
    background = CanvasWhite,
    onBackground = InkBlack,
    surface = PureWhite,
    onSurface = InkBlack,
    surfaceVariant = SoftLine
)

@Composable
fun BlankTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlankScheme,
        content = content
    )
}
