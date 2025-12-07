package ac.sbmax002.eye_on

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import ac.sbmax002.eye_on.database.AppDatabase
import ac.sbmax002.eye_on.repository.StatisticsRepository
import ac.sbmax002.eye_on.ui.home.CameraPermissionHandler
import ac.sbmax002.eye_on.ui.home.HomeViewModel
import ac.sbmax002.eye_on.ui.home.HomeViewModelFactory
import ac.sbmax002.eye_on.ui.statistics.StatisticsViewModel
import ac.sbmax002.eye_on.ui.statistics.StatisticsViewModelFactory
import ac.sbmax002.eye_on.ui.theme.EyeOnTheme
import ac.sbmax002.eye_on.navigation.EyeOnApp


class MainActivity : ComponentActivity() {

    // 1. DB와 Repository는 한 번만 생성해서 공유합니다.
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { StatisticsRepository(database.statisticsDao()) }

    // 2. HomeViewModel 생성 (Factory 사용)
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(repository)
    }

    // 3. StatisticsViewModel 생성 (Factory 사용) -> ★ 여기가 추가되어야 합니다.
    private val statisticsViewModel: StatisticsViewModel by viewModels {
        StatisticsViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                    // 4. 두 ViewModel을 모두 전달합니다.
                    EyeOnApp(
                        homeViewModel = homeViewModel,
                        statisticsViewModel = statisticsViewModel
                    )
                }
            }
        }
    }
}