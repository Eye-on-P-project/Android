package ac.sbmax002.eye_on.camera

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import ac.sbmax002.eye_on.camera.CameraManager

@Composable
fun CameraPreview(
    cameraManager: CameraManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // PreviewView를 remember로 보관
    val previewView = remember { // 블록 안에서 만든 객체를 한 번만 생성하고 재사용
        PreviewView(context).apply { // 실제 카메라 화면을 그림. apply를 통해 기본 크기 설정
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // LaunchedEffect는 Composable 스코프에서 호출
    LaunchedEffect(Unit) { //한 번만 실행되어야 하기 때문에 LaunchedEffect 안에서 실행(Compose 방식)
        cameraManager.startCamera(previewView)
    }

    // Compose → AndroidView
    AndroidView( //compose에서 기존 Android View 시스템을 브릿지
        modifier = modifier,
        factory = { previewView }
    )
}
