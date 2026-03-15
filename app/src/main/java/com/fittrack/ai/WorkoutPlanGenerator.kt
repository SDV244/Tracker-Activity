package com.fittrack.ai

import com.fittrack.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════
// 🏋️ AI WORKOUT PLAN GENERATOR
// Generates personalized workout programs based on goals, experience, and equipment
// ═══════════════════════════════════════════════════════════════

/**
 * User's training preferences and limitations
 */
data class TrainingPreferences(
    val goal: TrainingGoal = TrainingGoal.BUILD_MUSCLE,
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val availableEquipment: List<Equipment> = Equipment.entries,
    val daysPerWeek: Int = 4,
    val sessionDuration: Int = 60,  // minutes
    val injuries: List<Injury> = emptyList(),
    val preferredSplit: WorkoutSplit? = null,
    val cardioPreference: CardioPreference = CardioPreference.MODERATE
)

enum class TrainingGoal(val description: String) {
    BUILD_MUSCLE("Maximize muscle growth (hypertrophy)"),
    STRENGTH("Build maximal strength"),
    FAT_LOSS("Lose fat while preserving muscle"),
    ATHLETIC("Improve athletic performance"),
    GENERAL_FITNESS("Overall health and fitness"),
    POWERBUILDING("Mix of strength and size"),
    BODYWEIGHT("Calisthenics and bodyweight training")
}

enum class ExperienceLevel(val monthsTraining: IntRange) {
    BEGINNER(0..6),
    INTERMEDIATE(6..24),
    ADVANCED(24..60),
    ELITE(60..Int.MAX_VALUE)
}

enum class Equipment {
    BARBELL, DUMBBELLS, CABLES, MACHINES, PULL_UP_BAR, 
    KETTLEBELLS, RESISTANCE_BANDS, BENCH, SQUAT_RACK,
    BODYWEIGHT_ONLY
}

enum class Injury {
    LOWER_BACK, SHOULDER, KNEE, WRIST, ELBOW, NECK, HIP, ANKLE
}

enum class WorkoutSplit {
    FULL_BODY,           // 3x/week full body
    UPPER_LOWER,         // 4x/week upper/lower
    PUSH_PULL_LEGS,      // 6x/week PPL
    BRO_SPLIT,           // 5x/week body part split
    ARNOLD_SPLIT,        // 6x/week chest/back, shoulders/arms, legs
    PHAT,                // Power Hypertrophy Adaptive Training
    PHUL                 // Power Hypertrophy Upper Lower
}

enum class CardioPreference {
    NONE, MINIMAL, MODERATE, HIGH
}

/**
 * A complete workout program
 */
data class WorkoutProgram(
    val id: Long = 0,
    val name: String,
    val description: String,
    val goal: TrainingGoal,
    val split: WorkoutSplit,
    val durationWeeks: Int,
    val workoutsPerWeek: Int,
    val workouts: List<ProgrammedWorkout>,
    val progressionScheme: ProgressionScheme,
    val deloadWeek: Int?,  // Which week is deload (e.g., week 4)
    val tips: List<String>
)

/**
 * A single workout in the program
 */
data class ProgrammedWorkout(
    val dayOfWeek: DayOfWeek?,
    val name: String,
    val focus: String,  // e.g., "Push", "Upper Body", "Legs"
    val exercises: List<ProgrammedExercise>,
    val warmup: WarmupRoutine,
    val cooldown: CooldownRoutine?,
    val estimatedDuration: Duration,
    val notes: String? = null
)

/**
 * An exercise with prescribed sets/reps
 */
data class ProgrammedExercise(
    val exercise: Exercise,
    val sets: Int,
    val repsRange: IntRange,  // e.g., 8..12
    val restSeconds: Int,
    val rpe: Int?,  // Rate of Perceived Exertion target
    val tempoSeconds: String?,  // e.g., "3-1-2-0" (eccentric-pause-concentric-pause)
    val notes: String? = null,
    val alternatives: List<Exercise> = emptyList(),  // If equipment not available
    val supersetWith: String? = null  // Exercise name to superset with
)

data class WarmupRoutine(
    val cardioMinutes: Int,
    val dynamicStretches: List<String>,
    val activationExercises: List<String>
)

data class CooldownRoutine(
    val staticStretches: List<String>,
    val foamRollingAreas: List<String>
)

data class ProgressionScheme(
    val type: ProgressionType,
    val weeklyIncrement: Float?,  // kg or % increase
    val description: String
)

enum class ProgressionType {
    LINEAR,           // Add weight each session
    DOUBLE_PROGRESSION,  // Add reps, then weight
    WAVE_LOADING,     // Vary intensity weekly
    PERIODIZED,       // Planned phases
    RPE_BASED         // Autoregulated by feel
}

@Singleton
class WorkoutPlanGenerator @Inject constructor() {
    
    // ═══════════════════════════════════════════════════════════════
    // EXERCISE DATABASE
    // ═══════════════════════════════════════════════════════════════
    
    private val exercises = mapOf(
        // CHEST
        "bench_press" to Exercise(
            id = "bench_press",
            name = "Barbell Bench Press",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Chest", "Front Delts", "Triceps"),
            description = "Primary compound movement for chest development",
            instructions = "Lie on bench, grip slightly wider than shoulders, lower to chest, press up"
        ),
        "incline_db_press" to Exercise(
            id = "incline_db_press",
            name = "Incline Dumbbell Press",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Upper Chest", "Front Delts", "Triceps"),
            description = "Targets upper chest"
        ),
        "cable_fly" to Exercise(
            id = "cable_fly",
            name = "Cable Fly",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Chest"),
            description = "Isolation movement for chest"
        ),
        "dips" to Exercise(
            id = "dips",
            name = "Dips",
            category = ExerciseCategory.CHEST,
            type = ExerciseType.BODYWEIGHT_REPS,
            muscleGroups = listOf("Chest", "Triceps", "Front Delts"),
            description = "Compound pushing movement"
        ),
        
        // BACK
        "deadlift" to Exercise(
            id = "deadlift",
            name = "Conventional Deadlift",
            category = ExerciseCategory.BACK,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Lower Back", "Glutes", "Hamstrings", "Traps"),
            description = "King of posterior chain exercises"
        ),
        "barbell_row" to Exercise(
            id = "barbell_row",
            name = "Barbell Row",
            category = ExerciseCategory.BACK,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Lats", "Rhomboids", "Rear Delts", "Biceps"),
            description = "Primary horizontal pull"
        ),
        "pull_ups" to Exercise(
            id = "pull_ups",
            name = "Pull-Ups",
            category = ExerciseCategory.BACK,
            type = ExerciseType.BODYWEIGHT_REPS,
            muscleGroups = listOf("Lats", "Biceps", "Rear Delts"),
            description = "Primary vertical pull"
        ),
        "lat_pulldown" to Exercise(
            id = "lat_pulldown",
            name = "Lat Pulldown",
            category = ExerciseCategory.BACK,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Lats", "Biceps"),
            description = "Machine vertical pull"
        ),
        "cable_row" to Exercise(
            id = "cable_row",
            name = "Seated Cable Row",
            category = ExerciseCategory.BACK,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Lats", "Rhomboids", "Biceps"),
            description = "Machine horizontal pull"
        ),
        
        // SHOULDERS
        "ohp" to Exercise(
            id = "ohp",
            name = "Overhead Press",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Front Delts", "Side Delts", "Triceps"),
            description = "Primary shoulder compound"
        ),
        "lateral_raise" to Exercise(
            id = "lateral_raise",
            name = "Lateral Raise",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Side Delts"),
            description = "Isolation for side delts"
        ),
        "face_pull" to Exercise(
            id = "face_pull",
            name = "Face Pull",
            category = ExerciseCategory.SHOULDERS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Rear Delts", "Rotator Cuff"),
            description = "Rear delt and shoulder health"
        ),
        
        // LEGS
        "squat" to Exercise(
            id = "squat",
            name = "Barbell Back Squat",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Quadriceps", "Glutes", "Hamstrings"),
            description = "King of leg exercises"
        ),
        "leg_press" to Exercise(
            id = "leg_press",
            name = "Leg Press",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Quadriceps", "Glutes"),
            description = "Machine compound for legs"
        ),
        "rdl" to Exercise(
            id = "rdl",
            name = "Romanian Deadlift",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Hamstrings", "Glutes", "Lower Back"),
            description = "Hip hinge for posterior chain"
        ),
        "leg_curl" to Exercise(
            id = "leg_curl",
            name = "Lying Leg Curl",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Hamstrings"),
            description = "Isolation for hamstrings"
        ),
        "leg_extension" to Exercise(
            id = "leg_extension",
            name = "Leg Extension",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Quadriceps"),
            description = "Isolation for quads"
        ),
        "calf_raise" to Exercise(
            id = "calf_raise",
            name = "Standing Calf Raise",
            category = ExerciseCategory.LEGS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Calves"),
            description = "Primary calf exercise"
        ),
        
        // ARMS
        "barbell_curl" to Exercise(
            id = "barbell_curl",
            name = "Barbell Curl",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Biceps"),
            description = "Primary bicep exercise"
        ),
        "hammer_curl" to Exercise(
            id = "hammer_curl",
            name = "Hammer Curl",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Biceps", "Brachialis"),
            description = "Biceps and forearms"
        ),
        "tricep_pushdown" to Exercise(
            id = "tricep_pushdown",
            name = "Tricep Pushdown",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Triceps"),
            description = "Primary tricep isolation"
        ),
        "skull_crusher" to Exercise(
            id = "skull_crusher",
            name = "Skull Crushers",
            category = ExerciseCategory.ARMS,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Triceps"),
            description = "Tricep isolation with stretch"
        ),
        
        // CORE
        "plank" to Exercise(
            id = "plank",
            name = "Plank",
            category = ExerciseCategory.CORE,
            type = ExerciseType.DURATION,
            muscleGroups = listOf("Core", "Abs"),
            description = "Isometric core stability"
        ),
        "cable_crunch" to Exercise(
            id = "cable_crunch",
            name = "Cable Crunch",
            category = ExerciseCategory.CORE,
            type = ExerciseType.WEIGHT_REPS,
            muscleGroups = listOf("Abs"),
            description = "Weighted ab exercise"
        ),
        "hanging_leg_raise" to Exercise(
            id = "hanging_leg_raise",
            name = "Hanging Leg Raise",
            category = ExerciseCategory.CORE,
            type = ExerciseType.BODYWEIGHT_REPS,
            muscleGroups = listOf("Abs", "Hip Flexors"),
            description = "Advanced ab exercise"
        )
    )
    
    // ═══════════════════════════════════════════════════════════════
    // PROGRAM GENERATION
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Generate a complete workout program
     */
    suspend fun generateProgram(
        preferences: TrainingPreferences,
        userProfile: UserProfile,
        bodyScan: BodyScanResult? = null
    ): WorkoutProgram = withContext(Dispatchers.Default) {
        
        // Determine optimal split based on preferences
        val split = preferences.preferredSplit ?: recommendSplit(preferences)
        
        // Generate workouts based on split
        val workouts = when (split) {
            WorkoutSplit.FULL_BODY -> generateFullBodySplit(preferences)
            WorkoutSplit.UPPER_LOWER -> generateUpperLowerSplit(preferences)
            WorkoutSplit.PUSH_PULL_LEGS -> generatePPLSplit(preferences)
            else -> generateUpperLowerSplit(preferences)  // Default
        }
        
        // Add progression scheme
        val progression = recommendProgression(preferences)
        
        // Generate program tips
        val tips = generateProgramTips(preferences, bodyScan)
        
        WorkoutProgram(
            name = "${preferences.goal.description} - ${split.name.replace("_", " ")}",
            description = generateProgramDescription(preferences, split),
            goal = preferences.goal,
            split = split,
            durationWeeks = 8,
            workoutsPerWeek = preferences.daysPerWeek,
            workouts = workouts,
            progressionScheme = progression,
            deloadWeek = if (preferences.experienceLevel != ExperienceLevel.BEGINNER) 4 else null,
            tips = tips
        )
    }
    
    private fun recommendSplit(prefs: TrainingPreferences): WorkoutSplit {
        return when {
            prefs.daysPerWeek <= 3 -> WorkoutSplit.FULL_BODY
            prefs.daysPerWeek == 4 -> WorkoutSplit.UPPER_LOWER
            prefs.daysPerWeek >= 5 -> WorkoutSplit.PUSH_PULL_LEGS
            else -> WorkoutSplit.UPPER_LOWER
        }
    }
    
    private fun generateFullBodySplit(prefs: TrainingPreferences): List<ProgrammedWorkout> {
        val workouts = mutableListOf<ProgrammedWorkout>()
        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        
        for ((index, day) in days.take(prefs.daysPerWeek).withIndex()) {
            val variant = index % 2  // Alternate between A and B workouts
            
            val workout = ProgrammedWorkout(
                dayOfWeek = day,
                name = "Full Body ${if (variant == 0) "A" else "B"}",
                focus = "Full Body",
                exercises = generateFullBodyExercises(variant, prefs),
                warmup = generateWarmup("full_body"),
                cooldown = generateCooldown(),
                estimatedDuration = Duration.ofMinutes(prefs.sessionDuration.toLong())
            )
            workouts.add(workout)
        }
        
        return workouts
    }
    
    private fun generateUpperLowerSplit(prefs: TrainingPreferences): List<ProgrammedWorkout> {
        val workouts = mutableListOf<ProgrammedWorkout>()
        
        // Upper A
        workouts.add(ProgrammedWorkout(
            dayOfWeek = DayOfWeek.MONDAY,
            name = "Upper Body A",
            focus = "Upper - Push Focus",
            exercises = generateUpperExercises("push", prefs),
            warmup = generateWarmup("upper"),
            cooldown = generateCooldown(),
            estimatedDuration = Duration.ofMinutes(prefs.sessionDuration.toLong())
        ))
        
        // Lower A
        workouts.add(ProgrammedWorkout(
            dayOfWeek = DayOfWeek.TUESDAY,
            name = "Lower Body A",
            focus = "Lower - Quad Focus",
            exercises = generateLowerExercises("quad", prefs),
            warmup = generateWarmup("lower"),
            cooldown = generateCooldown(),
            estimatedDuration = Duration.ofMinutes(prefs.sessionDuration.toLong())
        ))
        
        // Upper B
        workouts.add(ProgrammedWorkout(
            dayOfWeek = DayOfWeek.THURSDAY,
            name = "Upper Body B",
            focus = "Upper - Pull Focus",
            exercises = generateUpperExercises("pull", prefs),
            warmup = generateWarmup("upper"),
            cooldown = generateCooldown(),
            estimatedDuration = Duration.ofMinutes(prefs.sessionDuration.toLong())
        ))
        
        // Lower B
        workouts.add(ProgrammedWorkout(
            dayOfWeek = DayOfWeek.FRIDAY,
            name = "Lower Body B",
            focus = "Lower - Posterior Focus",
            exercises = generateLowerExercises("posterior", prefs),
            warmup = generateWarmup("lower"),
            cooldown = generateCooldown(),
            estimatedDuration = Duration.ofMinutes(prefs.sessionDuration.toLong())
        ))
        
        return workouts.take(prefs.daysPerWeek)
    }
    
    private fun generatePPLSplit(prefs: TrainingPreferences): List<ProgrammedWorkout> {
        val workouts = mutableListOf<ProgrammedWorkout>()
        
        // Push
        workouts.add(ProgrammedWorkout(
            dayOfWeek = DayOfWeek.MONDAY,
            name = "Push",
            focus = "Chest, Shoulders, Triceps",
            exercises = generatePushExercises(prefs),
            warmup = generateWarmup("push"),
            cooldown = generateCooldown(),
            estimatedDuration = Duration.ofMinutes(prefs.sessionDuration.toLong())
        ))
        
        // Pull
        workouts.add(ProgrammedWorkout(
            dayOfWeek = DayOfWeek.TUESDAY,
            name = "Pull",
            focus = "Back, Rear Delts, Biceps",
            exercises = generatePullExercises(prefs),
            warmup = generateWarmup("pull"),
            cooldown = generateCooldown(),
            estimatedDuration = Duration.ofMinutes(prefs.sessionDuration.toLong())
        ))
        
        // Legs
        workouts.add(ProgrammedWorkout(
            dayOfWeek = DayOfWeek.WEDNESDAY,
            name = "Legs",
            focus = "Quads, Hamstrings, Glutes, Calves",
            exercises = generateLegExercises(prefs),
            warmup = generateWarmup("legs"),
            cooldown = generateCooldown(),
            estimatedDuration = Duration.ofMinutes(prefs.sessionDuration.toLong())
        ))
        
        return workouts
    }
    
    // ═══════════════════════════════════════════════════════════════
    // EXERCISE SELECTION HELPERS
    // ═══════════════════════════════════════════════════════════════
    
    private fun generateFullBodyExercises(variant: Int, prefs: TrainingPreferences): List<ProgrammedExercise> {
        val exerciseList = mutableListOf<ProgrammedExercise>()
        val repRange = getRepRange(prefs.goal)
        val sets = getSetsForGoal(prefs.goal, prefs.experienceLevel)
        
        if (variant == 0) {
            // Full Body A
            exerciseList.add(programExercise("squat", sets, repRange, 180, prefs))
            exerciseList.add(programExercise("bench_press", sets, repRange, 150, prefs))
            exerciseList.add(programExercise("barbell_row", sets, repRange, 120, prefs))
            exerciseList.add(programExercise("ohp", sets - 1, repRange, 120, prefs))
            exerciseList.add(programExercise("rdl", sets, repRange, 120, prefs))
            exerciseList.add(programExercise("face_pull", 3, 15..20, 60, prefs))
        } else {
            // Full Body B
            exerciseList.add(programExercise("deadlift", sets, 5..8, 180, prefs))
            exerciseList.add(programExercise("incline_db_press", sets, repRange, 120, prefs))
            exerciseList.add(programExercise("pull_ups", sets, 6..12, 120, prefs))
            exerciseList.add(programExercise("leg_press", sets, repRange, 120, prefs))
            exerciseList.add(programExercise("lateral_raise", 3, 12..15, 60, prefs))
            exerciseList.add(programExercise("plank", 3, 30..60, 60, prefs))
        }
        
        return filterForInjuries(exerciseList, prefs.injuries)
    }
    
    private fun generateUpperExercises(focus: String, prefs: TrainingPreferences): List<ProgrammedExercise> {
        val exerciseList = mutableListOf<ProgrammedExercise>()
        val repRange = getRepRange(prefs.goal)
        val sets = getSetsForGoal(prefs.goal, prefs.experienceLevel)
        
        if (focus == "push") {
            exerciseList.add(programExercise("bench_press", sets, repRange, 180, prefs))
            exerciseList.add(programExercise("ohp", sets, repRange, 150, prefs))
            exerciseList.add(programExercise("incline_db_press", sets, repRange, 120, prefs))
            exerciseList.add(programExercise("lateral_raise", 3, 12..15, 60, prefs))
            exerciseList.add(programExercise("tricep_pushdown", 3, 10..15, 60, prefs))
            exerciseList.add(programExercise("face_pull", 3, 15..20, 60, prefs))
        } else {
            exerciseList.add(programExercise("barbell_row", sets, repRange, 150, prefs))
            exerciseList.add(programExercise("pull_ups", sets, 6..12, 120, prefs))
            exerciseList.add(programExercise("cable_row", sets, repRange, 90, prefs))
            exerciseList.add(programExercise("face_pull", 3, 15..20, 60, prefs))
            exerciseList.add(programExercise("barbell_curl", 3, 8..12, 60, prefs))
            exerciseList.add(programExercise("hammer_curl", 3, 10..12, 60, prefs))
        }
        
        return filterForInjuries(exerciseList, prefs.injuries)
    }
    
    private fun generateLowerExercises(focus: String, prefs: TrainingPreferences): List<ProgrammedExercise> {
        val exerciseList = mutableListOf<ProgrammedExercise>()
        val repRange = getRepRange(prefs.goal)
        val sets = getSetsForGoal(prefs.goal, prefs.experienceLevel)
        
        if (focus == "quad") {
            exerciseList.add(programExercise("squat", sets, repRange, 180, prefs))
            exerciseList.add(programExercise("leg_press", sets, 10..15, 120, prefs))
            exerciseList.add(programExercise("leg_extension", 3, 12..15, 60, prefs))
            exerciseList.add(programExercise("leg_curl", 3, 10..12, 60, prefs))
            exerciseList.add(programExercise("calf_raise", 4, 12..15, 60, prefs))
        } else {
            exerciseList.add(programExercise("rdl", sets, repRange, 150, prefs))
            exerciseList.add(programExercise("leg_press", sets, 10..15, 120, prefs))
            exerciseList.add(programExercise("leg_curl", sets, 10..12, 90, prefs))
            exerciseList.add(programExercise("leg_extension", 3, 12..15, 60, prefs))
            exerciseList.add(programExercise("calf_raise", 4, 12..15, 60, prefs))
        }
        
        return filterForInjuries(exerciseList, prefs.injuries)
    }
    
    private fun generatePushExercises(prefs: TrainingPreferences): List<ProgrammedExercise> {
        return generateUpperExercises("push", prefs)
    }
    
    private fun generatePullExercises(prefs: TrainingPreferences): List<ProgrammedExercise> {
        return generateUpperExercises("pull", prefs)
    }
    
    private fun generateLegExercises(prefs: TrainingPreferences): List<ProgrammedExercise> {
        val quads = generateLowerExercises("quad", prefs)
        val posterior = generateLowerExercises("posterior", prefs)
        
        // Combine unique exercises
        val combined = mutableListOf<ProgrammedExercise>()
        combined.addAll(quads.take(3))
        combined.addAll(posterior.filter { p -> 
            combined.none { it.exercise.id == p.exercise.id } 
        }.take(3))
        
        return combined
    }
    
    private fun programExercise(
        exerciseId: String,
        sets: Int,
        reps: IntRange,
        restSeconds: Int,
        prefs: TrainingPreferences
    ): ProgrammedExercise {
        val exercise = exercises[exerciseId] ?: throw IllegalArgumentException("Unknown exercise: $exerciseId")
        
        return ProgrammedExercise(
            exercise = exercise,
            sets = sets,
            repsRange = reps,
            restSeconds = restSeconds,
            rpe = if (prefs.experienceLevel == ExperienceLevel.ADVANCED) 8 else null,
            tempoSeconds = if (prefs.goal == TrainingGoal.BUILD_MUSCLE) "3-0-1-0" else null
        )
    }
    
    private fun getRepRange(goal: TrainingGoal): IntRange {
        return when (goal) {
            TrainingGoal.STRENGTH -> 3..6
            TrainingGoal.BUILD_MUSCLE -> 8..12
            TrainingGoal.FAT_LOSS -> 12..15
            TrainingGoal.POWERBUILDING -> 5..8
            else -> 8..12
        }
    }
    
    private fun getSetsForGoal(goal: TrainingGoal, level: ExperienceLevel): Int {
        val baseSets = when (goal) {
            TrainingGoal.STRENGTH -> 5
            TrainingGoal.BUILD_MUSCLE -> 4
            TrainingGoal.FAT_LOSS -> 3
            else -> 4
        }
        
        return when (level) {
            ExperienceLevel.BEGINNER -> baseSets - 1
            ExperienceLevel.ADVANCED, ExperienceLevel.ELITE -> baseSets + 1
            else -> baseSets
        }
    }
    
    private fun filterForInjuries(
        exercises: List<ProgrammedExercise>,
        injuries: List<Injury>
    ): List<ProgrammedExercise> {
        if (injuries.isEmpty()) return exercises
        
        return exercises.filter { programmed ->
            val muscleGroups = programmed.exercise.muscleGroups.map { it.lowercase() }
            
            !injuries.any { injury ->
                when (injury) {
                    Injury.LOWER_BACK -> muscleGroups.any { it.contains("back") && it.contains("lower") }
                    Injury.SHOULDER -> muscleGroups.any { it.contains("delt") || it.contains("shoulder") }
                    Injury.KNEE -> muscleGroups.any { it.contains("quad") }
                    Injury.WRIST -> programmed.exercise.name.lowercase().contains("curl")
                    else -> false
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // WARMUP & COOLDOWN
    // ═══════════════════════════════════════════════════════════════
    
    private fun generateWarmup(focus: String): WarmupRoutine {
        val cardio = 5
        val stretches = when (focus) {
            "upper", "push", "pull" -> listOf(
                "Arm circles (30 sec each direction)",
                "Band pull-aparts (15 reps)",
                "Cat-cow stretches (10 reps)"
            )
            "lower", "legs" -> listOf(
                "Leg swings (15 each leg)",
                "Hip circles (10 each direction)",
                "Bodyweight squats (15 reps)"
            )
            else -> listOf(
                "Jumping jacks (30 sec)",
                "Arm circles (15 sec)",
                "Leg swings (10 each)"
            )
        }
        val activation = when (focus) {
            "upper", "push" -> listOf("Light band face pulls", "Push-up plus (10 reps)")
            "pull" -> listOf("Band rows (15 reps)", "Dead hangs (20 sec)")
            "lower", "legs" -> listOf("Glute bridges (15 reps)", "Monster walks (10 each way)")
            else -> listOf("Light movement prep")
        }
        
        return WarmupRoutine(cardio, stretches, activation)
    }
    
    private fun generateCooldown(): CooldownRoutine {
        return CooldownRoutine(
            staticStretches = listOf(
                "Chest doorway stretch (30 sec each)",
                "Lat stretch (30 sec each)",
                "Hip flexor stretch (30 sec each)",
                "Hamstring stretch (30 sec each)"
            ),
            foamRollingAreas = listOf("Quads", "IT Band", "Upper Back", "Lats")
        )
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PROGRESSION & TIPS
    // ═══════════════════════════════════════════════════════════════
    
    private fun recommendProgression(prefs: TrainingPreferences): ProgressionScheme {
        return when (prefs.experienceLevel) {
            ExperienceLevel.BEGINNER -> ProgressionScheme(
                type = ProgressionType.LINEAR,
                weeklyIncrement = 2.5f,
                description = "Add 2.5kg to compounds each week when all reps are completed"
            )
            ExperienceLevel.INTERMEDIATE -> ProgressionScheme(
                type = ProgressionType.DOUBLE_PROGRESSION,
                weeklyIncrement = null,
                description = "Hit top of rep range for all sets, then increase weight by 2.5-5kg"
            )
            else -> ProgressionScheme(
                type = ProgressionType.RPE_BASED,
                weeklyIncrement = null,
                description = "Train to RPE 7-9. Increase weight when RPE drops below target"
            )
        }
    }
    
    private fun generateProgramTips(
        prefs: TrainingPreferences,
        bodyScan: BodyScanResult?
    ): List<String> {
        val tips = mutableListOf<String>()
        
        tips.add("💪 Focus on progressive overload - track every workout!")
        tips.add("😴 Sleep 7-9 hours for optimal recovery")
        tips.add("🍗 Eat 1.6-2.2g protein per kg bodyweight")
        tips.add("💧 Stay hydrated - aim for 3+ liters daily")
        
        if (prefs.goal == TrainingGoal.BUILD_MUSCLE) {
            tips.add("📈 You need a caloric surplus of 200-500 kcal for muscle growth")
            tips.add("⏱️ Control the eccentric (lowering) for 2-3 seconds")
        }
        
        if (prefs.goal == TrainingGoal.FAT_LOSS) {
            tips.add("📉 Maintain a moderate deficit of 300-500 kcal")
            tips.add("🏃 Consider 2-3 cardio sessions per week")
        }
        
        bodyScan?.let { scan ->
            if (scan.estimatedBodyFatPercentage > 20f) {
                tips.add("🎯 Focus on fat loss first - you'll see muscle definition faster")
            }
            if (scan.symmetryScore < 80f) {
                tips.add("⚖️ Include unilateral exercises to address muscle imbalances")
            }
        }
        
        return tips
    }
    
    private fun generateProgramDescription(
        prefs: TrainingPreferences,
        split: WorkoutSplit
    ): String {
        return """
            This ${prefs.daysPerWeek}-day ${split.name.replace("_", " ").lowercase()} program 
            is designed for ${prefs.experienceLevel.name.lowercase()} trainees 
            focused on ${prefs.goal.description.lowercase()}.
            
            Each session takes approximately ${prefs.sessionDuration} minutes.
        """.trimIndent()
    }
}
