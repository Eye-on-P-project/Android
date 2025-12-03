package ac.sbmax002.eye_on.model.overlay

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 카메라 프리뷰 위에 눈 ROI / 상태를 그려줄 Compose 오버레이.
 * 지금은 빈 Canvas만 두고, 나중에 실제 그리기 로직을 넣는다.
 */
@Composable
fun EyeOverlay(
    modifier: Modifier = Modifier,
    leftEyeRect: RectF? = null,
    rightEyeRect: RectF? = null,
    isDrowsy: Boolean = false
) {
    Canvas(modifier = modifier) {
        // TODO: 다음 단계에서 RectF를 이용해 박스/라인/텍스트 등을 그림
    }
}