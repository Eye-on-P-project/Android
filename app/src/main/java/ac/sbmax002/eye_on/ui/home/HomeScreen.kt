package ac.sbmax002.eye_on.ui.home


import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermissionGranted by viewModel.cameraPermissionGranted.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            HomeTopBar(
                onStatisticsClick = onNavigateToStatistics,
                onSettingsClick = onNavigateToSettings
            )
        },
        containerColor = Color(0xFF1A1A1A)
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isCameraReady = cameraPermissionGranted && uiState.isReady
            
            // UI 레이어 먼저 렌더링 (레이아웃 계산을 위해)
            if (uiState.isMonitoring) {
                MonitoringView(
                    uiState = uiState,
                    onStopMonitoring = { viewModel.stopMonitoring() },
                    isCameraReady = isCameraReady
                )
            } else {
                ReadyView(
                    uiState = uiState,
                    cameraPermissionGranted = cameraPermissionGranted,
                    onStartMonitoring = { viewModel.startMonitoring() },
                    onModeSelected = { mode -> viewModel.selectMode(mode) },
                    isCameraReady = isCameraReady
                )
            }
            
            // 카메라 프리뷰를 절대 위치로 배치 (UI 요소들과 겹치지 않도록)
            // key를 사용하여 뷰 전환 시에도 인스턴스 유지
            key("shared_camera_preview") {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    CameraPreviewContainer(
                        isReady = isCameraReady,
                        isFaceDetected = uiState.isFaceDetected,
                        onFaceDetectionChanged = { detected ->
                            viewModel.updateFaceDetection(detected)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onStatisticsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF007AFF),
                                        Color(0xFF0051D5)
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 로고 아이콘 (나중에 실제 로고로 교체 가능)
                    }
                    Text(
                        text = "Eye:on",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.45).sp
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = onStatisticsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2A2A2A),
                                    Color(0xFF1F1F1F)
                                )
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Statistics",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2A2A2A),
                                    Color(0xFF1F1F1F)
                                )
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}


@Composable
private fun ReadyView(
    uiState: HomeUiState,
    cameraPermissionGranted: Boolean,
    onStartMonitoring: () -> Unit,
    onModeSelected: (AppMode) -> Unit,
    isCameraReady: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 상단: 모드 선택기
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                initialOffsetY = { -20 },
                animationSpec = tween(300)
            ),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            ModeSelector(
                selectedMode = uiState.appMode,
                onModeSelected = onModeSelected,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 중앙: 카메라 프리뷰는 HomeScreen에서 관리하므로 여기서는 공간만 차지
        // 실제 카메라는 HomeScreen 레벨에서 렌더링됨
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f))

        Spacer(modifier = Modifier.weight(1f))

        // 하단: 시작 버튼
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AnimatedButton(
                onClick = {
                    android.util.Log.d("HomeScreen", "Start monitoring button clicked")
                    onStartMonitoring()
                },
                // TODO: MediaPipe 연결 후 isFaceDetected 조건 다시 추가
                enabled = cameraPermissionGranted && uiState.isReady, // && uiState.isFaceDetected,
                backgroundColor = Color(0xFF007AFF),
                disabledBackgroundColor = Color(0xFF424242),
                text = "모니터링 시작",
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "• Keep device mounted securely while driving",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun MonitoringView(
    uiState: HomeUiState,
    onStopMonitoring: () -> Unit,
    isCameraReady: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // 중앙: 카메라 프리뷰는 HomeScreen에서 관리하므로 여기서는 공간만 차지
        // 실제 카메라는 HomeScreen 레벨에서 렌더링됨
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f))

        Spacer(modifier = Modifier.weight(1f))

        // 하단: 안내 텍스트 및 중단 버튼
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Monitoring in Progress",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.44).sp
                )

                Text(
                    text = "App will minimize to floating icon",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF99A1AF),
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.15).sp
                )
            }

            AnimatedButton(
                onClick = onStopMonitoring,
                enabled = true,
                backgroundColor = Color(0xFFE53935),
                disabledBackgroundColor = Color(0xFF424242),
                text = "모니터링 종료",
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "• Keep device mounted securely while driving",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun AnimatedButton(
    onClick: () -> Unit,
    enabled: Boolean,
    backgroundColor: Color,
    disabledBackgroundColor: Color,
    text: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )

    val buttonBackground = if (enabled) {
        Brush.linearGradient(
            colors = listOf(
                backgroundColor,
                backgroundColor.copy(alpha = 0.9f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                disabledBackgroundColor,
                disabledBackgroundColor.copy(alpha = 0.9f)
            )
        )
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = buttonBackground,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                letterSpacing = (-0.45).sp
            )
        }
    }
}

@Composable
private fun StatusIndicator(
    text: String,
    isActive: Boolean,
    isMonitoring: Boolean = false
) {
    val activeColor = if (isMonitoring) Color(0xFFE53935) else Color(0xFF2196F3)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isActive) activeColor else Color(0xFF757575)
                )
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isActive) activeColor else Color(0xFF757575),
            fontWeight = FontWeight.Medium
        )
    }
}

