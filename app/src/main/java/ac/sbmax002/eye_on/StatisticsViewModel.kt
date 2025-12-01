package ac.sbmax002.eye_on.ui.statistics

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

// ==========================================
// 1. 데이터 모델
// ==========================================

data class SessionEvent(
    val time: String,      // 발생 시각 (예: "14:22")
    val message: String,   // 메시지
    val duration: String,  // 지속 시간
    val level: Int         // 1: 주의(Yellow), 2: 경고(Red)
)

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
    val events: List<SessionEvent> = emptyList()
)

data class StatisticsUiState(
    val selectedFilter: String = "주간",
    val totalDrivingMinutes: Int = 0,
    val totalSessions: Int = 0,
    val level1Total: Int = 0,
    val level2Total: Int = 0,
    val timeDistribution: List<Int> = listOf(0, 0, 0, 0), // [오전, 오후, 저녁, 새벽]
    val sessions: List<DrivingSession> = emptyList()
)

// ==========================================
// 2. Mock Repository (데이터 생성소)
// ==========================================
class MockStatisticsRepository {

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
        val locations = listOf("Gangnam", "Seongnam", "Busan", "Incheon", "Seoul Station", "Highway", "Home", "Office")

        // 1. [주간 탭용] 최근 0~6일 전 데이터 15개 생성
        repeat(15) { i ->
            createRandomSession(idStart = 0, index = i, baseDate = today, minDays = 0, maxDays = 6, locations = locations)
        }

        // 2. [월간 탭용] 최근 7~30일 전 데이터 15개 생성
        repeat(15) { i ->
            createRandomSession(idStart = 100, index = i, baseDate = today, minDays = 7, maxDays = 30, locations = locations)
        }

        // 3. [전체 탭용] 최근 31~180일 전 데이터 15개 생성
        repeat(15) { i ->
            createRandomSession(idStart = 200, index = i, baseDate = today, minDays = 31, maxDays = 180, locations = locations)
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

        // 상세 타임라인 이벤트 생성
        val events = mutableListOf<SessionEvent>()
        repeat(lvl1) {
            events.add(createRandomEvent(startTime, durationMin, 1))
        }
        repeat(lvl2) {
            events.add(createRandomEvent(startTime, durationMin, 2))
        }
        events.sortBy { it.time }

        _allSessions.add(
            DrivingSession(
                id = "${idStart + index}",
                dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                time = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                location = locations.random(),
                durationMinutes = durationMin,
                durationStr = formatDuration(durationMin),
                level1Alerts = lvl1,
                level2Alerts = lvl2,
                rawDateTime = dateTime,
                events = events
            )
        )
    }

    private fun createRandomEvent(startTime: LocalTime, maxDurationMin: Int, level: Int): SessionEvent {
        val eventTime = startTime.plusMinutes(Random.nextLong(1, maxDurationMin.toLong()))
        val durationSec = Random.nextInt(2, 10)
        return SessionEvent(
            time = eventTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            message = if (level == 1) "Drowsiness detected" else "Sleep warning",
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

// ==========================================
// 3. ViewModel
// ==========================================
class StatisticsViewModel : ViewModel() {

    private val repository = MockStatisticsRepository()

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    val filters = listOf("주간", "월간", "전체")

    init {
        updateFilter("주간")
    }

    fun updateFilter(filter: String) {
        val allData = repository.getAllSessions()
        val today = LocalDate.now()

        // 1. 날짜 필터링 로직
        val filteredData = when (filter) {
            "주간" -> allData.filter {
                !it.rawDateTime.toLocalDate().isBefore(today.minusWeeks(1))
            }
            "월간" -> allData.filter {
                !it.rawDateTime.toLocalDate().isBefore(today.minusMonths(1))
            }
            else -> allData // 전체
        }

        // 2. 통계 계산
        val totalMinutes = filteredData.sumOf { it.durationMinutes }
        val totalLvl1 = filteredData.sumOf { it.level1Alerts }
        val totalLvl2 = filteredData.sumOf { it.level2Alerts }

        // 3. 시간대별 빈도 계산 (06~12, 12~18, 18~24, 00~06)
        val timeDist = IntArray(4) { 0 }
        filteredData.forEach { session ->
            val hour = session.rawDateTime.hour
            val alerts = session.level1Alerts + session.level2Alerts
            when (hour) {
                in 6..11 -> timeDist[0] += alerts  // 오전
                in 12..17 -> timeDist[1] += alerts // 오후
                in 18..23 -> timeDist[2] += alerts // 저녁
                else -> timeDist[3] += alerts      // 새벽
            }
        }

        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            sessions = filteredData,
            totalDrivingMinutes = totalMinutes,
            totalSessions = filteredData.size,
            level1Total = totalLvl1,
            level2Total = totalLvl2,
            timeDistribution = timeDist.toList()
        )
    }

    fun getSessionById(sessionId: String): DrivingSession? {
        return repository.getSessionById(sessionId)
    }
}