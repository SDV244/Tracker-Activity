package com.fittrack.data.repository

import com.fittrack.data.local.dao.MealDao
import com.fittrack.data.local.dao.NutritionTotals
import com.fittrack.data.local.entity.*
import com.fittrack.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for meal and nutrition data
 */
interface MealRepository {
    // Food Items
    suspend fun insertFoodItem(food: FoodItem)
    suspend fun insertFoodItems(foods: List<FoodItem>)
    suspend fun getFoodItemById(id: String): FoodItem?
    suspend fun getFoodItemByBarcode(barcode: String): FoodItem?
    fun searchFoodItems(query: String): Flow<List<FoodItem>>
    fun getCustomFoodItems(): Flow<List<FoodItem>>
    fun getRecentFoodItems(limit: Int = 20): Flow<List<FoodItem>>
    suspend fun deleteFoodItem(food: FoodItem)
    
    // Meal Entries
    suspend fun insertMealEntry(entry: MealEntry): Long
    suspend fun updateMealEntry(entry: MealEntry)
    suspend fun deleteMealEntry(entry: MealEntry)
    fun getMealEntriesForDate(date: LocalDate): Flow<List<MealEntry>>
    fun getMealEntriesForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<MealEntry>>
    fun getMealEntriesByType(date: LocalDate, mealType: MealType): Flow<List<MealEntry>>
    suspend fun getDailyNutritionTotals(date: LocalDate): NutritionTotals?
    
    // Water Entries
    suspend fun insertWaterEntry(amountMl: Int): Long
    suspend fun deleteWaterEntry(id: Long)
    fun getWaterEntriesForDate(date: LocalDate): Flow<List<WaterEntry>>
    suspend fun getTotalWaterForDate(date: LocalDate): Int
    
    // Daily Goals
    suspend fun setDailyGoals(date: LocalDate, goals: DailyNutritionGoals)
    suspend fun getDailyGoals(date: LocalDate): DailyNutritionGoals?
}

/**
 * Daily nutrition goals
 */
data class DailyNutritionGoals(
    val calorieGoal: Int = 2000,
    val proteinGoal: Float = 150f,
    val carbsGoal: Float = 200f,
    val fatGoal: Float = 65f,
    val waterGoal: Int = 2500
)

/**
 * Implementation of MealRepository
 */
@Singleton
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao
) : MealRepository {
    
    // ═══════════════════════════════════════════════════════════════
    // FOOD ITEMS
    // ═══════════════════════════════════════════════════════════════
    
    override suspend fun insertFoodItem(food: FoodItem) {
        mealDao.insertFoodItem(food.toEntity())
    }
    
    override suspend fun insertFoodItems(foods: List<FoodItem>) {
        mealDao.insertFoodItems(foods.map { it.toEntity() })
    }
    
    override suspend fun getFoodItemById(id: String): FoodItem? {
        return mealDao.getFoodItemById(id)?.toDomainModel()
    }
    
    override suspend fun getFoodItemByBarcode(barcode: String): FoodItem? {
        return mealDao.getFoodItemByBarcode(barcode)?.toDomainModel()
    }
    
    override fun searchFoodItems(query: String): Flow<List<FoodItem>> {
        return mealDao.searchFoodItems(query).map { list ->
            list.map { it.toDomainModel() }
        }
    }
    
    override fun getCustomFoodItems(): Flow<List<FoodItem>> {
        return mealDao.getCustomFoodItems().map { list ->
            list.map { it.toDomainModel() }
        }
    }
    
    override fun getRecentFoodItems(limit: Int): Flow<List<FoodItem>> {
        return mealDao.getRecentFoodItems(limit).map { list ->
            list.map { it.toDomainModel() }
        }
    }
    
    override suspend fun deleteFoodItem(food: FoodItem) {
        mealDao.deleteFoodItem(food.toEntity())
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MEAL ENTRIES
    // ═══════════════════════════════════════════════════════════════
    
    override suspend fun insertMealEntry(entry: MealEntry): Long {
        // Ensure food item exists in database
        mealDao.insertFoodItem(entry.foodItem.toEntity())
        
        return mealDao.insertMealEntry(
            MealEntryEntity(
                id = entry.id,
                foodItemId = entry.foodItem.id,
                servings = entry.servings,
                mealType = entry.mealType.name,
                loggedAt = entry.loggedAt,
                notes = entry.notes
            )
        )
    }
    
    override suspend fun updateMealEntry(entry: MealEntry) {
        mealDao.updateMealEntry(
            MealEntryEntity(
                id = entry.id,
                foodItemId = entry.foodItem.id,
                servings = entry.servings,
                mealType = entry.mealType.name,
                loggedAt = entry.loggedAt,
                notes = entry.notes
            )
        )
    }
    
    override suspend fun deleteMealEntry(entry: MealEntry) {
        mealDao.deleteMealEntry(
            MealEntryEntity(
                id = entry.id,
                foodItemId = entry.foodItem.id,
                servings = entry.servings,
                mealType = entry.mealType.name,
                loggedAt = entry.loggedAt,
                notes = entry.notes
            )
        )
    }
    
    override fun getMealEntriesForDate(date: LocalDate): Flow<List<MealEntry>> {
        val start = LocalDateTime.of(date, LocalTime.MIN)
        val end = LocalDateTime.of(date, LocalTime.MAX)
        return getMealEntriesForDateRange(start, end)
    }
    
    override fun getMealEntriesForDateRange(
        start: LocalDateTime, 
        end: LocalDateTime
    ): Flow<List<MealEntry>> {
        return mealDao.getMealEntriesForDateRange(start, end).map { list ->
            list.map { it.toDomainModel() }
        }
    }
    
    override fun getMealEntriesByType(date: LocalDate, mealType: MealType): Flow<List<MealEntry>> {
        val start = LocalDateTime.of(date, LocalTime.MIN)
        val end = LocalDateTime.of(date, LocalTime.MAX)
        return mealDao.getMealEntriesByType(start, end, mealType.name).map { list ->
            list.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getDailyNutritionTotals(date: LocalDate): NutritionTotals? {
        val start = LocalDateTime.of(date, LocalTime.MIN)
        val end = LocalDateTime.of(date, LocalTime.MAX)
        return mealDao.getDailyNutritionTotals(start, end)
    }
    
    // ═══════════════════════════════════════════════════════════════
    // WATER ENTRIES
    // ═══════════════════════════════════════════════════════════════
    
    override suspend fun insertWaterEntry(amountMl: Int): Long {
        return mealDao.insertWaterEntry(
            WaterEntryEntity(
                amountMl = amountMl,
                loggedAt = LocalDateTime.now()
            )
        )
    }
    
    override suspend fun deleteWaterEntry(id: Long) {
        mealDao.deleteWaterEntry(
            WaterEntryEntity(id = id, amountMl = 0, loggedAt = LocalDateTime.now())
        )
    }
    
    override fun getWaterEntriesForDate(date: LocalDate): Flow<List<WaterEntry>> {
        val start = LocalDateTime.of(date, LocalTime.MIN)
        val end = LocalDateTime.of(date, LocalTime.MAX)
        return mealDao.getWaterEntriesForDateRange(start, end).map { list ->
            list.map { WaterEntry(id = it.id, amountMl = it.amountMl, loggedAt = it.loggedAt) }
        }
    }
    
    override suspend fun getTotalWaterForDate(date: LocalDate): Int {
        val start = LocalDateTime.of(date, LocalTime.MIN)
        val end = LocalDateTime.of(date, LocalTime.MAX)
        return mealDao.getTotalWaterForDateRange(start, end)
    }
    
    // ═══════════════════════════════════════════════════════════════
    // DAILY GOALS
    // ═══════════════════════════════════════════════════════════════
    
    override suspend fun setDailyGoals(date: LocalDate, goals: DailyNutritionGoals) {
        mealDao.setDailyGoals(
            DailyGoalsEntity(
                date = date.toString(),
                calorieGoal = goals.calorieGoal,
                proteinGoal = goals.proteinGoal,
                carbsGoal = goals.carbsGoal,
                fatGoal = goals.fatGoal,
                waterGoal = goals.waterGoal
            )
        )
    }
    
    override suspend fun getDailyGoals(date: LocalDate): DailyNutritionGoals? {
        return mealDao.getDailyGoals(date.toString())?.let { entity ->
            DailyNutritionGoals(
                calorieGoal = entity.calorieGoal,
                proteinGoal = entity.proteinGoal,
                carbsGoal = entity.carbsGoal,
                fatGoal = entity.fatGoal,
                waterGoal = entity.waterGoal
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS - Entity <-> Domain Conversion
// ═══════════════════════════════════════════════════════════════

private fun FoodItemEntity.toDomainModel(): FoodItem {
    return FoodItem(
        id = id,
        name = name,
        brand = brand,
        servingSize = servingSize,
        servingUnit = servingUnit,
        nutrition = NutritionInfo(
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            fiber = fiber,
            sugar = sugar,
            sodium = sodium
        ),
        barcode = barcode,
        imageUrl = imageUrl,
        isCustom = isCustom
    )
}

private fun FoodItem.toEntity(): FoodItemEntity {
    return FoodItemEntity(
        id = id,
        name = name,
        brand = brand,
        servingSize = servingSize,
        servingUnit = servingUnit,
        calories = nutrition.calories,
        protein = nutrition.protein,
        carbs = nutrition.carbs,
        fat = nutrition.fat,
        fiber = nutrition.fiber,
        sugar = nutrition.sugar,
        sodium = nutrition.sodium,
        barcode = barcode,
        imageUrl = imageUrl,
        isCustom = isCustom
    )
}

private fun MealEntryWithFood.toDomainModel(): MealEntry {
    val nutrition = NutritionInfo(
        calories = foodItem.calories,
        protein = foodItem.protein,
        carbs = foodItem.carbs,
        fat = foodItem.fat,
        fiber = foodItem.fiber,
        sugar = foodItem.sugar,
        sodium = foodItem.sodium
    )
    
    val food = FoodItem(
        id = foodItem.id,
        name = foodItem.name,
        brand = foodItem.brand,
        servingSize = foodItem.servingSize,
        servingUnit = foodItem.servingUnit,
        nutrition = nutrition,
        barcode = foodItem.barcode,
        imageUrl = foodItem.imageUrl,
        isCustom = foodItem.isCustom
    )
    
    return MealEntry(
        id = mealEntry.id,
        foodItem = food,
        servings = mealEntry.servings,
        mealType = MealType.valueOf(mealEntry.mealType),
        loggedAt = mealEntry.loggedAt,
        notes = mealEntry.notes
    )
}
