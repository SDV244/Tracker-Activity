package com.fittrack.data.repository

import com.fittrack.data.local.dao.ExerciseDao
import com.fittrack.data.local.entity.*
import com.fittrack.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for workouts and templates
 */
interface WorkoutRepository {
    // Workouts
    suspend fun insertWorkout(workout: Workout): Long
    suspend fun updateWorkout(workout: Workout)
    suspend fun deleteWorkout(workout: Workout)
    suspend fun getWorkoutById(id: Long): Workout?
    fun getWorkoutsForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Workout>>
    fun getRecentWorkouts(limit: Int = 10): Flow<List<Workout>>
    suspend fun getActiveWorkout(): Workout?
    
    // Exercise Logs
    suspend fun insertExerciseLog(workoutId: Long, log: ExerciseLog): Long
    suspend fun updateExerciseLog(log: ExerciseLog)
    suspend fun deleteExerciseLog(log: ExerciseLog)
    fun getExerciseLogsForWorkout(workoutId: Long): Flow<List<ExerciseLog>>
    
    // Exercise Sets
    suspend fun insertExerciseSet(logId: Long, set: ExerciseSet): Long
    suspend fun insertExerciseSets(logId: Long, sets: List<ExerciseSet>)
    suspend fun updateExerciseSet(set: ExerciseSet, logId: Long)
    suspend fun deleteExerciseSet(setId: Long, logId: Long)
    
    // Templates
    suspend fun insertTemplate(template: WorkoutTemplate): Long
    suspend fun updateTemplate(template: WorkoutTemplate)
    suspend fun deleteTemplate(template: WorkoutTemplate)
    fun getAllTemplates(): Flow<List<WorkoutTemplate>>
    suspend fun getTemplateById(id: Long): WorkoutTemplate?
    
    // Stats
    suspend fun getWorkoutCountForDateRange(start: LocalDateTime, end: LocalDateTime): Int
    suspend fun getTotalVolumeForDateRange(start: LocalDateTime, end: LocalDateTime): Float
    suspend fun getWeeklyStats(): WorkoutStats
}

/**
 * Weekly workout statistics
 */
data class WorkoutStats(
    val workoutCount: Int = 0,
    val totalVolume: Float = 0f,
    val totalMinutes: Int = 0,
    val prCount: Int = 0
)

/**
 * Implementation of WorkoutRepository
 */
@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao
) : WorkoutRepository {
    
    // ═══════════════════════════════════════════════════════════════
    // WORKOUTS
    // ═══════════════════════════════════════════════════════════════
    
    override suspend fun insertWorkout(workout: Workout): Long {
        val workoutEntity = WorkoutEntity(
            id = workout.id,
            name = workout.name,
            startTime = workout.startTime,
            endTime = workout.endTime,
            notes = workout.notes,
            caloriesBurned = workout.caloriesBurned,
            averageHeartRate = workout.averageHeartRate,
            templateId = workout.templateId
        )
        
        val workoutId = exerciseDao.insertWorkout(workoutEntity)
        
        // Insert exercise logs
        for ((index, exerciseLog) in workout.exercises.withIndex()) {
            val logId = insertExerciseLog(workoutId, exerciseLog.copy(id = 0))
        }
        
        return workoutId
    }
    
    override suspend fun updateWorkout(workout: Workout) {
        exerciseDao.updateWorkout(
            WorkoutEntity(
                id = workout.id,
                name = workout.name,
                startTime = workout.startTime,
                endTime = workout.endTime,
                notes = workout.notes,
                caloriesBurned = workout.caloriesBurned,
                averageHeartRate = workout.averageHeartRate,
                templateId = workout.templateId
            )
        )
    }
    
    override suspend fun deleteWorkout(workout: Workout) {
        exerciseDao.deleteWorkout(
            WorkoutEntity(
                id = workout.id,
                name = workout.name,
                startTime = workout.startTime,
                endTime = workout.endTime,
                notes = workout.notes,
                caloriesBurned = workout.caloriesBurned,
                averageHeartRate = workout.averageHeartRate,
                templateId = workout.templateId
            )
        )
    }
    
    override suspend fun getWorkoutById(id: Long): Workout? {
        return exerciseDao.getWorkoutWithExercises(id)?.toDomainModel()
    }
    
    override fun getWorkoutsForDateRange(
        start: LocalDateTime, 
        end: LocalDateTime
    ): Flow<List<Workout>> {
        return exerciseDao.getWorkoutsForDateRange(start, end).map { list ->
            list.map { it.toDomainModel() }
        }
    }
    
    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> {
        return exerciseDao.getRecentWorkouts(limit).map { list ->
            list.map { entity ->
                // Get full workout with exercises
                exerciseDao.getWorkoutWithExercises(entity.id)?.toDomainModel() ?: Workout(
                    id = entity.id,
                    name = entity.name,
                    exercises = emptyList(),
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    notes = entity.notes,
                    caloriesBurned = entity.caloriesBurned,
                    averageHeartRate = entity.averageHeartRate,
                    templateId = entity.templateId
                )
            }
        }
    }
    
    override suspend fun getActiveWorkout(): Workout? {
        val entity = exerciseDao.getActiveWorkout() ?: return null
        return exerciseDao.getWorkoutWithExercises(entity.id)?.toDomainModel()
    }
    
    // ═══════════════════════════════════════════════════════════════
    // EXERCISE LOGS
    // ═══════════════════════════════════════════════════════════════
    
    override suspend fun insertExerciseLog(workoutId: Long, log: ExerciseLog): Long {
        // Ensure exercise exists
        exerciseDao.insertExercise(log.exercise.toEntity())
        
        val logEntity = ExerciseLogEntity(
            id = log.id,
            workoutId = workoutId,
            exerciseId = log.exercise.id,
            orderIndex = 0, // Will be set based on insertion order
            restBetweenSetsSec = log.restBetweenSets?.seconds?.toInt(),
            notes = log.notes
        )
        
        val logId = exerciseDao.insertExerciseLog(logEntity)
        
        // Insert sets
        if (log.sets.isNotEmpty()) {
            insertExerciseSets(logId, log.sets)
        }
        
        return logId
    }
    
    override suspend fun updateExerciseLog(log: ExerciseLog) {
        exerciseDao.updateExerciseLog(
            ExerciseLogEntity(
                id = log.id,
                workoutId = 0, // Not used in update
                exerciseId = log.exercise.id,
                orderIndex = 0,
                restBetweenSetsSec = log.restBetweenSets?.seconds?.toInt(),
                notes = log.notes
            )
        )
    }
    
    override suspend fun deleteExerciseLog(log: ExerciseLog) {
        exerciseDao.deleteExerciseLog(
            ExerciseLogEntity(
                id = log.id,
                workoutId = 0,
                exerciseId = log.exercise.id,
                orderIndex = 0,
                restBetweenSetsSec = null,
                notes = null
            )
        )
    }
    
    override fun getExerciseLogsForWorkout(workoutId: Long): Flow<List<ExerciseLog>> {
        return exerciseDao.getExerciseLogsForWorkout(workoutId).map { list ->
            list.map { it.toDomainModel() }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // EXERCISE SETS
    // ═══════════════════════════════════════════════════════════════
    
    override suspend fun insertExerciseSet(logId: Long, set: ExerciseSet): Long {
        return exerciseDao.insertExerciseSet(set.toEntity(logId))
    }
    
    override suspend fun insertExerciseSets(logId: Long, sets: List<ExerciseSet>) {
        exerciseDao.insertExerciseSets(sets.map { it.toEntity(logId) })
    }
    
    override suspend fun updateExerciseSet(set: ExerciseSet, logId: Long) {
        exerciseDao.updateExerciseSet(set.toEntity(logId))
    }
    
    override suspend fun deleteExerciseSet(setId: Long, logId: Long) {
        exerciseDao.deleteExerciseSet(
            ExerciseSetEntity(
                id = setId,
                exerciseLogId = logId,
                setNumber = 0,
                weight = null,
                reps = null,
                durationSec = null,
                distance = null,
                isWarmup = false,
                isDropSet = false,
                rpe = null,
                notes = null
            )
        )
    }
    
    // ═══════════════════════════════════════════════════════════════
    // TEMPLATES
    // ═══════════════════════════════════════════════════════════════
    
    override suspend fun insertTemplate(template: WorkoutTemplate): Long {
        val templateEntity = WorkoutTemplateEntity(
            id = template.id,
            name = template.name,
            description = template.description,
            estimatedDurationMin = template.estimatedDuration?.toMinutes()?.toInt(),
            category = template.category?.name,
            isBuiltIn = template.isBuiltIn
        )
        
        val templateId = exerciseDao.insertTemplate(templateEntity)
        
        // Insert template exercises
        for ((index, templateExercise) in template.exercises.withIndex()) {
            // Ensure exercise exists
            exerciseDao.insertExercise(templateExercise.exercise.toEntity())
            
            exerciseDao.insertTemplateExercise(
                TemplateExerciseEntity(
                    templateId = templateId,
                    exerciseId = templateExercise.exercise.id,
                    orderIndex = index,
                    targetSets = templateExercise.targetSets,
                    targetRepsMin = templateExercise.targetReps?.first,
                    targetRepsMax = templateExercise.targetReps?.last,
                    targetWeight = templateExercise.targetWeight,
                    restDurationSec = templateExercise.restDuration?.seconds?.toInt(),
                    notes = templateExercise.notes
                )
            )
        }
        
        return templateId
    }
    
    override suspend fun updateTemplate(template: WorkoutTemplate) {
        exerciseDao.updateTemplate(
            WorkoutTemplateEntity(
                id = template.id,
                name = template.name,
                description = template.description,
                estimatedDurationMin = template.estimatedDuration?.toMinutes()?.toInt(),
                category = template.category?.name,
                isBuiltIn = template.isBuiltIn
            )
        )
    }
    
    override suspend fun deleteTemplate(template: WorkoutTemplate) {
        exerciseDao.deleteTemplate(
            WorkoutTemplateEntity(
                id = template.id,
                name = template.name,
                description = template.description,
                estimatedDurationMin = template.estimatedDuration?.toMinutes()?.toInt(),
                category = template.category?.name,
                isBuiltIn = template.isBuiltIn
            )
        )
    }
    
    override fun getAllTemplates(): Flow<List<WorkoutTemplate>> {
        return exerciseDao.getAllTemplates().map { list ->
            list.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getTemplateById(id: Long): WorkoutTemplate? {
        return exerciseDao.getTemplateById(id)?.toDomainModel()
    }
    
    // ═══════════════════════════════════════════════════════════════
    // STATS
    // ═══════════════════════════════════════════════════════════════
    
    override suspend fun getWorkoutCountForDateRange(
        start: LocalDateTime, 
        end: LocalDateTime
    ): Int {
        return exerciseDao.getWorkoutCountForDateRange(start, end)
    }
    
    override suspend fun getTotalVolumeForDateRange(
        start: LocalDateTime, 
        end: LocalDateTime
    ): Float {
        return exerciseDao.getTotalVolumeForDateRange(start, end) ?: 0f
    }
    
    override suspend fun getWeeklyStats(): WorkoutStats {
        val now = LocalDateTime.now()
        val weekAgo = now.minus(7, ChronoUnit.DAYS)
        
        val workoutCount = getWorkoutCountForDateRange(weekAgo, now)
        val totalVolume = getTotalVolumeForDateRange(weekAgo, now)
        
        // Calculate total minutes from recent workouts
        val recentWorkouts = getRecentWorkouts(50).first()
        val weeklyWorkouts = recentWorkouts.filter { 
            it.startTime.isAfter(weekAgo) && it.startTime.isBefore(now) 
        }
        val totalMinutes = weeklyWorkouts.sumOf { workout ->
            workout.endTime?.let { end ->
                Duration.between(workout.startTime, end).toMinutes()
            } ?: 0L
        }.toInt()
        
        return WorkoutStats(
            workoutCount = workoutCount,
            totalVolume = totalVolume,
            totalMinutes = totalMinutes,
            prCount = 0 // Would need to track PRs separately
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS - Entity <-> Domain Conversion
// ═══════════════════════════════════════════════════════════════

private fun Exercise.toEntity(): ExerciseEntity {
    return ExerciseEntity(
        id = id,
        name = name,
        category = category.name,
        type = type.name,
        muscleGroups = muscleGroups.joinToString(","),
        description = description,
        instructions = instructions,
        imageUrl = imageUrl,
        isCustom = isCustom
    )
}

private fun ExerciseEntity.toDomainModel(): Exercise {
    return Exercise(
        id = id,
        name = name,
        category = try { ExerciseCategory.valueOf(category) } catch (e: Exception) { ExerciseCategory.OTHER },
        type = try { ExerciseType.valueOf(type) } catch (e: Exception) { ExerciseType.WEIGHT_REPS },
        muscleGroups = muscleGroups.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        description = description,
        instructions = instructions,
        imageUrl = imageUrl,
        isCustom = isCustom
    )
}

private fun ExerciseSet.toEntity(logId: Long): ExerciseSetEntity {
    return ExerciseSetEntity(
        exerciseLogId = logId,
        setNumber = setNumber,
        weight = weight,
        reps = reps,
        durationSec = duration?.seconds?.toInt(),
        distance = distance,
        isWarmup = isWarmup,
        isDropSet = isDropSet,
        rpe = rpe,
        notes = notes
    )
}

private fun ExerciseSetEntity.toDomainModel(): ExerciseSet {
    return ExerciseSet(
        setNumber = setNumber,
        weight = weight,
        reps = reps,
        duration = durationSec?.let { Duration.ofSeconds(it.toLong()) },
        distance = distance,
        isWarmup = isWarmup,
        isDropSet = isDropSet,
        rpe = rpe,
        notes = notes
    )
}

private fun ExerciseLogWithSets.toDomainModel(): ExerciseLog {
    return ExerciseLog(
        id = exerciseLog.id,
        exercise = exercise.toDomainModel(),
        sets = sets.map { it.toDomainModel() },
        restBetweenSets = exerciseLog.restBetweenSetsSec?.let { Duration.ofSeconds(it.toLong()) },
        notes = exerciseLog.notes
    )
}

private fun WorkoutWithExercises.toDomainModel(): Workout {
    return Workout(
        id = workout.id,
        name = workout.name,
        exercises = exerciseLogs.map { it.toDomainModel() },
        startTime = workout.startTime,
        endTime = workout.endTime,
        notes = workout.notes,
        caloriesBurned = workout.caloriesBurned,
        averageHeartRate = workout.averageHeartRate,
        templateId = workout.templateId
    )
}

private fun TemplateWithExercises.toDomainModel(): WorkoutTemplate {
    return WorkoutTemplate(
        id = template.id,
        name = template.name,
        description = template.description,
        exercises = exercises.map { it.toDomainModel() },
        estimatedDuration = template.estimatedDurationMin?.let { Duration.ofMinutes(it.toLong()) },
        category = template.category?.let { 
            try { ExerciseCategory.valueOf(it) } catch (e: Exception) { null } 
        },
        isBuiltIn = template.isBuiltIn
    )
}

private fun TemplateExerciseWithDetails.toDomainModel(): TemplateExercise {
    return TemplateExercise(
        exercise = exercise.toDomainModel(),
        targetSets = templateExercise.targetSets,
        targetReps = if (templateExercise.targetRepsMin != null && templateExercise.targetRepsMax != null) {
            templateExercise.targetRepsMin..templateExercise.targetRepsMax
        } else null,
        targetWeight = templateExercise.targetWeight,
        restDuration = templateExercise.restDurationSec?.let { Duration.ofSeconds(it.toLong()) },
        notes = templateExercise.notes
    )
}
