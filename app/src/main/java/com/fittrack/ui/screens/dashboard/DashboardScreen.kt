package com.fittrack.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.fittrack.ui.components.CalorieRingChart
import com.fittrack.ui.components.MacroProgressBar
import com.fittrack.ui.components.QuickActionCard
import com.fittrack.ui.navigation.Routes
import com.fittrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("FitTrack", fontWeight = FontWeight.Bold)
                        Text(
                            "Today's Summary",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.HEALTH_CONNECT) }) {
                        Icon(Icons.Default.Watch, contentDescription = "Health Connect")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // Calorie Summary Card
            item {
                CalorieSummaryCard(
                    consumed = uiState.caloriesConsumed,
                    burned = uiState.caloriesBurned,
                    goal = uiState.calorieGoal,
                    remaining = uiState.caloriesRemaining
                )
            }
            
            // Macros Progress
            item {
                MacrosCard(
                    protein = uiState.protein,
                    proteinGoal = uiState.proteinGoal,
                    carbs = uiState.carbs,
                    carbsGoal = uiState.carbsGoal,
                    fat = uiState.fat,
                    fatGoal = uiState.fatGoal
                )
            }
            
            // Activity Card (from wearable)
            item {
                ActivityCard(
                    steps = uiState.steps,
                    stepsGoal = uiState.stepsGoal,
                    activeMinutes = uiState.activeMinutes,
                    distance = uiState.distance
                )
            }
            
            // Quick Actions
            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Restaurant,
                        title = "Log Meal",
                        color = PrimaryGreen,
                        onClick = { navController.navigate(Routes.addMeal("BREAKFAST")) }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FitnessCenter,
                        title = "Start Workout",
                        color = SecondaryOrange,
                        onClick = { navController.navigate(Routes.START_WORKOUT) }
                    )
                }
            }
            
            // Recent Workouts
            item {
                Text(
                    "Recent Workouts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (uiState.recentWorkouts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No workouts yet. Start your first workout!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun CalorieSummaryCard(
    consumed: Int,
    burned: Int,
    goal: Int,
    remaining: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Calories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                CalorieRow(label = "Consumed", value = consumed, color = PrimaryGreen)
                CalorieRow(label = "Burned", value = burned, color = SecondaryOrange)
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row {
                    Text(
                        if (remaining >= 0) "Remaining: " else "Over by: ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${kotlin.math.abs(remaining)} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining >= 0) PrimaryGreen else ErrorRed
                    )
                }
            }
            
            CalorieRingChart(
                consumed = consumed,
                burned = burned,
                goal = goal,
                modifier = Modifier.size(120.dp)
            )
        }
    }
}

@Composable
private fun CalorieRow(label: String, value: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(end = 8.dp)
        )
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "$value kcal",
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MacrosCard(
    protein: Float,
    proteinGoal: Float,
    carbs: Float,
    carbsGoal: Float,
    fat: Float,
    fatGoal: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Macros",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            MacroProgressBar(
                label = "Protein",
                current = protein,
                goal = proteinGoal,
                unit = "g",
                color = ProteinColor
            )
            
            MacroProgressBar(
                label = "Carbs",
                current = carbs,
                goal = carbsGoal,
                unit = "g",
                color = CarbsColor
            )
            
            MacroProgressBar(
                label = "Fat",
                current = fat,
                goal = fatGoal,
                unit = "g",
                color = FatColor
            )
        }
    }
}

@Composable
fun ActivityCard(
    steps: Int,
    stepsGoal: Int,
    activeMinutes: Int,
    distance: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.Watch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActivityStat(
                    icon = Icons.Default.DirectionsWalk,
                    value = steps.toString(),
                    label = "Steps",
                    subLabel = "/ $stepsGoal"
                )
                ActivityStat(
                    icon = Icons.Default.Timer,
                    value = activeMinutes.toString(),
                    label = "Active",
                    subLabel = "min"
                )
                ActivityStat(
                    icon = Icons.Default.Route,
                    value = String.format("%.1f", distance),
                    label = "Distance",
                    subLabel = "km"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = (steps.toFloat() / stepsGoal).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryGreen
            )
        }
    }
}

@Composable
private fun ActivityStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    subLabel: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
