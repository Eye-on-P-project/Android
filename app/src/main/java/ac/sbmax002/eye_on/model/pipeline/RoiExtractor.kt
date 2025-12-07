package ac.sbmax002.eye_on.model.pipeline

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * 왼/오른쪽 눈 영역(랜드마크들) + 박스를 들고 있을 데이터 클래스
 */
data class EyeRoi(
    val leftEyePoints: List<NormalizedLandmark> = emptyList(),
    val rightEyePoints: List<NormalizedLandmark> = emptyList(),
    val leftEyeBox: EyeBox? = null, //roi 박스 추가
    val rightEyeBox: EyeBox? = null
)

/**
 * 한쪽 눈을 둘러싸는 정규화 박스 (0.0f ~ 1.0f 기준) 눈을 표시할 roi 박스
 */

data class EyeBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/**
 * MediaPipe 얼굴 랜드마크 결과에서
 * 눈 주변 좌표만 뽑아주는 역할.
 */
class RoiExtractor (
    // 박스를 조금 넉넉하게 잡기 위한 마진 비율 (0.3f = 30% 확장)
    private val boxMarginRatio: Float = 0.3f
) {
    // p1~p6 순서로 사용됨 (EAR 수식과 매핑)
    private val leftEyeIndices = listOf(362, 385, 387, 263, 373, 380)
    private val rightEyeIndices = listOf(33, 160, 158, 133, 153, 144)

    fun extractEyeRoi(landmarks: List<NormalizedLandmark>): EyeRoi {
        // 혹시라도 랜드마크 개수가 부족하면 빈 ROI 리턴
        val maxIndex = (leftEyeIndices + rightEyeIndices).maxOrNull() ?: 0
        if (landmarks.size <= maxIndex) {
            return EyeRoi()
        }
        //ear 계산용 눈 랜드마크
        val left = leftEyeIndices.map { idx -> landmarks[idx] }
        val right = rightEyeIndices.map { idx -> landmarks[idx] }

        // 각 눈을 둘러싸는 정규화 박스 계산 (여유 margin 포함)
        val leftBox = computeBox(left)
        val rightBox = computeBox(right)

        return EyeRoi(
            leftEyePoints = left,
            rightEyePoints = right,
            leftEyeBox = leftBox,
            rightEyeBox = rightBox
        )
    }

    /**
     * 주어진 랜드마크 리스트를 모두 포함하는 [EyeBox] 계산.
     * - x(), y()는 이미 0.0f ~ 1.0f 범위의 정규화 좌표
     * - boxMarginRatio 만큼 좌우/상하로 여유를 줘서 너무 딱 맞게 잘리지 않도록 한다.
     */
    private fun computeBox(points: List<NormalizedLandmark>): EyeBox? {
        if (points.isEmpty()) return null

        var minX = 1f
        var minY = 1f
        var maxX = 0f
        var maxY = 0f

        for (p in points) {
            val x = p.x()
            val y = p.y()
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
        }

        val width = maxX - minX
        val height = maxY - minY

        // 폭/높이에 비례해서 여유 margin 추가
        val marginX = width * boxMarginRatio
        val marginY = height * boxMarginRatio

        val expandedMinX = (minX - marginX).coerceIn(0f, 1f)
        val expandedMinY = (minY - marginY).coerceIn(0f, 1f)
        val expandedMaxX = (maxX + marginX).coerceIn(0f, 1f)
        val expandedMaxY = (maxY + marginY).coerceIn(0f, 1f)

        return EyeBox(
            left = expandedMinX,
            top = expandedMinY,
            right = expandedMaxX,
            bottom = expandedMaxY
        )
    }
}