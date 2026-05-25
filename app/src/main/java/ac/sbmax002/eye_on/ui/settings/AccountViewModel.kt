package ac.sbmax002.eye_on.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ac.sbmax002.eye_on.repository.AuthRepository
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
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // 고정 필드 (Mock)
    private val _email = MutableStateFlow("user@example.com")
    val email: StateFlow<String> = _email.asStateFlow()

    // 조직 (비어있으면 UI에서 숨김 처리, 테스트를 위해 현재는 비워둠)
    private val _organization = MutableStateFlow("") 
    val organization: StateFlow<String> = _organization.asStateFlow()

    private val _birthYear = MutableStateFlow("1990년")
    val birthYear: StateFlow<String> = _birthYear.asStateFlow()

    private val _gender = MutableStateFlow("남성")
    val gender: StateFlow<String> = _gender.asStateFlow()

    // 변경 가능 필드 (Mock)
    private val _name = MutableStateFlow("홍길동")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _nickname = MutableStateFlow("길동이")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    // 비밀번호 필드
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

    fun updateName(value: String) { _name.value = value }
    fun updateNickname(value: String) { _nickname.value = value }
    fun updateCurrentPassword(value: String) { _currentPassword.value = value }
    fun updateNewPassword(value: String) { _newPassword.value = value }
    fun updateConfirmPassword(value: String) { _confirmPassword.value = value }

    fun saveProfile() {
        val current = _currentPassword.value
        val newPw = _newPassword.value
        val confirm = _confirmPassword.value

        // 비밀번호 변경 시도 여부
        val isPasswordChangeAttempted = current.isNotBlank() || newPw.isNotBlank() || confirm.isNotBlank()

        if (isPasswordChangeAttempted) {
            if (current.isBlank() || newPw.isBlank() || confirm.isBlank()) {
                viewModelScope.launch {
                    _eventFlow.emit(UiEvent.ShowSnackbar("비밀번호를 변경하려면 모든 비밀번호 필드를 입력해주세요."))
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
        }

        // Mock 통신 시작
        viewModelScope.launch {
            _isLoading.value = true
            
            // 네트워크 요청 모방
            delay(1000)
            
            _isLoading.value = false
            _eventFlow.emit(UiEvent.ShowSnackbar("회원 정보가 성공적으로 수정되었습니다."))
            _eventFlow.emit(UiEvent.NavigateBack)
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val refreshToken = authRepository.getRefreshToken()
                if (refreshToken != null) {
                    ac.sbmax002.eye_on.network.NetworkConfig.authApiService.logout(
                        ac.sbmax002.eye_on.network.TokenRequest(refreshToken)
                    )
                }
            } catch (e: Exception) {
                // 네트워크 오류 등으로 로그아웃 실패해도 로컬에서는 로그아웃 처리
                e.printStackTrace()
            } finally {
                authRepository.clearAuthTokens()
                ac.sbmax002.eye_on.repository.AppStateRepository.accessToken = null
                ac.sbmax002.eye_on.repository.AppStateRepository.userId = null
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object NavigateBack : UiEvent()
    }
}
