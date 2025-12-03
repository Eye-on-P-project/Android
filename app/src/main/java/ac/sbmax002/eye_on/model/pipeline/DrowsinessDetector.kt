package ac.sbmax002.eye_on.model.pipeline

/**
 * EAR 기반 졸음 상태 감지기
 *
 * - 사람마다 눈 크기가 달라서 EAR 절대값이 다르기 때문에
 *   "기본 EAR(눈 뜨고 있을 때 평균)"을 세션 동안 학습해서
 *   그 값의 일정 비율 아래로 떨어지면 눈 감김으로 본다.
 */
class DrowsinessDetector(
    private val baseEarThreshold: Float = 0.25f,    // 초기 몇 프레임 동안 쓸 기본 임계값 (baseline 없을 때만 사용)
    private val closedRatio: Float = 0.7f,          // baselineEAR * closedRatio 보다 작으면 감김으로 판단
    private val drowsyFrameThreshold: Int = 15,     // 연속 N프레임 감기면 "졸음"
    private val sleepingFrameThreshold: Int = 60,   // 연속 N프레임 감기면 "잔다"
    private val recoverFrameThreshold: Int = 15,    // 연속 N프레임 뜨면 "평상시" 복귀
    private val baselineSmoothing: Float = 0.01f    // baselineEAR를 업데이트하는 가중치 (0~1, 작을수록 천천히 변화)
) {

    // 세션 동안의 "눈 뜬 상태" EAR 평균(적응형)
    private var baselineEar: Float = 0f

    private var closedFrameCount: Int = 0
    private var openFrameCount: Int = 0
    private var state: DrowsinessState = DrowsinessState.NORMAL

    /**
     * 매 프레임마다 왼/오른쪽 EAR 값을 받아서
     * - baseline EAR 업데이트
     * - 졸음 상태(NORMAL/DROWSY/SLEEPING) 업데이트
     * 를 수행하고 현재 상태를 반환한다.
     */
    fun update(leftEar: Float?, rightEar: Float?): DrowsinessState {
        // 1) 현재 EAR 평균 (존재하는 값만 평균)
        val ears = listOfNotNull(leftEar, rightEar)
        val avgEar: Float? = if (ears.isNotEmpty()) {
            ears.average().toFloat()
        } else {
            null
        }

        // 2) 이전 baseline 기준으로 "감김/뜸" 판단
        val leftClosedNow = isEyeClosed(leftEar)
        val rightClosedNow = isEyeClosed(rightEar)
        val bothClosedNow = leftClosedNow && rightClosedNow

        // 3) 눈을 뜨고 있을 때만 baselineEAR 를 조금씩 업데이트
        if (!bothClosedNow && avgEar != null && avgEar > 0f) {
            baselineEar = if (baselineEar == 0f) {
                avgEar                    // 초기값 세팅
            } else {
                // 지수 이동 평균 형태로 천천히 따라가게 함
                baselineEar * (1f - baselineSmoothing) + avgEar * baselineSmoothing
            }
        }

        // 4) 연속 프레임 카운트 갱신
        if (bothClosedNow) {
            closedFrameCount++
            openFrameCount = 0
        } else {
            openFrameCount++
            closedFrameCount = 0
        }

        // 5) 감긴 프레임 수에 따라 졸음/수면 상태 진입
        state = when {
            bothClosedNow && closedFrameCount >= sleepingFrameThreshold ->
                DrowsinessState.SLEEPING
            bothClosedNow && closedFrameCount >= drowsyFrameThreshold ->
                DrowsinessState.DROWSY
            else -> state   // 그 외에는 이전 상태 유지
        }

        // 6) 눈 뜬 상태가 일정 프레임 이상 유지되면 무조건 NORMAL 복귀
        if (!bothClosedNow && openFrameCount >= recoverFrameThreshold) {
            state = DrowsinessState.NORMAL
        }

        return state
    }

    /**
     * 현재 기준으로 한 프레임에서 "눈 감김" 여부를 판정
     */
    fun isEyeClosed(ear: Float?): Boolean {
        if (ear == null || ear <= 0f) return false

        // baseline 이 충분히 쌓였으면 그걸 기준으로,
        // 아니면 초기 baseEarThreshold를 사용
        val effectiveThreshold: Float = if (baselineEar > 0f) {
            baselineEar * closedRatio
        } else {
            baseEarThreshold
        }

        return ear < effectiveThreshold
    }

    fun currentState(): DrowsinessState = state

    fun reset() {
        baselineEar = 0f
        closedFrameCount = 0
        openFrameCount = 0
        state = DrowsinessState.NORMAL
    }
}
