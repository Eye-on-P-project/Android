package ac.sbmax002.eye_on.ui.settings

import android.annotation.SuppressLint
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 1단계 졸음 경고 알림음 설정 화면
 * 
 * Figma 디자인을 기반으로 구현된 알림음 선택 화면입니다.
 * Row 레이아웃으로 구성된 리스트에서 알림음을 선택하고, 재생 버튼으로 미리듣기 기능을 제공합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Level1AlertScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // 현재 저장된 설정값으로 선택값 동기화
    val initialSound = normalizeAlarmSound(uiState.level1AlarmSound)
    var selectedSound by remember(initialSound) { mutableStateOf(initialSound) }
    
    // 미리듣기 관련 상태
    var playingSound by remember { mutableStateOf<AlarmSound?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    // MediaPlayer 정리
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.apply {
                try {
                    if (isPlaying) {
                        stop()
                    }
                } catch (e: Exception) {
                    Log.e("Level1AlertScreen", "Error stopping MediaPlayer: ${e.message}")
                }
                try {
                    release()
                } catch (e: Exception) {
                    Log.e("Level1AlertScreen", "Error releasing MediaPlayer: ${e.message}")
                }
            }
            mediaPlayer = null
            playingSound = null
        }
    }
    
    Scaffold(
        topBar = {
            Level1AlertTopBar(onBackClick = onNavigateBack)
        },
        containerColor = Color(0xFF1A1A1A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 알림음 선택 리스트 (실제 파일명 기준)
            AlarmSoundList(
                sounds = listOf(
                    AlarmSound.BELL_NOTIFICATION,
                    AlarmSound.FIRE_ALARM,
                    AlarmSound.MEGA_HORN,
                    AlarmSound.SCHOOL_BELL,
                    AlarmSound.SECURITY_ALARM,
                    AlarmSound.SIREN
                ),
                selectedSound = selectedSound,
                playingSound = playingSound,
                volume = uiState.level1Volume,
                primaryColor = primaryColor,
                onSoundSelected = { sound ->
                    // 리스트 클릭: 선택만 수행 (소리 재생 안 함)
                    selectedSound = sound
                },
                onPlayClick = { sound ->
                    // 재생 버튼 클릭: 소리 재생만 수행
                    scope.launch {
                        playPreviewSound(
                            context = context,
                            sound = sound,
                            volume = uiState.level1Volume,
                            onStart = { playingSound = sound },
                            onComplete = { playingSound = null },
                            getMediaPlayer = { mediaPlayer },
                            setMediaPlayer = { mediaPlayer = it }
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 저장 버튼
            Button(
                onClick = {
                    viewModel.updateLevel1AlarmSound(selectedSound)
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "저장",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.44).sp
                )
            }
        }
    }
}

/**
 * 1단계 알림음 설정 화면의 TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Level1AlertTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "졸음 경고 1단계",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.07.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
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
}

/**
 * 알림음 선택 리스트
 */
@Composable
private fun AlarmSoundList(
    sounds: List<AlarmSound>,
    selectedSound: AlarmSound,
    playingSound: AlarmSound?,
    volume: Int,
    primaryColor: Color,
    onSoundSelected: (AlarmSound) -> Unit,
    onPlayClick: (AlarmSound) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sounds.forEach { sound ->
            AlarmSoundItem(
                sound = sound,
                isSelected = sound == selectedSound,
                isPlaying = sound == playingSound,
                primaryColor = primaryColor,
                onSoundClick = { onSoundSelected(sound) },
                onPlayClick = { onPlayClick(sound) }
            )
        }
    }
}

/**
 * 알림음 선택 아이템
 * Row 레이아웃: [왼쪽] 선택 아이콘 - [중앙] 알림음 이름 - [오른쪽] 재생 버튼
 */
@Composable
private fun AlarmSoundItem(
    sound: AlarmSound,
    isSelected: Boolean,
    isPlaying: Boolean,
    primaryColor: Color,
    onSoundClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2A2A2A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSoundClick)
                .padding(22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽: 선택 표시 아이콘
            Icon(
                imageVector = if (isSelected) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.RadioButtonUnchecked
                },
                contentDescription = if (isSelected) "선택됨" else "선택 안 됨",
                tint = if (isSelected) primaryColor else Color(0xFF6A7282),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 중앙: 알림음 이름
            Text(
                text = sound.displayName,
                color = if (isSelected) primaryColor else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.31).sp,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 오른쪽: 미리듣기 재생 버튼
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "미리듣기",
                    tint = if (isPlaying) primaryColor else Color(0xFF99A1AF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun normalizeAlarmSound(sound: AlarmSound): AlarmSound {
    return when (sound.name) {
        "CHIME" -> AlarmSound.BELL_NOTIFICATION
        "SIREN_OLD" -> AlarmSound.SIREN
        else -> sound
    }
}

/**
 * 미리듣기 소리 재생
 * 재생 버튼 클릭 시에만 호출됩니다.
 * 최대 5초간 재생하고 자동으로 멈춥니다.
 */
@SuppressLint("DiscouragedApi") // getIdentifier 사용 경고 억제 (동적 리소스 이름 지원)
private suspend fun playPreviewSound(
    context: android.content.Context,
    sound: AlarmSound,
    volume: Int,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    getMediaPlayer: () -> MediaPlayer?,
    setMediaPlayer: (MediaPlayer?) -> Unit
) {
    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    var audioFocusRequest: Int? = null
    
    try {
        // 기존 재생 중지 및 정리 (중복 방지)
        // 1. 먼저 현재 MediaPlayer 인스턴스를 가져옴
        val existingPlayer = getMediaPlayer()
        
        // 2. 기존 MediaPlayer가 있으면 정리
        existingPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                    Log.d("Level1Alert", "Stopped existing player")
                }
            } catch (e: Exception) {
                Log.e("Level1Alert", "Error stopping existing player: ${e.message}")
            }
            try {
                release()
                Log.d("Level1Alert", "Released existing player")
            } catch (e: Exception) {
                Log.e("Level1Alert", "Error releasing existing player: ${e.message}")
            }
        }
        
        // 3. 상태 변수를 null로 설정 (다음 재생을 위해)
        setMediaPlayer(null)
        
        // 확장자 제거 (.mp3 등)
        val resName = sound.fileName.removeSuffix(".mp3")
            .removeSuffix(".wav")
            .removeSuffix(".ogg")
        
        Log.d("Level1Alert", "=== Sound Playback Debug ===")
        Log.d("Level1Alert", "resName: $resName")
        Log.d("Level1Alert", "volume: $volume")
        
        // 리소스 ID 가져오기
        val resourceId = context.resources.getIdentifier(
            resName,
            "raw",
            context.packageName
        )
        
        Log.d("Level1Alert", "resId: $resourceId")
        
        if (resourceId == 0) {
            Log.e("Level1Alert", "Resource not found: $resName (original: ${sound.fileName})")
            onComplete()
            return
        }
        
        // 오디오 포커스 요청
        val focusRequest = audioManager.requestAudioFocus(
            null, // AudioManager.OnAudioFocusChangeListener (null이면 기본 동작)
            AudioManager.STREAM_MUSIC, // 스트림 타입
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT // 일시적인 포커스 (짧은 재생용)
        )
        
        audioFocusRequest = focusRequest
        
        Log.d("Level1Alert", "Audio focus request result: $focusRequest")
        
        if (focusRequest != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w("Level1Alert", "Audio focus not granted. Result: $focusRequest")
            // 포커스를 받지 못해도 재생 시도 (일부 기기에서는 작동할 수 있음)
        }
        
        // MediaPlayer 수동 생성 (스트림 타입을 prepare 전에 설정하기 위해)
        val mediaPlayer = MediaPlayer()
        
        try {
            // 스트림 타입 설정 (prepare 전에 호출해야 함)
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            
            // 데이터 소스 설정
            val assetFileDescriptor = context.resources.openRawResourceFd(resourceId)
            mediaPlayer.setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.length
            )
            assetFileDescriptor.close()
            
            // 준비
            mediaPlayer.prepare()
            
            Log.d("Level1Alert", "MediaPlayer prepared successfully")
            
            // 음량 설정 (0.0f ~ 1.0f)
            val volumeLevel = (volume / 100f).coerceIn(0f, 1f)
            mediaPlayer.setVolume(volumeLevel, volumeLevel)
            Log.d("Level1Alert", "Volume set: $volumeLevel (from $volume)")
            
            // 재생 완료 리스너
            mediaPlayer.setOnCompletionListener {
                Log.d("Level1Alert", "Playback completed")
                // 오디오 포커스 해제
                if (focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioManager.abandonAudioFocus(null)
                    Log.d("Level1Alert", "Audio focus abandoned")
                }
                onComplete()
                try {
                    mediaPlayer.release()
                } catch (e: Exception) {
                    Log.e("Level1Alert", "Error releasing on completion: ${e.message}")
                }
                setMediaPlayer(null)
            }
            
            // 에러 리스너
            mediaPlayer.setOnErrorListener { _, what, extra ->
                Log.e("Level1Alert", "MediaPlayer error: what=$what, extra=$extra")
                // 오디오 포커스 해제
                if (focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioManager.abandonAudioFocus(null)
                    Log.d("Level1Alert", "Audio focus abandoned on error")
                }
                onComplete()
                try {
                    mediaPlayer.release()
                } catch (e: Exception) {
                    Log.e("Level1Alert", "Error releasing on error: ${e.message}")
                }
                setMediaPlayer(null)
                true
            }
            
            // 재생 시작
            Log.d("Level1Alert", "Starting playback...")
            onStart()
            mediaPlayer.start()
            setMediaPlayer(mediaPlayer)
            
            // 재생 상태 확인
            delay(100) // 재생 시작 대기
            val isActuallyPlaying = mediaPlayer.isPlaying
            Log.d("Level1Alert", "Playback started. isPlaying: $isActuallyPlaying")
            
            if (!isActuallyPlaying) {
                Log.e("Level1Alert", "MediaPlayer start() called but not playing!")
                // 재시도
                try {
                    mediaPlayer.start()
                    delay(100)
                    Log.d("Level1Alert", "Retry start. isPlaying: ${mediaPlayer.isPlaying}")
                } catch (e: Exception) {
                    Log.e("Level1Alert", "Error retrying playback: ${e.message}", e)
                }
            }
            
            // 최대 5초 후 자동 중지
            val duration = mediaPlayer.duration
            val playDuration = if (duration > 0 && duration < 5000) {
                duration.toLong()
            } else {
                5000L
            }
            
            Log.d("Level1Alert", "Duration: ${duration}ms, Will play for: ${playDuration}ms")
            
            delay(playDuration)
            
            // 아직 재생 중이면 중지
            if (mediaPlayer.isPlaying) {
                Log.d("Level1Alert", "Stopping playback after ${playDuration}ms")
                try {
                    mediaPlayer.stop()
                } catch (e: Exception) {
                    Log.e("Level1Alert", "Error stopping after delay: ${e.message}")
                }
                // 오디오 포커스 해제
                if (focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioManager.abandonAudioFocus(null)
                    Log.d("Level1Alert", "Audio focus abandoned after delay")
                }
                onComplete()
                try {
                    mediaPlayer.release()
                } catch (e: Exception) {
                    Log.e("Level1Alert", "Error releasing after delay: ${e.message}")
                }
                setMediaPlayer(null)
            } else {
                Log.d("Level1Alert", "Playback already finished")
            }
            
        } catch (e: Exception) {
            Log.e("Level1Alert", "Error during playback: ${e.message}", e)
            // 오디오 포커스 해제
            if (focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioManager.abandonAudioFocus(null)
                Log.d("Level1Alert", "Audio focus abandoned on exception")
            }
            onComplete()
            try {
                mediaPlayer.release()
            } catch (ex: Exception) {
                Log.e("Level1Alert", "Error releasing on Exception: ${ex.message}")
            }
            setMediaPlayer(null)
        }
        
    } catch (e: Exception) {
        Log.e("Level1Alert", "Error creating MediaPlayer: ${e.message}", e)
        // 오디오 포커스 해제
        audioFocusRequest?.let {
            if (it == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioManager.abandonAudioFocus(null)
                Log.d("Level1Alert", "Audio focus abandoned on outer exception")
            }
        }
        onComplete()
    }
}
