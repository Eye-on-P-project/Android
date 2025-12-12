package ac.sbmax002.eye_on.ui.floating

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.core.content.ContextCompat
import ac.sbmax002.eye_on.DTO.DrowsinessState
import ac.sbmax002.eye_on.R

/**
 * 플로팅 아이콘에 표시할 눈 모양을 표시하는 ImageView
 * 
 * - isFaceDetected가 false일 때: none_detect.png
 * - isFaceDetected가 true이고 drowsinessState에 따라:
 *   - NORMAL: open.png (완전 뜬 눈)
 *   - DROWSY: half_close.png (좀 감은 눈)
 *   - SLEEPING: close.png (거의 완전 감은 눈)
 */
class EyeIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {
    
    private var isFaceDetected: Boolean = false
    private var drowsinessState: DrowsinessState = DrowsinessState.NORMAL
    
    init {
        scaleType = ScaleType.FIT_CENTER
        setColorFilter(0xFFFFFFFF.toInt()) // 흰색 필터 적용
    }
    
    /**
     * 상태 업데이트
     */
    fun updateState(faceDetected: Boolean, state: DrowsinessState) {
        if (isFaceDetected != faceDetected || drowsinessState != state) {
            isFaceDetected = faceDetected
            drowsinessState = state
            updateIcon()
        }
    }
    
    /**
     * 상태에 따라 아이콘 업데이트
     */
    private fun updateIcon() {
        val drawableRes = when {
            !isFaceDetected -> R.drawable.none_detect
            drowsinessState == DrowsinessState.NORMAL -> R.drawable.open
            drowsinessState == DrowsinessState.DROWSY -> R.drawable.half_close
            drowsinessState == DrowsinessState.SLEEPING -> R.drawable.close
            else -> R.drawable.none_detect
        }
        
        setImageResource(drawableRes)
    }
}
