package ac.sbmax002.eye_on.model.pipeline

import ac.sbmax002.eye_on.model.vision.FaceLandmarkerHelper
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy

/**
 * CameraX 프레임 -> MediaPipe FaceLandmarker -> ROI -> EAR -> 졸음 여부
 * 를 조율하는 파이프라인 클래스의 뼈대.
 *
 * 실제 로직은 다음 단계에서 채우고,
 * 지금은 구조/타입만 맞춰둔다.
 */
class FaceProcessingPipeline(
    context: Context,
    private val listener: PipelineListener,
    private val roiExtractor: RoiExtractor = RoiExtractor(),
    private val earCalculator: EarCalculator = EarCalculator(),
    private val drowsinessDetector: DrowsinessDetector = DrowsinessDetector()
) : FaceLandmarkerHelper.LandmarkerListener {

    // MediaPipe 헬퍼는 파이프라인이 직접 소유
    private val faceLandmarkerHelper = FaceLandmarkerHelper(
        context = context,
        faceLandmarkerHelperListener = this
    )

    /**
     * CameraX Analyzer에서 호출할 진입 메소드.
     * 지금은 메모리 누수만 막고, 추론은 다음 단계에서 연결할 거라
     * 프레임을 바로 닫기만 한다.
     */
    fun process(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        // TODO: 실제 모델 연결
        //테스트로그
        Log.d("FaceProcessingPipeline", "frame arrived: ${imageProxy.imageInfo.timestamp}")
        // faceLandmarkerHelper.detectLiveStream(imageProxy, isFrontCamera)
        // 로 바꾸고, 여기서 close()는 제거할 예정.

        imageProxy.close()
    }

    // ---------------------------------------------------------------------
    // FaceLandmarkerHelper.LandmarkerListener 구현부
    // ---------------------------------------------------------------------

    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        // TODO:
        //  1) resultBundle.result 로부터 얼굴 랜드마크 리스트 꺼내기
        //  2) RoiExtractor로 왼/오른쪽 눈 좌표 추출
        //  3) EarCalculator로 EAR 계산
        //  4) DrowsinessDetector로 졸음 여부 판단
        //  5) PipelineResult 만들어 listener로 전달

        // 일단 구조만 보이도록 더미 결과 한 번 만들어놓음.
        val dummyResult = PipelineResult(
            frameTimestampMs = System.currentTimeMillis(),
            isFaceDetected = true,
            leftEye = null,
            rightEye = null,
            isDrowsy = false
        )
        listener.onPipelineResult(dummyResult)
    }

    override fun onError(error: String, errorCode: Int) {
        listener.onPipelineError(error)
    }

    override fun onEmpty() {
        // 얼굴이 하나도 안 잡힌 프레임 처리하고 싶으면
        // 나중에 여기에서 listener.onPipelineResult(...) 호출 추가
    }
}