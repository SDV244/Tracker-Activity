package com.fittrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.fittrack.data.local.dao.ExerciseDao
import com.fittrack.data.local.dao.MealDao
import com.fittrack.data.local.entity.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Database(
    entities = [
        // Nutrition
        FoodItemEntity::class,
        MealEntryEntity::class,
        WaterEntryEntity::class,
        DailyGoalsEntity::class,
        // Exercise
        ExerciseEntity::class,
        WorkoutEntity::class,
        ExerciseLogEntity::class,
        ExerciseSetEntity::class,
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        PersonalRecordEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun exerciseDao(): ExerciseDao
}

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(formatter)
    }
    
    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }
}
