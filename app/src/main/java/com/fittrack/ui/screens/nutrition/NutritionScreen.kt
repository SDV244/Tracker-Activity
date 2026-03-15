package com.fittrack.ui.screens.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.fittrack.domain.model.MealEntry
import com.fittrack.domain.model.MealType
import com.fittrack.ui.components.MacroProgressBar
import com.fittrack.ui.navigation.Routes
import com.fittrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    navController: NavController,
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.FOOD_SEARCH) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Food")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.addMeal("BREAKFAST")) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Meal")
            }
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
            
            // Daily Summary Card
            item {
                DailySummaryCard(
                    caloriesConsumed = uiState.caloriesConsumed,
                    calorieGoal = uiState.calorieGoal,
                    protein = uiState.protein,
                    proteinGoal = uiState.proteinGoal,
                    carbs = uiState.carbs,
                    carbsGoal = uiState.carbsGoal,
                    fat = uiState.fat,
                    fatGoal = uiState.fatGoal
                )
            }
            
            // Water Tracker
            item {
                WaterTrackerCard(
                    current = uiState.waterConsumed,
                    goal = uiState.waterGoal,
                    onAddWater = { viewModel.addWater(it) }
                )
            }
            
            // Meal sections
            MealType.entries.forEach { mealType ->
                val meals = uiState.mealsByType[mealType] ?: emptyList()
                val mealCalories = meals.sumOf { it.totalCalories }
                
                item {
                    MealSection(
                        mealType = mealType,
                        totalCalories = mealCalories,
                        meals = meals,
                        onAddMeal = { navController.navigate(Routes.addMeal(mealType.name)) },
                        onMealClick = { /* Navigate to meal detail */ }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun DailySummaryCard(
    caloriesConsumed: Int,
    calorieGoal: Int,
    protein: Float,
    proteinGoal: Float,
    carbs: Float,
    carbsGoal: Float,
    fat: Float,
    fatGoal: Float
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$caloriesConsumed",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "of $calorieGoal kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                val remaining = calorieGoal - caloriesConsumed
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${kotlin.math.abs(remaining)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining >= 0) PrimaryGreen else ErrorRed
                    )
                    Text(
                        text = if (remaining >= 0) "remaining" else "over",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = (caloriesConsumed.toFloat() / calorieGoal).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
                color = if (caloriesConsumed <= calorieGoal) PrimaryGreen else ErrorRed
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroChip("Protein", protein, proteinGoal, ProteinColor)
                MacroChip("Carbs", carbs, carbsGoal, CarbsColor)
                MacroChip("Fat", fat, fatGoal, FatColor)
            }
        }
    }
}

@Composable
private fun MacroChip(
    name: String,
    current: Float,
    goal: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${current.toInt()}g",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "$name",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "/ ${goal.toInt()}g",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WaterTrackerCard(
    current: Int,
    goal: Int,
    onAddWater: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${current}ml / ${goal}ml",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Water intake",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row {
                FilledTonalButton(
                    onClick = { onAddWater(250) },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("+250ml")
                }
            }
        }
    }
}

@Composable
fun MealSection(
    mealType: MealType,
    totalCalories: Int,
    meals: List<MealEntry>,
    onAddMeal: () -> Unit,
    onMealClick: (MealEntry) -> Unit
) {
    val mealIcon = when (mealType) {
        MealType.BREAKFAST -> "🌅"
        MealType.LUNCH -> "☀️"
        MealType.DINNER -> "🌙"
        MealType.SNACK -> "🍎"
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mealIcon, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = mealType.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$totalCalories kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onAddMeal) {
                        Icon(Icons.Default.Add, contentDescription = "Add ${mealType.name}")
                    }
                }
            }
            
            if (meals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                meals.forEach { meal ->
                    MealItemRow(
                        meal = meal,
                        onClick = { onMealClick(meal) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MealItemRow(
    meal: MealEntry,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.foodItem.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${meal.servings} × ${meal.foodItem.servingSize}${meal.foodItem.servingUnit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${meal.totalCalories} kcal",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
