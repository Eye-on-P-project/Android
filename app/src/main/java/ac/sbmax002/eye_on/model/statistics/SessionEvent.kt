package ac.sbmax002.eye_on.model.statistics

data class SessionEvent(
    val time: String,      // 발생 시각 (예: "14:22")
    val message: String,   // 메시지
    val duration: String,  // 지속 시간
    val level: Int         // 1: 주의(Yellow), 2: 경고(Red)
)

