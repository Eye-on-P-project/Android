package ac.sbmax002.eye_on.model.pipeline

/**
 * 한 쪽 눈 상태
 */
data class EyeState(
    val ear: Float,      // 계산된 EAR 값
    val isClosed: Boolean // EAR 기준으로 눈이 감긴 상태인지
)

/**
 * 비전 파이프라인 전체 결과
 */
data class PipelineResult(
    val frameTimestampMs: Long,
    val isFaceDetected: Boolean,
    val leftEye: EyeState?,
    val rightEye: EyeState?,
    val isDrowsy: Boolean
)

/**
 * 파이프라인 결과를 ViewModel/UI 쪽으로 보내기 위한 콜백 인터페이스
 */
interface PipelineListener {
    fun onPipelineResult(result: PipelineResult)
    fun onPipelineError(message: String)
}