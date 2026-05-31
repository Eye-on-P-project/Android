package ac.sbmax002.eye_on.model.pipeline

data class TemporalDetectionConfig(
    // Number of frame-level feature rows to build per second.
    // The model was trained with 15 fps, so 15 is the safest default.
    val targetFps: Int = 15,

    // Seconds used at startup to learn the user's normal EAR, MAR, and head-pose baseline.
    // The app assumes the user is in a normal awake state during this window.
    val calibrationSeconds: Int = 8,

    // Length of the GRU input sequence in seconds.
    // With targetFps=15 and sequenceSeconds=10, the model receives 150 rows.
    val sequenceSeconds: Int = 10,

    // Minimum interval between GRU inferences.
    // 1000 ms means the app predicts once per second using the latest 10-second window.
    val gruIntervalMs: Long = 1_000L,

    // Minimum smoothed drowsy probability to count one drowsy candidate prediction.
    val drowsyThreshold: Float = 0.5f,

    // Minimum smoothed sleepy probability to count one sleeping candidate prediction.
    val sleepyThreshold: Float = 0.5f,

    // Number of consecutive drowsy candidate predictions required to enter DROWSY.
    val drowsyConsecutiveCount: Int = 2,

    // Number of consecutive sleepy candidate predictions required to enter SLEEPING.
    val sleepyConsecutiveCount: Int = 1,

    // Number of recent GRU probability outputs averaged before thresholding.
    // The training script used a streaming smoothing window of 3.
    val resultSmoothingWindow: Int = 3,

    // Rolling window used to compute PERCLOS and blink statistics.
    val rollingWindowSeconds: Int = 10,

    // Maximum time-since-last-blink value passed to the model.
    val timeSinceLastBlinkCapSeconds: Float = 10f
) {
    val sequenceLength: Int
        get() = targetFps * sequenceSeconds

    val frameIntervalMs: Long
        get() = 1_000L / targetFps.coerceAtLeast(1)

    val calibrationFrameCount: Int
        get() = targetFps * calibrationSeconds
}
