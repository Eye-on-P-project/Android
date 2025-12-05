package ac.sbmax002.eye_on.model.pipeline

import ac.sbmax002.eye_on.DTO.DrowsinessState

/**
 * 한 쪽 눈 상태
 */
// 한쪽 눈의 상태
data class EyeState(
    val ear: Float,      // 이 프레임의 EAR 값
    val isClosed: Boolean  // 이 프레임에서 "감겼다" 여부 (threshold 기반)
)

// 전체 파이프라인 결과
data class PipelineResult(
    val frameTimestampMs: Long,
    val isFaceDetected: Boolean,
    val leftEye: EyeState?,
    val rightEye: EyeState?,
    val drowsinessState: DrowsinessState
) {
    // 필요하면 이전처럼 Boolean 으로도 쓸 수 있게
    val isDrowsy: Boolean
        get() = drowsinessState != DrowsinessState.NORMAL
}

// ViewModel 쪽으로 결과를 전달하기 위한 리스너
interface PipelineListener {
    fun onPipelineResult(result: PipelineResult)
    fun onPipelineError(message: String)
}