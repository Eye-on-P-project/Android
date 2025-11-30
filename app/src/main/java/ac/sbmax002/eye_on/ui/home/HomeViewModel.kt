package ac.sbmax002.eye_on.ui.home


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 파이프라인 쪽 import
import ac.sbmax002.eye_on.model.pipeline.FaceProcessingPipeline
import ac.sbmax002.eye_on.model.pipeline.PipelineListener
import ac.sbmax002.eye_on.model.pipeline.PipelineResult
import android.app.Application
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel

class HomeViewModel(
    application: Application
) : AndroidViewModel(application), PipelineListener {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 비전 파이프라인 (얼굴/눈/EAR/졸음 처리할 애)
    private val faceProcessingPipeline: FaceProcessingPipeline =
        FaceProcessingPipeline(
            context = application,
            listener = this    // PipelineListener 구현체 = HomeViewModel
        )

    private val _cameraPermissionGranted = MutableStateFlow(false)
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted.asStateFlow()

    fun updateCameraPermission(granted: Boolean) {
        _cameraPermissionGranted.value = granted
        _uiState.value = _uiState.value.copy(
            isReady = granted
        )
    }

    /**
     * CameraManager에서 들어오는 프레임을 파이프라인으로 넘기는 진입점.
     */
    fun onFrameFromCamera(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        faceProcessingPipeline.process(imageProxy, isFrontCamera)
    }

    // --------------------------------------------------------------------
    // PipelineListener 구현부
    // --------------------------------------------------------------------
    override fun onPipelineResult(result: PipelineResult) {
        // 아직은 최소한의 정보만 UI에 반영.
        // 나중에 EAR / 졸음 카운트까지 여기서 다 업데이트할 거임.
        _uiState.value = _uiState.value.copy(
            isFaceDetected = result.isFaceDetected
            // drowsinessDetectionCount 등은 다음 단계
        )
    }

    override fun onPipelineError(message: String) {
        // 필요하면 로그만 찍어두고 나중에 에러 상태도 UI에 추가할 수 있음.
        // Log.e("HomeViewModel", "Pipeline error: $message")
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

data class HomeUiState(
    val isReady: Boolean = false,
    val isMonitoring: Boolean = false,
    val isFaceDetected: Boolean = false,
    val monitoringStartTime: Long = 0L,
    val lastSessionDuration: Long = 0L,
    val drowsinessDetectionCount: Int = 0
)