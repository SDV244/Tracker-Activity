package com.fittrack.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Health data from wearables via Health Connect
 */
data class HealthData(
    val date: LocalDate,
    val steps: Int = 0,
    val activeCalories: Int = 0,
    val totalCalories: Int = 0,
    val distance: Float = 0f,           // in km
    val activeMinutes: Int = 0,
    val heartRateSamples: List<HeartRateSample> = emptyList(),
    val sleepData: SleepData? = null,
    val weight: Float? = null           // in kg
)

/**
 * Heart rate sample
 */
data class HeartRateSample(
    val bpm: Int,
    val timestamp: LocalDateTime
) {
    companion object {
        fun getZone(bpm: Int, maxHeartRate: Int): HeartRateZone {
            val percentage = bpm.toFloat() / maxHeartRate
            return when {
                percentage < 0.5f -> HeartRateZone.REST
                percentage < 0.6f -> HeartRateZone.WARM_UP
                percentage < 0.7f -> HeartRateZone.FAT_BURN
                percentage < 0.8f -> HeartRateZone.CARDIO
                percentage < 0.9f -> HeartRateZone.HARD
                else -> HeartRateZone.MAXIMUM
            }
        }
    }
}

enum class HeartRateZone {
    REST,
    WARM_UP,
    FAT_BURN,
    CARDIO,
    HARD,
    MAXIMUM
}

/**
 * Sleep tracking data
 */
data class SleepData(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val stages: List<SleepStage> = emptyList()
) {
    val totalDurationMinutes: Int
        get() = java.time.Duration.between(startTime, endTime).toMinutes().toInt()
    
    val deepSleepMinutes: Int
        get() = stages.filter { it.stage == SleepStageType.DEEP }
            .sumOf { java.time.Duration.between(it.startTime, it.endTime).toMinutes().toInt() }
    
    val remSleepMinutes: Int
        get() = stages.filter { it.stage == SleepStageType.REM }
            .sumOf { java.time.Duration.between(it.startTime, it.endTime).toMinutes().toInt() }
}

data class SleepStage(
    val stage: SleepStageType,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
)

enum class SleepStageType {
    AWAKE,
    LIGHT,
    DEEP,
    REM
}

/**
 * Daily summary combining all health data
 */
data class DailyHealthSummary(
    val date: LocalDate,
    val nutrition: DailyNutritionSummary?,
    val healthData: HealthData?,
    val workouts: List<Workout>
) {
    val netCalories: Int
        get() {
            val consumed = nutrition?.totalCalories ?: 0
            val burned = healthData?.totalCalories ?: 0
            return consumed - burned
        }
    
    val caloriesBurnedFromWorkouts: Int
        get() = workouts.sumOf { it.caloriesBurned ?: 0 }
}

// FitnessGoals, WeightGoalType, UserProfile, Gender, and ActivityLevel 
// are defined in UserProfile.kt
