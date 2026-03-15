package com.fittrack.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fittrack.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    
    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(NutritionRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class)
        )
        
        fun isAvailable(context: Context): Int {
            return HealthConnectClient.getSdkStatus(context)
        }
        
        fun getHealthConnectSettingsIntent(): Intent {
            return Intent().apply {
                action = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
            }
        }
    }
    
    suspend fun hasAllPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(PERMISSIONS)
    }
    
    fun requestPermissionsActivityContract() = 
        PermissionController.createRequestPermissionResultContract()
    
    /**
     * Read health data for a specific date
     */
    suspend fun getHealthDataForDate(date: LocalDate): HealthData {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)
        
        val steps = readSteps(timeRange)
        val distance = readDistance(timeRange)
        val activeCalories = readActiveCalories(timeRange)
        val totalCalories = readTotalCalories(timeRange)
        val heartRateSamples = readHeartRate(timeRange)
        val sleep = readSleep(date)
        val weight = readLatestWeight(timeRange)
        
        return HealthData(
            date = date,
            steps = steps,
            distance = distance,
            activeCalories = activeCalories,
            totalCalories = totalCalories,
            heartRateSamples = heartRateSamples,
            sleepData = sleep,
            weight = weight
        )
    }
    
    /**
     * Read health data as a flow for real-time updates
     */
    fun getHealthDataFlow(date: LocalDate): Flow<HealthData> = flow {
        emit(getHealthDataForDate(date))
    }
    
    private suspend fun readSteps(timeRange: TimeRangeFilter): Int {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = timeRange
                )
            )
            response.records.sumOf { it.count.toInt() }
        } catch (e: Exception) {
            0
        }
    }
    
    private suspend fun readDistance(timeRange: TimeRangeFilter): Float {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = timeRange
                )
            )
            response.records.sumOf { it.distance.inKilometers }.toFloat()
        } catch (e: Exception) {
            0f
        }
    }
    
    private suspend fun readActiveCalories(timeRange: TimeRangeFilter): Int {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = timeRange
                )
            )
            response.records.sumOf { it.energy.inKilocalories.toInt() }
        } catch (e: Exception) {
            0
        }
    }
    
    private suspend fun readTotalCalories(timeRange: TimeRangeFilter): Int {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = timeRange
                )
            )
            response.records.sumOf { it.energy.inKilocalories.toInt() }
        } catch (e: Exception) {
            0
        }
    }
    
    private suspend fun readHeartRate(timeRange: TimeRangeFilter): List<HeartRateSample> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = timeRange
                )
            )
            response.records.flatMap { record ->
                record.samples.map { sample ->
                    HeartRateSample(
                        bpm = sample.beatsPerMinute.toInt(),
                        timestamp = LocalDateTime.ofInstant(sample.time, ZoneId.systemDefault())
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun readSleep(date: LocalDate): SleepData? {
        return try {
            // Sleep sessions typically span from previous evening to morning
            val startOfPreviousEvening = date.minusDays(1).atTime(18, 0)
                .atZone(ZoneId.systemDefault()).toInstant()
            val endOfMorning = date.atTime(12, 0)
                .atZone(ZoneId.systemDefault()).toInstant()
            
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfPreviousEvening, endOfMorning)
                )
            )
            
            response.records.lastOrNull()?.let { session ->
                SleepData(
                    startTime = LocalDateTime.ofInstant(session.startTime, ZoneId.systemDefault()),
                    endTime = LocalDateTime.ofInstant(session.endTime, ZoneId.systemDefault()),
                    stages = session.stages.map { stage ->
                        SleepStage(
                            stage = when (stage.stage) {
                                SleepSessionRecord.STAGE_TYPE_AWAKE -> SleepStageType.AWAKE
                                SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStageType.LIGHT
                                SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStageType.DEEP
                                SleepSessionRecord.STAGE_TYPE_REM -> SleepStageType.REM
                                else -> SleepStageType.LIGHT
                            },
                            startTime = LocalDateTime.ofInstant(stage.startTime, ZoneId.systemDefault()),
                            endTime = LocalDateTime.ofInstant(stage.endTime, ZoneId.systemDefault())
                        )
                    }
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun readLatestWeight(timeRange: TimeRangeFilter): Float? {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = timeRange
                )
            )
            response.records.lastOrNull()?.weight?.inKilograms?.toFloat()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Write a workout session to Health Connect
     */
    suspend fun writeWorkoutSession(workout: Workout) {
        try {
            val exerciseType = when {
                workout.exercises.any { it.exercise.category.name.contains("CARDIO") } -> 
                    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
                else -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            }
            
            val record = ExerciseSessionRecord(
                startTime = workout.startTime.atZone(ZoneId.systemDefault()).toInstant(),
                endTime = (workout.endTime ?: LocalDateTime.now()).atZone(ZoneId.systemDefault()).toInstant(),
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(workout.startTime),
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(workout.endTime ?: LocalDateTime.now()),
                exerciseType = exerciseType,
                title = workout.name,
                notes = workout.notes
            )
            
            healthConnectClient.insertRecords(listOf(record))
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Write nutrition data to Health Connect
     */
    suspend fun writeNutritionRecord(
        mealEntry: MealEntry,
        mealType: Int
    ) {
        try {
            val instant = mealEntry.loggedAt.atZone(ZoneId.systemDefault()).toInstant()
            
            val record = NutritionRecord(
                startTime = instant,
                endTime = instant.plusSeconds(1),
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(mealEntry.loggedAt),
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(mealEntry.loggedAt),
                name = mealEntry.foodItem.name,
                mealType = mealType,
                energy = androidx.health.connect.client.units.Energy.kilocalories(
                    mealEntry.totalCalories.toDouble()
                ),
                protein = androidx.health.connect.client.units.Mass.grams(
                    mealEntry.totalProtein.toDouble()
                ),
                totalCarbohydrate = androidx.health.connect.client.units.Mass.grams(
                    mealEntry.totalCarbs.toDouble()
                ),
                totalFat = androidx.health.connect.client.units.Mass.grams(
                    mealEntry.totalFat.toDouble()
                )
            )
            
            healthConnectClient.insertRecords(listOf(record))
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Write weight to Health Connect
     */
    suspend fun writeWeight(weightKg: Float) {
        try {
            val now = Instant.now()
            val record = WeightRecord(
                time = now,
                zoneOffset = ZoneId.systemDefault().rules.getOffset(now),
                weight = androidx.health.connect.client.units.Mass.kilograms(weightKg.toDouble())
            )
            
            healthConnectClient.insertRecords(listOf(record))
        } catch (e: Exception) {
            // Handle error
        }
    }
}
