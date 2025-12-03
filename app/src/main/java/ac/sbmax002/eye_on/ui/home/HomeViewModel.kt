package ac.sbmax002.eye_on.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _cameraPermissionGranted = MutableStateFlow(false)
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted.asStateFlow()

    fun updateCameraPermission(granted: Boolean) {
        _cameraPermissionGranted.value = granted
        _uiState.value = _uiState.value.copy(
            isReady = granted
        )
    }

    fun startMonitoring() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isMonitoring = true,
                monitoringStartTime = System.currentTimeMillis()
            )
        }
    }

    fun stopMonitoring() {
        viewModelScope.launch {
            val monitoringDuration = if (_uiState.value.monitoringStartTime > 0) {
                System.currentTimeMillis() - _uiState.value.monitoringStartTime
            } else 0L

            _uiState.value = _uiState.value.copy(
                isMonitoring = false,
                monitoringStartTime = 0L,
                lastSessionDuration = monitoringDuration
            )
        }
    }

    fun updateFaceDetection(detected: Boolean) {
        _uiState.value = _uiState.value.copy(
            isFaceDetected = detected
        )
    }

    fun incrementDrowsinessCount() {
        _uiState.value = _uiState.value.copy(
            drowsinessDetectionCount = _uiState.value.drowsinessDetectionCount + 1
        )
    }

    fun getCurrentMonitoringDuration(): Long {
        return if (_uiState.value.isMonitoring && _uiState.value.monitoringStartTime > 0) {
            System.currentTimeMillis() - _uiState.value.monitoringStartTime
        } else 0L
    }
}
