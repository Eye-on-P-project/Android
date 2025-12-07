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
import android.widget.ImageView
import android.util.Log
import androidx.core.content.ContextCompat
import ac.sbmax002.eye_on.MainActivity
import ac.sbmax002.eye_on.DTO.DrowsinessState
import android.graphics.Bitmap
import android.graphics.Outline
import android.view.ViewOutlineProvider
import android.widget.LinearLayout

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
    private var iconImageView: ImageView? = null

    // 플로팅 아이콘 내부의 눈 미리보기용 뷰
    private var leftEyeView: ImageView? = null
    private var rightEyeView: ImageView? = null
    private var drowsinessState: DrowsinessState = DrowsinessState.NORMAL

    // UI 업데이트용 메인 핸들러
    private val mainHandler = Handler(Looper.getMainLooper())

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
     *- 바깥은 동그란 배경(졸음 상태 색)
     * - 안쪽에 작은 동그라미 2개(왼/오른쪽 눈 프리뷰)
     */
    private fun createFloatingIconView(): View {
        val iconSizePx = ICON_SIZE_DP.dpToPx(context)

        // 원형 배경과 아이콘을 포함하는 FrameLayout 생성
        val iconView = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ICON_SIZE_DP.dpToPx(context),
                ICON_SIZE_DP.dpToPx(context)
            )

            // 원형 배경 설정
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(getStateColor(drowsinessState))
            }

            // 뷰 전체를 동그랗게 클리핑
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                clipToOutline = true
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
            }

            // 안쪽에 눈 두 개를 놓을 컨테이너 (가운데 정렬)
            val eyeContainer = LinearLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                orientation = LinearLayout.HORIZONTAL
            }

            // 눈 크기 : 아이콘 크기의 ~42%, 간격 약간
            val eyeSize = (iconSizePx * 0.42f).toInt()
            val eyeSpacing = (iconSizePx * 0.06f).toInt()

            // 동그란 "눈 창" 하나 만드는 헬퍼
            fun createEyeView(): ImageView {
                return ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        eyeSize,
                        eyeSize
                    ).apply {
                        setMargins(eyeSpacing / 2, 0, eyeSpacing / 2, 0)
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP

                    // 이 뷰 자체를 동그랗게 클리핑
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        clipToOutline = true
                        outlineProvider = object : ViewOutlineProvider() {
                            override fun getOutline(view: View, outline: Outline) {
                                outline.setOval(0, 0, view.width, view.height)
                            }
                        }
                    }

                    // 배경은 투명 (뒤에 아이콘 색 보이게)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            }


            // 왼쪽/오른쪽 눈 뷰 생성
            leftEyeView = createEyeView()
            rightEyeView = createEyeView()

            eyeContainer.addView(leftEyeView)
            eyeContainer.addView(rightEyeView)

            addView(eyeContainer)

            // 아이콘을 넣을 ImageView 생성 (중앙 정렬)
            iconImageView = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ICON_SIZE_DP.dpToPx(context) / 2, // 아이콘 크기는 배경의 절반
                    ICON_SIZE_DP.dpToPx(context) / 2
                ).apply {
                    gravity = Gravity.CENTER
                }
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                // TODO: 여기에 아이콘 리소스 넣을 수 ㅣㅇㅆ음
                // setImageResource(R.drawable.ic_eye_on)
            }
            addView(iconImageView)

            // 드래그와 클릭을 모두 처리하는 TouchListener 사용
            setOnTouchListener(FloatingIconTouchListener())
        }

        return iconView
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
                    // 드래그가 아니면 클릭으로 판단하여 앱으로 복귀
                    if (!isDragging) {
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
     * 플로팅 아이콘 상태 업데이트 (졸음 상태에 따라)
     */
    fun updateDrowsinessState(state: DrowsinessState) {
        Log.d(TAG, "Updating drowsiness state: $state")
        this.drowsinessState = state

        // UI 업데이트는 반드시 메인 스레드에서 실행되어야 함
        Handler(Looper.getMainLooper()).post {
            floatingView?.let { view ->
                // 색상 업데이트
                view.background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(getStateColor(state))
                }
                // TODO: 상태에 따라 아이콘도 변경 가능
                // iconImageView?.setImageResource(getStateIcon(state))
            }
        }
    }

    /**
     * 파이프라인에서 내려온 눈 Bitmap 두 개를
     * 플로팅 아이콘 안의 "눈 창"에 반영
     */
    fun updateEyePreview(left: Bitmap?, right: Bitmap?) {
        if (leftEyeView == null && rightEyeView == null) return

        mainHandler.post {
            // 전면 카메라 미러링 때문에 좌/우 뒤집어서 보여줌
            val visualLeft = right
            val visualRight = left

            leftEyeView?.let { view ->
                if (visualLeft != null) {
                    view.setImageBitmap(visualLeft)
                } else {
                    view.setImageDrawable(null)
                }
            }

            rightEyeView?.let { view ->
                if (visualRight != null) {
                    view.setImageBitmap(visualRight)
                } else {
                    view.setImageDrawable(null)
                }
            }
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
