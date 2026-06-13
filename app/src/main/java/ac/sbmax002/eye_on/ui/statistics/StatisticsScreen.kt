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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ac.sbmax002.eye_on.model.statistics.DrivingSession
import ac.sbmax002.eye_on.model.statistics.SessionEvent
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
        monitoringTitle(appMode)
    } else {
        reportTitle(appMode)
    }

    Scaffold(
        topBar = {
            StatisticsTopBar(
                title = title,
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                    viewModel = viewModel,
                    onNavigateToDetail = onNavigateToDetail
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
    val mainLabel = currentSessionMainLabel(appMode)
    val subLabel = currentSessionSubLabel(appMode)
    val themeColor = modeThemeColor(appMode)
    val events = homeUiState.sessionEvents

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        // verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 시간 카드
            DashboardCard(title = mainLabel, icon = Icons.Outlined.AccessTime, iconTint = themeColor) {
                Text("시작 시간", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                // [수정] 여기에 실제 포맷팅된 시작 시간을 표시
                Text(startTimeStr, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(16.dp))

                Text(subLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text(durationString, color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            // 감지 기록 카드
            DashboardCard(title = "감지 기록", icon = Icons.Outlined.Visibility, iconTint = themeColor) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(level1CountLabel(appMode), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${homeUiState.drowsinessDetectionCount}회", color = Color(0xFFFFC107), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(level2CountLabel(appMode), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${homeUiState.sleepDetectionCount}회", color = Color(0xFFE53935), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 이벤트 타임라인
            DashboardCard(title = "이벤트 타임라인", icon = Icons.Default.Warning, iconTint = themeColor) {
                if (events.isEmpty()) {
                    Text("실시간 이벤트 대기 중...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                } else {
                    // DetailScreen.kt에 있는 TimelineItem 재사용
                    // Column으로 감싸서 리스트 표시
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        events.forEachIndexed { index, event ->
                            TimelineItem(
                                event = event,
                                isLast = index == events.lastIndex
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onStopClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stopButtonLabel(appMode), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ================================================================
// 2. HistoryListView (데이터 연동 완료)
// ================================================================
@Composable
fun HistoryListView(
    viewModel: StatisticsViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val tabs = viewModel.filters
    val sessions = remember(uiState.sessions) {
        uiState.sessions.sortedByDescending { it.rawDateTime }
    }

    // 모드에 따른 UI 분기
    val themeColor = modeThemeColor(uiState.appMode)
    val timeTitle = historyTotalTimeTitle(uiState.appMode)
    val sessionLabel = sessionModeLabel(uiState.appMode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp)
    ) {
        SectionTitle(text = "전체 세션 통계")
        Spacer(modifier = Modifier.height(12.dp))

        // 1. 상단 탭 (주간/월간/전체)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = uiState.selectedFilter == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) themeColor else Color.Transparent)
                        .clickable { viewModel.updateFilter(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))

            val periodText = when(uiState.selectedFilter) {
                "주간" -> "최근 7일"
                "월간" -> "최근 30일"
                else -> "전체 기간"
            }
            Text(text = periodText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 총 세션 수 카드
        DashboardCard(title = "총 세션 수", icon = Icons.Default.CalendarToday, iconTint = themeColor) {
            Text(
                text = "${uiState.totalSessions}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = sessionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 졸음/집중 감지 현황 카드
        DashboardCard(title = detectionSummaryTitle(uiState.appMode), icon = Icons.Outlined.Visibility, iconTint = themeColor) {
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
                Text(level1CountLabel(uiState.appMode), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text("${uiState.level1Total}회", color = Color(0xFFFFC107), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { safeLvl1Ratio },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFFFC107),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Level 2 경고
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(level2CountLabel(uiState.appMode), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text("${uiState.level2Total}회", color = Color(0xFFE53935), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { safeLvl2Ratio },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFE53935),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. 시간대별 빈도
        DashboardCard(title = timeFrequencyTitle(uiState.appMode), icon = null, iconTint = null) {
            val maxCount = uiState.timeDistribution.maxOrNull()?.coerceAtLeast(1) ?: 1

            TimeFrequencyRow(label = "오전 (06:00-12:00)", count = uiState.timeDistribution[0], max = maxCount, color = themeColor)
            Spacer(modifier = Modifier.height(16.dp))
            TimeFrequencyRow(label = "오후 (12:00-18:00)", count = uiState.timeDistribution[1], max = maxCount, color = themeColor)
            Spacer(modifier = Modifier.height(16.dp))
            TimeFrequencyRow(label = "저녁 (18:00-24:00)", count = uiState.timeDistribution[2], max = maxCount, color = themeColor)
            Spacer(modifier = Modifier.height(16.dp))
            TimeFrequencyRow(label = "새벽 (00:00-06:00)", count = uiState.timeDistribution[3], max = maxCount, color = themeColor)
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle(text = "세션별 기록")
        Spacer(modifier = Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            Text(
                text = "표시할 세션이 없습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sessions.forEach { session ->
                    SessionListItem(
                        session = session,
                        appMode = uiState.appMode,
                        accentColor = themeColor,
                        onClick = { onNavigateToDetail(session.id) }
                    )
                }
            }
        }
    }
}

private fun modeThemeColor(mode: AppMode): Color = when (mode) {
    AppMode.DRIVING -> Color(0xFF2196F3)
    AppMode.STUDY -> Color(0xFFFF9800)
    AppMode.ORGANIZATION -> Color(0xFF00A86B)
}

private fun monitoringTitle(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "실시간 운행 현황"
    AppMode.STUDY -> "실시간 학습 현황"
    AppMode.ORGANIZATION -> "실시간 조직 모니터링"
}

private fun reportTitle(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "운전 리포트"
    AppMode.STUDY -> "집중 리포트"
    AppMode.ORGANIZATION -> "조직 리포트"
}

private fun currentSessionMainLabel(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "운행 시간"
    AppMode.STUDY -> "학습 시간"
    AppMode.ORGANIZATION -> "모니터링 시간"
}

private fun currentSessionSubLabel(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "총 운행 시간"
    AppMode.STUDY -> "총 학습 시간"
    AppMode.ORGANIZATION -> "총 모니터링 시간"
}

private fun historyTotalTimeTitle(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "총 운행 시간"
    AppMode.STUDY -> "총 학습 시간"
    AppMode.ORGANIZATION -> "총 모니터링 시간"
}

private fun detectionSummaryTitle(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "졸음 감지"
    AppMode.STUDY -> "집중 저하 감지"
    AppMode.ORGANIZATION -> "이상 상태 감지"
}

private fun level1CountLabel(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "졸음 감지 횟수"
    AppMode.STUDY -> "집중 저하 횟수"
    AppMode.ORGANIZATION -> "주의 알림 횟수"
}

private fun level2CountLabel(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "수면 경고 횟수"
    AppMode.STUDY -> "자리 비움 경고"
    AppMode.ORGANIZATION -> "긴급 경고 횟수"
}

private fun level1ShortLabel(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "졸음"
    AppMode.STUDY -> "주의"
    AppMode.ORGANIZATION -> "주의"
}

private fun level2ShortLabel(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "수면"
    AppMode.STUDY -> "자리"
    AppMode.ORGANIZATION -> "긴급"
}

private fun timeFrequencyTitle(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "시간대별 졸음 빈도"
    AppMode.STUDY -> "시간대별 집중 저하"
    AppMode.ORGANIZATION -> "시간대별 이상 빈도"
}

private fun sessionModeLabel(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "운전 모드"
    AppMode.STUDY -> "스터디 모드"
    AppMode.ORGANIZATION -> "조직 모드"
}

private fun stopButtonLabel(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "운행 종료"
    AppMode.STUDY -> "학습 종료"
    AppMode.ORGANIZATION -> "모니터링 종료"
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (icon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint ?: MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SessionListItem(
    session: DrivingSession,
    appMode: AppMode,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(session.dateStr, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${session.time}부터 • ${session.durationStr} 동안", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                Text(
                    text = sessionModeLabel(appMode),
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            //Text(session.location, color = Color(0xFFBDBDBD), fontSize = 13.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlertBadge(text = level1ShortLabel(appMode), value = session.level1Alerts, color = Color(0xFFFFC107))
                    AlertBadge(text = level2ShortLabel(appMode), value = session.level2Alerts, color = Color(0xFFE53935))
                }

                Text(
                    text = "자세히 보기",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AlertBadge(text: String, value: Int, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text("$text ${value}회", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
}

// [UI Components] 시간대별 빈도 Row
@Composable
fun TimeFrequencyRow(label: String, count: Int, max: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text("${count}회", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { count.toFloat() / max.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsTopBar(title: String, onBackClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}
