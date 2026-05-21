package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

enum class AppThemePreset(
    val title: String,
    val description: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val isDark: Boolean
) {
    BOTANICAL_JADE(
        title = "Botanical Jade",
        description = "Crisp basil herb & sage garden",
        primary = Color(0xFF1E5E3A),
        secondary = Color(0xFF4CAF50),
        tertiary = Color(0xFFE65100),
        background = Color(0xFFF7FBF7),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1B2A1E),
        onSurface = Color(0xFF1B2A1E),
        surfaceVariant = Color(0xFFE8F0E9),
        onSurfaceVariant = Color(0xFF2C3E31),
        isDark = false
    ),
    WARM_APRICOT(
        title = "Warm Apricot",
        description = "Cozy kitchen sun and orange honey",
        primary = Color(0xFFD35400),
        secondary = Color(0xFFF39C12),
        tertiary = Color(0xFF27AE60),
        background = Color(0xFFFFF9F2),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF3E2723),
        onSurface = Color(0xFF3E2723),
        surfaceVariant = Color(0xFFFFF1E0),
        onSurfaceVariant = Color(0xFF5D4037),
        isDark = false
    ),
    NORDIC_BLUE(
        title = "Nordic Blueberry",
        description = "Chilled blueberry & lingonberry",
        primary = Color(0xFF1F4E79),
        secondary = Color(0xFF4A90E2),
        tertiary = Color(0xFFE08283),
        background = Color(0xFFF4F8FA),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1A2530),
        onSurface = Color(0xFF1A2530),
        surfaceVariant = Color(0xFFE3F2FD),
        onSurfaceVariant = Color(0xFF263238),
        isDark = false
    ),
    RICH_BURGUNDY(
        title = "Rich Burgundy",
        description = "Gourmet cherry & cheese bistro",
        primary = Color(0xFF800020),
        secondary = Color(0xFFC0392B),
        tertiary = Color(0xFFE67E22),
        background = Color(0xFFFAF5ED),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF2C0A11),
        onSurface = Color(0xFF2C0A11),
        surfaceVariant = Color(0xFFF2E5D5),
        onSurfaceVariant = Color(0xFF4A3B32),
        isDark = false
    ),
    SLATE_OBSIDIAN(
        title = "Slate Obsidian",
        description = "Smart ambient dark chef mode",
        primary = Color(0xFF81C784),
        secondary = Color(0xFFA5D6A7),
        tertiary = Color(0xFFFFB74D),
        background = Color(0xFF121B14),
        surface = Color(0xFF1C271E),
        onBackground = Color(0xFFE8F0E9),
        onSurface = Color(0xFFE8F0E9),
        surfaceVariant = Color(0xFF28362B),
        onSurfaceVariant = Color(0xFFA5D6A7),
        isDark = true
    )
}

@Composable
fun MyApplicationTheme(
    themePreset: AppThemePreset = AppThemePreset.BOTANICAL_JADE,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (themePreset.isDark) {
        darkColorScheme(
            primary = themePreset.primary,
            secondary = themePreset.secondary,
            tertiary = themePreset.tertiary,
            background = themePreset.background,
            surface = themePreset.surface,
            onBackground = themePreset.onBackground,
            onSurface = themePreset.onSurface,
            surfaceVariant = themePreset.surfaceVariant,
            onSurfaceVariant = themePreset.onSurfaceVariant
        )
    } else {
        lightColorScheme(
            primary = themePreset.primary,
            secondary = themePreset.secondary,
            tertiary = themePreset.tertiary,
            background = themePreset.background,
            surface = themePreset.surface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = themePreset.onBackground,
            onSurface = themePreset.onSurface,
            surfaceVariant = themePreset.surfaceVariant,
            onSurfaceVariant = themePreset.onSurfaceVariant
        )
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
