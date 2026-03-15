package com.fittrack.domain.model

import java.time.Duration
import java.time.LocalDateTime

/**
 * Categories of exercises
 */
enum class ExerciseCategory {
    CHEST,
    BACK,
    SHOULDERS,
    ARMS,
    LEGS,
    CORE,
    CARDIO,
    FULL_BODY,
    OTHER
}

/**
 * Type of exercise tracking
 */
enum class ExerciseType {
    WEIGHT_REPS,      // Standard weight training (e.g., bench press)
    BODYWEIGHT_REPS,  // Bodyweight exercises (e.g., push-ups)
    DURATION,         // Time-based (e.g., plank)
    CARDIO,           // Cardio with distance/duration (e.g., running)
    DISTANCE          // Distance-based (e.g., swimming laps)
}

/**
 * An exercise definition
 */
data class Exercise(
    val id: String,
    val name: String,
    val category: ExerciseCategory,
    val type: ExerciseType,
    val muscleGroups: List<String>,
    val description: String? = null,
    val instructions: String? = null,
    val imageUrl: String? = null,
    val isCustom: Boolean = false
)

/**
 * A single set within an exercise
 */
data class ExerciseSet(
    val setNumber: Int,
    val weight: Float? = null,      // kg or lbs based on user preference
    val reps: Int? = null,
    val duration: Duration? = null, // for timed exercises
    val distance: Float? = null,    // km or miles
    val isWarmup: Boolean = false,
    val isDropSet: Boolean = false,
    val rpe: Int? = null,           // Rate of Perceived Exertion (1-10)
    val notes: String? = null
)

/**
 * A logged exercise with all its sets
 */
data class ExerciseLog(
    val id: Long = 0,
    val exercise: Exercise,
    val sets: List<ExerciseSet>,
    val restBetweenSets: Duration? = null,
    val notes: String? = null
) {
    val totalVolume: Float
        get() = sets.sumOf { set ->
            ((set.weight ?: 0f) * (set.reps ?: 0)).toDouble()
        }.toFloat()
    
    val totalReps: Int
        get() = sets.sumOf { it.reps ?: 0 }
    
    val maxWeight: Float
        get() = sets.maxOfOrNull { it.weight ?: 0f } ?: 0f
}

/**
 * A complete workout session
 */
data class Workout(
    val id: Long = 0,
    val name: String,
    val exercises: List<ExerciseLog>,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val notes: String? = null,
    val caloriesBurned: Int? = null,   // From wearable if available
    val averageHeartRate: Int? = null, // From wearable if available
    val templateId: Long? = null       // If created from a template
) {
    val duration: Duration
        get() = Duration.between(startTime, endTime ?: LocalDateTime.now())
    
    val totalVolume: Float
        get() = exercises.sumOf { it.totalVolume.toDouble() }.toFloat()
    
    val totalSets: Int
        get() = exercises.sumOf { it.sets.size }
    
    val exerciseCount: Int
        get() = exercises.size
}

/**
 * A workout template for quick starts
 */
data class WorkoutTemplate(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val exercises: List<TemplateExercise>,
    val estimatedDuration: Duration? = null,
    val category: ExerciseCategory? = null,
    val isBuiltIn: Boolean = false
)

/**
 * An exercise within a template (without logged sets)
 */
data class TemplateExercise(
    val exercise: Exercise,
    val targetSets: Int,
    val targetReps: IntRange? = null,  // e.g., 8-12 reps
    val targetWeight: Float? = null,
    val restDuration: Duration? = null,
    val notes: String? = null
)

/**
 * Personal record for an exercise
 */
data class PersonalRecord(
    val exercise: Exercise,
    val type: PRType,
    val value: Float,
    val achievedAt: LocalDateTime,
    val workoutId: Long
)

enum class PRType {
    MAX_WEIGHT,      // Heaviest weight lifted
    MAX_REPS,        // Most reps at any weight
    MAX_VOLUME,      // Highest volume in single workout
    BEST_E1RM        // Estimated 1 rep max
}
