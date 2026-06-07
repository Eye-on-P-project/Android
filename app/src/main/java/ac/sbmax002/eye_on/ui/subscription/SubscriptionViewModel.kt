package ac.sbmax002.eye_on.ui.subscription

import ac.sbmax002.eye_on.model.subscription.SubscriptionPlan
import ac.sbmax002.eye_on.model.subscription.SubscriptionStatus
import ac.sbmax002.eye_on.model.subscription.SubscriptionTier
import ac.sbmax002.eye_on.repository.SubscriptionRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 구독 화면 UI 상태
 */
data class SubscriptionUiState(
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus(),
    val plans: List<SubscriptionPlan> = emptyList(),
    val isLoading: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 구독 상태/요금제 화면의 ViewModel
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                subscriptionRepository.loadSubscriptionStatus()
                val plans = subscriptionRepository.getAvailablePlans()
                // subscriptionStatus를 collect
                subscriptionRepository.subscriptionStatus.collect { status ->
                    _uiState.value = _uiState.value.copy(
                        subscriptionStatus = status,
                        plans = plans,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "구독 정보를 불러오는데 실패했습니다."
                )
            }
        }
    }

    /**
     * 구독 (Mock: 즉시 활성화)
     */
    fun subscribe(tier: SubscriptionTier) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                subscriptionRepository.subscribe(tier)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showSuccessDialog = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "구독 처리 중 오류가 발생했습니다."
                )
            }
        }
    }

    /**
     * 구독 해지 (자동 갱신 해제)
     */
    fun cancelSubscription() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                subscriptionRepository.cancelSubscription()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showCancelDialog = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "해지 처리 중 오류가 발생했습니다."
                )
            }
        }
    }

    /**
     * 구독 복원 (해지 취소)
     */
    fun restoreSubscription() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                subscriptionRepository.restoreSubscription()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "복원 처리 중 오류가 발생했습니다."
                )
            }
        }
    }

    fun showCancelDialog() {
        _uiState.value = _uiState.value.copy(showCancelDialog = true)
    }

    fun dismissCancelDialog() {
        _uiState.value = _uiState.value.copy(showCancelDialog = false)
    }

    fun dismissSuccessDialog() {
        _uiState.value = _uiState.value.copy(showSuccessDialog = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
