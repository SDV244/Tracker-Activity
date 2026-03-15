package com.fittrack.data.repository

import com.fittrack.data.local.dao.ExerciseDao
import com.fittrack.data.local.entity.*
import com.fittrack.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for exercise definitions
 */
interface ExerciseRepository {
    // Exercise definitions
    suspend fun insertExercise(exercise: Exercise)
    suspend fun insertExercises(exercises: List<Exercise>)
    suspend fun getExerciseById(id: String): Exercise?
    fun getExercisesByCategory(category: ExerciseCategory): Flow<List<Exercise>>
    fun searchExercises(query: String): Flow<List<Exercise>>
    fun getAllExercises(): Flow<List<Exercise>>
    fun getCustomExercises(): Flow<List<Exercise>>
    suspend fun deleteExercise(exercise: Exercise)
    
    // Personal Records
    suspend fun insertPersonalRecord(pr: PersonalRecord)
    fun getPersonalRecordsForExercise(exerciseId: String): Flow<List<PersonalRecord>>
    suspend fun getBestRecordForExercise(exerciseId: String, type: PRType): PersonalRecord?
    fun getRecentPRs(limit: Int = 10): Flow<List<PersonalRecord>>
}

/**
 * Implementation of ExerciseRepository
 */
@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {
    
    // ═══════════════════════════════════════════════════════════════
    // EXERCISE DEFINITIONS
    // ═══════════════════════════════════════════════════════════════
    
    override suspend fun insertExercise(exercise: Exercise) {
        exerciseDao.insertExercise(exercise.toEntity())
    }
    
    override suspend fun insertExercises(exercises: List<Exercise>) {
        exerciseDao.insertExercises(exercises.map { it.toEntity() })
    }
    
    override suspend fun getExerciseById(id: String): Exercise? {
        return exerciseDao.getExerciseById(id)?.toDomainModel()
    }
    
    override fun getExercisesByCategory(category: ExerciseCategory): Flow<List<Exercise>> {
        return exerciseDao.getExercisesByCategory(category.name).map { list ->
            list.map { it.toDomainModel() }
        }
    }
    
    override fun searchExercises(query: String): Flow<List<Exercise>> {
        return exerciseDao.searchExercises(query).map { list ->
            list.map { it.toDomainModel() }
        }
    }
    
    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises().map { list ->
            list.map { it.toDomainModel() }
        }
    }
    
    override fun getCustomExercises(): Flow<List<Exercise>> {
        return exerciseDao.getCustomExercises().map { list ->
            list.map { it.toDomainModel() }
        }
    }
    
    override suspend fun deleteExercise(exercise: Exercise) {
        exerciseDao.deleteExercise(exercise.toEntity())
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PERSONAL RECORDS
    // ═══════════════════════════════════════════════════════════════
    
    override suspend fun insertPersonalRecord(pr: PersonalRecord) {
        exerciseDao.insertPersonalRecord(
            PersonalRecordEntity(
                exerciseId = pr.exercise.id,
                prType = pr.type.name,
                value = pr.value,
                achievedAt = pr.achievedAt,
                workoutId = pr.workoutId
            )
        )
    }
    
    override fun getPersonalRecordsForExercise(exerciseId: String): Flow<List<PersonalRecord>> {
        return exerciseDao.getPersonalRecordsForExercise(exerciseId).map { list ->
            list.mapNotNull { entity ->
                val exercise = exerciseDao.getExerciseById(entity.exerciseId)?.toDomainModel()
                exercise?.let {
                    PersonalRecord(
                        exercise = it,
                        type = PRType.valueOf(entity.prType),
                        value = entity.value,
                        achievedAt = entity.achievedAt,
                        workoutId = entity.workoutId
                    )
                }
            }
        }
    }
    
    override suspend fun getBestRecordForExercise(exerciseId: String, type: PRType): PersonalRecord? {
        val entity = exerciseDao.getBestRecordForExercise(exerciseId, type.name) ?: return null
        val exercise = exerciseDao.getExerciseById(entity.exerciseId)?.toDomainModel() ?: return null
        
        return PersonalRecord(
            exercise = exercise,
            type = PRType.valueOf(entity.prType),
            value = entity.value,
            achievedAt = entity.achievedAt,
            workoutId = entity.workoutId
        )
    }
    
    override fun getRecentPRs(limit: Int): Flow<List<PersonalRecord>> {
        return exerciseDao.getRecentPRs(limit).map { list ->
            list.mapNotNull { entity ->
                val exercise = exerciseDao.getExerciseById(entity.exerciseId)?.toDomainModel()
                exercise?.let {
                    PersonalRecord(
                        exercise = it,
                        type = PRType.valueOf(entity.prType),
                        value = entity.value,
                        achievedAt = entity.achievedAt,
                        workoutId = entity.workoutId
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS - Entity <-> Domain Conversion
// ═══════════════════════════════════════════════════════════════

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
