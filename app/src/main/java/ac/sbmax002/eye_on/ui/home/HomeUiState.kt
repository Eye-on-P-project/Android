package ac.sbmax002.eye_on.ui.home

data class HomeUiState(
    val isReady: Boolean = false,
    val isMonitoring: Boolean = false,
    val isFaceDetected: Boolean = false,
    val monitoringStartTime: Long = 0L,
    val lastSessionDuration: Long = 0L,
    val drowsinessDetectionCount: Int = 0,
    val sleepDetectionCount: Int = 0,
    val appMode: AppMode = AppMode.DRIVING,
    val cameraInitialized: Boolean = false
)

