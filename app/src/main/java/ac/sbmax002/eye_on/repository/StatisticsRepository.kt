package ac.sbmax002.eye_on.repository

import ac.sbmax002.eye_on.model.statistics.DrivingSession
import ac.sbmax002.eye_on.model.statistics.SessionEvent
import ac.sbmax002.eye_on.ui.home.AppMode // AppMode import 필수
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 통계 데이터를 관리하는 Repository
 * 현재는 Mock 데이터를 사용하지만, 추후 실제 데이터베이스나 API로 교체 가능
 */
class StatisticsRepository {

    private val _allSessions = mutableListOf<DrivingSession>()

    init {
        generateDummyData()
    }

    fun getAllSessions(): List<DrivingSession> {
        // 항상 최신순으로 정렬해서 반환
        return _allSessions.sortedByDescending { it.rawDateTime }
    }

    fun getSessionById(id: String): DrivingSession? {
        return _allSessions.find { it.id == id }
    }

    private fun generateDummyData() {
        val today = LocalDate.now()
        // 운전 모드용 장소 목록
        val drivingLocations = listOf("Gangnam", "Seongnam", "Busan", "Incheon", "Seoul Station", "Highway", "Home", "Office")

        // 1. [주간 탭용] 최근 0~6일 전 데이터 15개 생성
        repeat(15) { i ->
            createRandomSession(idStart = 0, index = i, baseDate = today, minDays = 0, maxDays = 6, locations = drivingLocations)
        }

        // 2. [월간 탭용] 최근 7~30일 전 데이터 15개 생성
        repeat(15) { i ->
            createRandomSession(idStart = 100, index = i, baseDate = today, minDays = 7, maxDays = 30, locations = drivingLocations)
        }

        // 3. [전체 탭용] 최근 31~180일 전 데이터 15개 생성
        repeat(15) { i ->
            createRandomSession(idStart = 200, index = i, baseDate = today, minDays = 31, maxDays = 180, locations = drivingLocations)
        }
    }

    private fun createRandomSession(
        idStart: Int,
        index: Int,
        baseDate: LocalDate,
        minDays: Int,
        maxDays: Int,
        locations: List<String>
    ) {
        val daysAgo = Random.nextLong(minDays.toLong(), maxDays.toLong() + 1)
        val date = baseDate.minusDays(daysAgo)

        // 시간 랜덤 설정
        val hour = Random.nextInt(0, 23)
        val minute = Random.nextInt(0, 59)
        val startTime = LocalTime.of(hour, minute)
        val dateTime = LocalDateTime.of(date, startTime)

        val durationMin = Random.nextInt(10, 300) // 10분 ~ 5시간

        val lvl1 = Random.nextInt(0, 8)
        val lvl2 = Random.nextInt(0, 3)

        // [변경] 랜덤하게 모드 결정 (50% 확률로 운전 또는 스터디)
        val randomMode = if (Random.nextBoolean()) AppMode.DRIVING else AppMode.STUDY

        // [변경] 모드에 따라 장소 텍스트 다르게 설정
        val finalLocation = if (randomMode == AppMode.STUDY) {
            listOf("Library", "Cafe", "Home", "Study Room", "School").random()
        } else {
            locations.random() // 운전 모드면 파라미터로 받은 지역명 사용
        }

        // 상세 타임라인 이벤트 생성 (메시지도 모드에 따라 다르게 할 수 있음)
        val events = mutableListOf<SessionEvent>()
        repeat(lvl1) {
            events.add(createRandomEvent(startTime, durationMin, 1, randomMode))
        }
        repeat(lvl2) {
            events.add(createRandomEvent(startTime, durationMin, 2, randomMode))
        }
        events.sortBy { it.time }

        _allSessions.add(
            DrivingSession(
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
                mode = randomMode // [중요] 1번에서 추가한 mode 필드에 값 주입
            )
        )
    }

    // 이벤트 생성 시 모드 정보 전달
    private fun createRandomEvent(startTime: LocalTime, maxDurationMin: Int, level: Int, mode: AppMode): SessionEvent {
        val eventTime = startTime.plusMinutes(Random.nextLong(1, maxDurationMin.toLong()))
        val durationSec = Random.nextInt(2, 10)

        // 모드에 따라 경고 메시지 내용 변경
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