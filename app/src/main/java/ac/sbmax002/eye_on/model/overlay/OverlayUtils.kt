package ac.sbmax002.eye_on.model.overlay

import android.graphics.RectF

/**
 * 정규화(0~1) 좌표를 PreviewView 기준 RectF로 바꾸는 등,
 * 오버레이 관련 좌표 변환 유틸 모음.
 */
object OverlayUtils {

    fun normalizedToViewRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        viewWidth: Int,
        viewHeight: Int
    ): RectF {
        // TODO: 필요하면 미러링/회전 보정까지 고려해서 수정
        return RectF(
            left * viewWidth,
            top * viewHeight,
            right * viewWidth,
            bottom * viewHeight
        )
    }
}