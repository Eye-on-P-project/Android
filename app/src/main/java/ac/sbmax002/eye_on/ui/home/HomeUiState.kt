package ac.sbmax002.eye_on.ui.home
import ac.sbmax002.eye_on.model.statistics.SessionEvent
import ac.sbmax002.eye_on.DTO.DrowsinessState

data class HomeUiState(
    val isReady: Boolean = false,
    val isMonitoring: Boolean = false,
    val isFaceDetected: Boolean = false,
    val currentDrowsinessState: DrowsinessState = DrowsinessState.NORMAL,
    val monitoringStartTime: Long = 0L,
    val lastSessionDuration: Long = 0L,
    val drowsinessDetectionCount: Int = 0,
    val sleepDetectionCount: Int = 0,
    val appMode: AppMode = AppMode.DRIVING,
    val cameraInitialized: Boolean = false,
    val sessionEvents: List<SessionEvent> = emptyList()
)

