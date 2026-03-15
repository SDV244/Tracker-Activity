package com.fittrack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import java.time.LocalTime

// ═══════════════════════════════════════════════════════════════
// 🎨 DYNAMIC COLOR SYSTEM - Adapts to time of day
// ═══════════════════════════════════════════════════════════════

enum class TimeOfDay {
    MORNING,    // 5AM - 12PM: Energetic
    AFTERNOON,  // 12PM - 6PM: Vibrant  
    EVENING,    // 6PM - 10PM: Calming
    NIGHT       // 10PM - 5AM: Restful
}

fun getCurrentTimeOfDay(): TimeOfDay {
    val hour = LocalTime.now().hour
    return when {
        hour in 5..11 -> TimeOfDay.MORNING
        hour in 12..17 -> TimeOfDay.AFTERNOON
        hour in 18..21 -> TimeOfDay.EVENING
        else -> TimeOfDay.NIGHT
    }
}

// Core Brand Colors
object FitColors {
    // Energy Gradient (Primary)
    val EnergyStart = Color(0xFFFF6B6B)
    val EnergyMid = Color(0xFFFF8E53)
    val EnergyEnd = Color(0xFFFFE66D)
    
    // Achievement Gold
    val AchievementGold = Color(0xFFFFD700)
    val AchievementBronze = Color(0xFFCD7F32)
    val AchievementSilver = Color(0xFFC0C0C0)
    
    // Macros - Distinct & Beautiful
    val ProteinCyan = Color(0xFF00D4FF)
    val ProteinGlow = Color(0xFF00F0FF)
    val CarbsAmber = Color(0xFFFFB800)
    val CarbsGlow = Color(0xFFFFD000)
    val FatPink = Color(0xFFFF6B9D)
    val FatGlow = Color(0xFFFF8FB3)
    
    // Semantic
    val Success = Color(0xFF00E676)
    val SuccessGlow = Color(0xFF69F0AE)
    val Warning = Color(0xFFFFAB00)
    val Error = Color(0xFFFF5252)
    
    // Surfaces (Dark Mode First)
    val SurfaceDeep = Color(0xFF0D0D0F)
    val SurfaceBase = Color(0xFF121214)
    val SurfaceElevated = Color(0xFF1A1A1F)
    val SurfaceCard = Color(0xFF242429)
    val SurfaceHighlight = Color(0xFF2E2E35)
    
    // Glass effect
    val GlassWhite = Color(0x1AFFFFFF)
    val GlassBorder = Color(0x33FFFFFF)
    
    // Time-based accent colors
    fun getAccentForTime(time: TimeOfDay): Color = when (time) {
        TimeOfDay.MORNING -> Color(0xFFFF9F43)   // Warm orange
        TimeOfDay.AFTERNOON -> Color(0xFF00E676) // Fresh green
        TimeOfDay.EVENING -> Color(0xFFA29BFE)   // Soft purple
        TimeOfDay.NIGHT -> Color(0xFF6C5CE7)     // Deep purple
    }
    
    // Gradients
    val EnergyGradient = Brush.linearGradient(
        colors = listOf(EnergyStart, EnergyMid, EnergyEnd)
    )
    
    val ProteinGradient = Brush.linearGradient(
        colors = listOf(ProteinCyan, ProteinGlow)
    )
    
    val CarbsGradient = Brush.linearGradient(
        colors = listOf(CarbsAmber, CarbsGlow)
    )
    
    val FatGradient = Brush.linearGradient(
        colors = listOf(FatPink, FatGlow)
    )
}

// ═══════════════════════════════════════════════════════════════
// 📝 TYPOGRAPHY
// ═══════════════════════════════════════════════════════════════

object FitTypography {
    // Display - Big celebration numbers
    val displayLarge = TextStyle(
        fontSize = 57.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.25).sp,
        lineHeight = 64.sp
    )
    
    val displayMedium = TextStyle(
        fontSize = 45.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 52.sp
    )
    
    // Headlines
    val headlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 40.sp
    )
    
    val headlineMedium = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 36.sp
    )
    
    // Numbers (for stats)
    val numberLarge = TextStyle(
        fontSize = 48.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1).sp,
        lineHeight = 56.sp
    )
    
    val numberMedium = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 40.sp
    )
    
    val numberSmall = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 28.sp
    )
}

// ═══════════════════════════════════════════════════════════════
// 🎭 DARK THEME
// ═══════════════════════════════════════════════════════════════

private val FitDarkColorScheme = darkColorScheme(
    primary = FitColors.EnergyStart,
    onPrimary = Color.White,
    primaryContainer = FitColors.SurfaceCard,
    onPrimaryContainer = FitColors.EnergyEnd,
    secondary = FitColors.Success,
    onSecondary = Color.Black,
    secondaryContainer = FitColors.SurfaceHighlight,
    tertiary = FitColors.ProteinCyan,
    background = FitColors.SurfaceDeep,
    onBackground = Color.White,
    surface = FitColors.SurfaceBase,
    onSurface = Color.White,
    surfaceVariant = FitColors.SurfaceCard,
    onSurfaceVariant = Color(0xFFB0B0B8),
    outline = FitColors.GlassBorder,
    error = FitColors.Error
)

private val FitLightColorScheme = lightColorScheme(
    primary = FitColors.EnergyStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0E0),
    secondary = FitColors.Success,
    tertiary = FitColors.ProteinCyan,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5),
    error = FitColors.Error
)

// ═══════════════════════════════════════════════════════════════
// 🎨 THEME COMPOSABLE
// ═══════════════════════════════════════════════════════════════

@Composable
fun FitTrackPremiumTheme(
    darkTheme: Boolean = true, // Dark mode first!
    dynamicColor: Boolean = false, // Our colors are better
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FitDarkColorScheme
        else -> FitLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════
// 🎯 ANIMATION SPECS
// ═══════════════════════════════════════════════════════════════

object FitAnimations {
    // Spring animations for natural feel
    val defaultSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val bouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val snappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    // Tween for controlled timing
    val fastFade = tween<Float>(durationMillis = 150)
    val mediumFade = tween<Float>(durationMillis = 300)
    val slowReveal = tween<Float>(durationMillis = 500, easing = FastOutSlowInEasing)
    
    // Number counting
    val countUp = tween<Float>(durationMillis = 800, easing = FastOutSlowInEasing)
}

// ═══════════════════════════════════════════════════════════════
// 📐 SPACING & SIZING
// ═══════════════════════════════════════════════════════════════

object FitDimens {
    // Spacing scale (4dp base)
    val spaceXXS = 2.dp
    val spaceXS = 4.dp
    val spaceSM = 8.dp
    val spaceMD = 16.dp
    val spaceLG = 24.dp
    val spaceXL = 32.dp
    val space2XL = 48.dp
    val space3XL = 64.dp
    
    // Corner radius
    val radiusXS = 4.dp
    val radiusSM = 8.dp
    val radiusMD = 16.dp
    val radiusLG = 24.dp
    val radiusXL = 32.dp
    val radiusFull = 999.dp
    
    // Card elevation
    val elevationNone = 0.dp
    val elevationSM = 2.dp
    val elevationMD = 4.dp
    val elevationLG = 8.dp
    
    // Icon sizes
    val iconSM = 16.dp
    val iconMD = 24.dp
    val iconLG = 32.dp
    val iconXL = 48.dp
    
    // Ring sizes
    val ringSmall = 80.dp
    val ringMedium = 120.dp
    val ringLarge = 180.dp
    val ringXL = 240.dp
}
