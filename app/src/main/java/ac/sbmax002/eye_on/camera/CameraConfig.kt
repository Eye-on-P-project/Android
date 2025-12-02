package ac.sbmax002.eye_on.camera

import androidx.camera.core.CameraSelector
import android.util.Size

/**
 * CameraX 설정을 모아둔 데이터 클래스
 *
 * @property cameraSelector 사용할 카메라(전면/후면)
 * @property resolutionPreset 해상도 프리셋(Low, Medium, High)
 */
data class CameraConfig(
    val cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA, // 전/후면
    val resolutionPreset: ResolutionPreset = ResolutionPreset.MEDIUM //카메라 해상도 지정
)

/**
 * 해상도 프리셋
 */
enum class ResolutionPreset(val size: Size) {
    LOW(Size(480, 360)),      // 빠른 속도
    MEDIUM(Size(640, 480)),   // 기본값 (MediaPipe 권장)
    HIGH(Size(1280, 720))     // 화질 우선

}