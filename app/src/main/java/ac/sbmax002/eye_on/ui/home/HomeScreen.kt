package ac.sbmax002.eye_on.ui.home


import ac.sbmax002.eye_on.camera.CameraManager
import ac.sbmax002.eye_on.camera.CameraPreview
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ac.sbmax002.eye_on.camera.CameraConfig

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

    //카메라매니저 객체 생성에 필요한 환경 객체와 라이프사이클 객체
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    //카메라매니저 객체 생성
    val cameraManager = remember {
        CameraManager(
            context = context,
            lifecycleOwner = lifecycleOwner,
            config = CameraConfig(),   // 필요하면 나중에 전/후면/해상도 바꿀 수 있음
            onFrameAvailable = { imageProxy ->
                // 프레임이 들어올 때마다 ViewModel로 전달
                viewModel.onFrameFromCamera(
                    imageProxy = imageProxy,
                    isFrontCamera = true   // 지금은 전면 카메라 기준으로 고정
                )
            }
        )
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                onStatisticsClick = onNavigateToStatistics,
                onSettingsClick = onNavigateToSettings
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isMonitoring) {
                MonitoringView(
                    uiState = uiState,
                    onStopMonitoring = { viewModel.stopMonitoring() },
                    cameraManager = cameraManager
                )
            } else {
                ReadyView(
                    uiState = uiState,
                    cameraPermissionGranted = cameraPermissionGranted,
                    onStartMonitoring = { viewModel.startMonitoring() },
                    cameraManager = cameraManager
                )
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
        shadowElevation = 6.dp
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Eye:on Logo",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Eye:on",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = onStatisticsClick) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Statistics",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
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
    cameraManager: CameraManager // 추가
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        StatusIndicator(
            text = "Ready",
            isActive = uiState.isReady
        )

        Spacer(modifier = Modifier.height(32.dp))

        CameraPreviewArea(
            isReady = uiState.isReady,
            isFaceDetected = uiState.isFaceDetected,
            cameraManager = cameraManager // 추가
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ready to Monitor",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Position your face within the frame",
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )

            AnimatedButton(
                onClick = onStartMonitoring,
                enabled = cameraPermissionGranted && uiState.isReady,
                backgroundColor = Color(0xFF2196F3),
                disabledBackgroundColor = Color(0xFF424242),
                text = "Start Monitoring"
            )

            Text(
                text = "• Keep device mounted securely while driving",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MonitoringView(
    uiState: HomeUiState,
    onStopMonitoring: () -> Unit,
    cameraManager: CameraManager // 추가
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        StatusIndicator(
            text = "Monitoring",
            isActive = true,
            isMonitoring = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        CameraPreviewArea(
            isReady = true,
            isFaceDetected = uiState.isFaceDetected,
            cameraManager = cameraManager // 추가
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Monitoring in Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "App will minimize to floating icon",
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )

            AnimatedButton(
                onClick = onStopMonitoring,
                enabled = true,
                backgroundColor = Color(0xFFE53935),
                disabledBackgroundColor = Color(0xFF424242),
                text = "Stop Monitoring"
            )

            Text(
                text = "• Keep device mounted securely while driving",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center
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

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = disabledBackgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        interactionSource = interactionSource
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
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

@Composable
private fun CameraPreviewArea(
    isReady: Boolean,
    isFaceDetected: Boolean,
    cameraManager: CameraManager // 추가
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = if (isReady) {
                        listOf(Color(0xFF2196F3), Color(0xFF1976D2))
                    } else {
                        listOf(Color(0xFF424242), Color(0xFF303030))
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) { // 카메라 준비되면 실행
        if (isReady) {
            CameraPreview(
                cameraManager = cameraManager,
                modifier = Modifier.fillMaxSize()
            )
        } else { // 아니면 기존 ui
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Camera",
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = "Camera Preview",
                    fontSize = 16.sp,
                    color = Color(0xFF757575)
                )

                Text(
                    text = "Align your face within the frame",
                    fontSize = 12.sp,
                    color = Color(0xFF616161),
                    textAlign = TextAlign.Center
                )
            }

            AnimatedVisibility(
                visible = isFaceDetected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Face Detected",
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}