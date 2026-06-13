package ac.sbmax002.eye_on.model.pipeline

import ac.sbmax002.eye_on.model.vision.FaceLandmarkerHelper
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy

class FaceProcessingPipeline(
    context: Context,
    private val listener: PipelineListener,
    drowsyDurationMs: Long = 1_000L,
    private val roiExtractor: RoiExtractor = RoiExtractor(),
    private val earCalculator: EarCalculator = EarCalculator(),
    private val drowsinessDetector: DrowsinessDetector = DrowsinessDetector(
        drowsyDurationMs = drowsyDurationMs
    )
) : FaceLandmarkerHelper.LandmarkerListener {

    private val faceLandmarkerHelper = FaceLandmarkerHelper(
        context = context.applicationContext,
        faceLandmarkerHelperListener = this
    )

    fun process(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        try {
            faceLandmarkerHelper.detectLiveStream(imageProxy, isFrontCamera)
        } catch (e: Exception) {
            try {
                imageProxy.close()
            } catch (_: Exception) {
                // ignore close failures from an already closed ImageProxy
            }
            Log.e(TAG, "process error: ${e.message}", e)
            listener.onPipelineError("FaceProcessing failed: ${e.message ?: "unknown error"}")
        }
    }

    fun reset() {
        drowsinessDetector.reset()
    }

    fun updateDrowsyDuration(newDurationMs: Long) {
        Log.d(TAG, "updateDrowsyDuration=$newDurationMs")
        drowsinessDetector.updateDrowsyDuration(newDurationMs)
    }

    override fun onError(error: String, errorCode: Int) {
        listener.onPipelineError(error)
    }

    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        val faceResult = resultBundle.result
        val timestampMs = faceResult.timestampMs()

        if (faceResult.faceLandmarks().isEmpty()) {
            listener.onPipelineResult(
                PipelineResult(
                    frameTimestampMs = timestampMs,
                    isFaceDetected = false,
                    leftEye = null,
                    rightEye = null,
                    drowsinessState = drowsinessDetector.currentState()
                )
            )
            return
        }

        val eyeRoi = roiExtractor.extractEyeRoi(faceResult.faceLandmarks()[0])
        val leftEar = eyeRoi.leftEyePoints.takeIf { it.isNotEmpty() }
            ?.let { earCalculator.calculateEar(it) }
        val rightEar = eyeRoi.rightEyePoints.takeIf { it.isNotEmpty() }
            ?.let { earCalculator.calculateEar(it) }
        val drowsinessState = drowsinessDetector.update(
            leftEar = leftEar,
            rightEar = rightEar,
            frameTimestampMs = timestampMs
        )

        listener.onPipelineResult(
            PipelineResult(
                frameTimestampMs = timestampMs,
                isFaceDetected = true,
                leftEye = leftEar?.let { ear ->
                    EyeState(ear = ear, isClosed = drowsinessDetector.isEyeClosed(ear))
                },
                rightEye = rightEar?.let { ear ->
                    EyeState(ear = ear, isClosed = drowsinessDetector.isEyeClosed(ear))
                },
                drowsinessState = drowsinessState
            )
        )
    }

    override fun onEmpty() {
        listener.onPipelineResult(
            PipelineResult(
                frameTimestampMs = System.currentTimeMillis(),
                isFaceDetected = false,
                leftEye = null,
                rightEye = null,
                drowsinessState = drowsinessDetector.currentState()
            )
        )
    }

    companion object {
        private const val TAG = "FacePipeline"
    }
}
