package ac.sbmax002.eye_on.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.util.Log
import androidx.core.content.ContextCompat
import ac.sbmax002.eye_on.MainActivity

/**
 * 플로팅 윈도우를 관리하는 매니저
 * 
 * - 플로팅 아이콘 생성 및 표시
 * - 드래그 가능
 * - 플로팅 아이콘 상태 업데이트 (졸음 상태에 따라)
 */
class FloatingWindowManager(private val context: Context) {
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isDrowsy = false
    
    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
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
        // 간단한 원형 뷰 생성 (일단 기본 아이콘)
        val iconView = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ICON_SIZE_DP.dpToPx(context),
                ICON_SIZE_DP.dpToPx(context)
            )
            setBackgroundColor(
                if (isDrowsy) {
                    ContextCompat.getColor(context, android.R.color.holo_red_dark)
                } else {
                    ContextCompat.getColor(context, android.R.color.holo_blue_dark)
                }
            )
            
            // 원형으로 만들기
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(
                    if (isDrowsy) {
                        ContextCompat.getColor(context, android.R.color.holo_red_dark)
                    } else {
                        ContextCompat.getColor(context, android.R.color.holo_blue_dark)
                    }
                )
            }
            
            // 클릭 리스너 - 원래 앱으로 돌아가기
            setOnClickListener {
                openMainActivity()
            }
            
            // 드래그 리스너
            setOnTouchListener(FloatingIconTouchListener())
        }
        
        return iconView
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
        
        return WindowManager.LayoutParams(
            ICON_SIZE_DP.dpToPx(context),
            ICON_SIZE_DP.dpToPx(context),
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
     * 플로팅 아이콘 드래그 리스너
     */
    private inner class FloatingIconTouchListener : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = (view.layoutParams as WindowManager.LayoutParams).x
                    initialY = (view.layoutParams as WindowManager.LayoutParams).y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val params = view.layoutParams as WindowManager.LayoutParams
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    return true
                }
            }
            return false
        }
    }
    
    /**
     * 메인 Activity 열기
     */
    private fun openMainActivity() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }
    
    /**
     * 플로팅 윈도우 숨김
     */
    fun hideFloatingWindow() {
        Log.d(TAG, "Hiding floating window")
        removeFloatingWindow()
    }
    
    /**
     * 플로팅 아이콘 상태 업데이트 (졸음 상태에 따라)
     */
    fun updateDrowsinessState(isDrowsy: Boolean) {
        Log.d(TAG, "Updating drowsiness state: $isDrowsy")
        this.isDrowsy = isDrowsy
        
        floatingView?.let { view ->
            // 색상 업데이트
            view.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(
                    if (isDrowsy) {
                        ContextCompat.getColor(context, android.R.color.holo_red_dark)
                    } else {
                        ContextCompat.getColor(context, android.R.color.holo_blue_dark)
                    }
                )
            }
        }
    }
    
    /**
     * 플로팅 윈도우 제거
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
    
    private fun Int.dpToPx(context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (this * density).toInt()
    }
    
    companion object {
        private const val TAG = "FloatingWindowManager"
        private const val ICON_SIZE_DP = 56 // 플로팅 아이콘 크기 (dp)
    }
}

