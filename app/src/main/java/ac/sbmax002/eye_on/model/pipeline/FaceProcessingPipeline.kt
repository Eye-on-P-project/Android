package ac.sbmax002.eye_on.model.pipeline

import ac.sbmax002.eye_on.DTO.DrowsinessState
import ac.sbmax002.eye_on.model.vision.FaceLandmarkerHelper
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy

/**
 * CameraX 프레임 -> MediaPipe FaceLandmarker -> ROI -> EAR -> 졸음 상태
 * 전체 파이프라인을 조율하는 클래스.
 */
class FaceProcessingPipeline(
    context: Context,
    private val listener: PipelineListener,
    private val roiExtractor: RoiExtractor = RoiExtractor(),
    private val earCalculator: EarCalculator = EarCalculator(),
    private val drowsinessDetector: DrowsinessDetector = DrowsinessDetector()
) : FaceLandmarkerHelper.LandmarkerListener {

    // MediaPipe 얼굴 랜드마커 헬퍼
    private val faceLandmarkerHelper = FaceLandmarkerHelper(
        context = context,
        faceLandmarkerHelperListener = this
    )

    /**
     * CameraX ImageAnalysis.Analyzer 쪽에서 매 프레임마다 호출해 줄 진입점.
     */
    fun process(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        try {
            // 비동기 라이브스트림 추론 시작 (내부에서 imageProxy.close()까지 처리)
            faceLandmarkerHelper.detectLiveStream(imageProxy, isFrontCamera)
        } catch (e: Exception) {
            // 예외 시에는 직접 닫아주고, 상위로 에러 전달
            try {
                imageProxy.close()
            } catch (_: Exception) { /* ignore */ }

            //테스트 로그
            Log.e("FacePipeline", "process error: ${e.message}", e)
            listener.onPipelineError(
                "FaceProcessing failed: ${e.message ?: "unknown error"}"
            )
        }
    }

    fun reset() {
        drowsinessDetector.reset()
    }

    override fun onError(error: String, errorCode: Int) {
        listener.onPipelineError(error)
    }
    // ---------------------------------------------------------------------
    // FaceLandmarkerHelper.LandmarkerListener 구현부
    // ---------------------------------------------------------------------

    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        val faceResult = resultBundle.result

        // 지연시간 로그 출력
        Log.d(
            "FacePipeline",
            "inferenceTime=${resultBundle.inferenceTime}ms, " +
                    "timestamp=${faceResult.timestampMs()}, " +
                    "faces=${faceResult.faceLandmarks().size}"
        )

        // 1. 얼굴이 하나도 없으면 바로 결과 내려주고 끝
        if (faceResult.faceLandmarks().isEmpty()) {
            val empty = PipelineResult(
                frameTimestampMs = faceResult.timestampMs(),
                isFaceDetected = false,
                leftEye = null,
                rightEye = null,
                drowsinessState = DrowsinessState.NORMAL
            )
            listener.onPipelineResult(empty)
            return
        }

        //  maxNumFaces = 1  기준으로 0번만 사용
        val landmarks = faceResult.faceLandmarks()[0]

        // 2. ROI 추출
        val eyeRoi = roiExtractor.extractEyeRoi(landmarks)

        // 3. EAR 계산 (좌표가 아직 비어 있다면 null 반환)
        val leftEar = eyeRoi.leftEyePoints.takeIf { it.isNotEmpty() }
            ?.let { earCalculator.calculateEar(it) }
        val rightEar = eyeRoi.rightEyePoints.takeIf { it.isNotEmpty() }
            ?.let { earCalculator.calculateEar(it) }

        // 4. 졸음 상태 업데이트
        val drowsinessState = drowsinessDetector.update(
            leftEar = leftEar,
            rightEar = rightEar,
            frameTimestampMs = faceResult.timestampMs() // MediaPipe 가 주는 timestamp (ms)
        )

        // 5. ViewModel에 넘겨줄 DTO 만들기
        val result = PipelineResult(
            frameTimestampMs = faceResult.timestampMs(),
            isFaceDetected = true,
            leftEye = leftEar?.let { ear ->
                EyeState(
                    ear = ear,
                    isClosed = drowsinessDetector.isEyeClosed(ear)
                )
            },
            rightEye = rightEar?.let { ear ->
                EyeState(
                    ear = ear,
                    isClosed = drowsinessDetector.isEyeClosed(ear)
                )
            },
            drowsinessState = drowsinessState
        )

        listener.onPipelineResult(result)
    }

    override fun onEmpty() {
        // 얼굴이 전혀 없는 프레임도 필요하면 이렇게 내려줄 수 있음
        val result = PipelineResult(
            frameTimestampMs = System.currentTimeMillis(),
            isFaceDetected = false,
            leftEye = null,
            rightEye = null,
            drowsinessState = drowsinessDetector.currentState()
        )
        listener.onPipelineResult(result)
    }
}