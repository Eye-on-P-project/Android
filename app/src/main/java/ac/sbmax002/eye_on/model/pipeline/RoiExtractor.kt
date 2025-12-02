package ac.sbmax002.eye_on.model.pipeline

import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * 왼/오른쪽 눈 영역(랜드마크들)을 묶어서 들고 있을 데이터 클래스
 */
data class EyeRoi(
    val leftEyePoints: List<Pair<Float, Float>> = emptyList(),
    val rightEyePoints: List<Pair<Float, Float>> = emptyList()
)

/**
 * MediaPipe 얼굴 랜드마크 결과에서
 * 눈 주변 좌표만 뽑아주는 역할.
 */
class RoiExtractor {

    /**
     * @param result FaceLandmarkerResult
     * @return 눈 주변 랜드마크 좌표들(정규화 또는 픽셀 좌표)
     */
    fun extractEyeRoi(result: FaceLandmarkerResult): EyeRoi {
        // TODO: 나중에 얼굴 랜드마크 인덱스를 이용해서
        //       왼/오른쪽 눈 좌표만 추출하는 로직 구현
        return EyeRoi()
    }
}