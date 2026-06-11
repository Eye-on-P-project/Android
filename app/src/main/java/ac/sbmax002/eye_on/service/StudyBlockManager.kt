package ac.sbmax002.eye_on.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import ac.sbmax002.eye_on.repository.StudyBlockRepository
import ac.sbmax002.eye_on.ui.settings.BlockedAppActivity
import kotlinx.coroutines.*

/**
 * 스터디 집중 모드 중에 주기적으로 현재 사용중인 포그라운드 앱을 검사하여 차단하는 매니저 클래스
 */
object StudyBlockManager {
    private const val TAG = "StudyBlockManager"
    private const val POLL_INTERVAL_MS = 1000L // 1초 간격으로 검사
    
    private var job: Job? = null
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 포그라운드 감시 시작
     */
    fun start(context: Context) {
        if (job != null && job?.isActive == true) {
            Log.d(TAG, "Study app monitoring is already running.")
            return
        }

        val appContext = context.applicationContext
        val repository = StudyBlockRepository(appContext)

        // 설정 저장소에서 차단 기능 활성화 여부 확인
        if (!repository.isBlockingEnabled()) {
            Log.d(TAG, "Study app blocking feature is disabled in settings.")
            return
        }

        // 사용 권한 허용 상태 재차 확인
        if (!hasUsageStatsPermission(appContext)) {
            Log.w(TAG, "UsageStats permission not granted. Cannot start study app blocking.")
            return
        }

        Log.d(TAG, "Starting study app monitoring loop...")
        job = managerScope.launch {
            while (isActive) {
                try {
                    val currentForegroundApp = getForegroundPackageName(appContext)
                    if (currentForegroundApp != null) {
                        val blockedApps = repository.getBlockedPackages()
                        if (blockedApps.contains(currentForegroundApp)) {
                            Log.d(TAG, "Blocked app detected: $currentForegroundApp. Redirecting to BlockedAppActivity.")
                            
                            // 차단 화면으로 즉시 리다이렉트
                            val blockIntent = Intent(appContext, BlockedAppActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("blocked_package", currentForegroundApp)
                            }
                            appContext.startActivity(blockIntent)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in study app monitoring loop", e)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * 포그라운드 감시 중지
     */
    fun stop() {
        if (job != null) {
            Log.d(TAG, "Stopping study app monitoring...")
            job?.cancel()
            job = null
        }
    }

    /**
     * 현재 화면 상단에 노출된 포그라운드 패키지명 조회 (UsageEvents API + Fallback logic)
     */
    private fun getForegroundPackageName(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000 // 최근 10초 범위 내 쿼리

        // 1. UsageEvents API를 사용한 정밀 검출
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        if (usageEvents != null) {
            val event = UsageEvents.Event()
            var lastResumedPackage: String? = null

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastResumedPackage = event.packageName
                }
            }
            if (lastResumedPackage != null) {
                return lastResumedPackage
            }
        }

        // 2. Event 기록에 지연이 있거나 누락되었을 경우를 대비한 queryUsageStats 백업 폴백 로직
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        if (!stats.isNullOrEmpty()) {
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            return sortedStats.firstOrNull()?.packageName
        }

        return null
    }

    /**
     * 권한 체크 유틸리티
     */
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
