package com.fittrack.ui.screens.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.ai.DailyMealPlan
import com.fittrack.ai.DietaryPreferences
import com.fittrack.ai.MealPlanGenerator
import com.fittrack.ai.PlannedMeal
import com.fittrack.ai.WeeklyMealPlan
import com.fittrack.data.repository.DailyNutritionGoals
import com.fittrack.data.repository.MealRepository
import com.fittrack.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class NutritionUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val caloriesConsumed: Int = 0,
    val calorieGoal: Int = 2000,
    val protein: Float = 0f,
    val proteinGoal: Float = 150f,
    val carbs: Float = 0f,
    val carbsGoal: Float = 200f,
    val fat: Float = 0f,
    val fatGoal: Float = 65f,
    val waterConsumed: Int = 0,
    val waterGoal: Int = 2500,
    val mealsByType: Map<MealType, List<MealEntry>> = emptyMap(),
    // AI Meal Planning
    val todayMealPlan: DailyMealPlan? = null,
    val weeklyMealPlan: WeeklyMealPlan? = null,
    val isGeneratingPlan: Boolean = false,
    val dietaryPreferences: DietaryPreferences = DietaryPreferences()
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val mealPlanGenerator: MealPlanGenerator
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()
    
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    
    init {
        viewModelScope.launch {
            _selectedDate.collectLatest { date ->
                loadDataForDate(date)
            }
        }
    }
    
    private fun loadDataForDate(date: LocalDate) {
        viewModelScope.launch {
            // Load meals
            mealRepository.getMealEntriesForDate(date)
                .combine(
                    mealRepository.getWaterEntriesForDate(date)
                ) { meals, waterEntries ->
                    Pair(meals, waterEntries)
                }
                .collect { (meals, waterEntries) ->
                    val mealsByType = meals.groupBy { it.mealType }
                    
                    val totalCalories = meals.sumOf { it.totalCalories }
                    val totalProtein = meals.sumOf { it.totalProtein.toDouble() }.toFloat()
                    val totalCarbs = meals.sumOf { it.totalCarbs.toDouble() }.toFloat()
                    val totalFat = meals.sumOf { it.totalFat.toDouble() }.toFloat()
                    val totalWater = waterEntries.sumOf { it.amountMl }
                    
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            selectedDate = date,
                            caloriesConsumed = totalCalories,
                            protein = totalProtein,
                            carbs = totalCarbs,
                            fat = totalFat,
                            waterConsumed = totalWater,
                            mealsByType = mealsByType
                        )
                    }
                }
        }
        
        // Load goals
        viewModelScope.launch {
            val goals = mealRepository.getDailyGoals(date)
            if (goals != null) {
                _uiState.update { state ->
                    state.copy(
                        calorieGoal = goals.calorieGoal,
                        proteinGoal = goals.proteinGoal,
                        carbsGoal = goals.carbsGoal,
                        fatGoal = goals.fatGoal,
                        waterGoal = goals.waterGoal
                    )
                }
            }
        }
    }
    
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }
    
    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            mealRepository.insertWaterEntry(amountMl)
            // Refresh water total
            val total = mealRepository.getTotalWaterForDate(_selectedDate.value)
            _uiState.update { it.copy(waterConsumed = total) }
        }
    }
    
    /**
     * Delete a meal entry
     */
    fun deleteMealEntry(entry: MealEntry) {
        viewModelScope.launch {
            mealRepository.deleteMealEntry(entry)
        }
    }
    
    /**
     * Update dietary preferences for meal planning
     */
    fun updateDietaryPreferences(preferences: DietaryPreferences) {
        _uiState.update { it.copy(dietaryPreferences = preferences) }
    }
    
    /**
     * Set daily nutrition goals
     */
    fun setDailyGoals(
        calorieGoal: Int,
        proteinGoal: Float,
        carbsGoal: Float,
        fatGoal: Float,
        waterGoal: Int
    ) {
        viewModelScope.launch {
            mealRepository.setDailyGoals(
                _selectedDate.value,
                DailyNutritionGoals(
                    calorieGoal = calorieGoal,
                    proteinGoal = proteinGoal,
                    carbsGoal = carbsGoal,
                    fatGoal = fatGoal,
                    waterGoal = waterGoal
                )
            )
            
            _uiState.update { state ->
                state.copy(
                    calorieGoal = calorieGoal,
                    proteinGoal = proteinGoal,
                    carbsGoal = carbsGoal,
                    fatGoal = fatGoal,
                    waterGoal = waterGoal
                )
            }
        }
    }
    
    /**
     * Generate a daily meal plan using AI
     */
    fun generateDailyMealPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPlan = true) }
            
            try {
                val state = _uiState.value
                
                val dailyPlan = mealPlanGenerator.generateDailyPlan(
                    date = state.selectedDate,
                    targetCalories = state.calorieGoal,
                    targetProtein = state.proteinGoal,
                    targetCarbs = state.carbsGoal,
                    targetFat = state.fatGoal,
                    preferences = state.dietaryPreferences
                )
                
                _uiState.update { 
                    it.copy(
                        todayMealPlan = dailyPlan,
                        isGeneratingPlan = false
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingPlan = false) }
            }
        }
    }
    
    /**
     * Generate a weekly meal plan using AI
     */
    fun generateWeeklyMealPlan(userProfile: UserProfile, fitnessGoals: FitnessGoals) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPlan = true) }
            
            try {
                val state = _uiState.value
                
                val weeklyPlan = mealPlanGenerator.generateWeeklyPlan(
                    startDate = state.selectedDate,
                    goals = fitnessGoals,
                    preferences = state.dietaryPreferences,
                    userProfile = userProfile
                )
                
                _uiState.update { 
                    it.copy(
                        weeklyMealPlan = weeklyPlan,
                        isGeneratingPlan = false
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingPlan = false) }
            }
        }
    }
    
    /**
     * Log a planned meal as actually consumed
     */
    fun logPlannedMeal(plannedMeal: PlannedMeal) {
        viewModelScope.launch {
            // Convert planned foods to food items and meal entries
            for (food in plannedMeal.foods) {
                val foodItem = FoodItem(
                    id = "planned_${food.name.hashCode()}_${System.currentTimeMillis()}",
                    name = food.name,
                    brand = "Meal Plan",
                    servingSize = food.amount,
                    servingUnit = food.unit,
                    nutrition = NutritionInfo(
                        calories = food.calories,
                        protein = food.protein,
                        carbs = food.carbs,
                        fat = food.fat
                    ),
                    isCustom = true
                )
                
                val entry = MealEntry(
                    foodItem = foodItem,
                    servings = 1f,
                    mealType = plannedMeal.mealType,
                    loggedAt = java.time.LocalDateTime.now()
                )
                
                mealRepository.insertMealEntry(entry)
            }
        }
    }
    
    /**
     * Clear the generated meal plan
     */
    fun clearMealPlan() {
        _uiState.update { 
            it.copy(
                todayMealPlan = null,
                weeklyMealPlan = null
            ) 
        }
    }
}
