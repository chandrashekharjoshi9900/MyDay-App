package com.myday.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MyDayPalette {
    val VioletPrimary = Color(0xFF8E7CFF)
    val VioletMuted = Color(0xFF282442)
    val BlueSecondary = Color(0xFF5298FF)
    val BlueMuted = Color(0xFF1D2D46)
    val GreenSuccess = Color(0xFF2FD180)
    val GreenMuted = Color(0xFF173628)
    val AmberWarning = Color(0xFFF59E0B)
    val CoralError = Color(0xFFF46472)

    // Dark Charcoal surfaces
    val CharcoalBackground = Color(0xFF0E1015)
    val CharcoalSurface = Color(0xFF161922)
    val CharcoalSurfaceElevated = Color(0xFF1E222E)
    val CharcoalOutline = Color(0xFF2B3040)
    val DarkTextPrimary = Color(0xFFF3F4F8)
    val DarkTextSecondary = Color(0xFFA0A7BA)

    // Light surfaces
    val LightBackground = Color(0xFFF5F6FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceElevated = Color(0xFFECEFF6)
    val LightOutline = Color(0xFFD8DEE9)
    val LightTextPrimary = Color(0xFF121521)
    val LightTextSecondary = Color(0xFF5D657A)

    val AccentSwatches = listOf(
        0xFF8E7CFFL,
        0xFF5298FFL,
        0xFF2FD180L,
        0xFF38BDF8L,
        0xFFF59E0BL,
        0xFFF46472L,
        0xFFE879F9L
    )
}

data class MyDayExtendedColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val cardBorder: Color,
    val elevatedSurface: Color,
    val isDark: Boolean
)

private val LocalExtendedColors = staticCompositionLocalOf {
    MyDayExtendedColors(
        success = MyDayPalette.GreenSuccess,
        successContainer = MyDayPalette.GreenMuted,
        warning = MyDayPalette.AmberWarning,
        cardBorder = MyDayPalette.CharcoalOutline,
        elevatedSurface = MyDayPalette.CharcoalSurfaceElevated,
        isDark = true
    )
}

val LocalHapticsEnabled = staticCompositionLocalOf { true }

object MyDayThemeExtras {
    val colors: MyDayExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}

@Composable
fun rememberMyDayHaptic(): () -> Unit {
    val enabled = LocalHapticsEnabled.current
    val haptic: HapticFeedback = LocalHapticFeedback.current
    return {
        if (enabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}

private val DarkColors = darkColorScheme(
    primary = MyDayPalette.VioletPrimary,
    onPrimary = Color(0xFF120D29),
    primaryContainer = MyDayPalette.VioletMuted,
    onPrimaryContainer = Color(0xFFE6E1FF),
    secondary = MyDayPalette.BlueSecondary,
    onSecondary = Color(0xFF0A1D38),
    secondaryContainer = MyDayPalette.BlueMuted,
    onSecondaryContainer = Color(0xFFD8E8FF),
    tertiary = MyDayPalette.GreenSuccess,
    onTertiary = Color(0xFF062615),
    tertiaryContainer = MyDayPalette.GreenMuted,
    onTertiaryContainer = Color(0xFFC5F9DC),
    background = MyDayPalette.CharcoalBackground,
    onBackground = MyDayPalette.DarkTextPrimary,
    surface = MyDayPalette.CharcoalSurface,
    onSurface = MyDayPalette.DarkTextPrimary,
    surfaceVariant = MyDayPalette.CharcoalSurfaceElevated,
    onSurfaceVariant = MyDayPalette.DarkTextSecondary,
    outline = MyDayPalette.CharcoalOutline,
    outlineVariant = Color(0xFF222634),
    error = MyDayPalette.CoralError,
    onError = Color(0xFF2A090E)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6B56F5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECE9FF),
    onPrimaryContainer = Color(0xFF23176B),
    secondary = Color(0xFF2C7BE5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1EEFF),
    onSecondaryContainer = Color(0xFF0E2C57),
    tertiary = Color(0xFF18A962),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9F7E7),
    onTertiaryContainer = Color(0xFF083820),
    background = MyDayPalette.LightBackground,
    onBackground = MyDayPalette.LightTextPrimary,
    surface = MyDayPalette.LightSurface,
    onSurface = MyDayPalette.LightTextPrimary,
    surfaceVariant = MyDayPalette.LightSurfaceElevated,
    onSurfaceVariant = MyDayPalette.LightTextSecondary,
    outline = MyDayPalette.LightOutline,
    outlineVariant = Color(0xFFE5E9F2),
    error = Color(0xFFD93F50),
    onError = Color.White
)

private val MyDayTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 14.sp
    )
)

private val MyDayShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyDayTheme(
    darkTheme: Boolean = true,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extended = if (darkTheme) {
        MyDayExtendedColors(
            success = MyDayPalette.GreenSuccess,
            successContainer = MyDayPalette.GreenMuted,
            warning = MyDayPalette.AmberWarning,
            cardBorder = MyDayPalette.CharcoalOutline,
            elevatedSurface = MyDayPalette.CharcoalSurfaceElevated,
            isDark = true
        )
    } else {
        MyDayExtendedColors(
            success = Color(0xFF18A962),
            successContainer = Color(0xFFD9F7E7),
            warning = Color(0xFFD97706),
            cardBorder = MyDayPalette.LightOutline,
            elevatedSurface = MyDayPalette.LightSurfaceElevated,
            isDark = false
        )
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extended,
        LocalHapticsEnabled provides hapticsEnabled
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MyDayTypography,
            shapes = MyDayShapes,
            content = content
        )
    }
}
