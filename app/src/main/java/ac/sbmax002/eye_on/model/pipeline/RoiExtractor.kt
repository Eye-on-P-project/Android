package ac.sbmax002.eye_on.model.pipeline

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

data class EyeRoi(
    val leftEyePoints: List<NormalizedLandmark> = emptyList(),
    val rightEyePoints: List<NormalizedLandmark> = emptyList()
)

class RoiExtractor {
    private val leftEyeIndices = listOf(362, 385, 387, 263, 373, 380)
    private val rightEyeIndices = listOf(33, 160, 158, 133, 153, 144)

    fun extractEyeRoi(landmarks: List<NormalizedLandmark>): EyeRoi {
        val maxIndex = (leftEyeIndices + rightEyeIndices).maxOrNull() ?: return EyeRoi()
        if (landmarks.size <= maxIndex) return EyeRoi()

        return EyeRoi(
            leftEyePoints = leftEyeIndices.map { landmarks[it] },
            rightEyePoints = rightEyeIndices.map { landmarks[it] }
        )
    }
}
