package com.fittrack.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════
// 🎯 ANIMATED CALORIE RING - Apple Fitness inspired but evolved
// ═══════════════════════════════════════════════════════════════

@Composable
fun AnimatedCalorieRing(
    consumed: Int,
    burned: Int,
    goal: Int,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val progress = (consumed.toFloat() / goal).coerceIn(0f, 1.5f)
    val burnedProgress = (burned.toFloat() / goal).coerceIn(0f, 0.5f)
    val netCalories = consumed - burned
    
    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    val animatedBurned by animateFloatAsState(
        targetValue = burnedProgress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "burned"
    )
    
    // Glow pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    // Rotation for shimmer effect
    val shimmerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "shimmer"
    )
    
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        // Glow effect behind
        Canvas(modifier = Modifier.size(size).blur(20.dp)) {
            val strokeWidth = 24.dp.toPx()
            val radius = (this.size.minDimension - strokeWidth) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            
            // Outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        FitColors.EnergyStart.copy(alpha = glowAlpha * animatedProgress),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.3f
                ),
                radius = radius * 1.3f,
                center = center
            )
        }
        
        // Main rings
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 20.dp.toPx()
            val radius = (this.size.minDimension - strokeWidth) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            
            // Background ring
            drawCircle(
                color = FitColors.SurfaceCard,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Consumed ring (gradient)
            drawArc(
                brush = Brush.sweepGradient(
                    0f to FitColors.EnergyStart,
                    0.5f to FitColors.EnergyMid,
                    1f to FitColors.EnergyEnd
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.coerceAtMost(1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Burned ring (inner, orange)
            val innerRadius = radius - strokeWidth - 8.dp.toPx()
            val innerStrokeWidth = strokeWidth * 0.6f
            val innerTopLeft = Offset(center.x - innerRadius, center.y - innerRadius)
            val innerSize = Size(innerRadius * 2, innerRadius * 2)
            
            drawArc(
                brush = Brush.sweepGradient(
                    0f to FitColors.Warning,
                    1f to FitColors.EnergyMid
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedBurned,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerSize,
                style = Stroke(width = innerStrokeWidth, cap = StrokeCap.Round)
            )
            
            // Shimmer highlight
            val shimmerAngle = Math.toRadians((shimmerRotation - 90).toDouble())
            val shimmerX = center.x + radius * cos(shimmerAngle).toFloat()
            val shimmerY = center.y + radius * sin(shimmerAngle).toFloat()
            
            if (animatedProgress > 0.05f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = strokeWidth / 3,
                    center = Offset(shimmerX, shimmerY)
                )
            }
        }
        
        // Center content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated number
            AnimatedNumber(
                value = netCalories,
                style = FitTypography.numberLarge.copy(
                    color = if (netCalories >= 0) FitColors.Success else FitColors.Error
                )
            )
            Text(
                text = "net kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 🫧 MACRO ORBS - Innovative liquid-fill visualization
// ═══════════════════════════════════════════════════════════════

@Composable
fun MacroOrb(
    label: String,
    current: Float,
    goal: Float,
    color: Color,
    glowColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val progress = (current / goal).coerceIn(0f, 1f)
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "orbProgress"
    )
    
    // Wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "waveOffset"
    )
    
    // Pulse when full
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (progress >= 1f) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(pulseScale),
            contentAlignment = Alignment.Center
        ) {
            // Glow
            Box(
                modifier = Modifier
                    .size(size)
                    .blur(15.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.4f * animatedProgress),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // Orb with wave fill
            Canvas(modifier = Modifier.size(size)) {
                val radius = this.size.minDimension / 2
                val center = Offset(this.size.width / 2, this.size.height / 2)
                
                // Background circle
                drawCircle(
                    color = color.copy(alpha = 0.15f),
                    radius = radius,
                    center = center
                )
                
                // Liquid fill with wave
                clipPath(Path().apply {
                    addOval(
                        androidx.compose.ui.geometry.Rect(
                            center.x - radius,
                            center.y - radius,
                            center.x + radius,
                            center.y + radius
                        )
                    )
                }) {
                    // Wave path
                    val fillHeight = this.size.height * (1 - animatedProgress)
                    val waveAmplitude = 8.dp.toPx()
                    
                    val wavePath = Path().apply {
                        moveTo(0f, fillHeight)
                        
                        // Draw wave
                        for (x in 0..this@Canvas.size.width.toInt() step 4) {
                            val angle = Math.toRadians((x + waveOffset).toDouble())
                            val y = fillHeight + (sin(angle) * waveAmplitude).toFloat()
                            lineTo(x.toFloat(), y)
                        }
                        
                        // Complete the shape
                        lineTo(this@Canvas.size.width, this@Canvas.size.height)
                        lineTo(0f, this@Canvas.size.height)
                        close()
                    }
                    
                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(color, glowColor)
                        )
                    )
                }
                
                // Border
                drawCircle(
                    color = color.copy(alpha = 0.5f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            // Value text
            Text(
                text = "${current.toInt()}g",
                style = FitTypography.numberSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun MacroOrbsRow(
    protein: Float,
    proteinGoal: Float,
    carbs: Float,
    carbsGoal: Float,
    fat: Float,
    fatGoal: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroOrb(
            label = "PROTEIN",
            current = protein,
            goal = proteinGoal,
            color = FitColors.ProteinCyan,
            glowColor = FitColors.ProteinGlow
        )
        MacroOrb(
            label = "CARBS",
            current = carbs,
            goal = carbsGoal,
            color = FitColors.CarbsAmber,
            glowColor = FitColors.CarbsGlow
        )
        MacroOrb(
            label = "FAT",
            current = fat,
            goal = fatGoal,
            color = FitColors.FatPink,
            glowColor = FitColors.FatGlow
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// 🔥 STREAK FLAME - Animated fire indicator
// ═══════════════════════════════════════════════════════════════

@Composable
fun StreakFlame(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    
    // Flame flicker
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameScale"
    )
    
    val flameColor = when {
        streakDays >= 100 -> FitColors.AchievementGold
        streakDays >= 30 -> FitColors.EnergyStart
        streakDays >= 7 -> FitColors.Warning
        else -> FitColors.EnergyMid
    }
    
    Row(
        modifier = modifier
            .background(
                color = flameColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(FitDimens.radiusFull)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🔥",
            fontSize = 20.sp,
            modifier = Modifier.scale(flameScale)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$streakDays",
            style = FitTypography.numberSmall.copy(
                color = flameColor,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = " days",
            style = MaterialTheme.typography.bodySmall,
            color = flameColor.copy(alpha = 0.7f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// 🔢 ANIMATED NUMBER - Count up effect
// ═══════════════════════════════════════════════════════════════

@Composable
fun AnimatedNumber(
    value: Int,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    var previousValue by remember { mutableIntStateOf(value) }
    
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "countUp"
    )
    
    LaunchedEffect(value) {
        previousValue = value
    }
    
    Text(
        text = animatedValue.toString(),
        style = style,
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════
// 💎 GLASSMORPHISM CARD
// ═══════════════════════════════════════════════════════════════

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(FitDimens.radiusMD))
            .background(FitColors.GlassWhite)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        // Border glow
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(FitDimens.radiusMD))
                .background(Color.Transparent)
                .drawBehind {
                    drawRoundRect(
                        color = FitColors.GlassBorder,
                        style = Stroke(width = 1.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(FitDimens.radiusMD.toPx())
                    )
                }
        )
        
        Box(modifier = Modifier.padding(FitDimens.spaceMD)) {
            content()
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ⚡ ACTIVITY STAT CARD
// ═══════════════════════════════════════════════════════════════

@Composable
fun ActivityStatCard(
    icon: String,
    value: String,
    label: String,
    subLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = FitTypography.numberMedium.copy(color = Color.White)
                )
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
        }
    }
}
