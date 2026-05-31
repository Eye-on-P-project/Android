package ac.sbmax002.eye_on.model.pipeline

import ac.sbmax002.eye_on.DTO.DrowsinessState
import ac.sbmax002.eye_on.model.inference.SleepModelRunner
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class TemporalDrowsinessDetector(
    private val modelRunner: SleepModelRunner,
    private val config: TemporalDetectionConfig = TemporalDetectionConfig()
) {
    private data class RawFrame(
        val timestampMs: Long,
        val meanEar: Float,
        val pClosed: Float,
        val mar: Float,
        val pitch: Float,
        val yaw: Float,
        val roll: Float
    )

    private data class NormalStats(
        val mean: FloatArray,
        val std: FloatArray
    )

    private data class DrowsinessProbabilities(
        val normal: Float,
        val drowsy: Float,
        val sleepy: Float
    )

    private data class BlinkEvent(
        val endTimeMs: Long,
        val durationSec: Float,
        val amplitude: Float,
        val openingVelocity: Float
    )

    private data class FeatureDebug(
        val pClosed: Float,
        val pClosedSmooth: Float,
        val currentClosedDuration: Float,
        val perclos: Float,
        val earZ: Float,
        val marZ: Float,
        val pitchZ: Float,
        val yawZ: Float,
        val rollZ: Float,
        val blinkRate: Float,
        val timeSinceLastBlink: Float,
        val sequenceSize: Int,
        val calibrated: Boolean
    )

    private data class FeatureRow(
        val values: FloatArray,
        val debug: FeatureDebug
    )

    data class DetectionOutput(
        val state: DrowsinessState,
        val leftEar: Float?,
        val rightEar: Float?,
        val leftClosed: Boolean,
        val rightClosed: Boolean
    )

    private val calibrationFrames = ArrayDeque<RawFrame>()
    private val rawFrames = ArrayDeque<RawFrame>()
    private val featureRows = ArrayDeque<FloatArray>()
    private val blinkEvents = ArrayDeque<BlinkEvent>()
    private val probabilityHistory = ArrayDeque<DrowsinessProbabilities>()

    private var calibration: NormalStats? = null
    private var lastAcceptedFrameMs: Long? = null
    private var lastGruInferenceMs: Long = 0L
    private var closedStartedAtMs: Long? = null
    private var blinkStartedAtMs: Long? = null
    private var blinkMinEarZ = 0f
    private var blinkStartEarZ = 0f
    private var drowsyStreak = 0
    private var sleepyStreak = 0
    private var state = DrowsinessState.NORMAL
    private var lastDebugLogMs: Long = 0L
    private var lastFpsSampleMs: Long = 0L
    private var inputFramesInSample = 0
    private var processedFramesInSample = 0
    private var inputFps = 0f
    private var processedFps = 0f

    fun update(
        bitmap: Bitmap?,
        landmarks: List<NormalizedLandmark>,
        timestampMs: Long,
        faceLandmarkerMs: Long
    ): DetectionOutput {
        val totalStartedAt = SystemClock.elapsedRealtime()
        val previousAccepted = lastAcceptedFrameMs
        val acceptedForModel = previousAccepted == null ||
            timestampMs - previousAccepted >= config.frameIntervalMs
        updateFpsSample(nowMs = totalStartedAt, acceptedForModel = acceptedForModel)
        if (!acceptedForModel) {
            return DetectionOutput(
                state = state,
                leftEar = null,
                rightEar = null,
                leftClosed = false,
                rightClosed = false
            )
        }
        lastAcceptedFrameMs = timestampMs

        val leftEar = calculateEar(landmarks, LEFT_EYE_EAR_IDX, bitmap?.width ?: 1, bitmap?.height ?: 1)
        val rightEar = calculateEar(landmarks, RIGHT_EYE_EAR_IDX, bitmap?.width ?: 1, bitmap?.height ?: 1)
        val meanEar = listOfNotNull(leftEar, rightEar).takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f
        val mar = calculateMar(landmarks, bitmap?.width ?: 1, bitmap?.height ?: 1)
        val headPose = estimateHeadPose(landmarks, bitmap?.width ?: 1, bitmap?.height ?: 1)

        val eyeStartedAt = SystemClock.elapsedRealtime()
        val pClosed = predictPClosed(bitmap, landmarks)
        val eyeInferenceMs = SystemClock.elapsedRealtime() - eyeStartedAt

        val raw = RawFrame(
            timestampMs = timestampMs,
            meanEar = meanEar,
            pClosed = pClosed,
            mar = mar,
            pitch = headPose[0],
            yaw = headPose[1],
            roll = headPose[2]
        )

        if (calibration == null) {
            calibrationFrames.addLast(raw)
            if (calibrationFrames.size >= config.calibrationFrameCount) {
                calibration = computeCalibration(calibrationFrames.toList())
                resetSequenceAfterCalibration()
            }
        }

        rawFrames.addLast(raw)
        trimRawFrames(timestampMs)

        val featureStartedAt = SystemClock.elapsedRealtime()
        val row = buildFeatureRow(raw)
        val featureBuildMs = SystemClock.elapsedRealtime() - featureStartedAt
        featureRows.addLast(row.values)
        while (featureRows.size > config.sequenceLength) featureRows.removeFirst()

        val isWarmingUp = calibration == null || featureRows.size < config.sequenceLength
        var probabilities = averageProbabilities()
        var gruInferenceMs = 0L

        if (!isWarmingUp && timestampMs - lastGruInferenceMs >= config.gruIntervalMs) {
            val flattened = FloatArray(featureRows.size * SleepModelRunner.FEATURE_COUNT)
            var offset = 0
            featureRows.forEach { rowValues ->
                rowValues.copyInto(flattened, destinationOffset = offset)
                offset += SleepModelRunner.FEATURE_COUNT
            }
            val gruStartedAt = SystemClock.elapsedRealtime()
            val prediction = modelRunner.runSleepGru(flattened, featureRows.size)
            gruInferenceMs = SystemClock.elapsedRealtime() - gruStartedAt
            val nextProbabilities = DrowsinessProbabilities(
                normal = prediction.getOrElse(0) { 0f },
                drowsy = prediction.getOrElse(1) { 0f },
                sleepy = prediction.getOrElse(2) { 0f }
            )
            probabilityHistory.addLast(nextProbabilities)
            while (probabilityHistory.size > config.resultSmoothingWindow) {
                probabilityHistory.removeFirst()
            }
            probabilities = averageProbabilities()
            state = classify(probabilities)
            lastGruInferenceMs = timestampMs
        }

        logDebugIfNeeded(
            timestampMs = timestampMs,
            debug = row.debug.copy(sequenceSize = featureRows.size),
            probabilities = probabilities,
            faceLandmarkerMs = faceLandmarkerMs,
            eyeInferenceMs = eyeInferenceMs,
            featureBuildMs = featureBuildMs,
            gruInferenceMs = gruInferenceMs,
            totalFrameMs = SystemClock.elapsedRealtime() - totalStartedAt
        )

        val closed = pClosed >= P_CLOSED_START_THRESHOLD
        return DetectionOutput(
            state = state,
            leftEar = leftEar,
            rightEar = rightEar,
            leftClosed = closed,
            rightClosed = closed
        )
    }

    fun reset() {
        calibrationFrames.clear()
        rawFrames.clear()
        featureRows.clear()
        blinkEvents.clear()
        probabilityHistory.clear()
        calibration = null
        lastAcceptedFrameMs = null
        lastGruInferenceMs = 0L
        closedStartedAtMs = null
        blinkStartedAtMs = null
        lastDebugLogMs = 0L
        lastFpsSampleMs = 0L
        inputFramesInSample = 0
        processedFramesInSample = 0
        inputFps = 0f
        processedFps = 0f
        drowsyStreak = 0
        sleepyStreak = 0
        state = DrowsinessState.NORMAL
    }

    fun currentState(): DrowsinessState = state

    private fun resetSequenceAfterCalibration() {
        rawFrames.clear()
        featureRows.clear()
        blinkEvents.clear()
        probabilityHistory.clear()
        closedStartedAtMs = null
        blinkStartedAtMs = null
        lastGruInferenceMs = 0L
        drowsyStreak = 0
        sleepyStreak = 0
        state = DrowsinessState.NORMAL
    }

    private fun predictPClosed(
        bitmap: Bitmap?,
        landmarks: List<NormalizedLandmark>
    ): Float {
        if (bitmap == null) return 0f
        val eyeRects = listOfNotNull(
            eyeCropRect(bitmap, landmarks, LEFT_EYE_CROP_IDX),
            eyeCropRect(bitmap, landmarks, RIGHT_EYE_CROP_IDX)
        )
        if (eyeRects.isEmpty()) return 0f

        val probabilities = modelRunner.runEyeModelBatch(bitmap, eyeRects).map {
            it.getOrElse(1) { 0f }
        }
        return probabilities.average().toFloat().coerceIn(0f, 1f)
    }

    private fun buildFeatureRow(raw: RawFrame): FeatureRow {
        val stats = calibration
        val z = if (stats != null) {
            floatArrayOf(
                zScore(raw.meanEar, stats.mean[0], stats.std[0]),
                zScore(raw.mar, stats.mean[1], stats.std[1]),
                zScore(raw.pitch, stats.mean[2], stats.std[2]),
                zScore(raw.yaw, stats.mean[3], stats.std[3]),
                zScore(raw.roll, stats.mean[4], stats.std[4])
            )
        } else {
            FloatArray(5)
        }

        val pClosedSmooth = rawFrames.takeLast(P_CLOSED_SMOOTH_WINDOW)
            .map { it.pClosed }
            .average()
            .toFloat()
            .coerceIn(0f, 1f)
        val currentClosedDuration = updateClosedState(raw.timestampMs, pClosedSmooth)
        updateBlinkEvents(raw.timestampMs, pClosedSmooth, z[0])
        trimBlinkEvents(raw.timestampMs)

        val rollingFrameCount = rawFrames.count {
            raw.timestampMs - it.timestampMs <= config.rollingWindowSeconds * 1_000L
        }.coerceAtLeast(1)
        val closedFrameCount = rawFrames.count {
            raw.timestampMs - it.timestampMs <= config.rollingWindowSeconds * 1_000L &&
                it.pClosed >= P_CLOSED_START_THRESHOLD
        }
        val perclos = closedFrameCount.toFloat() / rollingFrameCount.toFloat()
        val rollingEvents = blinkEvents.filter {
            raw.timestampMs - it.endTimeMs <= config.rollingWindowSeconds * 1_000L
        }
        val timeSinceLastBlink = blinkEvents.lastOrNull()
            ?.let { ((raw.timestampMs - it.endTimeMs) / 1_000f).coerceAtMost(config.timeSinceLastBlinkCapSeconds) }
            ?: config.timeSinceLastBlinkCapSeconds

        val values = floatArrayOf(
            z[0],
            raw.pClosed,
            z[1],
            z[2],
            z[3],
            z[4],
            currentClosedDuration,
            perclos,
            rollingEvents.map { it.durationSec }.averageOrZero(),
            rollingEvents.map { it.amplitude }.averageOrZero(),
            rollingEvents.map { it.openingVelocity }.averageOrZero(),
            rollingEvents.size.toFloat() / config.rollingWindowSeconds.toFloat(),
            timeSinceLastBlink
        )
        return FeatureRow(
            values = values,
            debug = FeatureDebug(
                pClosed = raw.pClosed,
                pClosedSmooth = pClosedSmooth,
                currentClosedDuration = currentClosedDuration,
                perclos = perclos,
                earZ = z[0],
                marZ = z[1],
                pitchZ = z[2],
                yawZ = z[3],
                rollZ = z[4],
                blinkRate = rollingEvents.size.toFloat() / config.rollingWindowSeconds.toFloat(),
                timeSinceLastBlink = timeSinceLastBlink,
                sequenceSize = featureRows.size + 1,
                calibrated = calibration != null
            )
        )
    }

    private fun updateClosedState(timestampMs: Long, pClosedSmooth: Float): Float {
        if (closedStartedAtMs == null && pClosedSmooth >= P_CLOSED_START_THRESHOLD) {
            closedStartedAtMs = timestampMs
        } else if (closedStartedAtMs != null && pClosedSmooth <= P_CLOSED_END_THRESHOLD) {
            closedStartedAtMs = null
        }
        return closedStartedAtMs?.let { (timestampMs - it).coerceAtLeast(0L) / 1_000f } ?: 0f
    }

    private fun updateBlinkEvents(timestampMs: Long, pClosedSmooth: Float, earZ: Float) {
        if (blinkStartedAtMs == null && pClosedSmooth >= P_CLOSED_START_THRESHOLD) {
            blinkStartedAtMs = timestampMs
            blinkStartEarZ = earZ
            blinkMinEarZ = earZ
            return
        }

        val startedAt = blinkStartedAtMs ?: return
        blinkMinEarZ = min(blinkMinEarZ, earZ)
        if (pClosedSmooth > P_CLOSED_END_THRESHOLD) return

        val durationSec = (timestampMs - startedAt) / 1_000f
        if (durationSec in MIN_BLINK_DURATION_SEC..MAX_BLINK_DURATION_SEC) {
            val amplitude = ((blinkStartEarZ + earZ) / 2f) - blinkMinEarZ
            if (amplitude >= MIN_BLINK_AMPLITUDE_Z) {
                val openingVelocity = (earZ - blinkMinEarZ) / max(durationSec, 1e-6f)
                blinkEvents.addLast(
                    BlinkEvent(
                        endTimeMs = timestampMs,
                        durationSec = durationSec,
                        amplitude = amplitude,
                        openingVelocity = openingVelocity
                    )
                )
            }
        }
        blinkStartedAtMs = null
    }

    private fun logDebugIfNeeded(
        timestampMs: Long,
        debug: FeatureDebug,
        probabilities: DrowsinessProbabilities?,
        faceLandmarkerMs: Long,
        eyeInferenceMs: Long,
        featureBuildMs: Long,
        gruInferenceMs: Long,
        totalFrameMs: Long
    ) {
        if (timestampMs - lastDebugLogMs < DEBUG_LOG_INTERVAL_MS) return
        lastDebugLogMs = timestampMs

        val probabilityText = if (probabilities != null) {
            "prob(n=${probabilities.normal.fmt3()}, d=${probabilities.drowsy.fmt3()}, s=${probabilities.sleepy.fmt3()})"
        } else {
            "prob=pending"
        }

        Log.d(
            TAG,
            "state=$state, warmup=${!debug.calibrated || debug.sequenceSize < config.sequenceLength}, " +
                "$probabilityText, " +
                "fps(in=${inputFps.fmt1()}, proc=${processedFps.fmt1()}), " +
                "ms(face=$faceLandmarkerMs, eye=$eyeInferenceMs, feature=$featureBuildMs, " +
                "gru=$gruInferenceMs, total=$totalFrameMs), " +
                "feat(pClosed=${debug.pClosed.fmt3()}, smooth=${debug.pClosedSmooth.fmt3()}, " +
                "closedSec=${debug.currentClosedDuration.fmt2()}, perclos=${debug.perclos.fmt3()}, " +
                "earZ=${debug.earZ.fmt2()}, marZ=${debug.marZ.fmt2()}, " +
                "headZ=${debug.pitchZ.fmt2()}/${debug.yawZ.fmt2()}/${debug.rollZ.fmt2()}, " +
                "blinkRate=${debug.blinkRate.fmt2()}, sinceBlink=${debug.timeSinceLastBlink.fmt2()}, " +
                "seq=${debug.sequenceSize}, cal=${debug.calibrated})"
        )
    }

    private fun updateFpsSample(nowMs: Long, acceptedForModel: Boolean) {
        inputFramesInSample++
        if (acceptedForModel) processedFramesInSample++
        if (lastFpsSampleMs == 0L) {
            lastFpsSampleMs = nowMs
            return
        }

        val elapsedMs = nowMs - lastFpsSampleMs
        if (elapsedMs < FPS_SAMPLE_INTERVAL_MS) return

        inputFps = inputFramesInSample * 1_000f / elapsedMs
        processedFps = processedFramesInSample * 1_000f / elapsedMs
        inputFramesInSample = 0
        processedFramesInSample = 0
        lastFpsSampleMs = nowMs
    }

    private fun classify(probabilities: DrowsinessProbabilities?): DrowsinessState {
        if (probabilities == null) return state

        val sleepyCandidate = probabilities.sleepy >= config.sleepyThreshold
        val drowsyCandidate = probabilities.drowsy >= config.drowsyThreshold

        sleepyStreak = if (sleepyCandidate) sleepyStreak + 1 else 0
        drowsyStreak = if (drowsyCandidate) drowsyStreak + 1 else 0

        return when {
            sleepyStreak >= config.sleepyConsecutiveCount -> DrowsinessState.SLEEPING
            drowsyStreak >= config.drowsyConsecutiveCount -> DrowsinessState.DROWSY
            probabilities.normal >= probabilities.drowsy && probabilities.normal >= probabilities.sleepy -> {
                drowsyStreak = 0
                sleepyStreak = 0
                DrowsinessState.NORMAL
            }
            else -> state
        }
    }

    private fun averageProbabilities(): DrowsinessProbabilities? {
        if (probabilityHistory.isEmpty()) return null
        return DrowsinessProbabilities(
            normal = probabilityHistory.map { it.normal }.averageOrZero(),
            drowsy = probabilityHistory.map { it.drowsy }.averageOrZero(),
            sleepy = probabilityHistory.map { it.sleepy }.averageOrZero()
        )
    }

    private fun computeCalibration(frames: List<RawFrame>): NormalStats {
        val columns = listOf(
            frames.map { it.meanEar },
            frames.map { it.mar },
            frames.map { it.pitch },
            frames.map { it.yaw },
            frames.map { it.roll }
        )
        val mean = FloatArray(columns.size)
        val std = FloatArray(columns.size)
        columns.forEachIndexed { index, values ->
            val avg = values.averageOrZero()
            mean[index] = avg
            val variance = values.map { (it - avg) * (it - avg) }.averageOrZero()
            std[index] = kotlin.math.sqrt(variance.toDouble()).toFloat().takeIf { it > 1e-6f } ?: 1f
        }
        return NormalStats(mean, std)
    }

    private fun eyeCropRect(
        bitmap: Bitmap,
        landmarks: List<NormalizedLandmark>,
        indices: List<Int>
    ): Rect? {
        if (landmarks.size <= indices.maxOrNull().orZero()) return null
        val xs = indices.map { landmarks[it].x() * bitmap.width }
        val ys = indices.map { landmarks[it].y() * bitmap.height }
        val minX = xs.minOrNull() ?: return null
        val maxX = xs.maxOrNull() ?: return null
        val minY = ys.minOrNull() ?: return null
        val maxY = ys.maxOrNull() ?: return null
        val side = max(maxX - minX, maxY - minY) * (1f + EYE_CROP_PADDING)
        if (side <= 2f) return null
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val rect = Rect(
            (centerX - side / 2f).toInt().coerceIn(0, bitmap.width - 1),
            (centerY - side / 2f).toInt().coerceIn(0, bitmap.height - 1),
            (centerX + side / 2f).toInt().coerceIn(1, bitmap.width),
            (centerY + side / 2f).toInt().coerceIn(1, bitmap.height)
        )
        if (rect.width() <= 1 || rect.height() <= 1) return null
        return rect
    }

    private fun calculateEar(
        landmarks: List<NormalizedLandmark>,
        indices: List<Int>,
        width: Int,
        height: Int
    ): Float? {
        if (landmarks.size <= indices.maxOrNull().orZero()) return null
        fun point(index: Int): Pair<Float, Float> {
            val landmark = landmarks[index]
            return landmark.x() * width to landmark.y() * height
        }
        fun distance(a: Pair<Float, Float>, b: Pair<Float, Float>): Float =
            hypot((a.first - b.first).toDouble(), (a.second - b.second).toDouble()).toFloat()

        val p = indices.map(::point)
        val horizontal = distance(p[0], p[3])
        if (horizontal <= 1e-6f) return null
        return (distance(p[1], p[5]) + distance(p[2], p[4])) / (2f * horizontal)
    }

    private fun calculateMar(
        landmarks: List<NormalizedLandmark>,
        width: Int,
        height: Int
    ): Float {
        if (landmarks.size <= MOUTH_RIGHT) return 0f
        fun point(index: Int): Pair<Float, Float> {
            val landmark = landmarks[index]
            return landmark.x() * width to landmark.y() * height
        }
        fun distance(a: Pair<Float, Float>, b: Pair<Float, Float>): Float =
            hypot((a.first - b.first).toDouble(), (a.second - b.second).toDouble()).toFloat()

        val horizontal = distance(point(MOUTH_LEFT), point(MOUTH_RIGHT))
        if (horizontal <= 1e-6f) return 0f
        return distance(point(MOUTH_TOP), point(MOUTH_BOTTOM)) / horizontal
    }

    private fun estimateHeadPose(
        landmarks: List<NormalizedLandmark>,
        width: Int,
        height: Int
    ): FloatArray {
        if (landmarks.size <= HEAD_CHIN) return FloatArray(3)
        fun point(index: Int): Pair<Float, Float> {
            val landmark = landmarks[index]
            return landmark.x() * width to landmark.y() * height
        }

        val leftEye = point(HEAD_LEFT_EYE_OUTER)
        val rightEye = point(HEAD_RIGHT_EYE_OUTER)
        val nose = point(HEAD_NOSE)
        val chin = point(HEAD_CHIN)
        val leftMouth = point(HEAD_LEFT_MOUTH)
        val rightMouth = point(HEAD_RIGHT_MOUTH)

        val faceWidth = max(abs(rightEye.first - leftEye.first), 1f)
        val faceHeight = max(abs(chin.second - ((leftEye.second + rightEye.second) / 2f)), 1f)
        val eyeCenterX = (leftEye.first + rightEye.first) / 2f
        val mouthCenterY = (leftMouth.second + rightMouth.second) / 2f
        val roll = Math.toDegrees(atan2((rightEye.second - leftEye.second).toDouble(), (rightEye.first - leftEye.first).toDouble())).toFloat()
        val yaw = ((nose.first - eyeCenterX) / faceWidth) * 45f
        val pitch = ((nose.second - mouthCenterY) / faceHeight) * 45f
        return floatArrayOf(pitch, yaw, roll)
    }

    private fun trimRawFrames(timestampMs: Long) {
        val maxAgeMs = max(config.sequenceSeconds, config.rollingWindowSeconds) * 1_000L
        while (rawFrames.isNotEmpty() && timestampMs - rawFrames.first().timestampMs > maxAgeMs) {
            rawFrames.removeFirst()
        }
    }

    private fun trimBlinkEvents(timestampMs: Long) {
        val maxAgeMs = config.rollingWindowSeconds * 1_000L
        while (blinkEvents.isNotEmpty() && timestampMs - blinkEvents.first().endTimeMs > maxAgeMs) {
            blinkEvents.removeFirst()
        }
    }

    private fun zScore(value: Float, mean: Float, std: Float): Float =
        (value - mean) / std.takeIf { it > 1e-6f }.orOne()

    private fun List<Float>.averageOrZero(): Float =
        if (isEmpty()) 0f else average().toFloat()

    private fun Int?.orZero(): Int = this ?: 0
    private fun Float?.orOne(): Float = this ?: 1f
    private fun Float.fmt1(): String = "%.1f".format(this)
    private fun Float.fmt2(): String = "%.2f".format(this)
    private fun Float.fmt3(): String = "%.3f".format(this)

    companion object {
        private const val TAG = "TemporalDrowsinessDetector"
        private const val DEBUG_LOG_INTERVAL_MS = 1_000L
        private const val FPS_SAMPLE_INTERVAL_MS = 1_000L

        private val LEFT_EYE_EAR_IDX = listOf(33, 160, 158, 133, 153, 144)
        private val RIGHT_EYE_EAR_IDX = listOf(362, 385, 387, 263, 373, 380)
        private val LEFT_EYE_CROP_IDX = listOf(33, 133, 160, 158, 153, 144, 159, 145)
        private val RIGHT_EYE_CROP_IDX = listOf(362, 263, 385, 387, 373, 380, 386, 374)

        private const val MOUTH_LEFT = 61
        private const val MOUTH_RIGHT = 291
        private const val MOUTH_TOP = 13
        private const val MOUTH_BOTTOM = 14

        private const val HEAD_NOSE = 1
        private const val HEAD_CHIN = 152
        private const val HEAD_LEFT_EYE_OUTER = 33
        private const val HEAD_RIGHT_EYE_OUTER = 263
        private const val HEAD_LEFT_MOUTH = 61
        private const val HEAD_RIGHT_MOUTH = 291

        private const val P_CLOSED_SMOOTH_WINDOW = 5
        private const val P_CLOSED_START_THRESHOLD = 0.70f
        private const val P_CLOSED_END_THRESHOLD = 0.30f
        private const val MIN_BLINK_DURATION_SEC = 0.05f
        private const val MAX_BLINK_DURATION_SEC = 2.0f
        private const val MIN_BLINK_AMPLITUDE_Z = 0.10f
        private const val EYE_CROP_PADDING = 0.65f
    }
}
