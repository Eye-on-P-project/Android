package ac.sbmax002.eye_on.ui.home

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ac.sbmax002.eye_on.DTO.DrowsinessState
import ac.sbmax002.eye_on.service.MonitoringService

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
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isCameraReady = cameraPermissionGranted && uiState.isReady

            if (uiState.isMonitoring) {
                key("shared_camera_preview") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
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
                ReadyView(
                    uiState = uiState,
                    cameraPermissionGranted = cameraPermissionGranted,
                    onStartMonitoring = {
                        viewModel.startMonitoring()
                        MonitoringService.startMonitoring(context)
                        (context as? Activity)?.moveTaskToBack(true)
                    },
                    onModeSelected = { mode -> viewModel.selectMode(mode) },
                    isCameraReady = isCameraReady
                )

                key("shared_camera_preview") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
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
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = ac.sbmax002.eye_on.R.drawable.logo),
                            contentDescription = "Eye:on",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = "Eye:on",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        actions = {
            TopBarAction(
                icon = Icons.Default.BarChart,
                contentDescription = "통계",
                onClick = onStatisticsClick
            )
            Spacer(modifier = Modifier.width(6.dp))
            TopBarAction(
                icon = Icons.Default.Settings,
                contentDescription = "설정",
                onClick = onSettingsClick
            )
            Spacer(modifier = Modifier.width(12.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun TopBarAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedButton(
                onClick = {
                    android.util.Log.d("HomeScreen", "Start monitoring button clicked")
                    onStartMonitoring()
                },
                enabled = cameraPermissionGranted && uiState.isReady,
                backgroundColor = appModePrimaryColor(uiState.appMode),
                disabledBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                text = "모니터링 시작",
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = if (isCameraReady) appModeGuideText(uiState.appMode) else "카메라 권한과 준비 상태를 확인하고 있어요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

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
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusPill(
                    text = if (isCameraReady) "모니터링 중 · 카메라 연결됨" else "모니터링 중 · 카메라 준비 중",
                    color = appModePrimaryColor(uiState.appMode)
                )

                Text(
                    text = "다른 앱을 사용해도 감지는 계속됩니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                AnimatedButton(
                    onClick = onSwitchToFloating,
                    enabled = true,
                    backgroundColor = appModePrimaryColor(uiState.appMode),
                    disabledBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    text = "플로팅 모드로 전환",
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedButton(
                    onClick = onAskAgentReply,
                    enabled = true,
                    backgroundColor = Color(0xFF7C5CFF),
                    disabledBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    text = "AI에게 대답하기",
                    icon = Icons.Default.Mic,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedButton(
                    onClick = onStopMonitoring,
                    enabled = true,
                    backgroundColor = MaterialTheme.colorScheme.error,
                    disabledBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    text = "모니터링 종료",
                    icon = Icons.Default.Stop,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = appModeGuideText(uiState.appMode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (uiState.currentDrowsinessState != DrowsinessState.NORMAL) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onAcknowledgeWake,
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AlarmOff,
                        contentDescription = "알람 해제",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "알람 해제",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun appModePrimaryColor(mode: AppMode): Color = when (mode) {
    AppMode.DRIVING -> Color(0xFF2477F2)
    AppMode.STUDY -> Color(0xFFFF9F0A)
    AppMode.ORGANIZATION -> Color(0xFF1EB980)
}

private fun appModeGuideText(mode: AppMode): String = when (mode) {
    AppMode.DRIVING -> "운전 중에는 스마트폰을 단단히 고정해 주세요"
    AppMode.STUDY -> "학습 중에는 화면을 벗어나지 않도록 자세를 유지해 주세요"
    AppMode.ORGANIZATION -> "조직 모드는 팀 운영 가이드에 맞춰 사용해 주세요"
}

@Composable
private fun AnimatedButton(
    onClick: () -> Unit,
    enabled: Boolean,
    backgroundColor: Color,
    disabledBackgroundColor: Color,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
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
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White,
            disabledContainerColor = disabledBackgroundColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(18.dp),
        interactionSource = interactionSource,
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
