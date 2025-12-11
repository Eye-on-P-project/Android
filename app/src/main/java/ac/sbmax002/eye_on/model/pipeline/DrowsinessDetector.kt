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
    // 시간 기준 임계값 (ms 단위)
    private val drowsyDurationMs: Long = 1_000L,     // 연속 N ms 감기면 "졸음"
    private val sleepingDurationMs: Long = 3_000L,   // 연속 N ms 감기면 "잔다"
    private val recoverDurationMs: Long = 1_000L,    // 연속 N ms 뜨면 "평상시" 복귀

    // baseline 학습 속도
    private val baselineSmoothing: Float = 0.01f,    // 일반 구간에서 baseline 업데이트 속도 (0~1, 작을수록 천천히)
    private val warmupDurationMs: Long = 2_000L,     // 워밍업 기간 (ms)
    private val warmupBaselineSmoothing: Float = 0.1f // 워밍업 구간에서만 사용하는 더 큰 학습률
) {

    // 세션 동안의 "눈 뜬 상태" EAR 평균 (적응형)
    private var baselineEar: Float = 0f

    // 시간 누적용 상태 (ms 단위)
    private var lastTimestampMs: Long? = null
    private var warmupElapsedMs: Long = 0L

    private var closedDurationMs: Long = 0L
    private var openDurationMs: Long = 0L
    private var state: DrowsinessState = DrowsinessState.NORMAL

    /**
     * 매 프레임마다 왼/오른쪽 EAR 값을 받아서
     * - 워밍업 구간에서는 baseline만 학습하고 항상 NORMAL 유지
     * - 워밍업 이후부터 졸음 상태(NORMAL/DROWSY/SLEEPING)를 판정
     */
    fun update(leftEar: Float?, rightEar: Float?, frameTimestampMs: Long): DrowsinessState {
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

        // 3) 시간 경과 계산 (이전 timestamp와의 차이)
        val prevTs = lastTimestampMs
        val deltaMs = if (prevTs != null) {
            val raw = frameTimestampMs - prevTs
            if (raw < 0L) 0L else raw
        } else {
            0L
        }
        lastTimestampMs = frameTimestampMs

        val hasValidEar = avgEar != null && avgEar > 0f

        // 4) 유효 EAR 가 있는 프레임에서만 워밍업 시간 누적
        if (hasValidEar) {
            warmupElapsedMs += deltaMs
        }

        val inWarmup = warmupElapsedMs < warmupDurationMs

        // 5) 눈 뜬 상태에서만 baselineEAR 업데이트
        if (!bothClosedNow && hasValidEar) {
            baselineEar = if (baselineEar == 0f) {
                avgEar!!
            } else {
                // 🔹 워밍업 구간에서는 더 큰 학습률, 이후에는 작은 학습률 사용
                val alpha = if (inWarmup) warmupBaselineSmoothing else baselineSmoothing
                baselineEar * (1f - alpha) + avgEar!! * alpha
            }
        }

        // 6) 워밍업 구간 동안에는 상태를 무조건 NORMAL 로 고정
        if (inWarmup) {
            closedDurationMs = 0L
            openDurationMs = 0L
            state = DrowsinessState.NORMAL
            return state
        }

        // 7) 워밍업 이후부터는 본격적으로 감김/뜸 "시간" 누적
        if (hasValidEar) {
            if (bothClosedNow) {
                closedDurationMs += deltaMs
                openDurationMs = 0L
            } else {
                openDurationMs += deltaMs
                closedDurationMs = 0L
            }
        }

        // 8) 감긴 시간에 따라 졸음/수면 상태 진입
        state = when {
            bothClosedNow && closedDurationMs >= sleepingDurationMs ->
                DrowsinessState.SLEEPING
            bothClosedNow && closedDurationMs >= drowsyDurationMs ->
                DrowsinessState.DROWSY
            else -> state   // 그 외에는 이전 상태 유지
        }

        // 9) 눈 뜬 상태가 일정 시간 이상 유지되면 NORMAL 복귀
        if (!bothClosedNow && openDurationMs >= recoverDurationMs) {
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
        lastTimestampMs = null
        warmupElapsedMs = 0L
        closedDurationMs = 0L
        openDurationMs = 0L
        state = DrowsinessState.NORMAL
    }
}