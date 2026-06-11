package ac.sbmax002.eye_on.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ac.sbmax002.eye_on.model.subscription.SubscriptionTier

/**
 * 구독 상태 확인 화면
 *
 * Free / Plus 상태에 따라 다른 UI를 표시합니다.
 * - Free: Plus 업그레이드 유도
 * - Plus (자동갱신 ON): 구독 정보 + 해지 버튼
 * - Plus (자동갱신 OFF): 해지 예정 안내 + 복원 버튼
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionStatusScreen(
    viewModel: SubscriptionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlan: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val status = uiState.subscriptionStatus

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "구독 관리",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                )
            )
        },
        containerColor = Color(0xFF1A1A1A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 현재 플랜 카드
            CurrentPlanCard(
                tier = status.currentTier,
                isActive = status.isActive,
                isAutoRenew = status.isAutoRenew
            )

            // 상태별 상세 정보
            when {
                status.currentTier == SubscriptionTier.FREE -> {
                    // Free 유저: Plus 업그레이드 유도
                    PlusUpgradeCard(onUpgradeClick = onNavigateToPlan)
                }
                status.isAutoRenew -> {
                    // Plus 구독 중 (자동갱신 ON)
                    SubscriptionDetailCard(
                        startDate = status.startDate,
                        expiryDate = status.expiryDate,
                        daysRemaining = status.daysRemaining,
                        isAutoRenew = true
                    )

                    // 만료 임박 경고 (3일 이하)
                    status.daysRemaining?.let { days ->
                        if (days <= 3) {
                            ExpiryWarningBanner(daysRemaining = days)
                        }
                    }

                    // 액션 버튼들
                    Button(
                        onClick = onNavigateToPlan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        )
                    ) {
                        Text("요금제 변경", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.showCancelDialog() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF3B30)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = Brush.linearGradient(listOf(Color(0xFFFF3B30), Color(0xFFFF3B30)))
                        )
                    ) {
                        Text("구독 해지", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    // 해지 예정 (자동갱신 OFF)
                    SubscriptionDetailCard(
                        startDate = status.startDate,
                        expiryDate = status.expiryDate,
                        daysRemaining = status.daysRemaining,
                        isAutoRenew = false
                    )

                    // 해지 예정 안내
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF3A2A00)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠️ 해지 예정",
                                color = Color(0xFFFF9F0A),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "현재 결제 기간이 끝나면 Free 플랜으로 전환됩니다.\n" +
                                        "LLM 음성 대화 기능을 계속 이용하려면 구독을 복원하세요.",
                                color = Color(0xFFCCCCCC),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.restoreSubscription() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("구독 복원", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 해지 확인 다이얼로그
    if (uiState.showCancelDialog) {
        CancelConfirmDialog(
            expiryDate = status.expiryDate,
            isLoading = uiState.isLoading,
            onConfirm = { viewModel.cancelSubscription() },
            onDismiss = { viewModel.dismissCancelDialog() }
        )
    }
}

/**
 * 현재 플랜 카드
 */
@Composable
private fun CurrentPlanCard(
    tier: SubscriptionTier,
    isActive: Boolean,
    isAutoRenew: Boolean
) {
    val gradientColors = when (tier) {
        SubscriptionTier.PLUS -> listOf(Color(0xFF667EEA), Color(0xFF764BA2))
        SubscriptionTier.FREE -> listOf(Color(0xFF2A2A2A), Color(0xFF3A3A3A))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(gradientColors))
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "현재 플랜",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )

                    // 상태 뱃지
                    val badgeText = when {
                        tier == SubscriptionTier.FREE -> "무료"
                        !isAutoRenew -> "해지 예정"
                        isActive -> "활성"
                        else -> "만료"
                    }
                    val badgeColor = when {
                        tier == SubscriptionTier.FREE -> Color(0xFF99A1AF)
                        !isAutoRenew -> Color(0xFFFF9F0A)
                        isActive -> Color(0xFF30D158)
                        else -> Color(0xFFFF3B30)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (tier == SubscriptionTier.PLUS) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD60A),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = tier.displayName,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (tier == SubscriptionTier.PLUS) {
                    Text(
                        text = "₩4,900 / 월",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Plus 업그레이드 유도 카드 (Free 유저용)
 */
@Composable
private fun PlusUpgradeCard(onUpgradeClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2A2A2A)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "✨ Plus로 업그레이드",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "LLM 음성 대화 기능으로 졸음 감지 시\n자동으로 대화를 시작하여 졸음을 예방하세요.",
                color = Color(0xFF99A1AF),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            // 혜택 목록
            val benefits = listOf(
                "LLM 음성 대화 (졸음 감지 시 자동 대화)",
                "AI 기반 맞춤형 졸음 예방"
            )
            benefits.forEach { benefit ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = benefit,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onUpgradeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF)
                )
            ) {
                Text(
                    text = "요금제 보기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 구독 상세 정보 카드
 */
@Composable
private fun SubscriptionDetailCard(
    startDate: String?,
    expiryDate: String?,
    daysRemaining: Int?,
    isAutoRenew: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2A2A2A)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "구독 정보",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            DetailRow(label = "구독 시작일", value = formatDate(startDate))
            DetailRow(label = "만료일", value = formatDate(expiryDate))
            DetailRow(
                label = "자동 갱신",
                value = if (isAutoRenew) "ON" else "OFF",
                valueColor = if (isAutoRenew) Color(0xFF30D158) else Color(0xFFFF3B30)
            )

            // 남은 일수 프로그레스 바
            daysRemaining?.let { days ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "남은 기간",
                            color = Color(0xFF99A1AF),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${days}일",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (days / 30f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when {
                            days <= 3 -> Color(0xFFFF3B30)
                            days <= 7 -> Color(0xFFFF9F0A)
                            else -> Color(0xFF007AFF)
                        },
                        trackColor = Color(0xFF1A1A1A)
                    )
                }
            }
        }
    }
}

/**
 * 상세 정보 행
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF99A1AF),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 만료 임박 경고 배너
 */
@Composable
private fun ExpiryWarningBanner(daysRemaining: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF3A1A1A)
    ) {
        Text(
            text = "⚠️ 구독이 ${daysRemaining}일 후 만료됩니다",
            color = Color(0xFFFF3B30),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 구독 해지 확인 다이얼로그
 */
@Composable
private fun CancelConfirmDialog(
    expiryDate: String?,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "구독 해지",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "정말로 구독을 해지하시겠습니까?",
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp
                )
                Text(
                    text = "해지 시 잃게 되는 혜택:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• LLM 음성 대화 (졸음 감지 시 자동 대화)\n• AI 기반 맞춤형 졸음 예방",
                    color = Color(0xFFFF3B30),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                expiryDate?.let {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A1A1A)
                    ) {
                        Text(
                            text = "📅 ${formatDate(it)}까지 서비스 이용 가능",
                            color = Color(0xFF99A1AF),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFFFF3B30),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("해지하기", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("유지하기", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF2A2A2A),
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * 날짜 문자열 포맷팅 (ISO 8601 → 표시용)
 */
private fun formatDate(dateStr: String?): String {
    if (dateStr == null) return "-"
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            "${parts[0]}년 ${parts[1].toInt()}월 ${parts[2].toInt()}일"
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}
