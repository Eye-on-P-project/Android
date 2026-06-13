package ac.sbmax002.eye_on.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ac.sbmax002.eye_on.ui.settings.DrowsinessSensitivity
import ac.sbmax002.eye_on.repository.SubscriptionRepository
import ac.sbmax002.eye_on.model.subscription.SubscriptionTier

/**
 * 설정 화면
 * 
 * Figma 디자인을 기반으로 구현된 설정 화면입니다.
 * MVVM 패턴을 따르며, SettingsViewModel을 통해 상태를 관리합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToLevel1Alert: () -> Unit = {},
    onNavigateToLevel2Alert: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    subscriptionRepository: SubscriptionRepository? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptionStatus = subscriptionRepository?.subscriptionStatus?.collectAsStateWithLifecycle()
    val currentTier = subscriptionStatus?.value?.currentTier ?: SubscriptionTier.FREE
    
    Scaffold(
        topBar = {
            SettingsTopBar(onBackClick = onNavigateBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 계정 섹션
            SettingsSection(
                title = "계정"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToAccount)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "회원 정보",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 구독 관리 섹션
            SettingsSection(
                title = "구독"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToSubscription)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "구독 관리",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 현재 티어 뱃지
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentTier == SubscriptionTier.PLUS)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = currentTier.displayName,
                                color = if (currentTier == SubscriptionTier.PLUS)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 졸음 경고 1단계 섹션
            SettingsSection(
                title = "졸음 경고 1단계"
            ) {
                AlarmSoundSelector(
                    label = "알림음",
                    currentSound = uiState.level1AlarmSound,
                    onSoundClick = onNavigateToLevel1Alert
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                VolumeSlider(
                    label = "음량",
                    volume = uiState.level1Volume,
                    onVolumeChange = { viewModel.updateLevel1Volume(it) }
                )
            }
            
            // 수면 경고 2단계 섹션
            SettingsSection(
                title = "수면 경고 2단계"
            ) {
                AlarmSoundSelector(
                    label = "알림음",
                    currentSound = uiState.level2AlarmSound,
                    onSoundClick = onNavigateToLevel2Alert
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                VolumeSlider(
                    label = "음량",
                    volume = uiState.level2Volume,
                    onVolumeChange = { viewModel.updateLevel2Volume(it) }
                )
            }

            // 민감도 섹션
            SettingsSection(
                title = "민감도"
            ) {
                SensitivitySelector(
                    selectedSensitivity = uiState.drowsinessSensitivity,
                    onSensitivitySelected = { viewModel.updateDrowsinessSensitivity(it) }
                )
            }
            
            // 플로팅 아이콘 크기 섹션
            SettingsSection(
                title = "플로팅 아이콘 크기"
            ) {
                FloatingIconSizeSelector(
                    selectedSize = uiState.floatingIconSize,
                    onSizeSelected = { viewModel.updateFloatingIconSize(it) }
                )
            }
            
            // 기타 섹션
            SettingsSection(
                title = "기타"
            ) {
                SettingToggleItem(
                    label = "진동 알림",
                    checked = uiState.vibrationEnabled,
                    onCheckedChange = { viewModel.toggleVibration() }
                )
            }
        }
    }
}

/**
 * 설정 화면의 TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "설정",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

/**
 * 설정 섹션 컨테이너
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 섹션 제목
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp
        )
        
        // 섹션 컨텐츠 컨테이너
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                content = content
            )
        }
    }
}

/**
 * 알림음 선택 버튼
 */
@Composable
private fun AlarmSoundSelector(
    label: String,
    currentSound: AlarmSound,
    onSoundClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSoundClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentSound.displayName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 음량 슬라이더
 */
@Composable
private fun VolumeSlider(
    label: String,
    volume: Int,
    onVolumeChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 라벨과 현재 값
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )
            Text(
                text = "$volume%",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )
        }
        
        // 슬라이더
        Slider(
            value = volume.toFloat(),
            onValueChange = { onVolumeChange(it.toInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = if (volume <= 70) Color(0xFFFF9F0A) else Color(0xFFFF3B30),
                activeTrackColor = if (volume <= 70) Color(0xFFFF9F0A) else Color(0xFFFF3B30),
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

/**
 * 플로팅 아이콘 크기 선택 버튼 그룹
 */
@Composable
private fun FloatingIconSizeSelector(
    selectedSize: FloatingIconSize,
    onSizeSelected: (FloatingIconSize) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FloatingIconSize.values().forEach { size ->
            val isSelected = size == selectedSize
            Button(
                onClick = { onSizeSelected(size) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = size.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

/**
 * 토글 스위치가 있는 설정 아이템
 */
@Composable
private fun SettingToggleItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

/**
 * 졸음 감지 민감도 선택 버튼 그룹
 */
@Composable
private fun SensitivitySelector(
    selectedSensitivity: DrowsinessSensitivity,
    onSensitivitySelected: (DrowsinessSensitivity) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DrowsinessSensitivity.values().forEach { sensitivity ->
            val isSelected = sensitivity == selectedSensitivity
            Button(
                onClick = { onSensitivitySelected(sensitivity) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = sensitivity.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}
