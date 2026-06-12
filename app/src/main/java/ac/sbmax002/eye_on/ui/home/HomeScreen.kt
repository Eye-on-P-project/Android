package ac.sbmax002.eye_on.ui.home


import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AlarmOff
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
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import ac.sbmax002.eye_on.service.MonitoringService
import ac.sbmax002.eye_on.DTO.DrowsinessState
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    monitoringService: MonitoringService?,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermissionGranted by viewModel.cameraPermissionGranted.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun createSpeechRecognitionIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "AI 동승자에게 답변해 주세요")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    }

    val speechRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

        val recognizedText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()

        if (recognizedText.isNullOrBlank()) {
            Toast.makeText(context, "음성을 인식하지 못했어요.", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        monitoringService?.askAgent(recognizedText)
            ?: Toast.makeText(context, "모니터링 서비스가 아직 연결되지 않았어요.", Toast.LENGTH_SHORT).show()
    }

    fun launchSpeechRecognition() {
        try {
            speechRecognitionLauncher.launch(createSpeechRecognitionIntent())
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "사용 가능한 음성 인식 앱이 없어요.", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchSpeechRecognition()
        } else {
            Toast.makeText(context, "마이크 권한이 필요해요.", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestAgentReply() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            launchSpeechRecognition()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

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
            
            if (uiState.isMonitoring) {
                // 모니터링 중일 때: 프리뷰는 위에, 버튼은 아래에
                
                // 카메라 프리뷰를 Top bar 밑에 배치
                key("shared_camera_preview") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        CameraPreviewContainer(
                            isReady = isCameraReady,
                            isFaceDetected = uiState.isFaceDetected,
                            isMonitoring = uiState.isMonitoring,
                            monitoringService = monitoringService,
                            onFaceDetectionChanged = { detected ->
                                viewModel.updateFaceDetection(detected)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // MonitoringView를 하단에 배치
                MonitoringView(
                    uiState = uiState,
                    onStopMonitoring = {
                        viewModel.stopMonitoring()
                        MonitoringService.stopMonitoring(context)
                    },
                    onSwitchToFloating = {
                        MonitoringService.startMonitoring(context)
                        (context as? Activity)?.moveTaskToBack(true)
                    },
                    onAcknowledgeWake = {
                        MonitoringService.acknowledgeWake(context)
                    },
                    onAskAgentReply = {
                        requestAgentReply()
                    },
                    isCameraReady = isCameraReady,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                // 모니터링 시작 전: 기존 레이아웃 유지
                ReadyView(
                    uiState = uiState,
                    cameraPermissionGranted = cameraPermissionGranted,
                    onStartMonitoring = {
                        viewModel.startMonitoring()
                        MonitoringService.startMonitoring(context)
                        // 바로 플로팅 모드로 전환
                        (context as? Activity)?.moveTaskToBack(true)
                    },
                    onModeSelected = { mode -> viewModel.selectMode(mode) },
                    isCameraReady = isCameraReady
                )
                
                // 카메라 프리뷰를 중앙에 배치
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
                            isMonitoring = uiState.isMonitoring,
                            monitoringService = monitoringService,
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
                    // 로고 아이콘 박스
                    Box(
                        modifier = Modifier
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = ac.sbmax002.eye_on.R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(24.dp)
                        )
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


// 모니터링 시작 버튼 누르기 전
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
            //
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
                    backgroundColor = appModePrimaryColor(uiState.appMode),
                    disabledBackgroundColor = Color(0xFF424242),
                    text = "모니터링 시작",
                    modifier = Modifier.fillMaxWidth()
                )

            Text(
                text = appModeGuideText(uiState.appMode),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
        }
    }
}

// 모니터링 시작 버튼 누르고 나서 화면
@Composable
private fun MonitoringView(
    uiState: HomeUiState,
    onStopMonitoring: () -> Unit,
    onSwitchToFloating: () -> Unit,
    onAcknowledgeWake: () -> Unit,
    onAskAgentReply: () -> Unit,
    isCameraReady: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
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
                        text = "플로팅 모드로 전환하여 다른 앱을 사용할 수 있습니다",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF99A1AF),
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.15).sp
                    )
                }

                AnimatedButton(
                    onClick = onSwitchToFloating,
                    enabled = true,
                    backgroundColor = appModePrimaryColor(uiState.appMode),
                    disabledBackgroundColor = Color(0xFF424242),
                    text = "플로팅 모드로 전환",
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedButton(
                    onClick = onAskAgentReply,
                    enabled = true,
                    backgroundColor = Color(0xFF7C4DFF),
                    disabledBackgroundColor = Color(0xFF424242),
                    text = "AI에게 대답하기",
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedButton(
                    onClick = onStopMonitoring,
                    enabled = true,
                    backgroundColor = Color(0xFFE53935),
                    disabledBackgroundColor = Color(0xFF424242),
                    text = "모니터링 종료",
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = appModeGuideText(uiState.appMode),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.sp
                )
        }

        // 모니터링 화면에서 바로 알람 해제할 수 있는 중앙 오버레이 버튼
        if (uiState.currentDrowsinessState != DrowsinessState.NORMAL) {
            Box(
                modifier = Modifier
                    .matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onAcknowledgeWake,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E88E5),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AlarmOff,
                        contentDescription = "알람 해제",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "알람 해제 (깨어났어요)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.2).sp
                    )
                }
            }
        }
    }
}

private fun appModePrimaryColor(mode: AppMode): Color = when (mode) {
    AppMode.DRIVING -> Color(0xFF007AFF)
    AppMode.STUDY -> Color(0xFFFF9800)
    AppMode.ORGANIZATION -> Color(0xFF00A86B)
}

private fun appModeGuideText(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "• 운전 중에 스마트폰을 잘 고정하여 주세요"
    AppMode.STUDY -> "• 학습 중 화면을 벗어나지 않도록 자세를 유지해 주세요"
    AppMode.ORGANIZATION -> "• 조직 모드는 팀 운영 가이드에 맞춰 사용해 주세요"
}


// 하단 모니터링 시작/종료 버튼의 모체? 그런 느낌
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
