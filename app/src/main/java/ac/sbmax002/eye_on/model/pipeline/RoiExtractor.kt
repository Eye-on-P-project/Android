package ac.sbmax002.eye_on.model.pipeline

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * 왼/오른쪽 눈 영역(랜드마크들)을 묶어서 들고 있을 데이터 클래스
 */
data class EyeRoi(
    val leftEyePoints: List<NormalizedLandmark> = emptyList(),
    val rightEyePoints: List<NormalizedLandmark> = emptyList()
)

/**
 * MediaPipe 얼굴 랜드마크 결과에서
 * 눈 주변 좌표만 뽑아주는 역할.
 */
class RoiExtractor {
    // p1~p6 순서로 사용됨 (EAR 수식과 매핑)
    private val leftEyeIndices = listOf(362, 385, 387, 263, 373, 380)
    private val rightEyeIndices = listOf(33, 160, 158, 133, 153, 144)

    fun extractEyeRoi(landmarks: List<NormalizedLandmark>): EyeRoi {
        // 혹시라도 랜드마크 개수가 부족하면 빈 ROI 리턴
        val maxIndex = (leftEyeIndices + rightEyeIndices).maxOrNull() ?: 0
        if (landmarks.size <= maxIndex) {
            return EyeRoi()
        }

        val left = leftEyeIndices.map { idx -> landmarks[idx] }
        val right = rightEyeIndices.map { idx -> landmarks[idx] }

        return EyeRoi(
            leftEyePoints = left,
            rightEyePoints = right
        )
    }
}