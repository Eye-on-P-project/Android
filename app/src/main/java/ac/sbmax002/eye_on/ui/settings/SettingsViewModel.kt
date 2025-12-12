package ac.sbmax002.eye_on.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ac.sbmax002.eye_on.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 설정 화면의 ViewModel
 * 
 * MVVM 패턴에 따라 UI 상태를 관리하고 비즈니스 로직을 처리합니다.
 * Hilt를 사용하여 의존성 주입을 받습니다.
 * DataStore를 통해 설정값을 영구적으로 저장
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    /**
     * DataStore에서 가져온 핵심 설정값 묶음
     */
    private data class CoreSettings(
        val level1Sound: AlarmSound,
        val level1Volume: Int,
        val level2Sound: AlarmSound,
        val level2Volume: Int,
        val iconSize: FloatingIconSize
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        // 초기화 시 저장된 설정값 불러오기
        loadSettings()
    }
    
    /**
     * 저장된 설정값을 불러와서 UI 상태에 반영
     */
    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                combine(
                    settingsRepository.level1AlarmSound,
                    settingsRepository.level1Volume,
                    settingsRepository.level2AlarmSound,
                    settingsRepository.level2Volume,
                    settingsRepository.floatingIconSize
                ) { level1Sound, level1Vol, level2Sound, level2Vol, iconSize ->
                    CoreSettings(
                        level1Sound = level1Sound,
                        level1Volume = level1Vol,
                        level2Sound = level2Sound,
                        level2Volume = level2Vol,
                        iconSize = iconSize
                    )
                },
                settingsRepository.vibrationEnabled,
                settingsRepository.darkModeEnabled
            ) { core, vibration, darkMode ->
                SettingsUiState(
                    level1AlarmSound = core.level1Sound,
                    level1Volume = core.level1Volume,
                    level2AlarmSound = core.level2Sound,
                    level2Volume = core.level2Volume,
                    floatingIconSize = core.iconSize,
                    vibrationEnabled = vibration,
                    darkModeEnabled = darkMode
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    /**
     * 졸음 경고 1단계 알림음 변경
     */
    fun updateLevel1AlarmSound(sound: AlarmSound) {
        _uiState.value = _uiState.value.copy(level1AlarmSound = sound)
        // DataStore에 저장
        viewModelScope.launch {
            settingsRepository.saveLevel1AlarmSound(sound)
        }
    }
    
    /**
     * 졸음 경고 1단계 음량 변경
     */
    fun updateLevel1Volume(volume: Int) {
        val clampedVolume = volume.coerceIn(0, 100)
        _uiState.value = _uiState.value.copy(level1Volume = clampedVolume)
        // DataStore에 저장
        viewModelScope.launch {
            settingsRepository.saveLevel1Volume(clampedVolume)
        }
    }
    
    /**
     * 수면 경고 2단계 알림음 변경
     */
    fun updateLevel2AlarmSound(sound: AlarmSound) {
        _uiState.value = _uiState.value.copy(level2AlarmSound = sound)
        // DataStore에 저장
        viewModelScope.launch {
            settingsRepository.saveLevel2AlarmSound(sound)
        }
    }
    
    /**
     * 수면 경고 2단계 음량 변경
     */
    fun updateLevel2Volume(volume: Int) {
        val clampedVolume = volume.coerceIn(0, 100)
        _uiState.value = _uiState.value.copy(level2Volume = clampedVolume)
        // DataStore에 저장
        viewModelScope.launch {
            settingsRepository.saveLevel2Volume(clampedVolume)
        }
    }
    
    /**
     * 플로팅 아이콘 크기 변경
     */
    fun updateFloatingIconSize(size: FloatingIconSize) {
        _uiState.value = _uiState.value.copy(floatingIconSize = size)
        // DataStore에 저장
        viewModelScope.launch {
            settingsRepository.saveFloatingIconSize(size)
        }
    }
    
    /**
     * 진동 알림 토글
     */
    fun toggleVibration() {
        val newValue = !_uiState.value.vibrationEnabled
        _uiState.value = _uiState.value.copy(vibrationEnabled = newValue)
        // DataStore에 저장
        viewModelScope.launch {
            settingsRepository.saveVibrationEnabled(newValue)
        }
    }
    
    /**
     * 다크 모드 토글
     */
    fun toggleDarkMode() {
        val newValue = !_uiState.value.darkModeEnabled
        _uiState.value = _uiState.value.copy(darkModeEnabled = newValue)
        // DataStore에 저장
        viewModelScope.launch {
            settingsRepository.saveDarkModeEnabled(newValue)
        }
    }
}

