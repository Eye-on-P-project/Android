package ac.sbmax002.eye_on.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import ac.sbmax002.eye_on.model.statistics.StatisticsUiState
import ac.sbmax002.eye_on.model.statistics.DrivingSession
import ac.sbmax002.eye_on.model.statistics.SessionEvent
import ac.sbmax002.eye_on.repository.StatisticsRepository
import ac.sbmax002.eye_on.repository.AppStateRepository
import ac.sbmax002.eye_on.ui.home.AppMode

class StatisticsViewModel(
    private val repository: StatisticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    // ★ 추가: DB에서 불러온 모든 데이터를 저장할 캐시 (동기적 접근용)
    private var _cachedAllSessions: List<DrivingSession> = emptyList()

    val filters = listOf("주간", "월간", "전체")

    init {
        // AppMode, 필터, DB 데이터가 변경될 때마다 자동으로 UI 업데이트
        viewModelScope.launch {
            combine(
                AppStateRepository.appMode,
                _uiState.map { it.selectedFilter }.distinctUntilChanged(),
                repository.allSessions // Repository의 Flow
            ) { mode, filter, allSessions ->
                Triple(mode, filter, allSessions)
            }.collectLatest { (mode, filter, allSessions) ->
                processData(mode, filter, allSessions)
            }
        }
    }

    // 필터 버튼 클릭 시 호출
    fun updateFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        // init의 combine 블록이 자동으로 감지하여 processData를 실행함
    }

    private fun processData(mode: AppMode, filter: String, allData: List<DrivingSession>) {
        // ★ 추가: 들어온 전체 데이터를 캐시에 저장 (나중에 getSessionById에서 씀)
        _cachedAllSessions = allData

        val today = LocalDate.now()

        // 1. 모드 필터링
        val modeFilteredData = allData.filter { it.mode == mode }

        // 2. 날짜 필터링
        val finalFilteredData = when (filter) {
            "주간" -> modeFilteredData.filter {
                !it.rawDateTime.toLocalDate().isBefore(today.minusWeeks(1))
            }
            "월간" -> modeFilteredData.filter {
                !it.rawDateTime.toLocalDate().isBefore(today.minusMonths(1))
            }
            else -> modeFilteredData
        }

        // 3. 통계 계산
        val totalMinutes = finalFilteredData.sumOf { it.durationMinutes }
        val totalLvl1 = finalFilteredData.sumOf { it.level1Alerts }
        val totalLvl2 = finalFilteredData.sumOf { it.level2Alerts }

        // 4. 시간대별 빈도
        val timeDist = IntArray(4) { 0 }
        finalFilteredData.forEach { session ->
            val hour = session.rawDateTime.hour
            val alerts = session.level1Alerts + session.level2Alerts
            when (hour) {
                in 6..11 -> timeDist[0] += alerts
                in 12..17 -> timeDist[1] += alerts
                in 18..23 -> timeDist[2] += alerts
                else -> timeDist[3] += alerts
            }
        }

        _uiState.update {
            it.copy(
                appMode = mode,
                sessions = finalFilteredData,
                totalDrivingMinutes = totalMinutes,
                totalSessions = finalFilteredData.size,
                level1Total = totalLvl1,
                level2Total = totalLvl2,
                timeDistribution = timeDist.toList()
            )
        }
    }

    // 상세 화면에서 사용: 세션 ID로 이벤트를 가져오는 함수
    suspend fun getSessionEvents(sessionId: String): List<SessionEvent> {
        return repository.getEventsForSession(sessionId)
    }

    // 상세 화면에서 사용: 세션 정보 가져오기
    fun getSessionById(sessionId: String): DrivingSession? {
        // 1. 현재 화면에 보이는(필터링된) 리스트에서 먼저 찾기
        val inFiltered = _uiState.value.sessions.find { it.id == sessionId }
        if (inFiltered != null) return inFiltered

        // 2. 없으면 전체 캐시 데이터에서 찾기
        // (필터링으로 가려진 데이터일 수도 있으므로 여기서 한 번 더 찾음)
        return _cachedAllSessions.find { it.id == sessionId }
    }
}

// Factory 추가 (MainActivity에서 사용)
class StatisticsViewModelFactory(private val repository: StatisticsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}