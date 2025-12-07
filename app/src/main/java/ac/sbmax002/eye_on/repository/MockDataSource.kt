package ac.sbmax002.eye_on.repository

import ac.sbmax002.eye_on.model.statistics.DrivingSession
import ac.sbmax002.eye_on.model.statistics.SessionEvent
import ac.sbmax002.eye_on.ui.home.AppMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 더미 데이터를 생성하여 제공하는 전용 객체
 */
object MockDataSource {

    fun generateDrivingSessions(): List<DrivingSession> {
        val sessions = mutableListOf<DrivingSession>()
        val today = LocalDate.now()
        val drivingLocations = listOf("Gangnam", "Seongnam", "Busan", "Incheon", "Seoul Station", "Highway", "Home", "Office")

        // 1. [주간] 최근 0~6일 전 데이터
        repeat(15) { i ->
            sessions.add(createRandomSession(0, i, today, 0, 6, drivingLocations))
        }

        // 2. [월간] 최근 7~30일 전 데이터
        repeat(15) { i ->
            sessions.add(createRandomSession(100, i, today, 7, 30, drivingLocations))
        }

        // 3. [전체] 최근 31~180일 전 데이터
        repeat(15) { i ->
            sessions.add(createRandomSession(200, i, today, 31, 180, drivingLocations))
        }

        return sessions
    }

    private fun createRandomSession(
        idStart: Int,
        index: Int,
        baseDate: LocalDate,
        minDays: Int,
        maxDays: Int,
        locations: List<String>
    ): DrivingSession {
        val daysAgo = Random.nextLong(minDays.toLong(), maxDays.toLong() + 1)
        val date = baseDate.minusDays(daysAgo)

        val hour = Random.nextInt(0, 23)
        val minute = Random.nextInt(0, 59)
        val startTime = LocalTime.of(hour, minute)
        val dateTime = LocalDateTime.of(date, startTime)

        val durationMin = Random.nextInt(10, 300)

        val lvl1 = Random.nextInt(0, 8)
        val lvl2 = Random.nextInt(0, 3)

        // 랜덤하게 모드 결정 (운전 vs 스터디)
        val randomMode = if (Random.nextBoolean()) AppMode.DRIVING else AppMode.STUDY

        // 모드에 따라 장소 텍스트 변경
        val finalLocation = if (randomMode == AppMode.STUDY) {
            listOf("Library", "Cafe", "Home", "Study Room", "School").random()
        } else {
            locations.random()
        }

        // 이벤트 생성
        val events = mutableListOf<SessionEvent>()
        repeat(lvl1) {
            events.add(createRandomEvent(startTime, durationMin, 1, randomMode))
        }
        repeat(lvl2) {
            events.add(createRandomEvent(startTime, durationMin, 2, randomMode))
        }
        events.sortBy { it.time }

        return DrivingSession(
            id = "${idStart + index}",
            dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            time = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            location = finalLocation,
            durationMinutes = durationMin,
            durationStr = formatDuration(durationMin),
            level1Alerts = lvl1,
            level2Alerts = lvl2,
            rawDateTime = dateTime,
            events = events,
            mode = randomMode
        )
    }

    private fun createRandomEvent(startTime: LocalTime, maxDurationMin: Int, level: Int, mode: AppMode): SessionEvent {
        val eventTime = startTime.plusMinutes(Random.nextLong(1, maxDurationMin.toLong()))
        val durationSec = Random.nextInt(2, 10)

        val message = if (mode == AppMode.DRIVING) {
            if (level == 1) "Drowsiness detected" else "Sleep warning"
        } else {
            if (level == 1) "Distraction detected" else "Away from seat"
        }

        return SessionEvent(
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