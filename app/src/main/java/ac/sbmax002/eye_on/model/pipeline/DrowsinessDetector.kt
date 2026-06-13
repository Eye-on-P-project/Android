package ac.sbmax002.eye_on.model.pipeline

import ac.sbmax002.eye_on.DTO.DrowsinessState

class DrowsinessDetector(
    private val baseDrowsyEarThreshold: Float = 0.12f,
    private val baseSleepingEarThreshold: Float = 0.07f,
    private val drowsyRatio: Float = 0.70f,
    private val sleepingRatio: Float = 0.40f,
    private var drowsyDurationMs: Long = 1_500L,
    private val sleepingDurationMs: Long = 2_500L,
    private val recoverDurationMs: Long = 1_000L,
    private val baselineSmoothing: Float = 0.01f,
    private val warmupDurationMs: Long = 2_000L,
    private val warmupBaselineSmoothing: Float = 0.1f
) {
    private var baselineEar: Float = 0f
    private var lastTimestampMs: Long? = null
    private var warmupElapsedMs: Long = 0L

    private var drowsyDurationMsAccumulated: Long = 0L
    private var sleepingDurationMsAccumulated: Long = 0L
    private var openDurationMs: Long = 0L
    private var state: DrowsinessState = DrowsinessState.NORMAL

    fun update(leftEar: Float?, rightEar: Float?, frameTimestampMs: Long): DrowsinessState {
        val avgEar = averageEar(leftEar, rightEar)
        val previousTimestampMs = lastTimestampMs
        val deltaMs = previousTimestampMs
            ?.let { (frameTimestampMs - it).coerceAtLeast(0L) }
            ?: 0L
        lastTimestampMs = frameTimestampMs

        val hasValidEar = avgEar != null && avgEar > 0f
        if (hasValidEar) {
            warmupElapsedMs += deltaMs
        }

        val drowsyNow = areBothEyesBelow(leftEar, rightEar, drowsyThreshold())
        val sleepingNow = areBothEyesBelow(leftEar, rightEar, sleepingThreshold())
        val inWarmup = warmupElapsedMs < warmupDurationMs

        if (hasValidEar && !drowsyNow) {
            updateBaseline(avgEar!!, inWarmup)
        }

        if (inWarmup) {
            resetDurations()
            state = DrowsinessState.NORMAL
            return state
        }

        if (!hasValidEar) return state

        state = when (state) {
            DrowsinessState.NORMAL -> updateFromNormal(deltaMs, drowsyNow, sleepingNow)
            DrowsinessState.DROWSY -> updateFromDrowsy(deltaMs, drowsyNow, sleepingNow)
            DrowsinessState.SLEEPING -> updateFromSleeping(deltaMs, drowsyNow, sleepingNow)
        }

        return state
    }

    fun updateDrowsyDuration(durationMs: Long) {
        drowsyDurationMs = durationMs.coerceAtLeast(0L)
    }

    fun isEyeClosed(ear: Float?): Boolean {
        return ear != null && ear > 0f && ear < drowsyThreshold()
    }

    fun isEyeSleeping(ear: Float?): Boolean {
        return ear != null && ear > 0f && ear < sleepingThreshold()
    }

    fun currentState(): DrowsinessState = state

    fun reset() {
        baselineEar = 0f
        lastTimestampMs = null
        warmupElapsedMs = 0L
        resetDurations()
        state = DrowsinessState.NORMAL
    }

    private fun updateBaseline(avgEar: Float, inWarmup: Boolean) {
        baselineEar = if (baselineEar == 0f) {
            avgEar
        } else {
            val alpha = if (inWarmup) warmupBaselineSmoothing else baselineSmoothing
            baselineEar * (1f - alpha) + avgEar * alpha
        }
    }

    private fun averageEar(leftEar: Float?, rightEar: Float?): Float? {
        val ears = listOfNotNull(leftEar, rightEar).filter { it > 0f }
        if (ears.isEmpty()) return null
        return ears.average().toFloat()
    }

    private fun areBothEyesBelow(leftEar: Float?, rightEar: Float?, threshold: Float): Boolean {
        return leftEar != null && rightEar != null &&
            leftEar > 0f && rightEar > 0f &&
            leftEar < threshold && rightEar < threshold
    }

    private fun updateFromNormal(
        deltaMs: Long,
        drowsyNow: Boolean,
        sleepingNow: Boolean
    ): DrowsinessState {
        return when {
            sleepingNow -> {
                sleepingDurationMsAccumulated += deltaMs
                drowsyDurationMsAccumulated = 0L
                openDurationMs = 0L
                if (sleepingDurationMsAccumulated >= sleepingDurationMs) {
                    DrowsinessState.SLEEPING
                } else {
                    DrowsinessState.NORMAL
                }
            }
            drowsyNow -> {
                drowsyDurationMsAccumulated += deltaMs
                sleepingDurationMsAccumulated = 0L
                openDurationMs = 0L
                if (drowsyDurationMsAccumulated >= drowsyDurationMs) {
                    DrowsinessState.DROWSY
                } else {
                    DrowsinessState.NORMAL
                }
            }
            else -> {
                resetDurations()
                DrowsinessState.NORMAL
            }
        }
    }

    private fun updateFromDrowsy(
        deltaMs: Long,
        drowsyNow: Boolean,
        sleepingNow: Boolean
    ): DrowsinessState {
        return when {
            sleepingNow -> {
                sleepingDurationMsAccumulated += deltaMs
                drowsyDurationMsAccumulated = 0L
                openDurationMs = 0L
                if (sleepingDurationMsAccumulated >= sleepingDurationMs) {
                    DrowsinessState.SLEEPING
                } else {
                    DrowsinessState.DROWSY
                }
            }
            drowsyNow -> {
                drowsyDurationMsAccumulated += deltaMs
                sleepingDurationMsAccumulated = 0L
                openDurationMs = 0L
                DrowsinessState.DROWSY
            }
            else -> {
                openDurationMs += deltaMs
                drowsyDurationMsAccumulated = 0L
                sleepingDurationMsAccumulated = 0L
                if (openDurationMs >= recoverDurationMs) {
                    DrowsinessState.NORMAL
                } else {
                    DrowsinessState.DROWSY
                }
            }
        }
    }

    private fun updateFromSleeping(
        deltaMs: Long,
        drowsyNow: Boolean,
        sleepingNow: Boolean
    ): DrowsinessState {
        return when {
            !drowsyNow -> {
                openDurationMs += deltaMs
                drowsyDurationMsAccumulated = 0L
                sleepingDurationMsAccumulated = 0L
                if (openDurationMs >= recoverDurationMs) {
                    DrowsinessState.NORMAL
                } else {
                    DrowsinessState.SLEEPING
                }
            }
            sleepingNow -> {
                openDurationMs = 0L
                DrowsinessState.SLEEPING
            }
            else -> {
                openDurationMs = 0L
                DrowsinessState.SLEEPING
            }
        }
    }

    private fun drowsyThreshold(): Float {
        return if (baselineEar > 0f) baselineEar * drowsyRatio else baseDrowsyEarThreshold
    }

    private fun sleepingThreshold(): Float {
        return if (baselineEar > 0f) baselineEar * sleepingRatio else baseSleepingEarThreshold
    }

    private fun resetDurations() {
        drowsyDurationMsAccumulated = 0L
        sleepingDurationMsAccumulated = 0L
        openDurationMs = 0L
    }
}
