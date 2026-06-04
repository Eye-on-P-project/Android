package ac.sbmax002.eye_on.model.pipeline

import ac.sbmax002.eye_on.DTO.DrowsinessState
import ac.sbmax002.eye_on.model.inference.SleepModelRunner
import ac.sbmax002.eye_on.model.vision.FaceLandmarkerHelper
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy

class FaceProcessingPipeline(
    context: Context,
    private val listener: PipelineListener,
    private val drowsyDurationMs: Long = 1_000L,
    private val temporalConfig: TemporalDetectionConfig = TemporalDetectionConfig()
) : FaceLandmarkerHelper.LandmarkerListener {

    private val temporalDetector = TemporalDrowsinessDetector(
        modelRunner = SleepModelRunner(context.applicationContext),
        config = temporalConfig
    )

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
        temporalDetector.reset()
    }

    fun updateDrowsyDuration(newDurationMs: Long) {
        Log.d(TAG, "updateDrowsyDuration ignored by temporal model: $newDurationMs")
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
                    drowsinessState = temporalDetector.currentState()
                )
            )
            return
        }

        val output = temporalDetector.update(
            bitmap = resultBundle.bitmap,
            landmarks = faceResult.faceLandmarks()[0],
            timestampMs = timestampMs,
            faceLandmarkerMs = resultBundle.inferenceTime
        )

        listener.onPipelineResult(
            PipelineResult(
                frameTimestampMs = timestampMs,
                isFaceDetected = true,
                leftEye = output.leftEar?.let {
                    EyeState(ear = it, isClosed = output.leftClosed)
                },
                rightEye = output.rightEar?.let {
                    EyeState(ear = it, isClosed = output.rightClosed)
                },
                drowsinessState = output.state
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
                drowsinessState = temporalDetector.currentState()
            )
        )
    }

    companion object {
        private const val TAG = "FacePipeline"
    }
}
