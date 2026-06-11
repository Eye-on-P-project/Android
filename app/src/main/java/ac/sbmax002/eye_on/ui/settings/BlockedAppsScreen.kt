package ac.sbmax002.eye_on.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Drawable 객체를 Compose의 ImageBitmap으로 안전하게 변환하는 헬퍼 함수
 */
fun drawableToImageBitmap(drawable: Drawable?): ImageBitmap? {
    if (drawable == null) return null
    if (drawable is BitmapDrawable) {
        return drawable.bitmap.asImageBitmap()
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 100
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 100
    return try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

/**
 * 스터디 앱 차단 설정 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedAppsScreen(
    viewModel: BlockedAppsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPermissionDialog by remember { mutableStateOf(false) }

    // 화면 복귀(ON_RESUME) 시 권한 상태 자동 동기화 처리
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "스터디 앱 차단",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                )
            )
        },
        containerColor = Color(0xFF1A1A1A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 마스터 토글 배너 카드 (그라데이션 스타일)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF9800).copy(alpha = 0.15f),
                                    Color(0xFF2A2A2A)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "앱 차단 설정",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "스터디 모드 활성화 중 감지된 차단 앱을 자동 차단합니다.",
                                color = Color(0xFF99A1AF),
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = uiState.isBlockingEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    // 권한 검사 수행
                                    if (uiState.hasUsagePermission) {
                                        viewModel.setBlockingEnabled(true)
                                    } else {
                                        showPermissionDialog = true
                                    }
                                } else {
                                    viewModel.setBlockingEnabled(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF9800),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFF424242)
                            )
                        )
                    }
                }
            }

            // 차단 활성화 상태일 때 앱 검색창 및 리스트 표시
            AnimatedVisibility(
                visible = uiState.isBlockingEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 검색창 UI
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "차단할 앱 이름을 검색하세요...",
                                color = Color(0xFF757575),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF99A1AF)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9800),
                            unfocusedBorderColor = Color(0xFF2A2A2A),
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF2A2A2A)
                        ),
                        singleLine = true
                    )

                    // 앱 목록 카드 컨테이너
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF2A2A2A)
                    ) {
                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFFFF9800))
                            }
                        } else if (uiState.filteredApps.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "설치된 앱이 없거나 검색 결과가 없습니다.",
                                    color = Color(0xFF99A1AF),
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.filteredApps,
                                    key = { it.packageName }
                                ) { appInfo ->
                                    AppListItem(
                                        appInfo = appInfo,
                                        onToggleBlock = { viewModel.toggleAppBlocked(appInfo.packageName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 차단 비활성화 시 노출할 안내 카드
            if (!uiState.isBlockingEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "스터디 앱 차단 설정을 켜서 집중 학습을 시작하세요.",
                        color = Color(0xFF757575),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // 사용 권한 동의 유도 팝업창
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    text = "사용 권한 필요",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "집중 감시 상태 동안 설정하신 앱 접속을 자동으로 차단하기 위해 '사용 정보 접근' 권한 승인이 필요합니다. 허용 메뉴로 이동하시겠습니까?",
                    color = Color(0xFFE0E0E0),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        try {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // 일부 기기 대응용 폴백 Intent
                            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("설정하러 가기", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("취소", color = Color(0xFF99A1AF))
                }
            },
            containerColor = Color(0xFF2E2E2E),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * 앱 목록의 각 항목 아이템 컴포저블
 */
@Composable
fun AppListItem(
    appInfo: AppInfo,
    onToggleBlock: () -> Unit
) {
    val bitmap = remember(appInfo.icon) { drawableToImageBitmap(appInfo.icon) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .clickable(onClick = onToggleBlock)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 앱 아이콘
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A2A2A))
                )
            }

            // 앱 상세 정보
            Column {
                Text(
                    text = appInfo.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = appInfo.packageName,
                    color = Color(0xFF757575),
                    fontSize = 11.sp
                )
            }
        }

        // 차단 토글 체크박스 / 스위치
        Switch(
            checked = appInfo.isBlocked,
            onCheckedChange = { onToggleBlock() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF9800),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF424242)
            )
        )
    }
}
