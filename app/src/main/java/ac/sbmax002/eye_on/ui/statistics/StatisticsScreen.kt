package ac.sbmax002.eye_on.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ac.sbmax002.eye_on.ui.home.HomeViewModel

@Composable
fun StatisticsScreen(
    homeViewModel: HomeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: StatisticsViewModel = viewModel()
) {
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val isMonitoring = homeUiState.isMonitoring

    Scaffold(
        topBar = {
            StatisticsTopBar(
                title = if (isMonitoring) "실시간 운행 현황" else "Statistics",
                onBackClick = onNavigateBack
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isMonitoring) {
                // [CASE 1] 모니터링 중
                CurrentSessionView(
                    homeViewModel = homeViewModel,
                    onStopClick = {
                        homeViewModel.stopMonitoring()
                        onNavigateBack()
                    }
                )
            } else {
                // [CASE 2] 통계 대시보드 (데이터 연동 완료)
                HistoryListView(
                    viewModel = viewModel
                )
            }
        }
    }
}

// ================================================================
// 1. CurrentSessionView (실시간 화면)
// ================================================================
@Composable
fun CurrentSessionView(
    homeViewModel: HomeViewModel,
    onStopClick: () -> Unit
) {
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while(true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val durationMillis = if (homeUiState.monitoringStartTime > 0) {
        currentTime - homeUiState.monitoringStartTime
    } else 0L

    val hours = durationMillis / 3600000
    val minutes = (durationMillis % 3600000) / 60000
    val durationString = String.format("%d시간 %02d분", hours, minutes)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard(title = "운행 시간", icon = Icons.Outlined.AccessTime) {
                Text("시작 시간", color = Color.Gray, fontSize = 14.sp)
                Text("모니터링 중...", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("총 운행 시간", color = Color.Gray, fontSize = 14.sp)
                Text(durationString, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            DashboardCard(title = "감지 기록", icon = Icons.Outlined.Visibility) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("졸음 감지 횟수", color = Color.Gray)
                    Text("${homeUiState.drowsinessDetectionCount}회", color = Color(0xFFFFC107), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("수면 경고 횟수", color = Color.Gray)
                    Text("0회", color = Color(0xFFE53935), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            DashboardCard(title = "이벤트 타임라인", icon = Icons.Default.Warning) {
                Text("실시간 이벤트 대기 중...", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Button(
            onClick = onStopClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("모니터링 중단", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ================================================================
// 2. HistoryListView (데이터 연동 완료)
// ================================================================
@Composable
fun HistoryListView(
    viewModel: StatisticsViewModel
) {
    // 뷰모델 상태 구독 (여기가 핵심!)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // 뷰모델의 필터 리스트 사용
    val tabs = viewModel.filters

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp)
    ) {
        // 1. 상단 탭 (ViewModel 연동)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = uiState.selectedFilter == tab // 뷰모델 상태와 비교
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFF2196F3) else Color.Transparent)
                        .clickable { viewModel.updateFilter(tab) }, // [중요] 클릭 시 뷰모델 업데이트 호출
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 총 운행 시간 카드 (동적 데이터)
        DashboardCard(title = "총 운행 시간", icon = Icons.Outlined.AccessTime) {
            // 시간 계산 헬퍼 사용 (분 -> 시간/분)
            val h = uiState.totalDrivingMinutes / 60
            val m = uiState.totalDrivingMinutes % 60

            Text(
                text = "${h}시간 ${m}분",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))

            // 필터에 따른 텍스트 변경
            val periodText = when(uiState.selectedFilter) {
                "주간" -> "최근 7일"
                "월간" -> "최근 30일"
                else -> "전체 기간"
            }
            Text(
                text = periodText,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 총 세션 수 카드 (동적 데이터)
        DashboardCard(title = "총 세션 수", icon = Icons.Default.CalendarToday) {
            Text(
                text = "${uiState.totalSessions}",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "운전 모드",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 졸음 감지 현황 카드 (동적 데이터)
        DashboardCard(title = "졸음 감지", icon = Icons.Outlined.Visibility) {
            // 졸음 감지 (Yellow)
            val totalAlerts = (uiState.level1Total + uiState.level2Total).coerceAtLeast(1) // 0으로 나누기 방지
            val lvl1Ratio = uiState.level1Total.toFloat() / totalAlerts
            val lvl2Ratio = uiState.level2Total.toFloat() / totalAlerts

            // 총 감지 수가 0일 때 그래프가 꽉 차지 않도록 처리
            val safeLvl1Ratio = if (uiState.level1Total == 0) 0f else lvl1Ratio
            val safeLvl2Ratio = if (uiState.level2Total == 0) 0f else lvl2Ratio

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("총 감지 횟수", color = Color.Gray, fontSize = 14.sp)
                Text("${uiState.level1Total}회", color = Color(0xFFFFC107), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { safeLvl1Ratio },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFFFC107),
                trackColor = Color(0xFF333333),
                strokeCap = StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 수면 경고 (Red)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("수면 경고 횟수", color = Color.Gray, fontSize = 14.sp)
                Text("${uiState.level2Total}회", color = Color(0xFFE53935), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { safeLvl2Ratio },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFE53935),
                trackColor = Color(0xFF333333),
                strokeCap = StrokeCap.Round,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. 시간대별 졸음 빈도 (동적 데이터)
        DashboardCard(title = "시간대별 졸음 빈도", icon = null) {
            // 그래프 최대값 계산 (가장 큰 막대를 기준으로 비율 정함)
            val maxCount = uiState.timeDistribution.maxOrNull()?.coerceAtLeast(1) ?: 1

            TimeFrequencyRow(label = "오전 (06:00-12:00)", count = uiState.timeDistribution[0], max = maxCount, color = Color(0xFF2196F3))
            Spacer(modifier = Modifier.height(16.dp))
            TimeFrequencyRow(label = "오후 (12:00-18:00)", count = uiState.timeDistribution[1], max = maxCount, color = Color(0xFFFFC107))
            Spacer(modifier = Modifier.height(16.dp))
            TimeFrequencyRow(label = "저녁 (18:00-24:00)", count = uiState.timeDistribution[2], max = maxCount, color = Color(0xFFE53935))
            Spacer(modifier = Modifier.height(16.dp))
            TimeFrequencyRow(label = "새벽 (00:00-06:00)", count = uiState.timeDistribution[3], max = maxCount, color = Color(0xFF2196F3))
        }
    }
}

// [UI Components] 공통 카드 디자인
@Composable
fun DashboardCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 헤더 (아이콘 + 제목)
            if (icon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

// [UI Components] 시간대별 빈도 Row
@Composable
fun TimeFrequencyRow(label: String, count: Int, max: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.Gray, fontSize = 13.sp)
            Text("${count}회", color = Color.White, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { count.toFloat() / max.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF333333),
            strokeCap = StrokeCap.Round,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsTopBar(title: String, onBackClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
    )
}

