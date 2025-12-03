package ac.sbmax002.eye_on.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import ac.sbmax002.eye_on.camera.CameraManager
import ac.sbmax002.eye_on.camera.CameraConfig
import android.util.Log
import android.view.ViewGroup
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreviewContainer(
    isReady: Boolean,
    isFaceDetected: Boolean,
    onFaceDetectionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // PreviewView를 remember로 관리
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // CameraManager는 remember로 한 번만 생성하여 뷰 전환 시에도 인스턴스 유지
    // TODO: MediaPipe 연결은 나중에 추가 (현재는 카메라만 작동)
    val cameraManager = remember {
        CameraManager(
            context = context,
            lifecycleOwner = lifecycleOwner,
            config = CameraConfig(),
            onFrameAvailable = { imageProxy ->
                // TODO: 나중에 MediaPipe 연결 시 여기에 FaceLandmarkerHelper 호출 추가
                // 현재는 카메라 프레임만 받아서 처리하지 않음
                imageProxy.close()
            }
        )
    }

    // isReady 상태에 따라 카메라 시작/중지만 수행 (재초기화하지 않음)
    LaunchedEffect(isReady) {
        if (isReady) {
            try {
                // 카메라 시작 (이미 생성된 인스턴스 사용)
                cameraManager.startCamera(previewView)
            } catch (e: Exception) {
                // 초기화 실패 시 로그만 남기고 앱은 계속 실행
                Log.e("CameraPreview", "Failed to start camera", e)
            }
        } else {
            // isReady가 false가 되면 카메라만 중지 (인스턴스는 유지)
            try {
                cameraManager.stopCamera()
            } catch (e: Exception) {
                Log.e("CameraPreview", "Failed to stop camera", e)
            }
        }
    }

    // 컴포저블이 dispose될 때 리소스 정리
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraManager.stopCamera()
            } catch (e: Exception) {
                Log.e("CameraPreview", "Failed to stop camera on dispose", e)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2A2A2A),
                        Color(0xFF1F1F1F)
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = if (isReady) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF007AFF),
                            Color(0xFF0051D5)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF424242),
                            Color(0xFF303030)
                        )
                    )
                },
                shape = RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isReady) {
            // 실제 카메라 프리뷰
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 카메라 프리뷰 전체 영역
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // 원형 가이드와 안내 텍스트 오버레이
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 원형 가이드 영역
                    CircularGuideOverlay(
                        isFaceDetected = isFaceDetected,
                        modifier = Modifier
                    )

                    // 안내 텍스트 (카메라 프리뷰 위에 오버레이)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "얼굴을 원 안에 맞춰주세요",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.44).sp
                        )
                    }
                }
            }
        } else {
            // 카메라 권한이 없을 때 표시할 플레이스홀더
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 원형 가이드 표시
                Box(
                    modifier = Modifier
                        .size(192.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF).copy(alpha = 0.2f))
                        .border(
                            width = 4.dp,
                            color = Color(0xFF007AFF),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 플레이스홀더 아이콘 들어갈 위치
                }

                // 안내 메시지
                // TODO: 나중에 얼굴 인식 상태에 따라 동적으로 메시지 변경
                // 예: "얼굴을 원 안에 맞춰주세요" -> "얼굴이 감지되었습니다" -> "얼굴 위치를 조정해주세요" 등
                Text(
                    text = "얼굴을 원 안에 맞춰주세요",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.44).sp
                )

                Text(
                    text = "카메라 권한이 필요합니다",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF99A1AF),
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.31).sp
                )
            }
        }
    }
}

/**
 * 원형 가이드 오버레이 컴포넌트
 * 나중에 디자인 결정에 따라 쉽게 수정할 수 있도록 분리
 */
@Composable
private fun CircularGuideOverlay(
    isFaceDetected: Boolean,
    modifier: Modifier = Modifier
) {
    // 원형 가이드 크기
    val guideSize = 256.dp
    
    Box(
        modifier = modifier.size(guideSize),
        contentAlignment = Alignment.Center
    ) {
        // 기본 원형 가이드 (항상 표시)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 2.dp,
                    color = Color(0xFF007AFF).copy(alpha = 0.5f),
                    shape = CircleShape
                )
        )

        // 얼굴 감지 인디케이터 (얼굴이 감지되면 표시)
        AnimatedVisibility(
            visible = isFaceDetected,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 4.dp,
                        color = Color(0xFF007AFF),
                        shape = CircleShape
                    )
            )
        }
    }
}

