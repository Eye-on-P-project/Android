package ac.sbmax002.eye_on.model.pipeline

/**
 * 프레임마다 EAR 값을 받아서
 * 일정 프레임 이상 "눈 감김" 상태가 지속되면 졸음으로 판단하는 클래스.
 */
class DrowsinessDetector(
    private val earThreshold: Float = 0.25f,          // 이 값보다 EAR가 작으면 눈 감김으로 판단
    private val consecutiveFrameThreshold: Int = 15   // 몇 프레임 연속이면 졸음으로 볼지
) {

    private var closedFrameCount: Int = 0

    /**
     * @return true 이면 이번 프레임에서 '졸음 상태'라고 판정
     */
    fun update(leftEar: Float?, rightEar: Float?): Boolean {
        // TODO: 나중에 EAR 기반 졸음 로직 구현
        //  - EAR 둘 다 threshold 아래면 closedFrameCount++
        //  - 아니면 closedFrameCount = 0
        //  - closedFrameCount >= consecutiveFrameThreshold 이면 true
        return false
    }

    fun reset() {
        closedFrameCount = 0
    }
}