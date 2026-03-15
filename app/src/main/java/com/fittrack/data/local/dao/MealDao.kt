package com.fittrack.data.local.dao

import androidx.room.*
import com.fittrack.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MealDao {
    
    // Food Items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(food: FoodItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(foods: List<FoodItemEntity>)
    
    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFoodItemById(id: String): FoodItemEntity?
    
    @Query("SELECT * FROM food_items WHERE barcode = :barcode")
    suspend fun getFoodItemByBarcode(barcode: String): FoodItemEntity?
    
    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' ORDER BY name")
    fun searchFoodItems(query: String): Flow<List<FoodItemEntity>>
    
    @Query("SELECT * FROM food_items WHERE isCustom = 1 ORDER BY createdAt DESC")
    fun getCustomFoodItems(): Flow<List<FoodItemEntity>>
    
    @Query("SELECT * FROM food_items ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentFoodItems(limit: Int = 20): Flow<List<FoodItemEntity>>
    
    @Delete
    suspend fun deleteFoodItem(food: FoodItemEntity)
    
    // Meal Entries
    @Insert
    suspend fun insertMealEntry(entry: MealEntryEntity): Long
    
    @Update
    suspend fun updateMealEntry(entry: MealEntryEntity)
    
    @Delete
    suspend fun deleteMealEntry(entry: MealEntryEntity)
    
    @Transaction
    @Query("""
        SELECT * FROM meal_entries 
        WHERE loggedAt >= :start AND loggedAt < :end 
        ORDER BY loggedAt DESC
    """)
    fun getMealEntriesForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<MealEntryWithFood>>
    
    @Transaction
    @Query("""
        SELECT * FROM meal_entries 
        WHERE loggedAt >= :start AND loggedAt < :end 
        AND mealType = :mealType
        ORDER BY loggedAt DESC
    """)
    fun getMealEntriesByType(
        start: LocalDateTime, 
        end: LocalDateTime, 
        mealType: String
    ): Flow<List<MealEntryWithFood>>
    
    @Query("""
        SELECT 
            SUM(fi.calories * me.servings) as totalCalories,
            SUM(fi.protein * me.servings) as totalProtein,
            SUM(fi.carbs * me.servings) as totalCarbs,
            SUM(fi.fat * me.servings) as totalFat
        FROM meal_entries me
        INNER JOIN food_items fi ON me.foodItemId = fi.id
        WHERE me.loggedAt >= :start AND me.loggedAt < :end
    """)
    suspend fun getDailyNutritionTotals(start: LocalDateTime, end: LocalDateTime): NutritionTotals?
    
    // Water Entries
    @Insert
    suspend fun insertWaterEntry(entry: WaterEntryEntity): Long
    
    @Delete
    suspend fun deleteWaterEntry(entry: WaterEntryEntity)
    
    @Query("""
        SELECT * FROM water_entries 
        WHERE loggedAt >= :start AND loggedAt < :end 
        ORDER BY loggedAt DESC
    """)
    fun getWaterEntriesForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<WaterEntryEntity>>
    
    @Query("""
        SELECT COALESCE(SUM(amountMl), 0) 
        FROM water_entries 
        WHERE loggedAt >= :start AND loggedAt < :end
    """)
    suspend fun getTotalWaterForDateRange(start: LocalDateTime, end: LocalDateTime): Int
    
    // Daily Goals
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setDailyGoals(goals: DailyGoalsEntity)
    
    @Query("SELECT * FROM daily_goals WHERE date = :date")
    suspend fun getDailyGoals(date: String): DailyGoalsEntity?
}

data class NutritionTotals(
    val totalCalories: Int?,
    val totalProtein: Float?,
    val totalCarbs: Float?,
    val totalFat: Float?
)
