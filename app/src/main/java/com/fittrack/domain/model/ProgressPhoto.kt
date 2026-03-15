package com.fittrack.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

// ═══════════════════════════════════════════════════════════════
// 📷 PROGRESS PHOTOS
// Track visual transformation over time
// ═══════════════════════════════════════════════════════════════

/**
 * A progress photo entry with metadata
 */
data class ProgressPhoto(
    val id: Long = 0,
    val photoUri: String,
    val thumbnailUri: String? = null,
    val capturedAt: LocalDateTime = LocalDateTime.now(),
    val pose: PhotoPose,
    val bodyPart: BodyPart = BodyPart.FULL_BODY,
    
    // Context at time of photo
    val weight: Float? = null,
    val bodyFatPercent: Float? = null,
    val muscleNote: String? = null,  // e.g., "After arm day pump"
    
    // Organization
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isPrivate: Boolean = true,  // Hidden from any sharing
    
    // AI analysis results (if available)
    val aiAnalysis: PhotoAnalysisResult? = null
)

enum class PhotoPose {
    FRONT_RELAXED,
    FRONT_FLEXED,
    FRONT_DOUBLE_BICEP,
    SIDE_RELAXED,
    SIDE_CHEST,
    SIDE_TRICEP,
    BACK_RELAXED,
    BACK_DOUBLE_BICEP,
    BACK_LAT_SPREAD,
    MOST_MUSCULAR,
    LEGS_FRONT,
    LEGS_BACK,
    CUSTOM
}

enum class BodyPart {
    FULL_BODY,
    UPPER_BODY,
    LOWER_BODY,
    ARMS,
    CHEST,
    BACK,
    SHOULDERS,
    ABS,
    LEGS,
    CALVES
}

/**
 * AI analysis result for a progress photo
 */
data class PhotoAnalysisResult(
    val analyzedAt: LocalDateTime = LocalDateTime.now(),
    
    // Estimated metrics
    val estimatedBodyFat: Float?,
    val muscleDefinitionScore: Float?,  // 0-100
    val symmetryScore: Float?,  // 0-100
    
    // Body part specific scores
    val bodyPartScores: Map<String, Float> = emptyMap(),  // e.g., "arms" -> 75
    
    // Key points detected
    val keypointsDetected: Boolean = false,
    val poseConfidence: Float = 0f
)

/**
 * Progress photo comparison
 */
data class PhotoComparison(
    val beforePhoto: ProgressPhoto,
    val afterPhoto: ProgressPhoto,
    val daysBetween: Int,
    
    // Changes detected
    val weightChange: Float?,
    val bodyFatChange: Float?,
    val visibleChanges: List<VisibleChange>,
    
    // AI-generated summary
    val summary: String,
    val motivationalMessage: String
)

data class VisibleChange(
    val bodyPart: String,
    val changeType: ChangeType,
    val description: String,
    val confidence: Float
)

enum class ChangeType {
    MUSCLE_GAIN,
    FAT_LOSS,
    DEFINITION_IMPROVED,
    SIZE_INCREASE,
    SIZE_DECREASE,
    SYMMETRY_IMPROVED,
    NO_CHANGE
}

/**
 * Collection of photos for a transformation journey
 */
data class TransformationJourney(
    val id: Long = 0,
    val name: String,  // e.g., "Summer Cut 2024"
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val goal: String,
    val photos: List<ProgressPhoto>,
    
    // Summary stats
    val startWeight: Float?,
    val currentWeight: Float?,
    val startBodyFat: Float?,
    val currentBodyFat: Float?,
    
    // Generated content
    val collageUri: String? = null,  // Auto-generated grid
    val timelapseUri: String? = null  // Auto-generated video
)

/**
 * Reminder to take progress photos
 */
data class PhotoReminder(
    val id: Long = 0,
    val frequency: ReminderFrequency = ReminderFrequency.WEEKLY,
    val preferredDay: Int = 1,  // Day of week (1=Monday) or day of month
    val preferredTime: String = "08:00",
    val poses: List<PhotoPose> = listOf(
        PhotoPose.FRONT_RELAXED,
        PhotoPose.SIDE_RELAXED,
        PhotoPose.BACK_RELAXED
    ),
    val isEnabled: Boolean = true,
    val includeWeightPrompt: Boolean = true,
    val lastTriggered: LocalDateTime? = null
)

enum class ReminderFrequency {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY
}
