package ac.sbmax002.eye_on.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor() : ViewModel() {

    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 성공 또는 실패 메시지를 이벤트로 전달
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun updateCurrentPassword(value: String) {
        _currentPassword.value = value
    }

    fun updateNewPassword(value: String) {
        _newPassword.value = value
    }

    fun updateConfirmPassword(value: String) {
        _confirmPassword.value = value
    }

    fun changePassword() {
        val current = _currentPassword.value
        val newPw = _newPassword.value
        val confirm = _confirmPassword.value

        // 유효성 검사
        if (current.isBlank() || newPw.isBlank() || confirm.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowSnackbar("모든 필드를 입력해주세요."))
            }
            return
        }

        if (newPw != confirm) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowSnackbar("새 비밀번호가 일치하지 않습니다."))
            }
            return
        }
        
        if (current == newPw) {
             viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowSnackbar("새 비밀번호는 현재 비밀번호와 달라야 합니다."))
            }
            return
        }

        // Mock 통신 시작
        viewModelScope.launch {
            _isLoading.value = true
            
            // 네트워크 요청 모방
            delay(1000)
            
            _isLoading.value = false
            _eventFlow.emit(UiEvent.ShowSnackbar("비밀번호가 성공적으로 변경되었습니다."))
            _eventFlow.emit(UiEvent.NavigateBack)
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object NavigateBack : UiEvent()
    }
}
