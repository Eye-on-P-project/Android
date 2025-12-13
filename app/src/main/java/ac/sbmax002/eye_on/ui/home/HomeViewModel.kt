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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ac.sbmax002.eye_on.model.statistics.BatteryUsageTracker
import ac.sbmax002.eye_on.model.statistics.SessionEvent

class HomeViewModel(
    private val repository: StatisticsRepository, // DB 처리를 위해 주입받음
    private val batteryUsageTracker: BatteryUsageTracker
) : ViewModel() {

    // 현재 실행 중인 세션 ID (DB 저장용)
    private var currentSessionId: String? = null

    // 🔹 졸음 에피소드 추적용 상태
    // NORMAL -> DROWSY/SLEEPING 진입 시 에피소드 시작,
    // 다시 NORMAL 될 때 에피소드 종료
    private var isInAlertState: Boolean = false          // 현재 에피소드 안에 있는지
    private var alertStartTimestampMs: Long = 0L         // 에피소드 시작 프레임 timestamp
    private var alertMaxLevel: Int = 0                   // 이 에피소드 동안 도달한 최대 레벨(1/2)
    private var totalDrowsyDurationMs: Long = 0L         // 세션 동안 누적 졸음 시간(필요시 사용)


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
            val startBattery = batteryUsageTracker.markStart()
            currentSessionId = repository.startDrivingSession(currentMode, startBattery)
            Log.d("HomeViewModel", "DB Session Created: $currentSessionId")

            // 🔹 졸음 에피소드 상태 초기화
            isInAlertState = false
            alertStartTimestampMs = 0L
            alertMaxLevel = 0
            totalDrowsyDurationMs = 0L

            _uiState.value = _uiState.value.copy(
                isMonitoring = true,
                monitoringStartTime = System.currentTimeMillis(),
                drowsinessDetectionCount = 0, // 카운트 초기화
                sessionEvents = emptyList()
            )
        }
    }

    // 2. 모니터링 종료 (DB 세션 종료 처리)
    fun stopMonitoring() {
        val sessionId = currentSessionId

        viewModelScope.launch {
            val batterySnapshot = batteryUsageTracker.markEnd()
            // ★ DB에 종료 알림
            if (sessionId != null) {
                repository.endDrivingSession(sessionId, batterySnapshot)
                Log.d("HomeViewModel", "DB Session Ended: $sessionId")
            }
            currentSessionId = null
            batteryUsageTracker.reset()

            // 🔹 에피소드 상태 리셋
            isInAlertState = false
            alertStartTimestampMs = 0L
            alertMaxLevel = 0
            totalDrowsyDurationMs = 0L

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
    fun onPipelineResult(result: PipelineResult) {
        // UI 업데이트: 얼굴 감지 상태 등
        updateFaceDetection(result.isFaceDetected)

        val state = result.drowsinessState
        _uiState.value = _uiState.value.copy(currentDrowsinessState = state)

        // 지금 프레임이 "경고 상태"인지 여부 (DROWSY 또는 SLEEPING)
        val isAlertNow =
            (state == DrowsinessState.DROWSY || state == DrowsinessState.SLEEPING)

        // ★ [수정됨] 상태에 따라 위험 레벨(Level) 결정
        // DrowsinessDetector.kt와 PipeLineResult.kt에 정의된 상태를 사용
        val alertLevel = when (state) {
            DrowsinessState.DROWSY -> 1   // 조금 졸림 -> Level 1
            DrowsinessState.SLEEPING -> 2 // 완전히 잠 -> Level 2
            else -> 0                     // 정상 -> 저장 안 함
        }
// 1) NORMAL → DROWSY/SLEEPING : 에피소드 시작
        if (isAlertNow && !isInAlertState) {
            isInAlertState = true
            alertStartTimestampMs = result.frameTimestampMs
            alertMaxLevel = alertLevel
            Log.d(
                "HomeViewModel",
                "Drowsiness episode started: level=$alertLevel, ts=${result.frameTimestampMs}"
            )
        }
        // 2) 에피소드 진행 중: 더 높은 레벨이 나오면 갱신
        else if (isAlertNow && isInAlertState) {
            if (alertLevel > alertMaxLevel) {
                alertMaxLevel = alertLevel
            }
        }
        // 3) DROWSY/SLEEPING → NORMAL : 에피소드 종료
        else if (!isAlertNow && isInAlertState) {
            val endTs = result.frameTimestampMs
            val durationMs = (endTs - alertStartTimestampMs).coerceAtLeast(0L)

            totalDrowsyDurationMs += durationMs

            onDrowsinessEpisodeFinished(alertMaxLevel, durationMs)

            // 상태 리셋
            isInAlertState = false
            alertStartTimestampMs = 0L
            alertMaxLevel = 0
        }
    }

    /**
     * 졸음 에피소드가 끝났을 때 한 번만 호출:
     * - DB에 SessionEvent 한 줄 저장
     * - duration을 "1.2s" 같은 문자열로 포맷해서 넣어줌
     */
    private fun onDrowsinessEpisodeFinished(level: Int, durationMs: Long) {
        val sessionId = currentSessionId ?: return
        if (level <= 0) return

        _uiState.value = if (level == 2) {
            _uiState.value.copy(
                sleepDetectionCount = _uiState.value.sleepDetectionCount + 1
            )
        } else {
            _uiState.value.copy(
                drowsinessDetectionCount = _uiState.value.drowsinessDetectionCount + 1
            )
        }
        // ms → 초 단위 문자열로 변환 (예: 1250ms -> "1.3s")
        val durationSeconds = durationMs / 1000f
        val durationStr = String.format("%.1fs", durationSeconds)

        val message = if (level == 2) "Sleep Detected" else "Drowsiness Detected"

        val currentTimeMs = System.currentTimeMillis()
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = timeFormatter.format(Date(currentTimeMs))

        val newEvent = SessionEvent(
            sessionId = sessionId,
            time = timeStr,
            message = message,
            duration = durationStr,
            level = level
        )

        val currentList = _uiState.value.sessionEvents
        _uiState.value = _uiState.value.copy(
            sessionEvents = currentList + newEvent
        )

        Log.d(
            "HomeViewModel",
            "Drowsiness episode finished: level=$level, duration=$durationStr"
        )

        viewModelScope.launch {
            repository.saveEvent(
                sessionId = sessionId,
                message = message,
                level = level,
                duration = durationStr
            )
        }
    }



    // 내부적으로 카운트 증가 및 DB 저장
    private fun incrementDrowsinessCount(level: Int) {
        val sessionId = currentSessionId ?: return // 세션 없으면 무시
        if (level <= 0) return

        // 1. UI 카운트 증가
        _uiState.value = if (level == 2) {
            // Level 2 (수면)
            _uiState.value.copy(
                sleepDetectionCount = _uiState.value.sleepDetectionCount + 1
            )
        } else {
            // Level 1 (졸음)
            _uiState.value.copy(
                drowsinessDetectionCount = _uiState.value.drowsinessDetectionCount + 1
            )
        }


        viewModelScope.launch {
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
class HomeViewModelFactory(
    private val repository: StatisticsRepository,
    private val batteryUsageTracker: BatteryUsageTracker
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, batteryUsageTracker) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}