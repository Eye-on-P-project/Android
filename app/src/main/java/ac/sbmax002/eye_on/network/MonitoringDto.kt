package ac.sbmax002.eye_on.network

/**
 * 모니터링 세션 시작 요청
 */
data class MonitoringStartRequest(
    val mode: String, // "DRIVING" | "STUDY" | "ORGANIZATION"
    val startedAtApp: String // yyyy-MM-dd'T'HH:mm:ss
)

/**
 * 모니터링 세션 시작/종료 공통 응답
 */
data class MonitoringSessionResponse(
    val sessionId: Long,
    val userId: Long,
    val mode: String,
    val startedAtApp: String,
    val startedAtServer: String,
    val endedAtApp: String?,
    val endedAtServer: String?,
    val durationMinutes: Int,
    val drowsyCount: Int,
    val sleepCount: Int
)

/**
 * 모니터링 이벤트 기록 요청 (발생 및 종료)
 */
data class MonitoringEventRequest(
    val eventType: String, // "DROWSY" | "SLEEP" | "NORMAL"
    val occurredAtApp: String, // yyyy-MM-dd'T'HH:mm:ss
    val eventId: Long? = null // 이벤트 종료(NORMAL) 시 필수
)

/**
 * 모니터링 이벤트 기록 응답
 */
data class MonitoringEventResponse(
    val eventId: Long,
    val sessionId: Long,
    val eventType: String,
    val occurredAtApp: String,
    val occurredAtServer: String,
    val resolvedAtApp: String?,
    val resolvedAtServer: String?,
    val durationSeconds: Double?,
    val drowsyCount: Int,
    val sleepCount: Int
)

/**
 * 모니터링 세션 종료 요청
 */
data class MonitoringEndRequest(
    val endedAtApp: String // yyyy-MM-dd'T'HH:mm:ss
)
