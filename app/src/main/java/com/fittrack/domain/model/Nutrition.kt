package com.fittrack.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Represents nutritional information for a food item
 */
data class NutritionInfo(
    val calories: Int,
    val protein: Float,      // grams
    val carbs: Float,        // grams
    val fat: Float,          // grams
    val fiber: Float = 0f,   // grams
    val sugar: Float = 0f,   // grams
    val sodium: Float = 0f,  // mg
)

/**
 * A food item from the database or custom entry
 */
data class FoodItem(
    val id: String,
    val name: String,
    val brand: String? = null,
    val servingSize: Float,
    val servingUnit: String,
    val nutrition: NutritionInfo,
    val barcode: String? = null,
    val imageUrl: String? = null,
    val isCustom: Boolean = false
)

/**
 * Meal types throughout the day
 */
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK
}

/**
 * A logged meal entry
 */
data class MealEntry(
    val id: Long = 0,
    val foodItem: FoodItem,
    val servings: Float,      // multiplier for serving size
    val mealType: MealType,
    val loggedAt: LocalDateTime,
    val notes: String? = null
) {
    val totalCalories: Int
        get() = (foodItem.nutrition.calories * servings).toInt()
    
    val totalProtein: Float
        get() = foodItem.nutrition.protein * servings
    
    val totalCarbs: Float
        get() = foodItem.nutrition.carbs * servings
    
    val totalFat: Float
        get() = foodItem.nutrition.fat * servings
}

/**
 * Daily nutrition summary
 */
data class DailyNutritionSummary(
    val date: LocalDate,
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val calorieGoal: Int,
    val proteinGoal: Float,
    val carbsGoal: Float,
    val fatGoal: Float,
    val meals: List<MealEntry>
) {
    val caloriesRemaining: Int
        get() = calorieGoal - totalCalories
    
    val proteinRemaining: Float
        get() = proteinGoal - totalProtein
    
    val carbsRemaining: Float
        get() = carbsGoal - totalCarbs
    
    val fatRemaining: Float
        get() = fatGoal - totalFat
    
    val calorieProgress: Float
        get() = if (calorieGoal > 0) totalCalories.toFloat() / calorieGoal else 0f
}

/**
 * Water intake entry
 */
data class WaterEntry(
    val id: Long = 0,
    val amountMl: Int,
    val loggedAt: LocalDateTime
)
