package com.fittrack.ui.screens.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.fittrack.domain.model.FoodItem
import com.fittrack.ui.components.FoodItemCard
import com.fittrack.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    mealType: String,
    navController: NavController,
    viewModel: AddMealViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showServingsDialog by remember { mutableStateOf(false) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    
    LaunchedEffect(mealType) {
        viewModel.setMealType(mealType)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add ${mealType.lowercase().replaceFirstChar { it.uppercase() }}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Open barcode scanner */ }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.search(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search foods...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = ""
                            viewModel.search("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Quick actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.showRecent,
                    onClick = { viewModel.toggleRecent() },
                    label = { Text("Recent") },
                    leadingIcon = if (uiState.showRecent) {
                        { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                    } else null
                )
                FilterChip(
                    selected = uiState.showCustom,
                    onClick = { viewModel.toggleCustom() },
                    label = { Text("My Foods") },
                    leadingIcon = if (uiState.showCustom) {
                        { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                    } else null
                )
                AssistChip(
                    onClick = { navController.navigate(Routes.FOOD_SEARCH) },
                    label = { Text("Create Custom") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp)) }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Results
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.searchResults) { food ->
                        FoodItemCard(
                            name = food.name,
                            brand = food.brand,
                            calories = food.nutrition.calories,
                            servingSize = "${food.servingSize.toInt()}${food.servingUnit}",
                            imageUrl = food.imageUrl,
                            onClick = {
                                selectedFood = food
                                showServingsDialog = true
                            }
                        )
                    }
                    
                    if (uiState.searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No results found",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Try searching online or create a custom food",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.searchOnline(searchQuery) }) {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Search Online")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Servings dialog
    if (showServingsDialog && selectedFood != null) {
        ServingsDialog(
            food = selectedFood!!,
            onDismiss = { 
                showServingsDialog = false
                selectedFood = null
            },
            onConfirm = { servings ->
                viewModel.addMealEntry(selectedFood!!, servings)
                showServingsDialog = false
                selectedFood = null
                navController.popBackStack()
            }
        )
    }
}

@Composable
fun ServingsDialog(
    food: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var servings by remember { mutableStateOf("1") }
    val servingsFloat = servings.toFloatOrNull() ?: 1f
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${food.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = servings,
                    onValueChange = { servings = it },
                    label = { Text("Number of servings") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Serving: ${food.servingSize.toInt()}${food.servingUnit}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Nutrition for $servings serving(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NutritionItem("Calories", "${(food.nutrition.calories * servingsFloat).toInt()}")
                            NutritionItem("Protein", "${(food.nutrition.protein * servingsFloat).toInt()}g")
                            NutritionItem("Carbs", "${(food.nutrition.carbs * servingsFloat).toInt()}g")
                            NutritionItem("Fat", "${(food.nutrition.fat * servingsFloat).toInt()}g")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(servingsFloat) },
                enabled = servingsFloat > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NutritionItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
