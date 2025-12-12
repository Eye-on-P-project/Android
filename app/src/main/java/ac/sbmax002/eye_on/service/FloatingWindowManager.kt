package ac.sbmax002.eye_on.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.util.Log
import androidx.core.content.ContextCompat
import ac.sbmax002.eye_on.MainActivity
import ac.sbmax002.eye_on.DTO.DrowsinessState
import ac.sbmax002.eye_on.repository.SettingsRepository
import ac.sbmax002.eye_on.ui.floating.EyeIconView
import ac.sbmax002.eye_on.ui.settings.FloatingIconSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 플로팅 윈도우를 관리하는 매니저
 * 
 * - 플로팅 아이콘 생성 및 표시
 * - 드래그 가능
 * - 플로팅 아이콘 상태 업데이트 (졸음 상태에 따라)
 */
class FloatingWindowManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository? = null,
    private val onWakeUpClicked: (() -> Unit)? = null
) {
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var eyeIconView: EyeIconView? = null
    private var drowsinessState: DrowsinessState = DrowsinessState.NORMAL
    private var isFaceDetected: Boolean = false
    private var iconSizeDp: Int = ICON_SIZE_DP
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        
        // 설정값으로부터 아이콘 크기 가져오기 (비동기)
        if (settingsRepository != null) {
            coroutineScope.launch {
                try {
                    val iconSize = settingsRepository.floatingIconSize.first()
                    iconSizeDp = when (iconSize) {
                        FloatingIconSize.SMALL -> 48
                        FloatingIconSize.MEDIUM -> 56
                        FloatingIconSize.LARGE -> 64
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get icon size from settings", e)
                    iconSizeDp = ICON_SIZE_DP
                }
            }
        }
    }
    
    /**
     * 플로팅 윈도우 표시
     */
    fun showFloatingWindow() {
        if (floatingView != null) {
            Log.d(TAG, "Floating window already shown")
            return
        }
        
        // 플로팅 윈도우 권한 체크
        if (!FloatingWindowPermissionHelper.hasPermission(context)) {
            Log.e(TAG, "Overlay permission not granted. Cannot show floating window.")
            return
        }
        
        Log.d(TAG, "Showing floating window")
        
        try {
            // 플로팅 아이콘 뷰 생성
            floatingView = createFloatingIconView()
            
            // WindowManager 파라미터 설정
            val params = createWindowParams()
            
            // 플로팅 윈도우 추가
            windowManager?.addView(floatingView, params)
            Log.d(TAG, "Floating window added successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show floating window", e)
        }
    }
    
    /**
     * 플로팅 아이콘 뷰 생성
     */
    private fun createFloatingIconView(): View {
        // 설정값으로부터 아이콘 크기 가져오기
        val iconSizeDp = getIconSizeFromSettings()
        
        // 원형 배경과 아이콘을 포함하는 FrameLayout 생성
        val iconView = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                iconSizeDp.dpToPx(context),
                iconSizeDp.dpToPx(context)
            )
            
            // 원형 배경 설정
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(getStateColor(drowsinessState))
            }
            
            // 눈 모양 아이콘을 넣을 EyeIconView 생성 (중앙 정렬)
            eyeIconView = EyeIconView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    iconSizeDp.dpToPx(context) * 2 / 3, // 아이콘 크기는 배경의 2/3
                    iconSizeDp.dpToPx(context) * 2 / 3
                ).apply {
                    gravity = Gravity.CENTER
                }
                updateState(isFaceDetected, drowsinessState)
            }
            addView(eyeIconView)
            
            // 드래그와 클릭을 모두 처리하는 TouchListener 사용
            setOnTouchListener(FloatingIconTouchListener())
        }
        
        return iconView
    }
    
    /**
     * 설정값으로부터 아이콘 크기 가져오기
     */
    private fun getIconSizeFromSettings(): Int {
        return iconSizeDp
    }
    
    /**
     * 졸음 상태에 따른 색상 반환
     */
    private fun getStateColor(state: DrowsinessState): Int {
        return when (state) {
            DrowsinessState.NORMAL -> ContextCompat.getColor(context, android.R.color.holo_blue_light)
            DrowsinessState.DROWSY -> ContextCompat.getColor(context, android.R.color.holo_orange_light)
            DrowsinessState.SLEEPING -> ContextCompat.getColor(context, android.R.color.holo_red_light)
        }
    }
    
    /**
     * WindowManager 파라미터 생성
     */
    private fun createWindowParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val iconSizeDp = getIconSizeFromSettings()
        
        return WindowManager.LayoutParams(
            iconSizeDp.dpToPx(context),
            iconSizeDp.dpToPx(context),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 200 // 상단에서 200px 아래
        }
    }
    
    /**
     * 플로팅 아이콘 드래그 및 클릭 리스너
     * 드래그와 클릭을 구분하여 처리
     */
    private inner class FloatingIconTouchListener : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var isDragging = false
        private val CLICK_THRESHOLD = 10 // 픽셀 단위, 이 값 이내 이동이면 클릭으로 판단
        
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = (view.layoutParams as WindowManager.LayoutParams).x
                    initialY = (view.layoutParams as WindowManager.LayoutParams).y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = kotlin.math.abs(event.rawX - initialTouchX)
                    val deltaY = kotlin.math.abs(event.rawY - initialTouchY)
                    
                    // 일정 거리 이상 이동하면 드래그로 판단
                    if (deltaX > CLICK_THRESHOLD || deltaY > CLICK_THRESHOLD) {
                        isDragging = true
                        val params = view.layoutParams as WindowManager.LayoutParams
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(view, params)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                // 드래그가 아니면 클릭으로 판단하여 앱으로 복귀 + 알람 해제 신호
                    if (!isDragging) {
                    onWakeUpClicked?.invoke()
                        openMainActivity()
                    }
                    return true
                }
            }
            return false
        }
    }
    
    /**
     * 메인 Activity 열기 (원래 앱 화면으로 복귀)
     */
    private fun openMainActivity() {
        Log.d(TAG, "Opening main activity")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open main activity", e)
        }
    }
    
    /**
     * 플로팅 윈도우 숨김 (임시, 다시 보일 수 있음)
     */
    fun hideFloatingWindow() {
        Log.d(TAG, "Hiding floating window")
        floatingView?.visibility = View.GONE
    }
    
    /**
     * 플로팅 윈도우 보이기 (숨긴 것을 다시 보이게)
     */
    fun showFloatingWindowIfExists() {
        Log.d(TAG, "Showing floating window if exists")
        floatingView?.visibility = View.VISIBLE
    }
    
    /**
     * 플로팅 윈도우 완전히 제거 (모니터링 종료 시)
     */
    fun removeFloatingWindow() {
        floatingView?.let { view ->
            try {
                windowManager?.removeView(view)
                Log.d(TAG, "Floating window removed")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing floating window", e)
            }
            floatingView = null
        }
    }
    
    /**
     * 플로팅 아이콘 상태 업데이트 (얼굴 감지 여부와 졸음 상태에 따라)
     */
    fun updateState(faceDetected: Boolean, state: DrowsinessState) {
        Log.d(TAG, "Updating state: faceDetected=$faceDetected, drowsinessState=$state")
        this.isFaceDetected = faceDetected
        this.drowsinessState = state
        
        // UI 업데이트는 반드시 메인 스레드에서 실행되어야 함
        Handler(Looper.getMainLooper()).post {
            floatingView?.let { view ->
                // 색상 업데이트
                view.background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(getStateColor(state))
                }
                // 눈 모양 아이콘 업데이트
                eyeIconView?.updateState(faceDetected, state)
            }
        }
    }
    
    /**
     * 플로팅 아이콘 상태 업데이트 (졸음 상태에 따라) - 하위 호환성을 위한 메서드
     */
    @Deprecated("Use updateState(faceDetected, state) instead", ReplaceWith("updateState(false, state)"))
    fun updateDrowsinessState(state: DrowsinessState) {
        updateState(isFaceDetected, state)
    }
    
    
    private fun Int.dpToPx(context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (this * density).toInt()
    }
    
    companion object {
        private const val TAG = "FloatingWindowManager"
        private const val ICON_SIZE_DP = 56 // 플로팅 아이콘 크기 (dp)
    }
}

