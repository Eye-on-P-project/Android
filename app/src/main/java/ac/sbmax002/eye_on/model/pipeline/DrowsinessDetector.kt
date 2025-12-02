package ac.sbmax002.eye_on.model.pipeline

class DrowsinessDetector(
    private val earThreshold: Float = 0.25f,   // EAR 이 이 값보다 작으면 "감겼다"
    private val drowsyFrameThreshold: Int = 15,   // 이만큼 연속으로 감기면 "졸음"
    private val sleepingFrameThreshold: Int = 45, // 이만큼 연속으로 감기면 "잔다"
    private val recoverFrameThreshold: Int = 10   // 다시 눈 뜬 상태가 이만큼 유지되면 NORMAL 복귀
) {

    private var closedFrameCount: Int = 0   // 연속으로 눈을 감은 프레임 수
    private var openFrameCount: Int = 0     // 연속으로 눈을 뜬 프레임 수
    private var state: DrowsinessState = DrowsinessState.NORMAL

    /**
     * 한 프레임의 EAR(left/right) 를 받아서
     * 내부 상태를 업데이트하고 현재 졸음 상태를 리턴한다.
     */
    fun update(leftEar: Float?, rightEar: Float?): DrowsinessState {
        val leftClosed = isEyeClosed(leftEar)
        val rightClosed = isEyeClosed(rightEar)
        // 둘 다 감겨 있을 때만 "닫힘"으로 본다 (필요하면 한쪽만으로도 감지하도록 바꿀 수 있음)
        val bothClosed = leftClosed && rightClosed

        if (bothClosed) {
            closedFrameCount += 1
            openFrameCount = 0
        } else {
            closedFrameCount = 0
            openFrameCount += 1
        }

        // 닫힌 프레임 수에 따라 상태 진입
        state = when {
            bothClosed && closedFrameCount >= sleepingFrameThreshold ->
                DrowsinessState.SLEEPING
            bothClosed && closedFrameCount >= drowsyFrameThreshold ->
                DrowsinessState.DROWSY
            else -> state   // 나머지는 상태 유지
        }

        // 다시 눈 뜬 상태가 일정 프레임 이상 유지되면 NORMAL 로 복귀
        if (!bothClosed && openFrameCount >= recoverFrameThreshold) {
            state = DrowsinessState.NORMAL
            closedFrameCount = 0
        }

        return state
    }

    fun reset() {
        closedFrameCount = 0
        openFrameCount = 0
        state = DrowsinessState.NORMAL
    }

    // EAR 값으로 한 프레임에서 "눈 감김" 여부만 판단
    fun isEyeClosed(ear: Float?): Boolean =
        ear != null && ear < earThreshold

    fun currentState(): DrowsinessState = state
}
