package com.fittrack.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.fittrack.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════
// 📸 AI BODY SCAN ANALYZER
// Uses device camera to analyze body composition from photos
// ═══════════════════════════════════════════════════════════════

/**
 * Body composition estimates from photo analysis
 */
data class BodyScanResult(
    val id: Long = 0,
    val scanDate: LocalDateTime = LocalDateTime.now(),
    
    // Estimated measurements
    val estimatedBodyFatPercentage: Float,
    val estimatedMuscleMassPercentage: Float,
    val bodyType: BodyType,
    
    // Visual analysis
    val shoulderToWaistRatio: Float,
    val waistToHipRatio: Float,
    val symmetryScore: Float,  // 0-100
    
    // Progress indicators
    val muscleDefinitionScore: Float,  // 0-100
    val vascularity: VascularityLevel,
    
    // Pose detection points (for overlay)
    val poseKeypoints: List<PoseKeypoint>,
    
    // Photo references
    val frontPhotoUri: String?,
    val sidePhotoUri: String?,
    val backPhotoUri: String?,
    
    // AI confidence
    val confidenceScore: Float,  // 0-1
    val analysisNotes: List<String>
)

enum class BodyType {
    ECTOMORPH,      // Lean, long limbs
    MESOMORPH,      // Muscular, athletic
    ENDOMORPH,      // Wider, stores fat easily
    ECTO_MESO,      // Mix
    ENDO_MESO       // Mix
}

enum class VascularityLevel {
    NONE,
    LOW,
    MODERATE,
    HIGH,
    EXTREME
}

data class PoseKeypoint(
    val name: String,
    val x: Float,
    val y: Float,
    val confidence: Float
)

/**
 * Progress comparison between two scans
 */
data class BodyScanComparison(
    val beforeScan: BodyScanResult,
    val afterScan: BodyScanResult,
    val daysBetween: Int,
    
    // Changes
    val bodyFatChange: Float,
    val muscleMassChange: Float,
    val definitionChange: Float,
    
    // AI insights
    val progressSummary: String,
    val recommendations: List<String>,
    val achievementUnlocked: String?
)

@Singleton
class BodyScanAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        // Pose landmark indices (MediaPipe compatible)
        const val NOSE = 0
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_KNEE = 25
        const val RIGHT_KNEE = 26
    }
    
    /**
     * Analyze body composition from photos
     * Requires: Front photo (mandatory), Side photo (optional), Back photo (optional)
     */
    suspend fun analyzeBodyComposition(
        frontPhoto: Bitmap,
        sidePhoto: Bitmap? = null,
        backPhoto: Bitmap? = null,
        userProfile: UserProfile
    ): BodyScanResult = withContext(Dispatchers.Default) {
        
        // Step 1: Detect pose keypoints
        val poseKeypoints = detectPose(frontPhoto)
        
        // Step 2: Calculate body ratios
        val shoulderWidth = calculateDistance(
            poseKeypoints.find { it.name == "left_shoulder" },
            poseKeypoints.find { it.name == "right_shoulder" }
        )
        val waistWidth = estimateWaistWidth(poseKeypoints, frontPhoto)
        val hipWidth = calculateDistance(
            poseKeypoints.find { it.name == "left_hip" },
            poseKeypoints.find { it.name == "right_hip" }
        )
        
        val shoulderToWaistRatio = if (waistWidth > 0) shoulderWidth / waistWidth else 1f
        val waistToHipRatio = if (hipWidth > 0) waistWidth / hipWidth else 1f
        
        // Step 3: Estimate body fat using Navy method approximation + visual cues
        val estimatedBF = estimateBodyFatFromVisuals(
            shoulderToWaistRatio = shoulderToWaistRatio,
            waistToHipRatio = waistToHipRatio,
            gender = userProfile.gender,
            age = userProfile.age,
            frontPhoto = frontPhoto
        )
        
        // Step 4: Determine body type
        val bodyType = classifyBodyType(
            shoulderToWaistRatio = shoulderToWaistRatio,
            estimatedBF = estimatedBF,
            userProfile = userProfile
        )
        
        // Step 5: Analyze muscle definition
        val definitionScore = analyzeMuscleDefinition(frontPhoto, poseKeypoints)
        val vascularity = analyzeVascularity(frontPhoto)
        
        // Step 6: Check symmetry
        val symmetryScore = analyzeSymmetry(poseKeypoints)
        
        // Step 7: Generate insights
        val insights = generateInsights(
            bodyFat = estimatedBF,
            bodyType = bodyType,
            definitionScore = definitionScore,
            shoulderToWaistRatio = shoulderToWaistRatio
        )
        
        BodyScanResult(
            estimatedBodyFatPercentage = estimatedBF,
            estimatedMuscleMassPercentage = 100f - estimatedBF - 15f, // Rough estimate
            bodyType = bodyType,
            shoulderToWaistRatio = shoulderToWaistRatio,
            waistToHipRatio = waistToHipRatio,
            symmetryScore = symmetryScore,
            muscleDefinitionScore = definitionScore,
            vascularity = vascularity,
            poseKeypoints = poseKeypoints,
            frontPhotoUri = null, // Set after saving
            sidePhotoUri = null,
            backPhotoUri = null,
            confidenceScore = calculateConfidence(poseKeypoints),
            analysisNotes = insights
        )
    }
    
    /**
     * Compare two body scans and generate progress report
     */
    suspend fun compareScans(
        beforeScan: BodyScanResult,
        afterScan: BodyScanResult
    ): BodyScanComparison = withContext(Dispatchers.Default) {
        
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
            beforeScan.scanDate.toLocalDate(),
            afterScan.scanDate.toLocalDate()
        ).toInt()
        
        val bfChange = afterScan.estimatedBodyFatPercentage - beforeScan.estimatedBodyFatPercentage
        val mmChange = afterScan.estimatedMuscleMassPercentage - beforeScan.estimatedMuscleMassPercentage
        val defChange = afterScan.muscleDefinitionScore - beforeScan.muscleDefinitionScore
        
        val summary = generateProgressSummary(bfChange, mmChange, defChange, daysBetween)
        val recommendations = generateRecommendations(afterScan, bfChange, mmChange)
        val achievement = checkForAchievement(bfChange, mmChange, defChange)
        
        BodyScanComparison(
            beforeScan = beforeScan,
            afterScan = afterScan,
            daysBetween = daysBetween,
            bodyFatChange = bfChange,
            muscleMassChange = mmChange,
            definitionChange = defChange,
            progressSummary = summary,
            recommendations = recommendations,
            achievementUnlocked = achievement
        )
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PRIVATE ANALYSIS METHODS
    // ═══════════════════════════════════════════════════════════════
    
    private fun detectPose(image: Bitmap): List<PoseKeypoint> {
        // In production: Use ML Kit Pose Detection or MediaPipe
        // This is a placeholder that would be replaced with actual ML inference
        
        val width = image.width.toFloat()
        val height = image.height.toFloat()
        
        // Simulated keypoints (in production, these come from ML model)
        return listOf(
            PoseKeypoint("nose", width * 0.5f, height * 0.1f, 0.95f),
            PoseKeypoint("left_shoulder", width * 0.35f, height * 0.22f, 0.92f),
            PoseKeypoint("right_shoulder", width * 0.65f, height * 0.22f, 0.92f),
            PoseKeypoint("left_elbow", width * 0.25f, height * 0.35f, 0.88f),
            PoseKeypoint("right_elbow", width * 0.75f, height * 0.35f, 0.88f),
            PoseKeypoint("left_wrist", width * 0.2f, height * 0.48f, 0.85f),
            PoseKeypoint("right_wrist", width * 0.8f, height * 0.48f, 0.85f),
            PoseKeypoint("left_hip", width * 0.4f, height * 0.52f, 0.90f),
            PoseKeypoint("right_hip", width * 0.6f, height * 0.52f, 0.90f),
            PoseKeypoint("left_knee", width * 0.38f, height * 0.72f, 0.87f),
            PoseKeypoint("right_knee", width * 0.62f, height * 0.72f, 0.87f),
            PoseKeypoint("left_ankle", width * 0.36f, height * 0.92f, 0.82f),
            PoseKeypoint("right_ankle", width * 0.64f, height * 0.92f, 0.82f)
        )
    }
    
    private fun calculateDistance(p1: PoseKeypoint?, p2: PoseKeypoint?): Float {
        if (p1 == null || p2 == null) return 0f
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    private fun estimateWaistWidth(keypoints: List<PoseKeypoint>, image: Bitmap): Float {
        // Estimate waist as midpoint between shoulders and hips
        val leftShoulder = keypoints.find { it.name == "left_shoulder" }
        val rightShoulder = keypoints.find { it.name == "right_shoulder" }
        val leftHip = keypoints.find { it.name == "left_hip" }
        val rightHip = keypoints.find { it.name == "right_hip" }
        
        if (leftShoulder == null || rightShoulder == null || 
            leftHip == null || rightHip == null) {
            return (calculateDistance(leftHip, rightHip) * 0.85f)
        }
        
        // Waist is typically 80-90% of hip width for fit individuals
        val hipWidth = calculateDistance(leftHip, rightHip)
        return hipWidth * 0.85f
    }
    
    private fun estimateBodyFatFromVisuals(
        shoulderToWaistRatio: Float,
        waistToHipRatio: Float,
        gender: Gender?,
        age: Int?,
        frontPhoto: Bitmap
    ): Float {
        // Visual estimation based on ratios and image analysis
        // In production: Use trained ML model for more accuracy
        
        var baseBF = when {
            shoulderToWaistRatio > 1.5f -> 10f  // Very V-tapered
            shoulderToWaistRatio > 1.3f -> 15f  // Athletic
            shoulderToWaistRatio > 1.1f -> 20f  // Fit
            shoulderToWaistRatio > 1.0f -> 25f  // Average
            else -> 30f  // Higher body fat
        }
        
        // Adjust for gender
        if (gender == Gender.FEMALE) {
            baseBF += 8f  // Women naturally carry more essential fat
        }
        
        // Adjust for age
        age?.let {
            if (it > 40) baseBF += (it - 40) * 0.1f
        }
        
        // Analyze image for visible abs (lower BF indicator)
        val absVisible = analyzeAbdominalDefinition(frontPhoto)
        if (absVisible > 0.7f) baseBF -= 5f
        else if (absVisible > 0.4f) baseBF -= 2f
        
        return baseBF.coerceIn(5f, 45f)
    }
    
    private fun analyzeAbdominalDefinition(image: Bitmap): Float {
        // In production: ML model to detect visible abs
        // Placeholder returning moderate definition
        return 0.5f
    }
    
    private fun classifyBodyType(
        shoulderToWaistRatio: Float,
        estimatedBF: Float,
        userProfile: UserProfile
    ): BodyType {
        return when {
            shoulderToWaistRatio > 1.4f && estimatedBF < 15f -> BodyType.MESOMORPH
            shoulderToWaistRatio > 1.2f && estimatedBF < 20f -> BodyType.ECTO_MESO
            shoulderToWaistRatio < 1.1f && estimatedBF > 25f -> BodyType.ENDOMORPH
            shoulderToWaistRatio > 1.2f && estimatedBF > 20f -> BodyType.ENDO_MESO
            else -> BodyType.ECTOMORPH
        }
    }
    
    private fun analyzeMuscleDefinition(image: Bitmap, keypoints: List<PoseKeypoint>): Float {
        // In production: Analyze muscle striations, separation, etc.
        // Placeholder based on pose confidence (better pose = likely more defined)
        val avgConfidence = keypoints.map { it.confidence }.average().toFloat()
        return (avgConfidence * 80f).coerceIn(0f, 100f)
    }
    
    private fun analyzeVascularity(image: Bitmap): VascularityLevel {
        // In production: Detect visible veins in forearms, biceps
        return VascularityLevel.MODERATE
    }
    
    private fun analyzeSymmetry(keypoints: List<PoseKeypoint>): Float {
        val leftShoulder = keypoints.find { it.name == "left_shoulder" }
        val rightShoulder = keypoints.find { it.name == "right_shoulder" }
        val leftHip = keypoints.find { it.name == "left_hip" }
        val rightHip = keypoints.find { it.name == "right_hip" }
        
        // Check if shoulders are level
        val shoulderDiff = kotlin.math.abs((leftShoulder?.y ?: 0f) - (rightShoulder?.y ?: 0f))
        val hipDiff = kotlin.math.abs((leftHip?.y ?: 0f) - (rightHip?.y ?: 0f))
        
        // Lower difference = better symmetry
        val symmetryPenalty = (shoulderDiff + hipDiff) / 2f
        return (100f - symmetryPenalty * 10f).coerceIn(0f, 100f)
    }
    
    private fun calculateConfidence(keypoints: List<PoseKeypoint>): Float {
        return keypoints.map { it.confidence }.average().toFloat()
    }
    
    private fun generateInsights(
        bodyFat: Float,
        bodyType: BodyType,
        definitionScore: Float,
        shoulderToWaistRatio: Float
    ): List<String> {
        val insights = mutableListOf<String>()
        
        when {
            bodyFat < 12f -> insights.add("🔥 Competition-ready body fat levels!")
            bodyFat < 15f -> insights.add("💪 Athletic body fat range - visible abs likely")
            bodyFat < 20f -> insights.add("✅ Healthy, fit body fat percentage")
            bodyFat < 25f -> insights.add("📊 Average body fat - room for improvement")
            else -> insights.add("🎯 Focus on gradual fat loss for best results")
        }
        
        when (bodyType) {
            BodyType.MESOMORPH -> insights.add("🏆 Naturally muscular build - great for strength sports")
            BodyType.ECTOMORPH -> insights.add("🏃 Lean build - focus on progressive overload and surplus calories")
            BodyType.ENDOMORPH -> insights.add("💪 Powerful build - prioritize HIIT and resistance training")
            else -> insights.add("⚖️ Mixed body type - balanced approach works best")
        }
        
        if (shoulderToWaistRatio > 1.4f) {
            insights.add("📐 Excellent V-taper! Your shoulder-to-waist ratio is impressive")
        }
        
        if (definitionScore > 70f) {
            insights.add("✨ Good muscle definition visible")
        }
        
        return insights
    }
    
    private fun generateProgressSummary(
        bfChange: Float,
        mmChange: Float,
        defChange: Float,
        days: Int
    ): String {
        return buildString {
            append("In $days days: ")
            
            when {
                bfChange < -2f && mmChange > 1f -> append("Amazing recomposition! Lost fat while gaining muscle. 🔥")
                bfChange < -1f -> append("Great fat loss progress! ${String.format("%.1f", -bfChange)}% body fat lost. 💪")
                mmChange > 2f -> append("Solid muscle gains! Keep pushing. 📈")
                defChange > 10f -> append("Visible improvement in muscle definition! ✨")
                bfChange > 2f -> append("Body fat increased. Review your nutrition plan. 📊")
                else -> append("Maintaining well. Consider adjusting for new goals. ⚖️")
            }
        }
    }
    
    private fun generateRecommendations(
        scan: BodyScanResult,
        bfChange: Float,
        mmChange: Float
    ): List<String> {
        val recs = mutableListOf<String>()
        
        if (scan.estimatedBodyFatPercentage > 20f) {
            recs.add("Consider a moderate calorie deficit (300-500 kcal) for fat loss")
            recs.add("Increase protein intake to preserve muscle mass")
        }
        
        if (scan.muscleDefinitionScore < 50f) {
            recs.add("Focus on progressive overload in your training")
            recs.add("Ensure adequate protein (1.6-2.2g per kg bodyweight)")
        }
        
        if (scan.symmetryScore < 80f) {
            recs.add("Include unilateral exercises to improve symmetry")
        }
        
        if (bfChange > 1f && mmChange < 0f) {
            recs.add("You may be in too large a surplus - consider reducing calories slightly")
        }
        
        return recs
    }
    
    private fun checkForAchievement(
        bfChange: Float,
        mmChange: Float,
        defChange: Float
    ): String? {
        return when {
            bfChange < -5f -> "🏆 Fat Burner - Lost 5%+ body fat!"
            mmChange > 3f -> "💪 Mass Monster - Gained significant muscle!"
            defChange > 20f -> "✨ Chiseled - Major definition improvement!"
            bfChange < -2f && mmChange > 1f -> "🔥 Recomp King - Lost fat and gained muscle!"
            else -> null
        }
    }
}
