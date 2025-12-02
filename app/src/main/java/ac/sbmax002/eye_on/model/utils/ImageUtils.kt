package ac.sbmax002.eye_on.model.utils

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

/**
 * CameraX ImageProxy -> Bitmap 변환 등,
 * 비전 공용 유틸 모음.
 */
object ImageUtils {

    /**
     * YUV_420 ImageProxy를 ARGB_8888 Bitmap으로 변환하는 자리.
     * 지금은 뼈대만 나중에 실제 변환 로직 구현.
     */
    fun yuvToRgba(imageProxy: ImageProxy): Bitmap {
        // 이 함수가 호출되면 바로 예외가 터지도록 해서
        // 구현 전에 실수로 쓰이지 않게
        throw NotImplementedError("yuvToRgba()는 아직 구현하지 않았습니다.")
    }
}