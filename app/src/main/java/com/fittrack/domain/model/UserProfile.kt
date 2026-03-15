package com.fittrack.domain.model

import java.time.LocalDate

// ═══════════════════════════════════════════════════════════════
// 👤 USER PROFILE & FITNESS GOALS
// Complete user configuration for personalized experience
// ═══════════════════════════════════════════════════════════════

/**
 * Complete user profile
 */
data class UserProfile(
    val id: Long = 0,
    val name: String = "",
    val email: String? = null,
    val dateOfBirth: LocalDate? = null,
    val gender: Gender? = null,
    val height: Float? = null,  // cm
    val currentWeight: Float? = null,  // kg
    
    // Fitness-specific
    val activityLevel: ActivityLevel = ActivityLevel.MODERATELY_ACTIVE,
    val fitnessExperience: FitnessExperience = FitnessExperience.INTERMEDIATE,
    val primaryGoal: FitnessGoalType = FitnessGoalType.BUILD_MUSCLE,
    
    // Preferences
    val preferredUnits: UnitSystem = UnitSystem.METRIC,
    val timezone: String = "America/Bogota",
    
    // Calculated
    val bmr: Float? = null,
    val tdee: Float? = null
) {
    val age: Int?
        get() = dateOfBirth?.let {
            java.time.Period.between(it, LocalDate.now()).years
        }
    
    /**
     * Calculate BMR using Mifflin-St Jeor equation
     */
    fun calculateBMR(): Float? {
        val w = currentWeight ?: return null
        val h = height ?: return null
        val a = age ?: return null
        
        return when (gender) {
            Gender.MALE -> 10f * w + 6.25f * h - 5f * a + 5f
            Gender.FEMALE -> 10f * w + 6.25f * h - 5f * a - 161f
            else -> 10f * w + 6.25f * h - 5f * a - 78f  // Average
        }
    }
    
    /**
     * Calculate TDEE based on activity level
     */
    fun calculateTDEE(): Float? {
        val baseBmr = calculateBMR() ?: return null
        
        return baseBmr * when (activityLevel) {
            ActivityLevel.SEDENTARY -> 1.2f
            ActivityLevel.LIGHTLY_ACTIVE -> 1.375f
            ActivityLevel.MODERATELY_ACTIVE -> 1.55f
            ActivityLevel.VERY_ACTIVE -> 1.725f
            ActivityLevel.EXTREMELY_ACTIVE -> 1.9f
        }
    }
}

enum class Gender {
    MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
}

enum class ActivityLevel(val description: String) {
    SEDENTARY("Little or no exercise"),
    LIGHTLY_ACTIVE("Light exercise 1-3 days/week"),
    MODERATELY_ACTIVE("Moderate exercise 3-5 days/week"),
    VERY_ACTIVE("Hard exercise 6-7 days/week"),
    EXTREMELY_ACTIVE("Very hard exercise + physical job")
}

enum class FitnessExperience(val monthsTraining: IntRange) {
    BEGINNER(0..6),
    INTERMEDIATE(6..24),
    ADVANCED(24..60),
    ELITE(60..Int.MAX_VALUE)
}

enum class UnitSystem {
    METRIC,   // kg, cm
    IMPERIAL  // lbs, inches
}

/**
 * User's fitness goals
 */
data class FitnessGoals(
    val id: Long = 0,
    val userId: Long = 0,
    
    // Weight goals
    val targetWeight: Float? = null,
    val weightGoalType: WeightGoalType = WeightGoalType.MAINTAIN,
    val weeklyWeightChange: Float = 0f,  // kg/week (+ for gain, - for loss)
    
    // Nutrition goals
    val dailyCalorieGoal: Int = 2000,
    val dailyProteinGoal: Float = 150f,  // grams
    val dailyCarbsGoal: Float = 200f,
    val dailyFatGoal: Float = 65f,
    val dailyFiberGoal: Float = 25f,
    val dailyWaterGoal: Float = 2500f,  // ml
    
    // Activity goals
    val weeklyWorkoutsGoal: Int = 4,
    val dailyStepsGoal: Int = 10000,
    val weeklyCardioMinutes: Int = 150,
    val dailyActiveCalories: Int = 500,
    
    // Sleep goals
    val sleepHoursGoal: Float = 8f,
    
    // Goal type
    val goalType: FitnessGoalType = FitnessGoalType.BUILD_MUSCLE,
    val targetDate: LocalDate? = null,
    
    // Notes
    val motivation: String? = null,
    val notes: String? = null
)

enum class FitnessGoalType {
    BUILD_MUSCLE,
    LOSE_FAT,
    RECOMP,        // Lose fat + gain muscle
    MAINTAIN,
    IMPROVE_HEALTH,
    TRAIN_FOR_EVENT,
    STRENGTH,
    ENDURANCE
}

enum class WeightGoalType {
    LOSE, GAIN, MAINTAIN
}

/**
 * Body measurements tracking
 */
data class BodyMeasurements(
    val id: Long = 0,
    val userId: Long = 0,
    val date: LocalDate = LocalDate.now(),
    
    // Core measurements (cm)
    val neck: Float? = null,
    val shoulders: Float? = null,
    val chest: Float? = null,
    val waist: Float? = null,
    val hips: Float? = null,
    
    // Arms (cm)
    val leftBicep: Float? = null,
    val rightBicep: Float? = null,
    val leftForearm: Float? = null,
    val rightForearm: Float? = null,
    
    // Legs (cm)
    val leftThigh: Float? = null,
    val rightThigh: Float? = null,
    val leftCalf: Float? = null,
    val rightCalf: Float? = null,
    
    // Weight & composition
    val weight: Float? = null,
    val bodyFatPercent: Float? = null,
    val muscleMassKg: Float? = null,
    
    // Notes
    val notes: String? = null,
    val photoId: Long? = null  // Link to progress photo
) {
    /**
     * Calculate body fat using Navy method
     */
    fun calculateNavyBodyFat(gender: Gender?, height: Float?): Float? {
        val w = waist ?: return null
        val n = neck ?: return null
        val h = height ?: return null
        
        return when (gender) {
            Gender.MALE -> {
                495f / (1.0324f - 0.19077f * kotlin.math.log10(w - n) + 0.15456f * kotlin.math.log10(h)) - 450f
            }
            Gender.FEMALE -> {
                val hip = hips ?: return null
                495f / (1.29579f - 0.35004f * kotlin.math.log10(w + hip - n) + 0.22100f * kotlin.math.log10(h)) - 450f
            }
            else -> null
        }
    }
}

/**
 * Weight entry for tracking over time
 */
data class WeightEntry(
    val id: Long = 0,
    val userId: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val weight: Float,  // kg
    val bodyFatPercent: Float? = null,
    val muscleMassPercent: Float? = null,
    val waterPercent: Float? = null,
    val notes: String? = null,
    val source: WeightSource = WeightSource.MANUAL
)

enum class WeightSource {
    MANUAL,
    SMART_SCALE,
    HEALTH_CONNECT,
    APPLE_HEALTH
}

/**
 * Achievement/badge earned
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val earnedDate: LocalDate? = null,
    val progress: Float = 0f,  // 0-1 for in-progress achievements
    val category: AchievementCategory,
    val tier: AchievementTier = AchievementTier.BRONZE
)

enum class AchievementCategory {
    WORKOUT,      // Workout streaks, PRs
    NUTRITION,    // Logging streaks, macro hits
    WEIGHT,       // Weight milestones
    STEPS,        // Step goals
    CONSISTENCY,  // Overall consistency
    SPECIAL       // Special achievements
}

enum class AchievementTier {
    BRONZE, SILVER, GOLD, PLATINUM, DIAMOND
}
