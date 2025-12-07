package ac.sbmax002.eye_on.model.statistics

import java.time.LocalDateTime
import ac.sbmax002.eye_on.ui.home.AppMode

data class DrivingSession(
    val id: String,
    val dateStr: String,   // 표시용 날짜 "2024-11-25"
    val time: String,      // 시작 시간 "14:30"
    val location: String,  // 위치
    val durationMinutes: Int, // 계산용 분 (통계 합산용)
    val durationStr: String,  // 표시용 "2h 15m" (DetailScreen 등에서 사용)
    val level1Alerts: Int,
    val level2Alerts: Int,
    val rawDateTime: LocalDateTime, // 정렬 및 필터링용
    val events: List<SessionEvent> = emptyList(),
    val mode: AppMode = AppMode.DRIVING
)

