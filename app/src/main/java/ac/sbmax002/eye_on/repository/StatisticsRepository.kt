package ac.sbmax002.eye_on.repository

import ac.sbmax002.eye_on.ui.home.AppMode
import ac.sbmax002.eye_on.database.StatisticsDao
import ac.sbmax002.eye_on.model.statistics.DrivingSession
import ac.sbmax002.eye_on.model.statistics.SessionEvent
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Duration
import java.util.UUID

class StatisticsRepository(private val statisticsDao: StatisticsDao) {

    // 1. 모든 운전 기록 가져오기 (Flow 사용)
    // DB 데이터가 변경되면(새 운전 기록 추가 등) UI도 자동으로 갱신됩니다.
    val allSessions: Flow<List<DrivingSession>> = statisticsDao.getAllSessions()

    // 2. 특정 세션의 이벤트(졸음 기록) 가져오기
    suspend fun getEventsForSession(sessionId: String): List<SessionEvent> {
        return statisticsDao.getEventsForSession(sessionId)
    }

    // 3. 운전 시작: 새 세션 ID 생성 및 DB 저장
    suspend fun startDrivingSession(currentMode: AppMode): String {
        val newSessionId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        // 초기 세션 정보 생성
        val newSession = DrivingSession(
            id = newSessionId,
            dateStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            time = now.format(DateTimeFormatter.ofPattern("HH:mm")),
            location = "Unknown", // 추후 위치 기능 추가 시 수정
            durationMinutes = 0,
            durationStr = "0m",
            level1Alerts = 0,
            level2Alerts = 0,
            rawDateTime = now,
            mode = currentMode
        )

        statisticsDao.insertSession(newSession)
        return newSessionId
    }

    // 4. 졸음 이벤트 발생 시: 이벤트 저장 + 세션 경고 횟수 증가
    suspend fun saveEvent(sessionId: String, message: String, level: Int, duration: String = "") {
        val now = LocalDateTime.now()

        // 이벤트 테이블에 기록
        val event = SessionEvent(
            sessionId = sessionId,
            time = now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            message = message,
            duration = duration,
            level = level
        )
        statisticsDao.insertEvent(event)

        // 세션 테이블의 경고 횟수 업데이트
        val currentSession = statisticsDao.getSessionById(sessionId)
        currentSession?.let { session ->
            val updatedSession = if (level == 2) {
                session.copy(level2Alerts = session.level2Alerts + 1)
            } else {
                session.copy(level1Alerts = session.level1Alerts + 1)
            }
            statisticsDao.updateSession(updatedSession)
        }
    }

    // 5. 운전 종료: 주행 시간 계산 및 저장
    suspend fun endDrivingSession(sessionId: String) {
        val session = statisticsDao.getSessionById(sessionId)
        session?.let {
            val endTime = LocalDateTime.now()
            // 시작 시간과 현재 시간 차이 계산 (분 단위)
            val durationMin = Duration.between(it.rawDateTime, endTime).toMinutes().toInt()

            // "2h 15m" 형식의 문자열 만들기
            val hours = durationMin / 60
            val minutes = durationMin % 60
            val durationStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

            // DB 업데이트
            val finishedSession = it.copy(
                durationMinutes = durationMin,
                durationStr = durationStr
            )
            statisticsDao.updateSession(finishedSession)
        }
    }
}