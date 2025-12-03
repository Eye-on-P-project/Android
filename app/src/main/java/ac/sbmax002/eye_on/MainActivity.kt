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
import ac.sbmax002.eye_on.ui.home.CameraPermissionHandler
import ac.sbmax002.eye_on.ui.home.HomeViewModel
import ac.sbmax002.eye_on.ui.theme.EyeOnTheme
import ac.sbmax002.eye_on.navigation.EyeOnApp


class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

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

                    // 2. 화면 관리 로직은 EyeOnApp으로 위임 (여기가 핵심!)
                    EyeOnApp(homeViewModel = homeViewModel)
                }
            }
        }
    }
}