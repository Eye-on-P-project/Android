package ac.sbmax002.eye_on.model.pipeline

import ac.sbmax002.eye_on.DTO.DrowsinessState

/**
 * EAR 기반 졸음 상태 감지기
 *
 * - 사람마다 눈 크기가 달라서 EAR 절대값이 다르므로,
 *   세션 초반 몇 초 동안은 "눈 뜬 상태" EAR의 평균(baseline)을 학습만 하고
 *   그 이후부터 baseline을 기준으로 졸음/수면을 판정한다.
 */
class DrowsinessDetector(
    private val baseEarThreshold: Float = 0.25f,    // baseline 없을 때 임시로 쓸 기본값
    private val closedRatio: Float = 0.7f,          // baselineEAR * closedRatio 보다 작으면 감김으로 판단
    private val drowsyFrameThreshold: Int = 15,     // 연속 N프레임 감기면 "졸음"
    private val sleepingFrameThreshold: Int = 60,   // 연속 N프레임 감기면 "잔다"
    private val recoverFrameThreshold: Int = 15,    // 연속 N프레임 뜨면 "평상시" 복귀
    private val baselineSmoothing: Float = 0.01f,   // baselineEAR 업데이트 속도 (0~1, 작을수록 천천히)
    private val warmupFrameThreshold: Int = 60      // 워밍업 기간 프레임 수
) {

    // 세션 동안의 "눈 뜬 상태" EAR 평균 (적응형)
    private var baselineEar: Float = 0f

    private var closedFrameCount: Int = 0
    private var openFrameCount: Int = 0
    private var processedFrameCount: Int = 0
    private var state: DrowsinessState = DrowsinessState.NORMAL

    /**
     * 매 프레임마다 왼/오른쪽 EAR 값을 받아서
     * - 워밍업 구간에서는 baseline만 학습하고 항상 NORMAL 유지
     * - 워밍업 이후부터 졸음 상태(NORMAL/DROWSY/SLEEPING)를 판정
     */
    fun update(leftEar: Float?, rightEar: Float?): DrowsinessState {
        // 1) 현재 프레임 EAR 평균 (존재하는 값만 평균)
        val ears = listOfNotNull(leftEar, rightEar)
        val avgEar: Float? = if (ears.isNotEmpty()) {
            ears.average().toFloat()
        } else {
            null
        }

        // 2) 현재 프레임에서 감김 여부 (임계값 기준)
        val leftClosedNow = isEyeClosed(leftEar)
        val rightClosedNow = isEyeClosed(rightEar)
        val bothClosedNow = leftClosedNow && rightClosedNow

        // 3) 처리된 프레임 카운트 (EAR가 아예 없으면 센다고 보지 않음)
        if (avgEar != null && avgEar > 0f) {
            processedFrameCount++
        }

        // 4) 눈 뜬 상태에서만 baselineEAR를 천천히 업데이트
        if (!bothClosedNow && avgEar != null && avgEar > 0f) {
            baselineEar = if (baselineEar == 0f) {
                avgEar               // 첫 값은 그대로 사용
            } else {
                // 지수 이동 평균 형태 (과거 baseline을 조금 남기고 최신 avgEar를 조금 섞음)
                baselineEar * (1f - baselineSmoothing) + avgEar * baselineSmoothing
            }
        }

        // 5) 워밍업 구간인지 체크
        val inWarmup = processedFrameCount < warmupFrameThreshold

        if (inWarmup) {
            // 워밍업 동안에는 상태를 무조건 NORMAL 로 고정
            //    (이때 baselineEar 만 학습되는 느낌)
            closedFrameCount = 0
            openFrameCount = 0
            state = DrowsinessState.NORMAL
            return state
        }

        // 6) 워밍업 이후부터는 본격적으로 감김/뜸 프레임 카운트 관리
        if (bothClosedNow) {
            closedFrameCount++
            openFrameCount = 0
        } else {
            openFrameCount++
            closedFrameCount = 0
        }

        // 7) 감긴 프레임 수에 따라 졸음/수면 상태 진입
        state = when {
            bothClosedNow && closedFrameCount >= sleepingFrameThreshold ->
                DrowsinessState.SLEEPING
            bothClosedNow && closedFrameCount >= drowsyFrameThreshold ->
                DrowsinessState.DROWSY
            else -> state   // 그 외에는 이전 상태 유지
        }

        // 8) 눈 뜬 상태가 일정 프레임 이상 유지되면 무조건 NORMAL 복귀
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
        // 아직 없으면 초기 baseEarThreshold 사용
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
        processedFrameCount = 0
        state = DrowsinessState.NORMAL
    }
}
