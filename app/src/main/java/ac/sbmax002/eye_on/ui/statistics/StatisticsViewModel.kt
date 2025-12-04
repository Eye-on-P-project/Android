package ac.sbmax002.eye_on.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import ac.sbmax002.eye_on.model.statistics.StatisticsUiState
import ac.sbmax002.eye_on.model.statistics.DrivingSession
import ac.sbmax002.eye_on.repository.StatisticsRepository
import ac.sbmax002.eye_on.repository.AppStateRepository
import ac.sbmax002.eye_on.ui.home.AppMode

class StatisticsViewModel(
    private val repository: StatisticsRepository = StatisticsRepository(),
    private val appStateRepository: AppStateRepository = AppStateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    val filters = listOf("주간", "월간", "전체")

    init {
        // 앱 모드 변경 감지 (이 안에서 updateFilter가 호출됨)
        observeAppMode()
    }

    private fun observeAppMode() {
        viewModelScope.launch {
            appStateRepository.appMode.collectLatest { mode ->
                _uiState.update { it.copy(appMode = mode) }
                // 모드가 바뀌면 현재 필터 기준으로 데이터 새로고침
                updateFilter(_uiState.value.selectedFilter)
            }
        }
    }

    fun updateFilter(filter: String) {
        viewModelScope.launch {
            val allData = repository.getAllSessions()
            val today = LocalDate.now()

            // 1. 현재 모드 가져오기
            val currentMode = _uiState.value.appMode

            // 2. [수정] 모드 필터링 적용 (이 부분이 핵심입니다!)
            // 현재 모드와 데이터의 모드가 일치하는 것만 남깁니다.
            val modeFilteredData = allData.filter { it.mode == currentMode }

            // 3. 날짜 필터링 (modeFilteredData를 기준으로)
            val finalFilteredData = when (filter) {
                "주간" -> modeFilteredData.filter {
                    !it.rawDateTime.toLocalDate().isBefore(today.minusWeeks(1))
                }
                "월간" -> modeFilteredData.filter {
                    !it.rawDateTime.toLocalDate().isBefore(today.minusMonths(1))
                }
                else -> modeFilteredData // 전체
            }

            // 4. 통계 계산 (최종 필터링된 데이터 사용)
            val totalMinutes = finalFilteredData.sumOf { it.durationMinutes }
            val totalLvl1 = finalFilteredData.sumOf { it.level1Alerts }
            val totalLvl2 = finalFilteredData.sumOf { it.level2Alerts }

            // 5. 시간대별 빈도 계산
            val timeDist = IntArray(4) { 0 }
            finalFilteredData.forEach { session ->
                val hour = session.rawDateTime.hour
                val alerts = session.level1Alerts + session.level2Alerts
                when (hour) {
                    in 6..11 -> timeDist[0] += alerts  // 오전
                    in 12..17 -> timeDist[1] += alerts // 오후
                    in 18..23 -> timeDist[2] += alerts // 저녁
                    else -> timeDist[3] += alerts      // 새벽
                }
            }

            _uiState.update { currentState ->
                currentState.copy(
                    selectedFilter = filter,
                    sessions = finalFilteredData,
                    totalDrivingMinutes = totalMinutes,
                    totalSessions = finalFilteredData.size,
                    level1Total = totalLvl1,
                    level2Total = totalLvl2,
                    timeDistribution = timeDist.toList()
                )
            }
        }
    }

    fun getSessionById(sessionId: String): DrivingSession? {
        return repository.getSessionById(sessionId)
    }
}