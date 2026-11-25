package com.fworld.fos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FosDark = darkColorScheme(
    primary = Color(0xFFC7BFFF),
    onPrimary = Color(0xFF2B1F7B),
    primaryContainer = Color(0xFF42379A),
    onPrimaryContainer = Color(0xFFE5DEFF),
    secondary = Color(0xFFC9C3DC),
    onSecondary = Color(0xFF312E41),
    secondaryContainer = Color(0xFF484459),
    onSecondaryContainer = Color(0xFFE6DFF9),
    tertiary = Color(0xFFFFB0CD),
    onTertiary = Color(0xFF541D36),
    tertiaryContainer = Color(0xFF6F334C),
    onTertiaryContainer = Color(0xFFFFD9E5),
    background = Color(0xFF141318),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF141318),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF47454F),
    onSurfaceVariant = Color(0xFFC9C5D0),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1C1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B292F),
    surfaceContainerHighest = Color(0xFF36343A),
    outline = Color(0xFF928F99),
    outlineVariant = Color(0xFF47454F),
    error = Color(0xFFFFB4AB),
)

private val FosLight = lightColorScheme(
    primary = Color(0xFF5A4FCF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF160066),
    secondary = Color(0xFF5F5C71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5DFF9),
    onSecondaryContainer = Color(0xFF1C192B),
    tertiary = Color(0xFF8C4A64),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E5),
    onTertiaryContainer = Color(0xFF39071F),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE5E0EC),
    onSurfaceVariant = Color(0xFF47454F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F2FA),
    surfaceContainer = Color(0xFFF0ECF4),
    surfaceContainerHigh = Color(0xFFEAE6EE),
    surfaceContainerHighest = Color(0xFFE5E1E9),
    outline = Color(0xFF79767F),
    outlineVariant = Color(0xFFC9C5D0),
    error = Color(0xFFBA1A1A),
)

private val FosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

/** Material 3 Expressive: expressive motion scheme + крупные скруглённые формы */
@Composable
fun FosTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialExpressiveTheme(
        colorScheme = if (dark) FosDark else FosLight,
        motionScheme = MotionScheme.expressive(),
        shapes = FosShapes,
        content = content,
    )
}
