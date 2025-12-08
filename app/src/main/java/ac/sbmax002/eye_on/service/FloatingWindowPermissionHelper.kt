package ac.sbmax002.eye_on.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * 플로팅 윈도우 권한을 체크하고 요청하는 헬퍼
 */
object FloatingWindowPermissionHelper {
    
    /**
     * 플로팅 윈도우 권한이 있는지 확인
     */
    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            // Android 6.0 미만에서는 항상 true
            true
        }
    }
    
    /**
     * 플로팅 윈도우 권한 요청을 위한 Intent 반환
     */
    fun getPermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            // Android 6.0 미만에서는 설정 화면으로
            Intent(Settings.ACTION_SETTINGS)
        }
    }
    
    /**
     * 플로팅 윈도우 권한 요청 (Activity에서 사용)
     */
    fun requestPermission(context: Context) {
        if (!hasPermission(context)) {
            try {
                val intent = getPermissionIntent(context)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "Opening overlay permission settings")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open overlay permission settings", e)
            }
        }
    }
    
    private const val TAG = "FloatingWindowPermissionHelper"
}

