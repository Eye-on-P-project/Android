package ac.sbmax002.eye_on.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ac.sbmax002.eye_on.model.subscription.SubscriptionFeature
import ac.sbmax002.eye_on.model.subscription.SubscriptionPlan
import ac.sbmax002.eye_on.model.subscription.SubscriptionStatus
import ac.sbmax002.eye_on.model.subscription.SubscriptionTier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.subscriptionDataStore: DataStore<Preferences> by preferencesDataStore(name = "subscription")

/**
 * 구독 상태를 관리하는 Repository
 *
 * 현재는 Mock 데이터 기반으로 동작하며,
 * 추후 API 연결 시 내부 구현만 교체하면 됩니다.
 *
 * LLM 브랜치에서 [hasFeatureAccess]를 호출하여
 * LLM 음성 기능의 활성화 여부를 확인할 수 있습니다.
 */
@Singleton
class SubscriptionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.subscriptionDataStore

    private object Keys {
        val TIER = stringPreferencesKey("subscription_tier")
        val IS_ACTIVE = booleanPreferencesKey("subscription_is_active")
        val START_DATE = stringPreferencesKey("subscription_start_date")
        val EXPIRY_DATE = stringPreferencesKey("subscription_expiry_date")
        val IS_AUTO_RENEW = booleanPreferencesKey("subscription_is_auto_renew")
    }

    private val _subscriptionStatus = MutableStateFlow(SubscriptionStatus())
    /** 현재 구독 상태를 관찰하는 StateFlow */
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()

    /** DataStore에서 저장된 구독 상태를 로드 */
    suspend fun loadSubscriptionStatus() {
        val prefs = dataStore.data.first()
        val tierName = prefs[Keys.TIER] ?: SubscriptionTier.FREE.name
        val tier = try {
            SubscriptionTier.valueOf(tierName)
        } catch (e: IllegalArgumentException) {
            SubscriptionTier.FREE
        }
        val isActive = prefs[Keys.IS_ACTIVE] ?: (tier == SubscriptionTier.FREE)
        val startDate = prefs[Keys.START_DATE]
        val expiryDate = prefs[Keys.EXPIRY_DATE]
        val isAutoRenew = prefs[Keys.IS_AUTO_RENEW] ?: false

        val daysRemaining = expiryDate?.let {
            try {
                val expiry = LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
                ChronoUnit.DAYS.between(LocalDate.now(), expiry).toInt().coerceAtLeast(0)
            } catch (e: Exception) {
                null
            }
        }

        _subscriptionStatus.value = SubscriptionStatus(
            currentTier = tier,
            isActive = isActive,
            startDate = startDate,
            expiryDate = expiryDate,
            isAutoRenew = isAutoRenew,
            daysRemaining = daysRemaining
        )
    }

    /**
     * 구독 활성화 (Mock)
     * 추후 API 연결 시 서버 호출로 대체
     */
    suspend fun subscribe(tier: SubscriptionTier) {
        val today = LocalDate.now()
        val expiryDate = today.plusMonths(1)

        val status = SubscriptionStatus(
            currentTier = tier,
            isActive = true,
            startDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE),
            expiryDate = expiryDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            isAutoRenew = true,
            daysRemaining = ChronoUnit.DAYS.between(today, expiryDate).toInt()
        )

        saveToDataStore(status)
        _subscriptionStatus.value = status
    }

    /**
     * 구독 해지 (자동 갱신 해제)
     * 즉시 해지가 아닌, 현재 결제 기간 만료 후 해지
     */
    suspend fun cancelSubscription() {
        val current = _subscriptionStatus.value
        val updated = current.copy(isAutoRenew = false)

        saveToDataStore(updated)
        _subscriptionStatus.value = updated
    }

    /**
     * 구독 복원 (해지 취소 → 자동 갱신 재활성화)
     */
    suspend fun restoreSubscription() {
        val current = _subscriptionStatus.value
        val updated = current.copy(isAutoRenew = true)

        saveToDataStore(updated)
        _subscriptionStatus.value = updated
    }

    /**
     * 특정 기능에 대한 접근 권한 확인
     * LLM 브랜치에서 이 메서드를 호출하여 LLM 음성 기능 활성화 여부를 체크합니다.
     */
    fun hasFeatureAccess(feature: SubscriptionFeature): Boolean {
        val status = _subscriptionStatus.value
        return when (feature) {
            SubscriptionFeature.LLM_VOICE -> status.currentTier == SubscriptionTier.PLUS && status.isActive
        }
    }

    /**
     * 이용 가능한 요금제 목록 반환
     */
    fun getAvailablePlans(): List<SubscriptionPlan> {
        return listOf(
            SubscriptionPlan(
                tier = SubscriptionTier.FREE,
                monthlyPrice = 0,
                features = listOf(
                    "실시간 졸음 감지",
                    "1단계 / 2단계 경고 알림",
                    "운행 통계 및 기록",
                    "알림음 커스터마이징",
                    "민감도 조정"
                )
            ),
            SubscriptionPlan(
                tier = SubscriptionTier.PLUS,
                monthlyPrice = 4900,
                features = listOf(
                    "Free 플랜의 모든 기능",
                    "LLM 음성 대화 (졸음 감지 시 자동 대화)",
                    "AI 기반 맞춤형 졸음 예방"
                )
            )
        )
    }

    /** 구독 상태 초기화 (로그아웃/탈퇴 시 사용) */
    suspend fun clearSubscription() {
        dataStore.edit { it.clear() }
        _subscriptionStatus.value = SubscriptionStatus()
    }

    private suspend fun saveToDataStore(status: SubscriptionStatus) {
        dataStore.edit { prefs ->
            prefs[Keys.TIER] = status.currentTier.name
            prefs[Keys.IS_ACTIVE] = status.isActive
            status.startDate?.let { prefs[Keys.START_DATE] = it } ?: prefs.remove(Keys.START_DATE)
            status.expiryDate?.let { prefs[Keys.EXPIRY_DATE] = it } ?: prefs.remove(Keys.EXPIRY_DATE)
            prefs[Keys.IS_AUTO_RENEW] = status.isAutoRenew
        }
    }
}
