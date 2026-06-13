package ac.sbmax002.eye_on.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ac.sbmax002.eye_on.service.FloatingWindowPermissionHelper
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 플로팅 윈도우 권한 확인 및 요청을 처리하는 컴포저블
 */
@Composable
fun FloatingWindowPermissionHandler(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDialog by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var dialogShownThisSession by remember { mutableStateOf(false) }
    
    // 권한 체크 함수
    fun checkPermission() {
        val permission = FloatingWindowPermissionHelper.hasPermission(context)
        hasPermission = permission
        
        if (permission) {
            // 권한이 있으면 콜백 호출
            onPermissionGranted()
            showDialog = false
            dialogShownThisSession = false // 권한이 있으면 초기화
        } else if (!dialogShownThisSession) {
            // 권한이 없고, 이번 세션에서 아직 다이얼로그를 표시하지 않았으면 표시
            showDialog = true
            dialogShownThisSession = true
        }
    }
    
    // 앱 실행 시 권한 확인
    LaunchedEffect(Unit) {
        checkPermission()
    }
    
    // Activity가 다시 포그라운드로 올라올 때 권한 재확인
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 설정 화면에서 돌아왔을 때 권한 재확인
                // 권한이 변경되었으면 dialogShownThisSession을 초기화하여 다시 확인
                val currentPermission = FloatingWindowPermissionHelper.hasPermission(context)
                if (currentPermission != hasPermission) {
                    dialogShownThisSession = false
                }
                checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 권한 요청 다이얼로그
    if (showDialog) {
        OverlayPermissionDialog(
            onConfirm = {
                showDialog = false
                // 설정 화면으로 이동
                FloatingWindowPermissionHelper.requestPermission(context)
            },
            onDismiss = {
                showDialog = false
                onPermissionDenied()
            }
        )
    }
}

/**
 * 플로팅 윈도우 권한 요청 다이얼로그
 */
@Composable
private fun OverlayPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 제목
                Text(
                    text = "플로팅 모드 권한 필요",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.sp
                )
                
                // 안내 메시지
                Text(
                    text = "플로팅 모드를 사용하려면\n다른 앱 위에 표시 권한 동의가 필요합니다.\n\n동의해야 플로팅 모드를 사용할 수 있습니다.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp
                )
                
                // 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 취소 버튼
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "취소",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // 확인 버튼
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "설정으로 이동",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
