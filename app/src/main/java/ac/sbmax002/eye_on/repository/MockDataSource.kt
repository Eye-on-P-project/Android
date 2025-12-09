package ac.sbmax002.eye_on.repository

import ac.sbmax002.eye_on.model.statistics.DrivingSession
import ac.sbmax002.eye_on.model.statistics.SessionEvent
import ac.sbmax002.eye_on.ui.home.AppMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object MockDataSource {

    // ★ 반환 타입을 Pair로 변경 (세션 리스트, 이벤트 리스트)
    // DB 초기 데이터 적재용으로 사용할 수 있게 구조 변경
    fun generateData(): Pair<List<DrivingSession>, List<SessionEvent>> {
        val sessions = mutableListOf<DrivingSession>()
        val allEvents = mutableListOf<SessionEvent>()

        val today = LocalDate.now()
        val drivingLocations = listOf("Gangnam", "Seongnam", "Busan", "Incheon", "Seoul Station", "Highway", "Home", "Office")

        var idCounter = 0

        // 데이터 생성 도우미 함수
        fun addSessions(count: Int, minDays: Int, maxDays: Int) {
            repeat(count) {
                val sessionId = idCounter.toString()
                val (session, events) = createRandomSession(sessionId, today, minDays, maxDays, drivingLocations)
                sessions.add(session)
                allEvents.addAll(events)
                idCounter++
            }
        }

        // 1. [주간]
        addSessions(15, 0, 6)
        // 2. [월간]
        addSessions(15, 7, 30)
        // 3. [전체]
        addSessions(15, 31, 180)

        return Pair(sessions, allEvents)
    }

    private fun createRandomSession(
        id: String,
        baseDate: LocalDate,
        minDays: Int,
        maxDays: Int,
        locations: List<String>
    ): Pair<DrivingSession, List<SessionEvent>> { // 세션과 이벤트를 분리해서 반환
        val daysAgo = Random.nextLong(minDays.toLong(), maxDays.toLong() + 1)
        val date = baseDate.minusDays(daysAgo)

        val hour = Random.nextInt(0, 23)
        val minute = Random.nextInt(0, 59)
        val startTime = LocalTime.of(hour, minute)
        val dateTime = LocalDateTime.of(date, startTime)

        val durationMin = Random.nextInt(10, 300)

        val lvl1 = Random.nextInt(0, 8)
        val lvl2 = Random.nextInt(0, 3)

        val randomMode = if (Random.nextBoolean()) AppMode.DRIVING else AppMode.STUDY

        val finalLocation = if (randomMode == AppMode.STUDY) {
            listOf("Library", "Cafe", "Home", "Study Room", "School").random()
        } else {
            locations.random()
        }

        // 세션 생성 (events 필드 제거됨)
        val session = DrivingSession(
            id = id,
            dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            time = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            location = finalLocation,
            durationMinutes = durationMin,
            durationStr = formatDuration(durationMin),
            level1Alerts = lvl1,
            level2Alerts = lvl2,
            rawDateTime = dateTime,
            mode = randomMode
        )

        // 이벤트 생성 (sessionId 추가)
        val events = mutableListOf<SessionEvent>()
        repeat(lvl1) {
            events.add(createRandomEvent(id, startTime, durationMin, 1, randomMode))
        }
        repeat(lvl2) {
            events.add(createRandomEvent(id, startTime, durationMin, 2, randomMode))
        }
        events.sortBy { it.time }

        return Pair(session, events)
    }

    private fun createRandomEvent(sessionId: String, startTime: LocalTime, maxDurationMin: Int, level: Int, mode: AppMode): SessionEvent {
        val eventTime = startTime.plusMinutes(Random.nextLong(1, maxDurationMin.toLong()))
        val durationSec = Random.nextInt(2, 10)

        val message = if (mode == AppMode.DRIVING) {
            if (level == 1) "Drowsiness detected" else "Sleep warning"
        } else {
            if (level == 1) "Distraction detected" else "Away from seat"
        }

        return SessionEvent(
            sessionId = sessionId, // ★ 필수 필드 추가
            time = eventTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            message = message,
            duration = "${durationSec}s",
            level = level
        )
    }

    private fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}