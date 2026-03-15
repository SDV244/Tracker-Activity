package com.fittrack.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.fittrack.ui.components.*
import com.fittrack.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ═══════════════════════════════════════════════════════════════
// 🏠 PREMIUM DASHBOARD - Top-tier fitness experience
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumDashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val timeOfDay = remember { getCurrentTimeOfDay() }
    val greeting = remember { getGreeting(timeOfDay) }
    
    Scaffold(
        containerColor = FitColors.SurfaceDeep,
        topBar = {
            PremiumTopBar(
                greeting = greeting,
                streakDays = 12, // From state
                onProfileClick = { /* Navigate to profile */ },
                onNotificationsClick = { /* Show notifications */ }
            )
        },
        floatingActionButton = {
            PremiumQuickLogFAB(
                onMealClick = { navController.navigate("add_meal/BREAKFAST") },
                onWorkoutClick = { navController.navigate("start_workout") },
                onWaterClick = { /* Quick log water */ },
                onWeightClick = { /* Quick log weight */ }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // Hero: Calorie Ring
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedCalorieRing(
                        consumed = uiState.caloriesConsumed,
                        burned = uiState.caloriesBurned,
                        goal = uiState.calorieGoal,
                        size = 220.dp
                    )
                }
            }
            
            // Macro Orbs
            item {
                MacroOrbsRow(
                    protein = uiState.protein,
                    proteinGoal = uiState.proteinGoal,
                    carbs = uiState.carbs,
                    carbsGoal = uiState.carbsGoal,
                    fat = uiState.fat,
                    fatGoal = uiState.fatGoal
                )
            }
            
            // Activity from Wearable
            item {
                SectionHeader(
                    title = "Activity",
                    subtitle = "from Health Connect",
                    icon = "⌚"
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActivityStatCard(
                        icon = "👣",
                        value = formatNumber(uiState.steps),
                        label = "Steps",
                        subLabel = "/ ${formatNumber(uiState.stepsGoal)}",
                        accentColor = FitColors.Success,
                        modifier = Modifier.weight(1f)
                    )
                    ActivityStatCard(
                        icon = "🔥",
                        value = formatNumber(uiState.caloriesBurned),
                        label = "Burned",
                        subLabel = "kcal",
                        accentColor = FitColors.Warning,
                        modifier = Modifier.weight(1f)
                    )
                    ActivityStatCard(
                        icon = "❤️",
                        value = "72",
                        label = "Heart",
                        subLabel = "bpm",
                        accentColor = FitColors.EnergyStart,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Today's Meals
            item {
                SectionHeader(
                    title = "Today's Meals",
                    subtitle = "${uiState.caloriesConsumed} kcal logged",
                    icon = "🍽️"
                )
            }
            
            item {
                MealTimeline()
            }
            
            // Quick Actions
            item {
                SectionHeader(
                    title = "Quick Actions",
                    subtitle = null,
                    icon = "⚡"
                )
            }
            
            item {
                QuickActionsRow(
                    onLogMealClick = { navController.navigate("add_meal/BREAKFAST") },
                    onStartWorkoutClick = { navController.navigate("start_workout") },
                    onLogWaterClick = { /* Log water */ },
                    onLogWeightClick = { /* Log weight */ }
                )
            }
            
            // Recent Workouts
            item {
                SectionHeader(
                    title = "Recent Workouts",
                    subtitle = "Keep the momentum",
                    icon = "💪"
                )
            }
            
            item {
                if (uiState.recentWorkouts.isEmpty()) {
                    EmptyWorkoutsCard(
                        onStartClick = { navController.navigate("start_workout") }
                    )
                } else {
                    // Show recent workouts
                }
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 🎯 TOP BAR
// ═══════════════════════════════════════════════════════════════

@Composable
fun PremiumTopBar(
    greeting: String,
    streakDays: Int,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Let's crush your goals",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StreakFlame(streakDays = streakDays)
            
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 📝 SECTION HEADER
// ═══════════════════════════════════════════════════════════════

@Composable
fun SectionHeader(
    title: String,
    subtitle: String?,
    icon: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 🍽️ MEAL TIMELINE
// ═══════════════════════════════════════════════════════════════

@Composable
fun MealTimeline() {
    val meals = listOf(
        Triple("🌅", "Breakfast", true),
        Triple("☀️", "Lunch", true),
        Triple("🌙", "Dinner", false),
        Triple("🍎", "Snacks", false)
    )
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(meals.size) { index ->
            val (icon, name, isLogged) = meals[index]
            MealTimelineItem(
                icon = icon,
                name = name,
                isLogged = isLogged,
                onClick = { /* Navigate to meal */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTimelineItem(
    icon: String,
    name: String,
    isLogged: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(FitDimens.radiusMD),
        colors = CardDefaults.cardColors(
            containerColor = if (isLogged) 
                FitColors.Success.copy(alpha = 0.15f) 
            else 
                FitColors.SurfaceCard
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = if (isLogged) FitColors.Success else Color.White
            )
            if (isLogged) {
                Text(
                    text = "✓ Logged",
                    style = MaterialTheme.typography.labelSmall,
                    color = FitColors.Success
                )
            } else {
                Text(
                    text = "Add",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ⚡ QUICK ACTIONS ROW
// ═══════════════════════════════════════════════════════════════

@Composable
fun QuickActionsRow(
    onLogMealClick: () -> Unit,
    onStartWorkoutClick: () -> Unit,
    onLogWaterClick: () -> Unit,
    onLogWeightClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(
            icon = "🍽️",
            label = "Meal",
            color = FitColors.Success,
            onClick = onLogMealClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = "💪",
            label = "Workout",
            color = FitColors.Warning,
            onClick = onStartWorkoutClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = "💧",
            label = "Water",
            color = FitColors.ProteinCyan,
            onClick = onLogWaterClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = "⚖️",
            label = "Weight",
            color = FitColors.FatPink,
            onClick = onLogWeightClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionButton(
    icon: String,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(FitDimens.radiusMD),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 🏋️ EMPTY WORKOUTS
// ═══════════════════════════════════════════════════════════════

@Composable
fun EmptyWorkoutsCard(onStartClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🏋️", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No workouts yet",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "Start your first workout and build the habit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStartClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FitColors.EnergyStart
                )
            ) {
                Text("Start Workout")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ➕ QUICK LOG FAB
// ═══════════════════════════════════════════════════════════════

@Composable
fun PremiumQuickLogFAB(
    onMealClick: () -> Unit,
    onWorkoutClick: () -> Unit,
    onWaterClick: () -> Unit,
    onWeightClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Simple FAB for now - could expand to radial menu
    FloatingActionButton(
        onClick = onMealClick,
        containerColor = FitColors.EnergyStart,
        contentColor = Color.White
    ) {
        Icon(Icons.Default.Add, contentDescription = "Quick Log")
    }
}

// ═══════════════════════════════════════════════════════════════
// 🔧 HELPERS
// ═══════════════════════════════════════════════════════════════

private fun getGreeting(timeOfDay: TimeOfDay): String {
    return when (timeOfDay) {
        TimeOfDay.MORNING -> "Good morning ☀️"
        TimeOfDay.AFTERNOON -> "Good afternoon 💪"
        TimeOfDay.EVENING -> "Good evening 🌙"
        TimeOfDay.NIGHT -> "Good night 😴"
    }
}

private fun formatNumber(number: Int): String {
    return when {
        number >= 1000000 -> "${number / 1000000}M"
        number >= 1000 -> "${number / 1000}K"
        else -> number.toString()
    }
}
