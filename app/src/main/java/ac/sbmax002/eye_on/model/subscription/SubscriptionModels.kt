package ac.sbmax002.eye_on.model.subscription

/**
 * 구독 티어 정의
 */
enum class SubscriptionTier(val displayName: String) {
    FREE("Free"),
    PLUS("Plus")
}

/**
 * 구독 기능 (유료 게이팅 대상)
 * 현재는 LLM 음성 대화만 유료 기능으로 제공
 */
enum class SubscriptionFeature {
    /** LLM 음성 대화 — 졸음 감지 시 자동으로 대화 시작 */
    LLM_VOICE
}

/**
 * 요금제 정보 (UI 표시용)
 *
 * @param tier 구독 티어
 * @param monthlyPrice 월 가격 (원), Free=0
 * @param features 포함 기능 설명 목록 (UI에 체크마크로 표시)
 */
data class SubscriptionPlan(
    val tier: SubscriptionTier,
    val monthlyPrice: Int,
    val features: List<String>
)

/**
 * 현재 구독 상태
 *
 * @param currentTier 현재 구독 티어
 * @param isActive 구독 활성 여부
 * @param startDate 구독 시작일 (ISO 8601, e.g. "2026-06-07")
 * @param expiryDate 구독 만료일 (ISO 8601)
 * @param isAutoRenew 자동 갱신 여부 (false면 해지 예정)
 * @param daysRemaining 남은 일수
 */
data class SubscriptionStatus(
    val currentTier: SubscriptionTier = SubscriptionTier.FREE,
    val isActive: Boolean = true,
    val startDate: String? = null,
    val expiryDate: String? = null,
    val isAutoRenew: Boolean = false,
    val daysRemaining: Int? = null
)
