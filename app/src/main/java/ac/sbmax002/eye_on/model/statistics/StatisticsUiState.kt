package ac.sbmax002.eye_on.model.statistics

import ac.sbmax002.eye_on.ui.home.AppMode

data class StatisticsUiState(
    val selectedFilter: String = "주간",
    val totalDrivingMinutes: Int = 0,
    val totalSessions: Int = 0,
    val level1Total: Int = 0,
    val level2Total: Int = 0,
    val timeDistribution: List<Int> = listOf(0, 0, 0, 0), // [오전, 오후, 저녁, 새벽]
    val sessions: List<DrivingSession> = emptyList(),
    val isLoading: Boolean = false,
    val appMode: AppMode = AppMode.DRIVING
)

