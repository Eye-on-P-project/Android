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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    
    private var isMonitoringStarted = false
    
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
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MonitoringService onStartCommand")
        
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                startMonitoring()
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelf()
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
                // FaceProcessingPipeline 초기화
                faceProcessingPipeline = FaceProcessingPipeline(
                    context = applicationContext,
                    listener = this@MonitoringService
                )
                
                // CameraManager 초기화 및 시작
                cameraManager = ServiceCameraManager(
                    context = applicationContext,
                    config = CameraConfig(),
                    onFrameAvailable = { imageProxy ->
                        // 카메라 프레임을 파이프라인에 전달
                        faceProcessingPipeline?.process(imageProxy, true) // 전면 카메라
                    }
                )
                
                cameraManager?.startCamera()
                
                // 플로팅 윈도우 매니저 초기화 및 표시
                floatingWindowManager = FloatingWindowManager(applicationContext)
                floatingWindowManager?.showFloatingWindow()
                
                Log.d(TAG, "Monitoring started successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start monitoring", e)
            }
        }
    }
    
    /**
     * 모니터링 중지
     */
    private fun stopMonitoring() {
        Log.d(TAG, "Stopping monitoring...")
        
        isMonitoringStarted = false
        
        cameraManager?.stopCamera()
        cameraManager = null
        
        faceProcessingPipeline = null
        
        // 플로팅 윈도우 제거
        floatingWindowManager?.removeFloatingWindow()
        floatingWindowManager = null
        
        Log.d(TAG, "Monitoring stopped")
    }
    
    override fun onDestroy() {
        super.onDestroy()
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
        Log.d(TAG, "Pipeline result: isDrowsy=${result.isDrowsy}, faceDetected=${result.isFaceDetected}")
        // 졸음 상태에 따른 플로팅 아이콘 업데이트
        floatingWindowManager?.updateDrowsinessState(result.isDrowsy)
        // TODO: 사운드 재생 (나중에 구현)
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
    
    companion object {
        private const val TAG = "MonitoringService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "monitoring_channel"
        private const val CHANNEL_NAME = "모니터링"
        private const val CHANNEL_DESCRIPTION = "졸음 감지 모니터링 중"
        
        const val ACTION_START_MONITORING = "ac.sbmax002.eye_on.START_MONITORING"
        const val ACTION_STOP_MONITORING = "ac.sbmax002.eye_on.STOP_MONITORING"
        
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
    }
}

