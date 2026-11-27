package com.myday.app.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
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

/**
 * Unslop-refined design tokens for MYDAY:
 * - Graphite neutral ramp with quiet 8% hairlines
 * - Three text roles: Primary, Secondary, Muted
 * - Single primary violet accent + toned semantic indicators
 * - 4dp spacing grid, tabular numerals, single ease-out motion curve
 */
object MyDayPalette {
    val VioletPrimary = Color(0xFF8E7CFF)
    val VioletPrimaryLight = Color(0xFF6350E8)
    val VioletContainerDark = Color(0xFF232033)
    val VioletContainerLight = Color(0xFFEDEAFE)

    val BlueSecondary = Color(0xFF5298FF)
    val BlueSecondaryLight = Color(0xFF2B6FD6)
    val BlueContainerDark = Color(0xFF1A2230)

    val GreenSuccess = Color(0xFF4CC38A)
    val GreenSuccessLight = Color(0xFF16A34A)
    val GreenContainerDark = Color(0xFF172921)
    val GreenContainerLight = Color(0xFFE7F7EE)

    val AmberWarning = Color(0xFFE5A93B)
    val CoralError = Color(0xFFE5484D)
    val CoralErrorLight = Color(0xFFDC2626)

    // Graphite Dark Ramp
    val DarkDeepest = Color(0xFF0F0F10)
    val DarkBackground = Color(0xFF141415)
    val DarkCard = Color(0xFF19191B)
    val DarkRaised = Color(0xFF232325)
    val DarkTextPrimary = Color(0xFFEDEDED)
    val DarkTextSecondary = Color(0xFFA1A1A6)
    val DarkTextMuted = Color(0xFF85858B)
    val DarkHairline = Color(0x14EDEDED) // 8% primary text

    // Graphite Light Ramp
    val LightBackground = Color(0xFFFAFAFA)
    val LightCard = Color(0xFFFFFFFF)
    val LightRaised = Color(0xFFF4F4F5)
    val LightTextPrimary = Color(0xFF171717)
    val LightTextSecondary = Color(0xFF525252)
    val LightTextMuted = Color(0xFF737373)
    val LightHairline = Color(0x14171717) // 8% primary text

    val AccentSwatches = listOf(
        0xFF8E7CFFL,
        0xFF5298FFL,
        0xFF4CC38AL,
        0xFFE5A93BL,
        0xFFE5484DL,
        0xFF737373L
    )
}

object MyDayMotion {
    val EaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    const val FAST_MS = 160
    const val MEDIUM_MS = 240
    const val CHART_MS = 360
}

data class MyDayExtendedColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val cardBorder: Color,
    val elevatedSurface: Color,
    val navBarSurface: Color,
    val mutedText: Color,
    val isDark: Boolean
)

private val LocalExtendedColors = staticCompositionLocalOf {
    MyDayExtendedColors(
        success = MyDayPalette.GreenSuccess,
        successContainer = MyDayPalette.GreenContainerDark,
        warning = MyDayPalette.AmberWarning,
        cardBorder = MyDayPalette.DarkHairline,
        elevatedSurface = MyDayPalette.DarkRaised,
        navBarSurface = MyDayPalette.DarkDeepest,
        mutedText = MyDayPalette.DarkTextMuted,
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
    onPrimary = Color(0xFF100E1B),
    primaryContainer = MyDayPalette.VioletContainerDark,
    onPrimaryContainer = Color(0xFFE5E1FF),
    secondary = MyDayPalette.BlueSecondary,
    onSecondary = Color(0xFF0A172B),
    secondaryContainer = MyDayPalette.BlueContainerDark,
    onSecondaryContainer = Color(0xFFDCE8FA),
    tertiary = MyDayPalette.GreenSuccess,
    onTertiary = Color(0xFF072115),
    tertiaryContainer = MyDayPalette.GreenContainerDark,
    onTertiaryContainer = Color(0xFFCFF5DF),
    background = MyDayPalette.DarkBackground,
    onBackground = MyDayPalette.DarkTextPrimary,
    surface = MyDayPalette.DarkCard,
    onSurface = MyDayPalette.DarkTextPrimary,
    surfaceVariant = MyDayPalette.DarkRaised,
    onSurfaceVariant = MyDayPalette.DarkTextSecondary,
    outline = MyDayPalette.DarkHairline,
    outlineVariant = MyDayPalette.DarkHairline,
    error = MyDayPalette.CoralError,
    onError = Color(0xFF280A0C)
)

private val LightColors = lightColorScheme(
    primary = MyDayPalette.VioletPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = MyDayPalette.VioletContainerLight,
    onPrimaryContainer = Color(0xFF221866),
    secondary = MyDayPalette.BlueSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7EFFC),
    onSecondaryContainer = Color(0xFF0F2B56),
    tertiary = MyDayPalette.GreenSuccessLight,
    onTertiary = Color.White,
    tertiaryContainer = MyDayPalette.GreenContainerLight,
    onTertiaryContainer = Color(0xFF0A361F),
    background = MyDayPalette.LightBackground,
    onBackground = MyDayPalette.LightTextPrimary,
    surface = MyDayPalette.LightCard,
    onSurface = MyDayPalette.LightTextPrimary,
    surfaceVariant = MyDayPalette.LightRaised,
    onSurfaceVariant = MyDayPalette.LightTextSecondary,
    outline = MyDayPalette.LightHairline,
    outlineVariant = MyDayPalette.LightHairline,
    error = MyDayPalette.CoralErrorLight,
    onError = Color.White
)

private val MyDayTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
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
        fontSize = 13.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
        fontFeatureSettings = "tnum"
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.15.sp,
        fontFeatureSettings = "tnum"
    )
)

private val MyDayShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
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
            successContainer = MyDayPalette.GreenContainerDark,
            warning = MyDayPalette.AmberWarning,
            cardBorder = MyDayPalette.DarkHairline,
            elevatedSurface = MyDayPalette.DarkRaised,
            navBarSurface = MyDayPalette.DarkDeepest,
            mutedText = MyDayPalette.DarkTextMuted,
            isDark = true
        )
    } else {
        MyDayExtendedColors(
            success = MyDayPalette.GreenSuccessLight,
            successContainer = MyDayPalette.GreenContainerLight,
            warning = Color(0xFFD97706),
            cardBorder = MyDayPalette.LightHairline,
            elevatedSurface = MyDayPalette.LightRaised,
            navBarSurface = MyDayPalette.LightCard,
            mutedText = MyDayPalette.LightTextMuted,
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
