package ac.sbmax002.eye_on.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import ac.sbmax002.eye_on.MainActivity
import ac.sbmax002.eye_on.R
import ac.sbmax002.eye_on.camera.CameraConfig
import ac.sbmax002.eye_on.camera.ServiceCameraManager
import ac.sbmax002.eye_on.model.pipeline.FaceProcessingPipeline
import ac.sbmax002.eye_on.model.pipeline.PipelineListener
import ac.sbmax002.eye_on.model.pipeline.PipelineResult
import ac.sbmax002.eye_on.DTO.DrowsinessState
import ac.sbmax002.eye_on.repository.SettingsRepository
import ac.sbmax002.eye_on.ui.settings.AlarmSound
import ac.sbmax002.eye_on.ui.settings.DrowsinessSensitivity
import ac.sbmax002.eye_on.repository.AppStateRepository
import ac.sbmax002.eye_on.network.NetworkConfig
import ac.sbmax002.eye_on.network.MonitoringStartRequest
import ac.sbmax002.eye_on.network.MonitoringEndRequest
import ac.sbmax002.eye_on.network.MonitoringEventRequest
import ac.sbmax002.eye_on.network.TokenRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import retrofit2.Response

/**
 * 모니터링을 관리하는 Foreground Service
 * 
 * - 카메라를 Service에서 중앙 관리
 * - FaceProcessingPipeline 실행
 * - 플로팅 윈도우 관리
 */
class MonitoringService : Service(), PipelineListener {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var cameraManager: ServiceCameraManager? = null
    private var faceProcessingPipeline: FaceProcessingPipeline? = null
    private var floatingWindowManager: FloatingWindowManager? = null
    private var settingsRepository: SettingsRepository? = null
    private var settingsJob: Job? = null
    private var alarmPlayer: AlarmPlayer? = null
    
    private var isMonitoringStarted = false
    
    // 서버 연동용 세션 ID
    private var serverSessionId: Long? = null

    private var currentAlarmLevel: AlarmLevel = AlarmLevel.NONE
    private var level1AlarmSound: AlarmSound = AlarmSound.BELL_NOTIFICATION
    private var level2AlarmSound: AlarmSound = AlarmSound.SIREN
    private var level1Volume: Int = 70
    private var level2Volume: Int = 100

    // 파이프라인 결과를 Activity/HomeViewModel로 전달하기 위한 콜백
    private var pipelineResultListener: ((PipelineResult) -> Unit)? = null
    fun setOnPipelineResultListener(listener: ((PipelineResult) -> Unit)?) {
        pipelineResultListener = listener
    }
    
    // Service 바인딩을 위한 Binder
    inner class MonitoringBinder : Binder() {
        fun getService(): MonitoringService = this@MonitoringService
    }
    
    private val binder = MonitoringBinder()
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MonitoringService onCreate")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MonitoringService onStartCommand")
        
        // 서비스가 완전히 시작된 후에 startForeground 호출
        startForeground(NOTIFICATION_ID, createNotification())
        
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                startMonitoring()
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring {
                    stopSelf()
                }
            }
            ACTION_ACK_WAKE -> {
                acknowledgeWake()
            }
        }
        
        return START_STICKY
    }
    
    /**
     * 모니터링 시작
     */
    private fun startMonitoring() {
        if (isMonitoringStarted) {
            Log.d(TAG, "Monitoring already started, skipping...")
            return
        }
        
        Log.d(TAG, "Starting monitoring...")
        isMonitoringStarted = true
        
        serviceScope.launch {
            try {
                // SettingsRepository 초기화 (Hilt 없이 직접 생성)
                val settingsRepo = ensureSettingsRepository()
                val initialSensitivity = settingsRepo.drowsinessSensitivity.first()
                val initialLevel1Sound = settingsRepo.level1AlarmSound.first()
                val initialLevel2Sound = settingsRepo.level2AlarmSound.first()
                val initialLevel1Volume = settingsRepo.level1Volume.first()
                val initialLevel2Volume = settingsRepo.level2Volume.first()

                level1AlarmSound = initialLevel1Sound
                level2AlarmSound = initialLevel2Sound
                level1Volume = initialLevel1Volume
                level2Volume = initialLevel2Volume

                val initialDuration = initialSensitivity.drowsyDurationMs
                Log.d(TAG, "Drowsiness sensitivity initial ms=$initialDuration")
                
                // FaceProcessingPipeline 초기화
                faceProcessingPipeline = FaceProcessingPipeline(
                    context = applicationContext,
                    listener = this@MonitoringService,
                    drowsyDurationMs = initialDuration
                )
                alarmPlayer = AlarmPlayer(applicationContext)
                settingsJob = serviceScope.launch {
                    settingsRepo.let { repo ->
                        combine(
                            repo.drowsinessSensitivity,
                            repo.level1AlarmSound,
                            repo.level1Volume,
                            repo.level2AlarmSound,
                            repo.level2Volume
                        ) { sensitivity, l1Sound, l1VolumeValue, l2Sound, l2VolumeValue ->
                            SettingsSnapshot(
                                sensitivity = sensitivity,
                                level1Sound = l1Sound,
                                level1Volume = l1VolumeValue,
                                level2Sound = l2Sound,
                                level2Volume = l2VolumeValue
                            )
                        }.collectLatest { snapshot ->
                            Log.d(TAG, "Settings updated: sensitivity=${snapshot.sensitivity.name}, l1Sound=${snapshot.level1Sound.name}, l2Sound=${snapshot.level2Sound.name}")
                            faceProcessingPipeline?.updateDrowsyDuration(snapshot.sensitivity.drowsyDurationMs)
                            level1AlarmSound = snapshot.level1Sound
                            level2AlarmSound = snapshot.level2Sound
                            level1Volume = snapshot.level1Volume
                            level2Volume = snapshot.level2Volume
                            refreshAlarmIfNeeded()
                        }
                    }
                }
                
                // CameraManager 초기화 및 시작
                cameraManager = ServiceCameraManager(
                    context = applicationContext,
                    config = CameraConfig(),
                    onFrameAvailable = { imageProxy ->
                        // 카메라 프레임을 파이프라인에 전달
                        faceProcessingPipeline?.process(imageProxy, true) // 전면 카메라


                        imageProxy.close()

                    }
                )
                
                cameraManager?.startCamera()
                
                // 플로팅 윈도우 매니저 초기화 및 표시
                floatingWindowManager = FloatingWindowManager(
                    context = applicationContext,
                    settingsRepository = settingsRepository,
                    onWakeUpClicked = { acknowledgeWake() }
                )
                floatingWindowManager?.showFloatingWindow()
                
                // === 서버 세션 시작 로직 ===
                try {
                    val nowStr = currentKstTimestampString()
                    val request = MonitoringStartRequest(
                        mode = AppStateRepository.getCurrentAppMode().name,
                        startedAtApp = nowStr
                    )
                    val response = NetworkConfig.monitoringApiService.startMonitoring(request)
                    if (response.isSuccessful) {
                        serverSessionId = response.body()?.sessionId
                        Log.d(TAG, "Server session started: $serverSessionId")
                    } else {
                        logHttpFailure("Failed to start server session", response)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting server session", e)
                }
                
                Log.d(TAG, "Monitoring started successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start monitoring", e)
            }
        }
    }
    
    /**
     * 모니터링 중지
     */
    private fun stopMonitoring(onFinished: (() -> Unit)? = null) {
        Log.d(TAG, "Stopping monitoring...")
        
        isMonitoringStarted = false
        
        cameraManager?.stopCamera()
        cameraManager = null
        
        faceProcessingPipeline = null
        settingsJob?.cancel()
        settingsJob = null
        stopAlarm()
        alarmPlayer = null
        
        // 플로팅 윈도우 제거
        floatingWindowManager?.removeFloatingWindow()
        floatingWindowManager = null
        
        // === 서버 세션 종료 로직 ===
        val currentSessionId = serverSessionId
        serverSessionId = null
        if (currentSessionId != null) {
            serviceScope.launch {
                try {
                    val nowStr = currentKstTimestampString()
                    val request = MonitoringEndRequest(endedAtApp = nowStr)
                    val response = NetworkConfig.monitoringApiService.endMonitoring(currentSessionId, request)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Server session ended: $currentSessionId")
                    } else {
                        logHttpFailure("Failed to end server session", response)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error ending server session", e)
                } finally {
                    onFinished?.invoke()
                }
            }
        } else {
            onFinished?.invoke()
        }
        
        Log.d(TAG, "Monitoring stopped")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        pipelineResultListener = null
        Log.d(TAG, "MonitoringService onDestroy")
        stopMonitoring()
    }
    
    // ---------------------------------------------------------------------
    // Notification
    // ---------------------------------------------------------------------
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Eye:on 모니터링 중")
            .setContentText("졸음 감지를 수행하고 있습니다")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    // ---------------------------------------------------------------------
    // PipelineListener 구현
    // ---------------------------------------------------------------------
    
    override fun onPipelineResult(result: PipelineResult) {
        Log.d(TAG, "Pipeline result: drowsinessState=${result.drowsinessState}, faceDetected=${result.isFaceDetected}")

        // 얼굴 감지 여부와 졸음 상태에 따른 플로팅 아이콘 업데이트
        floatingWindowManager?.updateState(
            result.isFaceDetected,
            result.drowsinessState
        )

        // Activity/HomeViewModel 쪽으로도 결과 전달
        pipelineResultListener?.invoke(result)

        handleAlarm(result)
    }
    
    override fun onPipelineError(message: String) {
        Log.e(TAG, "Pipeline error: $message")
    }
    
    // ---------------------------------------------------------------------
    // Public API for Activity
    // ---------------------------------------------------------------------
    
    /**
     * Activity에서 Preview를 연결하기 위한 메서드
     */
    fun getCameraManager(): ServiceCameraManager? {
        return cameraManager
    }
    
    /**
     * 플로팅 아이콘 숨기기 (Activity가 포그라운드일 때)
     */
    fun hideFloatingIcon() {
        floatingWindowManager?.hideFloatingWindow()
    }
    
    /**
     * 플로팅 아이콘 보이기 (Activity가 백그라운드일 때)
     */
    fun showFloatingIcon() {
        floatingWindowManager?.showFloatingWindowIfExists()
    }

    private fun handleAlarm(result: PipelineResult) {
        val targetLevel = when {
            result.isFaceDetected && result.drowsinessState == DrowsinessState.NORMAL -> AlarmLevel.NONE
            result.drowsinessState == DrowsinessState.SLEEPING -> AlarmLevel.LEVEL2
            result.drowsinessState == DrowsinessState.DROWSY -> AlarmLevel.LEVEL1
            currentAlarmLevel != AlarmLevel.NONE -> currentAlarmLevel // 얼굴 미검출 시에도 기존 알람 유지
            else -> AlarmLevel.NONE
        }

        if (targetLevel == AlarmLevel.NONE) {
            if (currentAlarmLevel != AlarmLevel.NONE) {
                recordServerEvent(currentAlarmLevel, targetLevel)
            }
            stopAlarm()
            return
        }

        if (targetLevel != currentAlarmLevel) {
            when (targetLevel) {
                AlarmLevel.LEVEL1 -> alarmPlayer?.play(level1AlarmSound, level1Volume)
                AlarmLevel.LEVEL2 -> alarmPlayer?.play(level2AlarmSound, level2Volume)
                else -> {}
            }
            
            // 서버 이벤트 기록
            recordServerEvent(currentAlarmLevel, targetLevel)
            
            currentAlarmLevel = targetLevel
            return
        }

        // 같은 단계가 유지되지만 플레이어가 멈췄다면 재개
        if (alarmPlayer?.isPlaying() != true) {
            when (targetLevel) {
                AlarmLevel.LEVEL1 -> alarmPlayer?.play(level1AlarmSound, level1Volume)
                AlarmLevel.LEVEL2 -> alarmPlayer?.play(level2AlarmSound, level2Volume)
                else -> {}
            }
        }
    }

    private fun recordServerEvent(oldLevel: AlarmLevel, newLevel: AlarmLevel) {
        val sessionId = serverSessionId ?: return
        if (oldLevel == newLevel) return

        serviceScope.launch {
            try {
                val nowStr = currentKstTimestampString()

                // 경고 단계 변화 (정상->경고, 졸음<->수면)는 모두 상태 이벤트를 서버에 기록한다.
                if (newLevel != AlarmLevel.NONE) {
                    val eventType = if (newLevel == AlarmLevel.LEVEL2) "SLEEP" else "DROWSY"
                    val request = MonitoringEventRequest(
                        eventType = eventType,
                        occurredAtApp = nowStr
                    )
                    val response = NetworkConfig.monitoringApiService.recordEvent(sessionId, request)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Server event recorded: $eventType ($oldLevel -> $newLevel)")
                    } else {
                        logHttpFailure("Failed to record server event($eventType)", response)
                    }
                }
                // 이벤트 종료 (경고 -> 정상)
                else if (oldLevel != AlarmLevel.NONE && newLevel == AlarmLevel.NONE) {
                    val request = MonitoringEventRequest(
                        eventType = "NORMAL",
                        occurredAtApp = nowStr
                    )
                    val response = NetworkConfig.monitoringApiService.recordEvent(sessionId, request)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Server event recorded: NORMAL ($oldLevel -> $newLevel)")
                    } else {
                        logHttpFailure("Failed to record server event(NORMAL)", response)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recording server event", e)
            }
        }
    }

    private fun ensureSettingsRepository(): SettingsRepository {
        return settingsRepository ?: SettingsRepository(applicationContext).also {
            settingsRepository = it
        }
    }

    private fun logHttpFailure(prefix: String, response: Response<*>?) {
        if (response == null) {
            Log.e(TAG, "$prefix: no response")
            return
        }
        val errorBodyText = runCatching { response.errorBody()?.string() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        if (errorBodyText != null) {
            Log.e(TAG, "$prefix: ${response.code()} - $errorBodyText")
        } else {
            Log.e(TAG, "$prefix: ${response.code()}")
        }
    }

    private fun refreshAlarmIfNeeded() {
        when (currentAlarmLevel) {
            AlarmLevel.LEVEL1 -> alarmPlayer?.play(level1AlarmSound, level1Volume)
            AlarmLevel.LEVEL2 -> alarmPlayer?.play(level2AlarmSound, level2Volume)
            else -> {}
        }
    }

    private fun stopAlarm() {
        alarmPlayer?.stop()
        currentAlarmLevel = AlarmLevel.NONE
    }

    private fun acknowledgeWake() {
        Log.d(TAG, "Acknowledge wake requested")
        stopAlarm()

        // UI를 즉시 정상 상태로 전환 (다음 프레임에서 다시 판정됨)
        val normalized = PipelineResult(
            frameTimestampMs = System.currentTimeMillis(),
            isFaceDetected = true,
            leftEye = null,
            rightEye = null,
            drowsinessState = DrowsinessState.NORMAL
        )
        floatingWindowManager?.updateState(true, DrowsinessState.NORMAL)
        pipelineResultListener?.invoke(normalized)
    }

    private fun currentKstTimestampString(): String {
        return LocalDateTime.now(KST_ZONE_ID).format(APP_TIME_FORMATTER)
    }

    private data class SettingsSnapshot(
        val sensitivity: DrowsinessSensitivity,
        val level1Sound: AlarmSound,
        val level1Volume: Int,
        val level2Sound: AlarmSound,
        val level2Volume: Int
    )

    private enum class AlarmLevel { NONE, LEVEL1, LEVEL2 }
    
    companion object {
        private const val TAG = "MonitoringService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "monitoring_channel"
        private const val CHANNEL_NAME = "모니터링"
        private const val CHANNEL_DESCRIPTION = "졸음 감지 모니터링 중"
        private val APP_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        private val KST_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        
        const val ACTION_START_MONITORING = "ac.sbmax002.eye_on.START_MONITORING"
        const val ACTION_STOP_MONITORING = "ac.sbmax002.eye_on.STOP_MONITORING"
        const val ACTION_ACK_WAKE = "ac.sbmax002.eye_on.ACK_WAKE"
        
        fun startMonitoring(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopMonitoring(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }

        fun acknowledgeWake(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_ACK_WAKE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
