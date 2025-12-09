package ac.sbmax002.eye_on.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ac.sbmax002.eye_on.ui.home.HomeViewModel
import ac.sbmax002.eye_on.ui.home.AppMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    homeViewModel: HomeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: StatisticsViewModel //  "= viewModel()" 제거
) {
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val statsUiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isMonitoring = homeUiState.isMonitoring
    val appMode = statsUiState.appMode

    // 모드에 따른 동적 타이틀 설정
    val title = if (isMonitoring) {
        if (appMode == AppMode.DRIVING) "실시간 운행 현황" else "실시간 학습 현황"
    } else {
        if (appMode == AppMode.DRIVING) "운전 리포트" else "집중 리포트"
    }

    Scaffold(
        topBar = {
            StatisticsTopBar(
                title = title,
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
                // [CASE 1] 모니터링 중 화면
                CurrentSessionView(
                    homeViewModel = homeViewModel,
                    appMode = appMode,
                    onStopClick = {
                        homeViewModel.stopMonitoring()
                        onNavigateBack()
                    }
                )
            } else {
                // [CASE 2] 통계 대시보드 화면
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
    appMode: AppMode,
    onStopClick: () -> Unit
) {
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 1초마다 현재 시간 갱신 (경과 시간 계산용)
    LaunchedEffect(Unit) {
        while(true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    // 경과 시간 계산
    val durationMillis = if (homeUiState.monitoringStartTime > 0) {
        currentTime - homeUiState.monitoringStartTime
    } else 0L

    val hours = durationMillis / 3600000
    val minutes = (durationMillis % 3600000) / 60000
    val durationString = String.format("%d시간 %02d분", hours, minutes)

    // [수정] 시작 시간 포맷팅 (예: "오후 3:45")
    val startTimeStr = remember(homeUiState.monitoringStartTime) {
        if (homeUiState.monitoringStartTime > 0) {
            val date = Date(homeUiState.monitoringStartTime)
            val formatter = SimpleDateFormat("a h:mm", Locale.KOREA) // 오전/오후 표시
            formatter.format(date)
        } else {
            "--:--"
        }
    }

    // 모드별 텍스트 및 색상 설정
    val isDriving = appMode == AppMode.DRIVING
    val mainLabel = if (isDriving) "운행 시간" else "학습 시간"
    val subLabel = if (isDriving) "총 운행 시간" else "총 학습 시간"

    // 운전 모드는 파란색(Blue), 스터디 모드는 주황색(Orange) 테마 사용
    val themeColor = if (isDriving) Color(0xFF2196F3) else Color(0xFFFF9800)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 시간 카드
            DashboardCard(title = mainLabel, icon = Icons.Outlined.AccessTime, iconTint = themeColor) {
                Text("시작 시간", color = Color.Gray, fontSize = 14.sp)
                // [수정] 여기에 실제 포맷팅된 시작 시간을 표시
                Text(startTimeStr, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(16.dp))

                Text(subLabel, color = Color.Gray, fontSize = 14.sp)
                Text(durationString, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            // 감지 기록 카드
            DashboardCard(title = "감지 기록", icon = Icons.Outlined.Visibility, iconTint = themeColor) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if(isDriving) "졸음 감지 횟수" else "집중 저하 횟수", color = Color.Gray)
                    Text("${homeUiState.drowsinessDetectionCount}회", color = Color(0xFFFFC107), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if(isDriving) "수면 경고 횟수" else "자리 비움 경고", color = Color.Gray)
                    Text("0회", color = Color(0xFFE53935), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 이벤트 타임라인
            DashboardCard(title = "이벤트 타임라인", icon = Icons.Default.Warning, iconTint = themeColor) {
                Text("실시간 이벤트 대기 중...", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Button(
            onClick = onStopClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(if(isDriving) "운행 종료" else "학습 종료", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val tabs = viewModel.filters

    // 모드에 따른 UI 분기
    val isDriving = uiState.appMode == AppMode.DRIVING
    val themeColor = if (isDriving) Color(0xFF2196F3) else Color(0xFFFF9800)

    val timeTitle = if (isDriving) "총 운행 시간" else "총 학습 시간"
    val sessionLabel = if (isDriving) "운전 모드" else "스터디 모드"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp)
    ) {
        // 1. 상단 탭 (주간/월간/전체)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = uiState.selectedFilter == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) themeColor else Color.Transparent)
                        .clickable { viewModel.updateFilter(tab) },
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

        // 2. 총 시간 카드
        DashboardCard(title = timeTitle, icon = Icons.Outlined.AccessTime, iconTint = themeColor) {
            val h = uiState.totalDrivingMinutes / 60
            val m = uiState.totalDrivingMinutes % 60

            Text(
                text = "${h}시간 ${m}분",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))

            val periodText = when(uiState.selectedFilter) {
                "주간" -> "최근 7일"
                "월간" -> "최근 30일"
                else -> "전체 기간"
            }
            Text(text = periodText, color = Color.Gray, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 총 세션 수 카드
        DashboardCard(title = "총 세션 수", icon = Icons.Default.CalendarToday, iconTint = themeColor) {
            Text(
                text = "${uiState.totalSessions}",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = sessionLabel, color = Color.Gray, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 졸음/집중 감지 현황 카드
        DashboardCard(title = if(isDriving) "졸음 감지" else "집중 저하 감지", icon = Icons.Outlined.Visibility, iconTint = themeColor) {
            val totalAlerts = (uiState.level1Total + uiState.level2Total).coerceAtLeast(1)
            val lvl1Ratio = uiState.level1Total.toFloat() / totalAlerts
            val lvl2Ratio = uiState.level2Total.toFloat() / totalAlerts

            val safeLvl1Ratio = if (uiState.level1Total == 0) 0f else lvl1Ratio
            val safeLvl2Ratio = if (uiState.level2Total == 0) 0f else lvl2Ratio

            // Level 1 경고
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(if(isDriving) "졸음 감지 횟수" else "주의 산만", color = Color.Gray, fontSize = 14.sp)
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

            // Level 2 경고
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(if(isDriving) "수면 경고 횟수" else "자리 비움", color = Color.Gray, fontSize = 14.sp)
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

        // 5. 시간대별 빈도
        DashboardCard(title = if(isDriving) "시간대별 졸음 빈도" else "시간대별 집중 저하", icon = null, iconTint = null) {
            val maxCount = uiState.timeDistribution.maxOrNull()?.coerceAtLeast(1) ?: 1

            TimeFrequencyRow(label = "오전 (06:00-12:00)", count = uiState.timeDistribution[0], max = maxCount, color = themeColor)
            Spacer(modifier = Modifier.height(16.dp))
            TimeFrequencyRow(label = "오후 (12:00-18:00)", count = uiState.timeDistribution[1], max = maxCount, color = Color(0xFFFFC107))
            Spacer(modifier = Modifier.height(16.dp))
            TimeFrequencyRow(label = "저녁 (18:00-24:00)", count = uiState.timeDistribution[2], max = maxCount, color = Color(0xFFE53935))
            Spacer(modifier = Modifier.height(16.dp))
            TimeFrequencyRow(label = "새벽 (00:00-06:00)", count = uiState.timeDistribution[3], max = maxCount, color = themeColor)
        }
    }
}

// [UI Components] 공통 카드 디자인
@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector?,
    iconTint: Color? = Color(0xFF2196F3),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (icon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint ?: Color.White,
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