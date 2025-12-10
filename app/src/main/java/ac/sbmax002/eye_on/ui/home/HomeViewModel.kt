package ac.sbmax002.eye_on.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ac.sbmax002.eye_on.repository.AppStateRepository
import ac.sbmax002.eye_on.repository.StatisticsRepository
import ac.sbmax002.eye_on.model.pipeline.PipelineResult // 파이프라인 결과 클래스 import 필요
import ac.sbmax002.eye_on.DTO.DrowsinessState

class HomeViewModel(
    private val repository: StatisticsRepository // DB 처리를 위해 주입받음
) : ViewModel() {

    // 현재 실행 중인 세션 ID (DB 저장용)
    private var currentSessionId: String? = null

    // HomeUiState 초기값을 AppStateRepository의 현재 값과 동기화
    private val _uiState = MutableStateFlow(
        HomeUiState(appMode = AppStateRepository.getCurrentAppMode())
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val appMode: StateFlow<AppMode> = AppStateRepository.appMode

    private val _cameraPermissionGranted = MutableStateFlow(false)
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted.asStateFlow()

    fun updateCameraPermission(granted: Boolean) {
        _cameraPermissionGranted.value = granted
        _uiState.value = _uiState.value.copy(
            isReady = granted
        )
    }

    // 1. 모니터링 시작 (DB 세션 생성)
    fun startMonitoring() {
        Log.d("HomeViewModel", "startMonitoring() called")
        viewModelScope.launch {
            // ★ DB에 세션 시작 알림 & ID 발급
            val currentMode = _uiState.value.appMode
            currentSessionId = repository.startDrivingSession(currentMode)
            Log.d("HomeViewModel", "DB Session Created: $currentSessionId")

            _uiState.value = _uiState.value.copy(
                isMonitoring = true,
                monitoringStartTime = System.currentTimeMillis(),
                drowsinessDetectionCount = 0 // 카운트 초기화
            )
        }
    }

    // 2. 모니터링 종료 (DB 세션 종료 처리)
    fun stopMonitoring() {
        val sessionId = currentSessionId

        viewModelScope.launch {
            // ★ DB에 종료 알림
            if (sessionId != null) {
                repository.endDrivingSession(sessionId)
                Log.d("HomeViewModel", "DB Session Ended: $sessionId")
            }
            currentSessionId = null

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

    // 3. 파이프라인 결과 처리 (졸음 감지 시 DB 저장)
    // 기존의 incrementDrowsinessCount 대신 이 함수를 파이프라인에서 호출하는 것을 추천합니다.
    fun onPipelineResult(result: PipelineResult) {
        // UI 업데이트: 얼굴 감지 상태 등
        updateFaceDetection(result.isFaceDetected)

// ★ [수정됨] 상태에 따라 위험 레벨(Level) 결정
        // DrowsinessDetector.kt와 PipeLineResult.kt에 정의된 상태를 사용
        val alertLevel = when (result.drowsinessState) {
            DrowsinessState.DROWSY -> 1   // 조금 졸림 -> Level 1
            DrowsinessState.SLEEPING -> 2 // 완전히 잠 -> Level 2
            else -> 0                     // 정상 -> 저장 안 함
        }

        // 위험 상황(1, 2)일 때만 저장 로직 실행
        if (alertLevel > 0) {
            incrementDrowsinessCount(alertLevel)
        }
    }

    // 내부적으로 카운트 증가 및 DB 저장
    private fun incrementDrowsinessCount(level: Int) {
        val sessionId = currentSessionId ?: return // 세션 없으면 무시

        viewModelScope.launch {
            // 1. UI 업데이트 우영님 요청 사항 - 주석 처리
           /*
           _uiState.value = _uiState.value.copy(
                drowsinessDetectionCount = _uiState.value.drowsinessDetectionCount + 1
            )
            */


// 2. DB에 이벤트 저장 (Level에 따라 메시지 구분)
            val message = if (level == 2) "Sleep Detected" else "Drowsiness Detected"

            repository.saveEvent(
                sessionId = sessionId,
                message = message,
                level = level // ★ 전달받은 레벨(1 or 2)로 저장
            )

            Log.d("HomeViewModel", "Event Saved: Level $level - $message")
        }
    }

    fun getCurrentMonitoringDuration(): Long {
        return if (_uiState.value.isMonitoring && _uiState.value.monitoringStartTime > 0) {
            System.currentTimeMillis() - _uiState.value.monitoringStartTime
        } else 0L
    }

    fun selectMode(mode: AppMode) {
        Log.d("HomeViewModel", "Mode selected: ${mode.name}")
        AppStateRepository.setAppMode(mode)
        _uiState.value = _uiState.value.copy(
            appMode = mode
        )
    }

    fun updateCameraInitialized(initialized: Boolean) {
        _uiState.value = _uiState.value.copy(
            cameraInitialized = initialized
        )
    }
}

// ★ ViewModelFactory 추가 (MainActivity에서 사용)
class HomeViewModelFactory(private val repository: StatisticsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}