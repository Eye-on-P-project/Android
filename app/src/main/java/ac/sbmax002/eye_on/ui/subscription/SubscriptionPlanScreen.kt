package ac.sbmax002.eye_on.ui.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ac.sbmax002.eye_on.model.subscription.SubscriptionPlan
import ac.sbmax002.eye_on.model.subscription.SubscriptionTier

/**
 * 요금제 선택/결제 화면
 *
 * Free / Plus 요금제를 비교하고 구독할 수 있습니다.
 * 현재 Mock 처리로, 구독 버튼 클릭 시 즉시 활성화됩니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlanScreen(
    viewModel: SubscriptionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTier = uiState.subscriptionStatus.currentTier

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "요금제",
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
            // 헤더
            Text(
                text = "나에게 맞는 플랜을 선택하세요",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = "언제든 변경하거나 해지할 수 있습니다",
                color = Color(0xFF99A1AF),
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 요금제 카드들
            uiState.plans.forEach { plan ->
                PlanCard(
                    plan = plan,
                    isCurrentPlan = plan.tier == currentTier,
                    isLoading = uiState.isLoading,
                    onSubscribe = {
                        if (plan.tier != SubscriptionTier.FREE) {
                            viewModel.subscribe(plan.tier)
                        }
                    }
                )
            }

            // 안내 문구
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2A2A2A)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 안내사항",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• 구독은 월 단위로 자동 갱신됩니다\n" +
                                "• 해지 시 결제 기간 종료까지 서비스 이용 가능\n" +
                                "• 요금제 변경은 즉시 적용됩니다",
                        color = Color(0xFF99A1AF),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 구독 성공 다이얼로그
    if (uiState.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessDialog() },
            title = {
                Text(
                    text = "🎉 구독 완료!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Plus 플랜이 활성화되었습니다.\nLLM 음성 대화 기능을 이용할 수 있습니다.",
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissSuccessDialog()
                    onNavigateBack()
                }) {
                    Text("확인", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 에러 다이얼로그
    uiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = {
                Text(
                    text = "오류",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = error,
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("확인", color = Color(0xFF007AFF))
                }
            },
            containerColor = Color(0xFF2A2A2A),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * 요금제 카드
 */
@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isCurrentPlan: Boolean,
    isLoading: Boolean,
    onSubscribe: () -> Unit
) {
    val isPlus = plan.tier == SubscriptionTier.PLUS
    val borderColor = when {
        isCurrentPlan -> Color(0xFF30D158)
        isPlus -> Color(0xFF007AFF)
        else -> Color.Transparent
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2A2A2A),
        border = BorderStroke(
            width = if (isCurrentPlan || isPlus) 2.dp else 0.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 헤더: 이름 + 뱃지
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPlus) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD60A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = plan.tier.displayName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isCurrentPlan) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF30D158).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "현재 플랜",
                            color = Color(0xFF30D158),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 가격
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (plan.monthlyPrice == 0) "무료" else "₩${"%,d".format(plan.monthlyPrice)}",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                if (plan.monthlyPrice > 0) {
                    Text(
                        text = "/ 월",
                        color = Color(0xFF99A1AF),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // 구분선
            HorizontalDivider(
                color = Color(0xFF3A3A3A),
                thickness = 1.dp
            )

            // 기능 목록
            plan.features.forEach { feature ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isPlus) Color(0xFF007AFF) else Color(0xFF30D158),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = feature,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            // 버튼
            if (!isCurrentPlan && isPlus) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onSubscribe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "구독하기",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
