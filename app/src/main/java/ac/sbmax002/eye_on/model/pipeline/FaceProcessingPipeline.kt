package ac.sbmax002.eye_on.model.pipeline

import ac.sbmax002.eye_on.DTO.DrowsinessState
import ac.sbmax002.eye_on.model.vision.FaceLandmarkerHelper
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

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

        // 4. 졸음 상태 업데이트 (3단계)
        val drowsinessState = drowsinessDetector.update(leftEar, rightEar)

        // Bitmap이 있을 때만 눈을 잘라낸다
        val frameBitmap = resultBundle.frameBitmap
        val leftEyeBitmap = frameBitmap?.let { bmp ->
            cropEyeBitmap(bmp, eyeRoi.leftEyePoints)
        }
        val rightEyeBitmap = frameBitmap?.let { bmp ->
            cropEyeBitmap(bmp, eyeRoi.rightEyePoints)
        }

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
            drowsinessState = drowsinessState,
            leftEyeBitmap = leftEyeBitmap,
            rightEyeBitmap = rightEyeBitmap
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

    // 파일 상단에 필요하다면
// import android.graphics.Bitmap
// import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

    /**
     * 프레임 Bitmap과 눈 랜드마크 리스트를 받아서
     * 눈 주변부를 넉넉하게 잘라낸 작은 Bitmap을 만든다.
     *
     * - 눈이 감겨도 주변부가 같이 보이도록 margin을 준다.
     * - bbox가 너무 작아지면 최소 크기를 강제해서 "뭉개짐" 방지.
     */
    private fun cropEyeBitmap(
        frame: Bitmap,
        eyePoints: List<NormalizedLandmark>,
        marginRatio: Float = 0.4f,
        targetSize: Int = 72
    ): Bitmap? {
        if (eyePoints.isEmpty()) return null

        var minX = 1f
        var minY = 1f
        var maxX = 0f
        var maxY = 0f

        for (p in eyePoints) {
            val x = p.x()
            val y = p.y()
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
        }

        val width = maxX - minX
        val height = maxY - minY

        // 눈 주변까지 여유를 주기 위한 margin
        val marginX = width * marginRatio
        val marginY = height * marginRatio

        var leftNorm = (minX - marginX).coerceIn(0f, 1f)
        var topNorm = (minY - marginY).coerceIn(0f, 1f)
        var rightNorm = (maxX + marginX).coerceIn(0f, 1f)
        var bottomNorm = (maxY + marginY).coerceIn(0f, 1f)

        // 🔒 최소 박스 크기 강제 (너무 작게 잘라서 픽셀이 뭉개지는 것 방지)
        val minNormSize = 0.08f // 프레임의 최소 8% 정도는 보이게
        var curWidthNorm = rightNorm - leftNorm
        var curHeightNorm = bottomNorm - topNorm

        if (curWidthNorm < minNormSize) {
            val cx = (leftNorm + rightNorm) / 2f
            leftNorm = (cx - minNormSize / 2f).coerceIn(0f, 1f)
            rightNorm = (cx + minNormSize / 2f).coerceIn(0f, 1f)
            curWidthNorm = rightNorm - leftNorm
        }

        if (curHeightNorm < minNormSize) {
            val cy = (topNorm + bottomNorm) / 2f
            topNorm = (cy - minNormSize / 2f).coerceIn(0f, 1f)
            bottomNorm = (cy + minNormSize / 2f).coerceIn(0f, 1f)
            curHeightNorm = bottomNorm - topNorm
        }

        val leftPx = (leftNorm * frame.width).toInt().coerceIn(0, frame.width - 1)
        val topPx = (topNorm * frame.height).toInt().coerceIn(0, frame.height - 1)
        val rightPx = (rightNorm * frame.width).toInt().coerceIn(leftPx + 1, frame.width)
        val bottomPx = (bottomNorm * frame.height).toInt().coerceIn(topPx + 1, frame.height)

        val cropWidth = rightPx - leftPx
        val cropHeight = bottomPx - topPx

        if (cropWidth <= 0 || cropHeight <= 0) return null

        val cropped = Bitmap.createBitmap(frame, leftPx, topPx, cropWidth, cropHeight)
        return Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
    }


}