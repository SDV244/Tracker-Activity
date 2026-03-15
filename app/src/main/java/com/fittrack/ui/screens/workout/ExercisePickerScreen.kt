package com.fittrack.ui.screens.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fittrack.domain.model.ExerciseCategory
import com.fittrack.ui.components.ExerciseItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExerciseCategory?>(null) }
    
    // Sample exercises - would come from ViewModel
    val exercises = remember {
        listOf(
            SampleExercise("bench_press", "Bench Press", ExerciseCategory.CHEST, "Chest, Triceps"),
            SampleExercise("squat", "Barbell Squat", ExerciseCategory.LEGS, "Quadriceps, Glutes"),
            SampleExercise("deadlift", "Deadlift", ExerciseCategory.BACK, "Back, Hamstrings"),
            SampleExercise("overhead_press", "Overhead Press", ExerciseCategory.SHOULDERS, "Shoulders, Triceps"),
            SampleExercise("barbell_row", "Barbell Row", ExerciseCategory.BACK, "Back, Biceps"),
            SampleExercise("pull_up", "Pull Up", ExerciseCategory.BACK, "Lats, Biceps"),
            SampleExercise("dip", "Dip", ExerciseCategory.CHEST, "Chest, Triceps"),
            SampleExercise("leg_press", "Leg Press", ExerciseCategory.LEGS, "Quadriceps, Glutes"),
            SampleExercise("lat_pulldown", "Lat Pulldown", ExerciseCategory.BACK, "Lats, Biceps"),
            SampleExercise("bicep_curl", "Bicep Curl", ExerciseCategory.ARMS, "Biceps"),
            SampleExercise("tricep_pushdown", "Tricep Pushdown", ExerciseCategory.ARMS, "Triceps"),
            SampleExercise("plank", "Plank", ExerciseCategory.CORE, "Core"),
            SampleExercise("running", "Running", ExerciseCategory.CARDIO, "Cardio"),
        )
    }
    
    val filteredExercises = exercises.filter { exercise ->
        val matchesSearch = searchQuery.isEmpty() || 
            exercise.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || 
            exercise.category == selectedCategory
        matchesSearch && matchesCategory
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Exercise") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Create custom exercise */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Exercise")
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
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search exercises...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Category chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") }
                    )
                }
                
                items(ExerciseCategory.entries.toList()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { 
                            selectedCategory = if (selectedCategory == category) null else category
                        },
                        label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Exercise list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredExercises) { exercise ->
                    ExerciseItemCard(
                        name = exercise.name,
                        category = exercise.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        muscleGroups = exercise.muscleGroups,
                        onClick = {
                            // Add exercise to workout and go back
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LazyRow(
    modifier: Modifier,
    horizontalArrangement: Arrangement.Horizontal,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        item { content() }
    }
}

private data class SampleExercise(
    val id: String,
    val name: String,
    val category: ExerciseCategory,
    val muscleGroups: String
)
