package com.fittrack.ui.screens.workout

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
import androidx.navigation.NavController
import com.fittrack.ui.navigation.Routes
import java.time.Duration
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    workoutId: Long?,
    navController: NavController
) {
    var workoutName by remember { mutableStateOf("New Workout") }
    var startTime by remember { mutableStateOf(LocalDateTime.now()) }
    var exercises by remember { mutableStateOf(listOf<ActiveExercise>()) }
    var showFinishDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(workoutName, style = MaterialTheme.typography.titleMedium)
                        WorkoutTimer(startTime = startTime)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = { showFinishDialog = true }) {
                        Text("Finish")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.EXERCISE_PICKER) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Exercise") }
            )
        }
    ) { paddingValues ->
        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No exercises yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Add exercises to start your workout",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(exercises) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onAddSet = { /* Add set */ },
                        onUpdateSet = { setIndex, set -> /* Update set */ },
                        onDeleteSet = { setIndex -> /* Delete set */ }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
    
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finish Workout?") },
            text = { 
                Text("Are you sure you want to finish this workout?") 
            },
            confirmButton = {
                Button(onClick = {
                    // Save workout
                    showFinishDialog = false
                    navController.popBackStack()
                }) {
                    Text("Finish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Continue")
                }
            }
        )
    }
}

@Composable
fun WorkoutTimer(startTime: LocalDateTime) {
    var elapsed by remember { mutableStateOf(Duration.ZERO) }
    
    LaunchedEffect(startTime) {
        while (true) {
            elapsed = Duration.between(startTime, LocalDateTime.now())
            kotlinx.coroutines.delay(1000)
        }
    }
    
    val hours = elapsed.toHours()
    val minutes = elapsed.toMinutesPart()
    val seconds = elapsed.toSecondsPart()
    
    Text(
        text = if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
    )
}

data class ActiveExercise(
    val id: String,
    val name: String,
    val category: String,
    val sets: List<ActiveSet> = listOf(ActiveSet())
)

data class ActiveSet(
    val weight: Float? = null,
    val reps: Int? = null,
    val isCompleted: Boolean = false
)

@Composable
fun ExerciseCard(
    exercise: ActiveExercise,
    onAddSet: () -> Unit,
    onUpdateSet: (Int, ActiveSet) -> Unit,
    onDeleteSet: (Int) -> Unit
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
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = exercise.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = { /* Show exercise options */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "SET",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(0.5f)
                )
                Text(
                    "PREVIOUS",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "KG",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sets
            exercise.sets.forEachIndexed { index, set ->
                SetRow(
                    setNumber = index + 1,
                    set = set,
                    previousWeight = null,
                    previousReps = null,
                    onUpdate = { updatedSet -> onUpdateSet(index, updatedSet) },
                    onDelete = { onDeleteSet(index) }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Add set button
            OutlinedButton(
                onClick = onAddSet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Set")
            }
        }
    }
}

@Composable
fun SetRow(
    setNumber: Int,
    set: ActiveSet,
    previousWeight: Float?,
    previousReps: Int?,
    onUpdate: (ActiveSet) -> Unit,
    onDelete: () -> Unit
) {
    var weight by remember { mutableStateOf(set.weight?.toString() ?: "") }
    var reps by remember { mutableStateOf(set.reps?.toString() ?: "") }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = setNumber.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.5f)
        )
        
        Text(
            text = if (previousWeight != null && previousReps != null) {
                "${previousWeight}×$previousReps"
            } else "-",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        
        OutlinedTextField(
            value = weight,
            onValueChange = { 
                weight = it
                onUpdate(set.copy(weight = it.toFloatOrNull()))
            },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        OutlinedTextField(
            value = reps,
            onValueChange = { 
                reps = it
                onUpdate(set.copy(reps = it.toIntOrNull()))
            },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true
        )
        
        Checkbox(
            checked = set.isCompleted,
            onCheckedChange = { onUpdate(set.copy(isCompleted = it)) }
        )
    }
}
