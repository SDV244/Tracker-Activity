package com.fittrack.ai

import com.fittrack.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════
// 🍽️ AI MEAL PLAN GENERATOR
// Generates personalized meal plans based on goals, preferences, and macros
// ═══════════════════════════════════════════════════════════════

/**
 * User's dietary preferences and restrictions
 */
data class DietaryPreferences(
    val dietType: DietType = DietType.STANDARD,
    val allergies: List<Allergy> = emptyList(),
    val dislikedFoods: List<String> = emptyList(),
    val preferredFoods: List<String> = emptyList(),
    val cuisinePreferences: List<Cuisine> = Cuisine.entries,
    val mealPrepWillingness: MealPrepLevel = MealPrepLevel.MODERATE,
    val budget: BudgetLevel = BudgetLevel.MODERATE,
    val mealsPerDay: Int = 4  // 3 meals + 1 snack
)

enum class DietType(val description: String) {
    STANDARD("Balanced diet"),
    HIGH_PROTEIN("High protein for muscle building"),
    LOW_CARB("Reduced carbohydrates"),
    KETO("Ketogenic - very low carb, high fat"),
    PALEO("Paleo - whole foods, no grains"),
    VEGETARIAN("No meat"),
    VEGAN("No animal products"),
    MEDITERRANEAN("Mediterranean style"),
    INTERMITTENT_FASTING("Time-restricted eating")
}

enum class Allergy {
    GLUTEN, DAIRY, NUTS, EGGS, SOY, SHELLFISH, FISH, SESAME
}

enum class Cuisine {
    AMERICAN, MEXICAN, ITALIAN, ASIAN, INDIAN, MEDITERRANEAN, MIDDLE_EASTERN, LATIN
}

enum class MealPrepLevel {
    MINIMAL,    // Quick meals, minimal cooking
    MODERATE,   // Some meal prep okay
    EXTENSIVE   // Willing to batch cook
}

enum class BudgetLevel {
    LOW, MODERATE, HIGH
}

/**
 * A complete meal plan for one day
 */
data class DailyMealPlan(
    val date: LocalDate,
    val meals: List<PlannedMeal>,
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val shoppingItems: List<ShoppingItem>
)

/**
 * A single planned meal
 */
data class PlannedMeal(
    val mealType: MealType,
    val name: String,
    val description: String,
    val foods: List<PlannedFood>,
    val prepTime: Int,  // minutes
    val cookTime: Int,  // minutes
    val recipe: String? = null,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList()  // "high-protein", "quick", "meal-prep-friendly"
) {
    val totalCalories: Int get() = foods.sumOf { it.calories }
    val totalProtein: Float get() = foods.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs: Float get() = foods.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat: Float get() = foods.sumOf { it.fat.toDouble() }.toFloat()
}

data class PlannedFood(
    val name: String,
    val amount: Float,
    val unit: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

data class ShoppingItem(
    val name: String,
    val amount: Float,
    val unit: String,
    val category: ShoppingCategory,
    val estimatedCost: Float? = null
)

enum class ShoppingCategory {
    PRODUCE, PROTEIN, DAIRY, GRAINS, PANTRY, FROZEN, OTHER
}

/**
 * Weekly meal plan
 */
data class WeeklyMealPlan(
    val startDate: LocalDate,
    val days: List<DailyMealPlan>,
    val weeklyShoppingList: List<ShoppingItem>,
    val estimatedWeeklyCost: Float?,
    val mealPrepSuggestions: List<String>
)

@Singleton
class MealPlanGenerator @Inject constructor() {
    
    // ═══════════════════════════════════════════════════════════════
    // MEAL DATABASE (In production: from API or larger local DB)
    // ═══════════════════════════════════════════════════════════════
    
    private val mealDatabase = listOf(
        // HIGH PROTEIN BREAKFASTS
        MealTemplate(
            name = "Protein Power Eggs",
            mealTypes = listOf(MealType.BREAKFAST),
            foods = listOf(
                FoodTemplate("Whole eggs", 3f, "large", 210, 18f, 1f, 15f),
                FoodTemplate("Egg whites", 100f, "g", 52, 11f, 0.7f, 0.2f),
                FoodTemplate("Spinach", 50f, "g", 12, 1.4f, 1.8f, 0.2f),
                FoodTemplate("Whole wheat toast", 2f, "slices", 160, 8f, 26f, 2f)
            ),
            prepTime = 5, cookTime = 10,
            tags = listOf("high-protein", "quick"),
            dietTypes = listOf(DietType.STANDARD, DietType.HIGH_PROTEIN, DietType.VEGETARIAN)
        ),
        MealTemplate(
            name = "Greek Yogurt Parfait",
            mealTypes = listOf(MealType.BREAKFAST, MealType.SNACK),
            foods = listOf(
                FoodTemplate("Greek yogurt (0%)", 200f, "g", 130, 24f, 8f, 0f),
                FoodTemplate("Mixed berries", 100f, "g", 57, 0.7f, 14f, 0.3f),
                FoodTemplate("Granola", 30f, "g", 140, 3f, 22f, 5f),
                FoodTemplate("Honey", 10f, "g", 30, 0f, 8f, 0f)
            ),
            prepTime = 5, cookTime = 0,
            tags = listOf("quick", "no-cook", "high-protein"),
            dietTypes = listOf(DietType.STANDARD, DietType.HIGH_PROTEIN, DietType.VEGETARIAN)
        ),
        MealTemplate(
            name = "Overnight Protein Oats",
            mealTypes = listOf(MealType.BREAKFAST),
            foods = listOf(
                FoodTemplate("Rolled oats", 50f, "g", 189, 7f, 34f, 3f),
                FoodTemplate("Protein powder", 30f, "g", 120, 24f, 3f, 1f),
                FoodTemplate("Almond milk", 200f, "ml", 30, 1f, 1f, 2.5f),
                FoodTemplate("Banana", 1f, "medium", 105, 1.3f, 27f, 0.4f),
                FoodTemplate("Peanut butter", 15f, "g", 94, 4f, 3f, 8f)
            ),
            prepTime = 5, cookTime = 0,
            tags = listOf("meal-prep-friendly", "high-protein"),
            dietTypes = listOf(DietType.STANDARD, DietType.HIGH_PROTEIN, DietType.VEGETARIAN)
        ),
        
        // HIGH PROTEIN LUNCHES
        MealTemplate(
            name = "Grilled Chicken Salad",
            mealTypes = listOf(MealType.LUNCH),
            foods = listOf(
                FoodTemplate("Chicken breast", 200f, "g", 330, 62f, 0f, 7f),
                FoodTemplate("Mixed greens", 100f, "g", 20, 2f, 3f, 0.3f),
                FoodTemplate("Cherry tomatoes", 100f, "g", 18, 0.9f, 3.9f, 0.2f),
                FoodTemplate("Cucumber", 100f, "g", 15, 0.7f, 3.6f, 0.1f),
                FoodTemplate("Olive oil dressing", 15f, "ml", 120, 0f, 0f, 14f),
                FoodTemplate("Feta cheese", 30f, "g", 75, 4f, 1f, 6f)
            ),
            prepTime = 10, cookTime = 15,
            tags = listOf("high-protein", "low-carb"),
            dietTypes = listOf(DietType.STANDARD, DietType.HIGH_PROTEIN, DietType.LOW_CARB, DietType.MEDITERRANEAN)
        ),
        MealTemplate(
            name = "Turkey & Avocado Wrap",
            mealTypes = listOf(MealType.LUNCH),
            foods = listOf(
                FoodTemplate("Turkey breast slices", 150f, "g", 157, 30f, 3f, 2f),
                FoodTemplate("Whole wheat tortilla", 1f, "large", 140, 5f, 24f, 3f),
                FoodTemplate("Avocado", 0.5f, "medium", 120, 1.5f, 6f, 11f),
                FoodTemplate("Lettuce", 30f, "g", 5, 0.4f, 1f, 0.1f),
                FoodTemplate("Tomato", 50f, "g", 9, 0.4f, 2f, 0.1f)
            ),
            prepTime = 10, cookTime = 0,
            tags = listOf("quick", "high-protein"),
            dietTypes = listOf(DietType.STANDARD, DietType.HIGH_PROTEIN)
        ),
        MealTemplate(
            name = "Salmon Quinoa Bowl",
            mealTypes = listOf(MealType.LUNCH, MealType.DINNER),
            foods = listOf(
                FoodTemplate("Salmon fillet", 150f, "g", 280, 37f, 0f, 14f),
                FoodTemplate("Quinoa (cooked)", 150f, "g", 180, 7f, 32f, 3f),
                FoodTemplate("Broccoli", 100f, "g", 34, 3f, 7f, 0.4f),
                FoodTemplate("Edamame", 50f, "g", 60, 5f, 5f, 2.5f),
                FoodTemplate("Soy sauce", 15f, "ml", 8, 1f, 1f, 0f)
            ),
            prepTime = 10, cookTime = 20,
            tags = listOf("high-protein", "omega-3"),
            dietTypes = listOf(DietType.STANDARD, DietType.HIGH_PROTEIN, DietType.MEDITERRANEAN)
        ),
        
        // HIGH PROTEIN DINNERS
        MealTemplate(
            name = "Lean Beef Stir-Fry",
            mealTypes = listOf(MealType.DINNER),
            foods = listOf(
                FoodTemplate("Lean beef strips", 200f, "g", 300, 50f, 0f, 10f),
                FoodTemplate("Brown rice (cooked)", 150f, "g", 165, 4f, 35f, 1.5f),
                FoodTemplate("Bell peppers", 100f, "g", 31, 1f, 6f, 0.3f),
                FoodTemplate("Snap peas", 50f, "g", 21, 1.5f, 4f, 0.1f),
                FoodTemplate("Garlic", 5f, "g", 7, 0.3f, 1.5f, 0f),
                FoodTemplate("Olive oil", 10f, "ml", 88, 0f, 0f, 10f)
            ),
            prepTime = 15, cookTime = 15,
            tags = listOf("high-protein"),
            dietTypes = listOf(DietType.STANDARD, DietType.HIGH_PROTEIN)
        ),
        MealTemplate(
            name = "Baked Chicken Thighs with Vegetables",
            mealTypes = listOf(MealType.DINNER),
            foods = listOf(
                FoodTemplate("Chicken thighs (skinless)", 250f, "g", 325, 43f, 0f, 16f),
                FoodTemplate("Sweet potato", 200f, "g", 180, 4f, 41f, 0.2f),
                FoodTemplate("Asparagus", 100f, "g", 20, 2.2f, 3.9f, 0.1f),
                FoodTemplate("Olive oil", 15f, "ml", 132, 0f, 0f, 15f),
                FoodTemplate("Herbs & spices", 5f, "g", 5, 0.2f, 1f, 0.1f)
            ),
            prepTime = 10, cookTime = 35,
            tags = listOf("meal-prep-friendly", "high-protein"),
            dietTypes = listOf(DietType.STANDARD, DietType.HIGH_PROTEIN, DietType.PALEO)
        ),
        
        // SNACKS
        MealTemplate(
            name = "Protein Shake",
            mealTypes = listOf(MealType.SNACK),
            foods = listOf(
                FoodTemplate("Whey protein", 35f, "g", 140, 28f, 4f, 2f),
                FoodTemplate("Banana", 1f, "medium", 105, 1.3f, 27f, 0.4f),
                FoodTemplate("Almond milk", 250f, "ml", 38, 1.3f, 1.3f, 3f)
            ),
            prepTime = 3, cookTime = 0,
            tags = listOf("quick", "high-protein", "post-workout"),
            dietTypes = listOf(DietType.STANDARD, DietType.HIGH_PROTEIN, DietType.VEGETARIAN)
        ),
        MealTemplate(
            name = "Cottage Cheese & Fruit",
            mealTypes = listOf(MealType.SNACK),
            foods = listOf(
                FoodTemplate("Cottage cheese (low-fat)", 150f, "g", 120, 18f, 5f, 3f),
                FoodTemplate("Pineapple chunks", 80f, "g", 40, 0.4f, 10f, 0.1f)
            ),
            prepTime = 2, cookTime = 0,
            tags = listOf("quick", "high-protein"),
            dietTypes = listOf(DietType.STANDARD, DietType.HIGH_PROTEIN, DietType.VEGETARIAN)
        ),
        MealTemplate(
            name = "Almonds & Apple",
            mealTypes = listOf(MealType.SNACK),
            foods = listOf(
                FoodTemplate("Almonds", 30f, "g", 173, 6f, 6f, 15f),
                FoodTemplate("Apple", 1f, "medium", 95, 0.5f, 25f, 0.3f)
            ),
            prepTime = 1, cookTime = 0,
            tags = listOf("quick", "portable"),
            dietTypes = listOf(DietType.STANDARD, DietType.PALEO, DietType.VEGETARIAN, DietType.VEGAN)
        )
    )
    
    // ═══════════════════════════════════════════════════════════════
    // PLAN GENERATION
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Generate a personalized meal plan for one day
     */
    suspend fun generateDailyPlan(
        date: LocalDate,
        targetCalories: Int,
        targetProtein: Float,
        targetCarbs: Float,
        targetFat: Float,
        preferences: DietaryPreferences
    ): DailyMealPlan = withContext(Dispatchers.Default) {
        
        val meals = mutableListOf<PlannedMeal>()
        var remainingCalories = targetCalories
        var remainingProtein = targetProtein
        var remainingCarbs = targetCarbs
        var remainingFat = targetFat
        
        // Allocate macros by meal type
        val mealAllocations = when (preferences.mealsPerDay) {
            3 -> mapOf(
                MealType.BREAKFAST to 0.30f,
                MealType.LUNCH to 0.35f,
                MealType.DINNER to 0.35f
            )
            4 -> mapOf(
                MealType.BREAKFAST to 0.25f,
                MealType.LUNCH to 0.30f,
                MealType.DINNER to 0.30f,
                MealType.SNACK to 0.15f
            )
            else -> mapOf(
                MealType.BREAKFAST to 0.20f,
                MealType.SNACK to 0.10f,
                MealType.LUNCH to 0.25f,
                MealType.SNACK to 0.10f,
                MealType.DINNER to 0.25f,
                MealType.SNACK to 0.10f
            )
        }
        
        // Generate each meal
        for ((mealType, allocation) in mealAllocations) {
            val mealCalories = (targetCalories * allocation).toInt()
            val mealProtein = targetProtein * allocation
            
            val selectedMeal = selectBestMeal(
                mealType = mealType,
                targetCalories = mealCalories,
                targetProtein = mealProtein,
                preferences = preferences,
                remainingCalories = remainingCalories,
                remainingProtein = remainingProtein
            )
            
            selectedMeal?.let { template ->
                val plannedMeal = template.toPlannedMeal(mealType)
                meals.add(plannedMeal)
                
                remainingCalories -= plannedMeal.totalCalories
                remainingProtein -= plannedMeal.totalProtein
                remainingCarbs -= plannedMeal.totalCarbs
                remainingFat -= plannedMeal.totalFat
            }
        }
        
        // Generate shopping list
        val shoppingItems = generateShoppingList(meals)
        
        DailyMealPlan(
            date = date,
            meals = meals,
            totalCalories = meals.sumOf { it.totalCalories },
            totalProtein = meals.sumOf { it.totalProtein.toDouble() }.toFloat(),
            totalCarbs = meals.sumOf { it.totalCarbs.toDouble() }.toFloat(),
            totalFat = meals.sumOf { it.totalFat.toDouble() }.toFloat(),
            shoppingItems = shoppingItems
        )
    }
    
    /**
     * Generate a weekly meal plan
     */
    suspend fun generateWeeklyPlan(
        startDate: LocalDate,
        goals: FitnessGoals,
        preferences: DietaryPreferences,
        userProfile: UserProfile
    ): WeeklyMealPlan = withContext(Dispatchers.Default) {
        
        val days = mutableListOf<DailyMealPlan>()
        
        // Calculate daily targets from goals
        val dailyCalories = goals.dailyCalorieGoal
        val dailyProtein = goals.dailyProteinGoal
        val dailyCarbs = goals.dailyCarbsGoal
        val dailyFat = goals.dailyFatGoal
        
        // Generate 7 days
        for (i in 0..6) {
            val date = startDate.plusDays(i.toLong())
            
            // Slight variation in calories based on day
            val dayVariation = when (date.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 1.1f  // More on weekends
                else -> 1.0f
            }
            
            val dailyPlan = generateDailyPlan(
                date = date,
                targetCalories = (dailyCalories * dayVariation).toInt(),
                targetProtein = dailyProtein,
                targetCarbs = dailyCarbs,
                targetFat = dailyFat,
                preferences = preferences
            )
            
            days.add(dailyPlan)
        }
        
        // Consolidate shopping list
        val weeklyShoppingList = consolidateShoppingList(days)
        
        // Generate meal prep suggestions
        val mealPrepSuggestions = generateMealPrepSuggestions(days, preferences)
        
        WeeklyMealPlan(
            startDate = startDate,
            days = days,
            weeklyShoppingList = weeklyShoppingList,
            estimatedWeeklyCost = estimateWeeklyCost(weeklyShoppingList, preferences.budget),
            mealPrepSuggestions = mealPrepSuggestions
        )
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════
    
    private fun selectBestMeal(
        mealType: MealType,
        targetCalories: Int,
        targetProtein: Float,
        preferences: DietaryPreferences,
        remainingCalories: Int,
        remainingProtein: Float
    ): MealTemplate? {
        return mealDatabase
            .filter { it.mealTypes.contains(mealType) }
            .filter { it.dietTypes.contains(preferences.dietType) }
            .filter { meal -> 
                // Check allergies
                preferences.allergies.none { allergy ->
                    meal.containsAllergen(allergy)
                }
            }
            .filter { meal ->
                // Check prep time willingness
                when (preferences.mealPrepWillingness) {
                    MealPrepLevel.MINIMAL -> meal.totalTime <= 20
                    MealPrepLevel.MODERATE -> meal.totalTime <= 40
                    MealPrepLevel.EXTENSIVE -> true
                }
            }
            .minByOrNull { meal ->
                // Score by how close it is to targets
                val caloriesDiff = kotlin.math.abs(meal.totalCalories - targetCalories)
                val proteinDiff = kotlin.math.abs(meal.totalProtein - targetProtein) * 10
                caloriesDiff + proteinDiff
            }
    }
    
    private fun generateShoppingList(meals: List<PlannedMeal>): List<ShoppingItem> {
        val itemMap = mutableMapOf<String, ShoppingItem>()
        
        for (meal in meals) {
            for (food in meal.foods) {
                val key = food.name.lowercase()
                val existing = itemMap[key]
                
                if (existing != null) {
                    itemMap[key] = existing.copy(
                        amount = existing.amount + food.amount
                    )
                } else {
                    itemMap[key] = ShoppingItem(
                        name = food.name,
                        amount = food.amount,
                        unit = food.unit,
                        category = categorizeFood(food.name)
                    )
                }
            }
        }
        
        return itemMap.values.toList().sortedBy { it.category.ordinal }
    }
    
    private fun consolidateShoppingList(days: List<DailyMealPlan>): List<ShoppingItem> {
        val allItems = days.flatMap { it.shoppingItems }
        val consolidated = mutableMapOf<String, ShoppingItem>()
        
        for (item in allItems) {
            val key = item.name.lowercase()
            val existing = consolidated[key]
            
            if (existing != null) {
                consolidated[key] = existing.copy(
                    amount = existing.amount + item.amount
                )
            } else {
                consolidated[key] = item
            }
        }
        
        return consolidated.values.toList().sortedBy { it.category.ordinal }
    }
    
    private fun categorizeFood(name: String): ShoppingCategory {
        val lower = name.lowercase()
        return when {
            lower.contains("chicken") || lower.contains("beef") || 
            lower.contains("turkey") || lower.contains("salmon") ||
            lower.contains("egg") || lower.contains("fish") -> ShoppingCategory.PROTEIN
            
            lower.contains("milk") || lower.contains("yogurt") ||
            lower.contains("cheese") || lower.contains("cottage") -> ShoppingCategory.DAIRY
            
            lower.contains("rice") || lower.contains("oat") ||
            lower.contains("bread") || lower.contains("tortilla") ||
            lower.contains("quinoa") -> ShoppingCategory.GRAINS
            
            lower.contains("spinach") || lower.contains("broccoli") ||
            lower.contains("tomato") || lower.contains("pepper") ||
            lower.contains("banana") || lower.contains("apple") ||
            lower.contains("berry") || lower.contains("avocado") -> ShoppingCategory.PRODUCE
            
            lower.contains("oil") || lower.contains("honey") ||
            lower.contains("protein powder") || lower.contains("almond") -> ShoppingCategory.PANTRY
            
            else -> ShoppingCategory.OTHER
        }
    }
    
    private fun generateMealPrepSuggestions(
        days: List<DailyMealPlan>,
        preferences: DietaryPreferences
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (preferences.mealPrepWillingness != MealPrepLevel.MINIMAL) {
            suggestions.add("🥗 Prep salad bases for the week on Sunday - wash and chop greens, store in containers")
            suggestions.add("🍗 Batch cook chicken breasts on Sunday - slice for salads and wraps throughout the week")
            suggestions.add("🍚 Cook a large batch of rice/quinoa - portion into containers for easy meal assembly")
            suggestions.add("🥚 Hard boil a dozen eggs for quick protein snacks")
        }
        
        suggestions.add("📦 Pre-portion snacks into grab-and-go containers")
        suggestions.add("🥤 Prep overnight oats in mason jars for quick breakfasts")
        
        return suggestions
    }
    
    private fun estimateWeeklyCost(
        items: List<ShoppingItem>,
        budget: BudgetLevel
    ): Float {
        // Rough cost estimates per category (in Colombian Pesos)
        val baseCost = items.sumOf { item ->
            when (item.category) {
                ShoppingCategory.PROTEIN -> 15000.0 * (item.amount / 500f)
                ShoppingCategory.DAIRY -> 8000.0 * (item.amount / 500f)
                ShoppingCategory.PRODUCE -> 5000.0 * (item.amount / 500f)
                ShoppingCategory.GRAINS -> 4000.0 * (item.amount / 500f)
                ShoppingCategory.PANTRY -> 10000.0 * (item.amount / 500f)
                else -> 5000.0 * (item.amount / 500f)
            }
        }.toFloat()
        
        return when (budget) {
            BudgetLevel.LOW -> baseCost * 0.8f
            BudgetLevel.MODERATE -> baseCost
            BudgetLevel.HIGH -> baseCost * 1.3f
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // DATA CLASSES FOR TEMPLATES
    // ═══════════════════════════════════════════════════════════════
    
    private data class MealTemplate(
        val name: String,
        val mealTypes: List<MealType>,
        val foods: List<FoodTemplate>,
        val prepTime: Int,
        val cookTime: Int,
        val tags: List<String>,
        val dietTypes: List<DietType>,
        val recipe: String? = null
    ) {
        val totalCalories: Int get() = foods.sumOf { it.calories }
        val totalProtein: Float get() = foods.sumOf { it.protein.toDouble() }.toFloat()
        val totalCarbs: Float get() = foods.sumOf { it.carbs.toDouble() }.toFloat()
        val totalFat: Float get() = foods.sumOf { it.fat.toDouble() }.toFloat()
        val totalTime: Int get() = prepTime + cookTime
        
        fun containsAllergen(allergy: Allergy): Boolean {
            val allergenFoods = when (allergy) {
                Allergy.GLUTEN -> listOf("bread", "tortilla", "oat", "wheat")
                Allergy.DAIRY -> listOf("milk", "yogurt", "cheese", "cottage")
                Allergy.NUTS -> listOf("almond", "peanut", "walnut", "cashew")
                Allergy.EGGS -> listOf("egg")
                Allergy.SOY -> listOf("soy", "tofu", "edamame")
                Allergy.SHELLFISH -> listOf("shrimp", "lobster", "crab")
                Allergy.FISH -> listOf("salmon", "tuna", "fish")
                Allergy.SESAME -> listOf("sesame")
            }
            return foods.any { food ->
                allergenFoods.any { allergen ->
                    food.name.lowercase().contains(allergen)
                }
            }
        }
        
        fun toPlannedMeal(mealType: MealType): PlannedMeal {
            return PlannedMeal(
                mealType = mealType,
                name = name,
                description = generateDescription(),
                foods = foods.map { it.toPlannedFood() },
                prepTime = prepTime,
                cookTime = cookTime,
                recipe = recipe,
                tags = tags
            )
        }
        
        private fun generateDescription(): String {
            return "$totalCalories kcal • ${totalProtein.toInt()}g protein • ${totalTime}min"
        }
    }
    
    private data class FoodTemplate(
        val name: String,
        val amount: Float,
        val unit: String,
        val calories: Int,
        val protein: Float,
        val carbs: Float,
        val fat: Float
    ) {
        fun toPlannedFood(): PlannedFood {
            return PlannedFood(
                name = name,
                amount = amount,
                unit = unit,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat
            )
        }
    }
}
