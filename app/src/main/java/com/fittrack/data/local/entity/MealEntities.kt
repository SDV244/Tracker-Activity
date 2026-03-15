package com.fittrack.data.local.entity

import androidx.room.*
import java.time.LocalDateTime

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String?,
    val servingSize: Float,
    val servingUnit: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val sugar: Float,
    val sodium: Float,
    val barcode: String?,
    val imageUrl: String?,
    val isCustom: Boolean,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity(
    tableName = "meal_entries",
    foreignKeys = [
        ForeignKey(
            entity = FoodItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("foodItemId"), Index("loggedAt")]
)
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodItemId: String,
    val servings: Float,
    val mealType: String,  // BREAKFAST, LUNCH, DINNER, SNACK
    val loggedAt: LocalDateTime,
    val notes: String?
)

data class MealEntryWithFood(
    @Embedded val mealEntry: MealEntryEntity,
    @Relation(
        parentColumn = "foodItemId",
        entityColumn = "id"
    )
    val foodItem: FoodItemEntity
)

@Entity(tableName = "water_entries")
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMl: Int,
    val loggedAt: LocalDateTime
)

@Entity(tableName = "daily_goals")
data class DailyGoalsEntity(
    @PrimaryKey val date: String,  // LocalDate as string
    val calorieGoal: Int,
    val proteinGoal: Float,
    val carbsGoal: Float,
    val fatGoal: Float,
    val waterGoal: Int
)
