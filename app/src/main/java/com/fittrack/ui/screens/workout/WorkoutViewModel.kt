package com.fittrack.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.ai.*
import com.fittrack.data.repository.ExerciseRepository
import com.fittrack.data.repository.WorkoutRepository
import com.fittrack.data.repository.WorkoutStats
import com.fittrack.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val templates: List<WorkoutTemplate> = emptyList(),
    val recentWorkouts: List<Workout> = emptyList(),
    val weeklyWorkouts: Int = 0,
    val weeklyVolume: Float = 0f,
    val weeklyMinutes: Int = 0,
    val weeklyPRs: Int = 0,
    val weeklyStats: WorkoutStats = WorkoutStats(),
    val selectedTemplateId: Long? = null,
    // AI Generated Program
    val generatedProgram: WorkoutProgram? = null,
    val isGeneratingProgram: Boolean = false,
    val trainingPreferences: TrainingPreferences = TrainingPreferences(),
    // Active Workout
    val activeWorkout: Workout? = null,
    // Exercise library
    val exercises: List<Exercise> = emptyList(),
    val exerciseSearchQuery: String = ""
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutPlanGenerator: WorkoutPlanGenerator
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()
    
    init {
        loadTemplates()
        loadRecentWorkouts()
        loadWeeklyStats()
        loadExercises()
        checkActiveWorkout()
    }
    
    private fun loadTemplates() {
        viewModelScope.launch {
            workoutRepository.getAllTemplates().collect { templates ->
                _uiState.update { state ->
                    state.copy(templates = templates)
                }
            }
        }
    }
    
    private fun loadRecentWorkouts() {
        viewModelScope.launch {
            workoutRepository.getRecentWorkouts(10).collect { workouts ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        recentWorkouts = workouts
                    )
                }
            }
        }
    }
    
    private fun loadWeeklyStats() {
        viewModelScope.launch {
            val stats = workoutRepository.getWeeklyStats()
            _uiState.update { state ->
                state.copy(
                    weeklyStats = stats,
                    weeklyWorkouts = stats.workoutCount,
                    weeklyVolume = stats.totalVolume,
                    weeklyMinutes = stats.totalMinutes,
                    weeklyPRs = stats.prCount
                )
            }
        }
    }
    
    private fun loadExercises() {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _uiState.update { it.copy(exercises = exercises) }
            }
        }
    }
    
    private fun checkActiveWorkout() {
        viewModelScope.launch {
            val activeWorkout = workoutRepository.getActiveWorkout()
            _uiState.update { it.copy(activeWorkout = activeWorkout) }
        }
    }
    
    fun startWorkoutFromTemplate(templateId: Long) {
        _uiState.update { it.copy(selectedTemplateId = templateId) }
        
        viewModelScope.launch {
            val template = workoutRepository.getTemplateById(templateId) ?: return@launch
            
            // Create a new workout from the template
            val workout = Workout(
                name = template.name,
                exercises = template.exercises.map { templateExercise ->
                    ExerciseLog(
                        exercise = templateExercise.exercise,
                        sets = (1..templateExercise.targetSets).map { setNum ->
                            ExerciseSet(
                                setNumber = setNum,
                                weight = templateExercise.targetWeight,
                                reps = templateExercise.targetReps?.first
                            )
                        },
                        restBetweenSets = templateExercise.restDuration
                    )
                },
                startTime = LocalDateTime.now(),
                templateId = templateId
            )
            
            val workoutId = workoutRepository.insertWorkout(workout)
            val savedWorkout = workoutRepository.getWorkoutById(workoutId)
            _uiState.update { it.copy(activeWorkout = savedWorkout) }
        }
    }
    
    /**
     * Start a quick empty workout
     */
    fun startQuickWorkout(name: String = "Quick Workout") {
        viewModelScope.launch {
            val workout = Workout(
                name = name,
                exercises = emptyList(),
                startTime = LocalDateTime.now()
            )
            
            val workoutId = workoutRepository.insertWorkout(workout)
            val savedWorkout = workoutRepository.getWorkoutById(workoutId)
            _uiState.update { it.copy(activeWorkout = savedWorkout) }
        }
    }
    
    /**
     * Finish the current workout
     */
    fun finishWorkout() {
        viewModelScope.launch {
            val activeWorkout = _uiState.value.activeWorkout ?: return@launch
            
            val finishedWorkout = activeWorkout.copy(
                endTime = LocalDateTime.now()
            )
            
            workoutRepository.updateWorkout(finishedWorkout)
            _uiState.update { it.copy(activeWorkout = null) }
            
            // Refresh stats
            loadWeeklyStats()
            loadRecentWorkouts()
        }
    }
    
    /**
     * Cancel the current workout
     */
    fun cancelWorkout() {
        viewModelScope.launch {
            val activeWorkout = _uiState.value.activeWorkout ?: return@launch
            workoutRepository.deleteWorkout(activeWorkout)
            _uiState.update { it.copy(activeWorkout = null) }
        }
    }
    
    /**
     * Update training preferences for AI generation
     */
    fun updateTrainingPreferences(preferences: TrainingPreferences) {
        _uiState.update { it.copy(trainingPreferences = preferences) }
    }
    
    /**
     * Generate an AI workout program based on preferences
     */
    fun generateWorkoutProgram(userProfile: UserProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingProgram = true) }
            
            try {
                val program = workoutPlanGenerator.generateProgram(
                    preferences = _uiState.value.trainingPreferences,
                    userProfile = userProfile
                )
                
                _uiState.update { 
                    it.copy(
                        generatedProgram = program,
                        isGeneratingProgram = false
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingProgram = false) }
            }
        }
    }
    
    /**
     * Save the generated program as templates
     */
    fun saveGeneratedProgramAsTemplates() {
        viewModelScope.launch {
            val program = _uiState.value.generatedProgram ?: return@launch
            
            for (programmedWorkout in program.workouts) {
                val template = WorkoutTemplate(
                    name = programmedWorkout.name,
                    description = "${program.name} - ${programmedWorkout.focus}",
                    exercises = programmedWorkout.exercises.map { programmedExercise ->
                        TemplateExercise(
                            exercise = programmedExercise.exercise,
                            targetSets = programmedExercise.sets,
                            targetReps = programmedExercise.repsRange,
                            targetWeight = null, // User will set their own weights
                            restDuration = java.time.Duration.ofSeconds(programmedExercise.restSeconds.toLong()),
                            notes = programmedExercise.notes
                        )
                    },
                    estimatedDuration = programmedWorkout.estimatedDuration,
                    category = when {
                        programmedWorkout.focus.contains("Push", ignoreCase = true) -> ExerciseCategory.CHEST
                        programmedWorkout.focus.contains("Pull", ignoreCase = true) -> ExerciseCategory.BACK
                        programmedWorkout.focus.contains("Leg", ignoreCase = true) -> ExerciseCategory.LEGS
                        programmedWorkout.focus.contains("Upper", ignoreCase = true) -> ExerciseCategory.CHEST
                        programmedWorkout.focus.contains("Lower", ignoreCase = true) -> ExerciseCategory.LEGS
                        else -> ExerciseCategory.FULL_BODY
                    },
                    isBuiltIn = false
                )
                
                workoutRepository.insertTemplate(template)
            }
            
            // Refresh templates
            loadTemplates()
        }
    }
    
    /**
     * Search exercises
     */
    fun searchExercises(query: String) {
        _uiState.update { it.copy(exerciseSearchQuery = query) }
        
        if (query.isBlank()) {
            loadExercises()
            return
        }
        
        viewModelScope.launch {
            exerciseRepository.searchExercises(query).collect { exercises ->
                _uiState.update { it.copy(exercises = exercises) }
            }
        }
    }
    
    /**
     * Get exercises by category
     */
    fun filterExercisesByCategory(category: ExerciseCategory) {
        viewModelScope.launch {
            exerciseRepository.getExercisesByCategory(category).collect { exercises ->
                _uiState.update { it.copy(exercises = exercises) }
            }
        }
    }
    
    /**
     * Add a custom exercise
     */
    fun addCustomExercise(exercise: Exercise) {
        viewModelScope.launch {
            exerciseRepository.insertExercise(exercise.copy(isCustom = true))
        }
    }
    
    /**
     * Create a new template
     */
    fun createTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            workoutRepository.insertTemplate(template)
        }
    }
    
    /**
     * Delete a template
     */
    fun deleteTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            workoutRepository.deleteTemplate(template)
        }
    }
}
