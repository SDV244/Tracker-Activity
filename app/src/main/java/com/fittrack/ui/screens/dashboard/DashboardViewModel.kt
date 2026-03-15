package com.fittrack.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.ai.DailyMealPlan
import com.fittrack.ai.DietaryPreferences
import com.fittrack.ai.MealPlanGenerator
import com.fittrack.ai.PlannedMeal
import com.fittrack.data.repository.MealRepository
import com.fittrack.data.repository.WorkoutRepository
import com.fittrack.data.repository.WorkoutStats
import com.fittrack.domain.model.MealEntry
import com.fittrack.domain.model.Workout
import com.fittrack.health.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    // Calories
    val caloriesConsumed: Int = 0,
    val caloriesBurned: Int = 0,
    val calorieGoal: Int = 2000,
    val caloriesRemaining: Int = 2000,
    // Macros
    val protein: Float = 0f,
    val proteinGoal: Float = 150f,
    val carbs: Float = 0f,
    val carbsGoal: Float = 200f,
    val fat: Float = 0f,
    val fatGoal: Float = 65f,
    // Activity
    val steps: Int = 0,
    val stepsGoal: Int = 10000,
    val activeMinutes: Int = 0,
    val distance: Float = 0f,
    // Recent
    val recentWorkouts: List<Workout> = emptyList(),
    val todayMeals: List<MealEntry> = emptyList(),
    val waterConsumed: Int = 0,
    val waterGoal: Int = 2500,
    // Weekly Stats
    val weeklyStats: WorkoutStats = WorkoutStats(),
    // AI Suggestions
    val mealSuggestions: List<PlannedMeal> = emptyList(),
    val isGeneratingSuggestions: Boolean = false,
    // Health Connect
    val healthConnectAvailable: Boolean = false,
    val healthConnectConnected: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
    private val mealPlanGenerator: MealPlanGenerator,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadTodayData()
        loadHealthData()
        loadRecentWorkouts()
        loadWeeklyStats()
    }
    
    private fun loadTodayData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            
            // Load nutrition totals
            val nutritionTotals = mealRepository.getDailyNutritionTotals(today)
            
            // Load daily goals
            val goals = mealRepository.getDailyGoals(today)
            
            // Load water
            val waterTotal = mealRepository.getTotalWaterForDate(today)
            
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    caloriesConsumed = nutritionTotals?.totalCalories ?: 0,
                    protein = nutritionTotals?.totalProtein ?: 0f,
                    carbs = nutritionTotals?.totalCarbs ?: 0f,
                    fat = nutritionTotals?.totalFat ?: 0f,
                    calorieGoal = goals?.calorieGoal ?: 2000,
                    proteinGoal = goals?.proteinGoal ?: 150f,
                    carbsGoal = goals?.carbsGoal ?: 200f,
                    fatGoal = goals?.fatGoal ?: 65f,
                    waterGoal = goals?.waterGoal ?: 2500,
                    waterConsumed = waterTotal,
                    caloriesRemaining = (goals?.calorieGoal ?: 2000) - 
                        (nutritionTotals?.totalCalories ?: 0) + 
                        state.caloriesBurned
                )
            }
            
            // Load today's meals as Flow
            mealRepository.getMealEntriesForDate(today).collect { meals ->
                _uiState.update { it.copy(todayMeals = meals) }
            }
        }
    }
    
    private fun loadHealthData() {
        viewModelScope.launch {
            try {
                val hasPermissions = healthConnectManager.hasAllPermissions()
                
                if (hasPermissions) {
                    val healthData = healthConnectManager.getHealthDataForDate(LocalDate.now())
                    
                    _uiState.update { state ->
                        state.copy(
                            healthConnectAvailable = true,
                            healthConnectConnected = true,
                            steps = healthData.steps,
                            activeMinutes = healthData.activeMinutes,
                            distance = healthData.distance,
                            caloriesBurned = healthData.totalCalories,
                            caloriesRemaining = state.calorieGoal - state.caloriesConsumed + healthData.totalCalories
                        )
                    }
                } else {
                    _uiState.update { it.copy(
                        healthConnectAvailable = true,
                        healthConnectConnected = false
                    )}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    healthConnectAvailable = false,
                    healthConnectConnected = false
                )}
            }
        }
    }
    
    private fun loadRecentWorkouts() {
        viewModelScope.launch {
            workoutRepository.getRecentWorkouts(5).collect { workouts ->
                _uiState.update { it.copy(recentWorkouts = workouts) }
            }
        }
    }
    
    private fun loadWeeklyStats() {
        viewModelScope.launch {
            val stats = workoutRepository.getWeeklyStats()
            _uiState.update { it.copy(weeklyStats = stats) }
        }
    }
    
    /**
     * Generate AI-powered meal suggestions based on remaining macros
     */
    fun generateMealSuggestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingSuggestions = true) }
            
            try {
                val state = _uiState.value
                
                // Calculate remaining macros for the day
                val remainingCalories = (state.calorieGoal - state.caloriesConsumed).coerceAtLeast(200)
                val remainingProtein = (state.proteinGoal - state.protein).coerceAtLeast(10f)
                val remainingCarbs = (state.carbsGoal - state.carbs).coerceAtLeast(20f)
                val remainingFat = (state.fatGoal - state.fat).coerceAtLeast(5f)
                
                // Generate a daily plan with the remaining macros
                val dailyPlan = mealPlanGenerator.generateDailyPlan(
                    date = LocalDate.now(),
                    targetCalories = remainingCalories,
                    targetProtein = remainingProtein,
                    targetCarbs = remainingCarbs,
                    targetFat = remainingFat,
                    preferences = DietaryPreferences() // Use defaults for now
                )
                
                _uiState.update { 
                    it.copy(
                        mealSuggestions = dailyPlan.meals,
                        isGeneratingSuggestions = false
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingSuggestions = false) }
            }
        }
    }
    
    /**
     * Add water intake
     */
    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            mealRepository.insertWaterEntry(amountMl)
            // Refresh water total
            val total = mealRepository.getTotalWaterForDate(LocalDate.now())
            _uiState.update { it.copy(waterConsumed = total) }
        }
    }
    
    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadTodayData()
        loadHealthData()
        loadRecentWorkouts()
        loadWeeklyStats()
    }
}
