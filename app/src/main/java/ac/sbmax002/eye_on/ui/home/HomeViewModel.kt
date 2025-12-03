package ac.sbmax002.eye_on.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ac.sbmax002.eye_on.repository.AppStateRepository

class HomeViewModel : ViewModel() {
    
    // HomeUiState 초기값을 AppStateRepository의 현재 값과 동기화
    private val _uiState = MutableStateFlow(
        HomeUiState(appMode = AppStateRepository.getCurrentAppMode())
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // 앱 상태 Repository에서 공유 상태를 가져옴 (Singleton이므로 직접 접근)
    // 다른 화면에서는 이 StateFlow를 직접 구독 가능
    val appMode: StateFlow<AppMode> = AppStateRepository.appMode

    private val _cameraPermissionGranted = MutableStateFlow(false)
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted.asStateFlow()

    fun updateCameraPermission(granted: Boolean) {
        _cameraPermissionGranted.value = granted
        _uiState.value = _uiState.value.copy(
            isReady = granted
        )
    }

    fun startMonitoring() {
        Log.d("HomeViewModel", "startMonitoring() called")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isMonitoring = true,
                monitoringStartTime = System.currentTimeMillis()
            )
            Log.d("HomeViewModel", "Monitoring started. isMonitoring: ${_uiState.value.isMonitoring}")
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

    fun selectMode(mode: AppMode) {
        Log.d("HomeViewModel", "Mode selected: ${mode.name}")
        // 공유 상태 Repository에 업데이트 (모든 화면에 자동 반영됨)
        AppStateRepository.setAppMode(mode)
        // HomeUiState도 동기화 (기존 코드 호환성 유지)
        _uiState.value = _uiState.value.copy(
            appMode = mode
        )
        Log.d("HomeViewModel", "Current appMode: ${AppStateRepository.getCurrentAppMode().name}")
    }

    fun updateCameraInitialized(initialized: Boolean) {
        _uiState.value = _uiState.value.copy(
            cameraInitialized = initialized
        )
    }
}
