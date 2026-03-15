package com.fittrack.data.local.dao

import androidx.room.*
import com.fittrack.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ExerciseDao {
    
    // Exercises
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)
    
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): ExerciseEntity?
    
    @Query("SELECT * FROM exercises WHERE category = :category ORDER BY name")
    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>>
    
    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>
    
    @Query("SELECT * FROM exercises ORDER BY name")
    fun getAllExercises(): Flow<List<ExerciseEntity>>
    
    @Query("SELECT * FROM exercises WHERE isCustom = 1 ORDER BY name")
    fun getCustomExercises(): Flow<List<ExerciseEntity>>
    
    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)
    
    // Workouts
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long
    
    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)
    
    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)
    
    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutWithExercises(id: Long): WorkoutWithExercises?
    
    @Transaction
    @Query("""
        SELECT * FROM workouts 
        WHERE startTime >= :start AND startTime < :end 
        ORDER BY startTime DESC
    """)
    fun getWorkoutsForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<WorkoutWithExercises>>
    
    @Query("SELECT * FROM workouts ORDER BY startTime DESC LIMIT :limit")
    fun getRecentWorkouts(limit: Int = 10): Flow<List<WorkoutEntity>>
    
    @Query("""
        SELECT * FROM workouts 
        WHERE endTime IS NULL 
        ORDER BY startTime DESC 
        LIMIT 1
    """)
    suspend fun getActiveWorkout(): WorkoutEntity?
    
    // Exercise Logs
    @Insert
    suspend fun insertExerciseLog(log: ExerciseLogEntity): Long
    
    @Update
    suspend fun updateExerciseLog(log: ExerciseLogEntity)
    
    @Delete
    suspend fun deleteExerciseLog(log: ExerciseLogEntity)
    
    @Transaction
    @Query("SELECT * FROM exercise_logs WHERE workoutId = :workoutId ORDER BY orderIndex")
    fun getExerciseLogsForWorkout(workoutId: Long): Flow<List<ExerciseLogWithSets>>
    
    // Exercise Sets
    @Insert
    suspend fun insertExerciseSet(set: ExerciseSetEntity): Long
    
    @Insert
    suspend fun insertExerciseSets(sets: List<ExerciseSetEntity>)
    
    @Update
    suspend fun updateExerciseSet(set: ExerciseSetEntity)
    
    @Delete
    suspend fun deleteExerciseSet(set: ExerciseSetEntity)
    
    @Query("SELECT * FROM exercise_sets WHERE exerciseLogId = :logId ORDER BY setNumber")
    fun getSetsForExerciseLog(logId: Long): Flow<List<ExerciseSetEntity>>
    
    // Templates
    @Insert
    suspend fun insertTemplate(template: WorkoutTemplateEntity): Long
    
    @Update
    suspend fun updateTemplate(template: WorkoutTemplateEntity)
    
    @Delete
    suspend fun deleteTemplate(template: WorkoutTemplateEntity)
    
    @Transaction
    @Query("SELECT * FROM workout_templates ORDER BY name")
    fun getAllTemplates(): Flow<List<TemplateWithExercises>>
    
    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): TemplateWithExercises?
    
    @Insert
    suspend fun insertTemplateExercise(exercise: TemplateExerciseEntity): Long
    
    @Delete
    suspend fun deleteTemplateExercise(exercise: TemplateExerciseEntity)
    
    // Personal Records
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalRecord(pr: PersonalRecordEntity)
    
    @Query("""
        SELECT * FROM personal_records 
        WHERE exerciseId = :exerciseId 
        ORDER BY achievedAt DESC
    """)
    fun getPersonalRecordsForExercise(exerciseId: String): Flow<List<PersonalRecordEntity>>
    
    @Query("""
        SELECT * FROM personal_records 
        WHERE exerciseId = :exerciseId AND prType = :type 
        ORDER BY value DESC 
        LIMIT 1
    """)
    suspend fun getBestRecordForExercise(exerciseId: String, type: String): PersonalRecordEntity?
    
    @Query("SELECT * FROM personal_records ORDER BY achievedAt DESC LIMIT :limit")
    fun getRecentPRs(limit: Int = 10): Flow<List<PersonalRecordEntity>>
    
    // Stats queries
    @Query("""
        SELECT COUNT(*) FROM workouts 
        WHERE startTime >= :start AND startTime < :end
    """)
    suspend fun getWorkoutCountForDateRange(start: LocalDateTime, end: LocalDateTime): Int
    
    @Query("""
        SELECT SUM(
            (SELECT SUM(es.weight * es.reps) 
             FROM exercise_sets es 
             INNER JOIN exercise_logs el ON es.exerciseLogId = el.id 
             WHERE el.workoutId = w.id)
        )
        FROM workouts w
        WHERE w.startTime >= :start AND w.startTime < :end
    """)
    suspend fun getTotalVolumeForDateRange(start: LocalDateTime, end: LocalDateTime): Float?
}
