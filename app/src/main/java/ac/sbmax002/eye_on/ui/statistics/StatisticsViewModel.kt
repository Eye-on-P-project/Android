package ac.sbmax002.eye_on.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import ac.sbmax002.eye_on.model.statistics.StatisticsUiState
import ac.sbmax002.eye_on.model.statistics.DrivingSession
import ac.sbmax002.eye_on.repository.StatisticsRepository

class StatisticsViewModel(
    private val repository: StatisticsRepository = StatisticsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    val filters = listOf("주간", "월간", "전체")

    init {
        updateFilter("주간")
    }

    fun updateFilter(filter: String) {
        viewModelScope.launch {
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
    }

    fun getSessionById(sessionId: String): DrivingSession? {
        return repository.getSessionById(sessionId)
    }
}

