package ac.sbmax002.eye_on.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 설정 화면의 ViewModel
 * 
 * MVVM 패턴에 따라 UI 상태를 관리하고 비즈니스 로직을 처리합니다.
 * Hilt를 사용하여 의존성 주입을 받습니다.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    /**
     * 졸음 경고 1단계 알림음 변경
     */
    fun updateLevel1AlarmSound(sound: AlarmSound) {
        _uiState.value = _uiState.value.copy(level1AlarmSound = sound)
        // TODO: 실제 설정 저장 로직 추가 (SharedPreferences 또는 DataStore)
    }
    
    /**
     * 졸음 경고 1단계 음량 변경
     */
    fun updateLevel1Volume(volume: Int) {
        val clampedVolume = volume.coerceIn(0, 100)
        _uiState.value = _uiState.value.copy(level1Volume = clampedVolume)
        // TODO: 실제 설정 저장 로직 추가
    }
    
    /**
     * 수면 경고 2단계 알림음 변경
     */
    fun updateLevel2AlarmSound(sound: AlarmSound) {
        _uiState.value = _uiState.value.copy(level2AlarmSound = sound)
        // TODO: 실제 설정 저장 로직 추가
    }
    
    /**
     * 수면 경고 2단계 음량 변경
     */
    fun updateLevel2Volume(volume: Int) {
        val clampedVolume = volume.coerceIn(0, 100)
        _uiState.value = _uiState.value.copy(level2Volume = clampedVolume)
        // TODO: 실제 설정 저장 로직 추가
    }
    
    /**
     * 플로팅 아이콘 크기 변경
     */
    fun updateFloatingIconSize(size: FloatingIconSize) {
        _uiState.value = _uiState.value.copy(floatingIconSize = size)
        // TODO: 실제 설정 저장 로직 추가
    }
    
    /**
     * 진동 알림 토글
     */
    fun toggleVibration() {
        _uiState.value = _uiState.value.copy(vibrationEnabled = !_uiState.value.vibrationEnabled)
        // TODO: 실제 설정 저장 로직 추가
    }
    
    /**
     * 다크 모드 토글
     */
    fun toggleDarkMode() {
        _uiState.value = _uiState.value.copy(darkModeEnabled = !_uiState.value.darkModeEnabled)
        // TODO: 실제 설정 저장 로직 추가
    }
}

