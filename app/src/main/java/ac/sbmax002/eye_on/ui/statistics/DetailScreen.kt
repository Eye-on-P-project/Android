package ac.sbmax002.eye_on.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.* // ★ 수정: runtime.* 로 변경 (getValue, setValue, LaunchedEffect 등 포함)
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ac.sbmax002.eye_on.model.statistics.SessionEvent

@Composable
fun DetailScreen(
    sessionId: String,
    viewModel: StatisticsViewModel,
    onNavigateBack: () -> Unit
) {
    // 1. 세션 기본 정보 가져오기 (메모리 캐시 or 리스트에서 조회)
    val session = remember(sessionId) { viewModel.getSessionById(sessionId) }

    // 2. ★ 수정: 이벤트 리스트는 DB에서 비동기로 가져와야 합니다.
    var events by remember { mutableStateOf<List<SessionEvent>>(emptyList()) }

    // 화면이 진입할 때(sessionId가 변경될 때) DB에서 이벤트를 불러옴
    LaunchedEffect(sessionId) {
        events = viewModel.getSessionEvents(sessionId)
    }

    Scaffold(
        topBar = {
            SessionDetailTopBar(onNavigateBack)
        },
        containerColor = Color(0xFF0A0A0A)
    ) { paddingValues ->
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Session not found", color = Color.White)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                // 1. Header (날짜, 시간)
                Text(
                    text = session.dateStr,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${session.time} • ${session.durationStr}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 2. Timeline Title
                Text(
                    text = "Alert Timeline",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 3. Timeline List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // ★ 수정: session.events가 아니라 로드된 events 상태 변수 사용
                    itemsIndexed(events) { index, event ->
                        TimelineItem(
                            event = event,
                            isLast = index == events.lastIndex
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Details",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
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

@Composable
fun TimelineItem(event: SessionEvent, isLast: Boolean) {
    val color = if (event.level == 2) Color(0xFFE53935) else Color(0xFFFFC107)

    IntrinsicHeightRow {
        // [Left] Timeline Line & Dot
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Vertical Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(top = 16.dp)
                        .background(Color(0xFF333333))
                )
            }

            // Dot
            Box(
                modifier = Modifier
                    .padding(top = 26.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // [Right] Content Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = if (event.level == 2) Icons.Outlined.ErrorOutline else Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = event.time,
                            color = color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = event.duration,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.message,
                        color = Color(0xFFE0E0E0),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun IntrinsicHeightRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        content = content
    )
}