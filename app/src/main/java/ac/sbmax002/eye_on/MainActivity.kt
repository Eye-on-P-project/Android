package ac.sbmax002.eye_on

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.ViewModelProvider
import ac.sbmax002.eye_on.database.AppDatabase
import ac.sbmax002.eye_on.model.statistics.BatteryUsageTracker
import ac.sbmax002.eye_on.repository.StatisticsRepository
import ac.sbmax002.eye_on.service.MonitoringService
import ac.sbmax002.eye_on.ui.home.CameraPermissionHandler
import ac.sbmax002.eye_on.ui.home.FloatingWindowPermissionHandler
import ac.sbmax002.eye_on.ui.home.HomeViewModel
import ac.sbmax002.eye_on.ui.home.HomeViewModelFactory
import ac.sbmax002.eye_on.ui.statistics.StatisticsViewModel
import ac.sbmax002.eye_on.ui.statistics.StatisticsViewModelFactory
import ac.sbmax002.eye_on.ui.theme.EyeOnTheme
import ac.sbmax002.eye_on.navigation.EyeOnApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 1. DB와 Repository는 한 번만 생성해서 공유합니다.
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { StatisticsRepository(database.statisticsDao()) }
    private val batteryUsageTracker by lazy { BatteryUsageTracker(applicationContext) }

    // 2. HomeViewModel 생성 (Factory 사용)
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(repository, batteryUsageTracker)
    }

    // 3. StatisticsViewModel 생성 (Factory 사용) -> ★ 여기가 추가되어야 합니다.
    private val statisticsViewModel: StatisticsViewModel by viewModels {
        StatisticsViewModelFactory(repository)
    }
    private var monitoringService: MonitoringService? = null
    // Compose에 전달하기 위한 상태 래퍼
    private var monitoringServiceState by mutableStateOf<MonitoringService?>(null)
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MonitoringService.MonitoringBinder
            monitoringService = binder?.getService()
            monitoringServiceState = monitoringService
            isServiceBound = true
            Log.d(TAG, "MonitoringService connected")

            //서비스에서 올라오는 PipelineResult를 HomeViewModel로 전달
            monitoringService?.setOnPipelineResultListener { result ->
                homeViewModel.onPipelineResult(result)
            }

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // 🔹 더 이상 콜백이 불리지 않도록 해제
            monitoringService?.setOnPipelineResultListener(null)

            monitoringService = null
            monitoringServiceState = null
            isServiceBound = false
            Log.d(TAG, "MonitoringService disconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 스플래시 화면 설정 (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }
        
        super.onCreate(savedInstanceState)

        // Service 바인딩
        bindMonitoringService()

        setContent {
            EyeOnTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraPermissionHandler(
                        onPermissionGranted = {
                            homeViewModel.updateCameraPermission(true)
                        },
                        onPermissionDenied = {
                            homeViewModel.updateCameraPermission(false)
                        }
                    )
                    
                    // 플로팅 윈도우 권한 확인
                    FloatingWindowPermissionHandler()

                    // 4. 두 ViewModel을 모두 전달합니다.
                    EyeOnApp(
                        homeViewModel = homeViewModel,
                        statisticsViewModel = statisticsViewModel,
                        monitoringService = monitoringServiceState
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Activity가 포그라운드로 올라올 때 플로팅 아이콘 숨기기
        monitoringService?.hideFloatingIcon()
        Log.d(TAG, "onResume - hiding floating icon")
    }

    override fun onPause() {
        super.onPause()
        // Activity가 백그라운드로 갈 때 플로팅 아이콘 보이기
        monitoringService?.showFloatingIcon()
        Log.d(TAG, "onPause - showing floating icon")
    }

    override fun onDestroy() {
        super.onDestroy()
        // 콜백 해제
        monitoringService?.setOnPipelineResultListener(null)
        monitoringServiceState = null
        // Service 바인딩 해제
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    private fun bindMonitoringService() {
        val intent = Intent(this, MonitoringService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}