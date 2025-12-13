package ac.sbmax002.eye_on.model.statistics

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlin.math.roundToInt

/**
 * 배터리 시작/종료 시점을 기록해 사용량(%)을 계산하는 헬퍼.
 * - 측정 실패 시 null을 반환하여 호출 측에서 안전하게 처리하도록 한다.
 */
class BatteryUsageTracker(private val context: Context) {

    private var startLevel: Int? = null

    data class Snapshot(
        val startBatteryPercent: Int?,
        val endBatteryPercent: Int?,
        val usagePercent: Int?
    )

    /** 모니터링 시작 시 배터리 퍼센트를 기록한다. */
    fun markStart(): Int? {
        val level = currentBatteryPercent()
        startLevel = level
        return level
    }

    /** 모니터링 종료 시 배터리 퍼센트를 읽고 사용량을 계산한다. */
    fun markEnd(): Snapshot {
        val end = currentBatteryPercent()
        val start = startLevel
        val usage = if (start != null && end != null) {
            (start - end).coerceAtLeast(0)
        } else {
            null
        }
        return Snapshot(
            startBatteryPercent = start,
            endBatteryPercent = end,
            usagePercent = usage
        )
    }

    /** 다음 세션을 위해 상태를 초기화한다. */
    fun reset() {
        startLevel = null
    }

    private fun currentBatteryPercent(): Int? {
        // 우선 BatteryManager API 시도
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val capacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (capacity != null && capacity in 1..100) {
                return capacity
            }
        }

        // fallback: ACTION_BATTERY_CHANGED 브로드캐스트 사용
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null

        return ((level.toFloat() / scale.toFloat()) * 100f).roundToInt()
    }
}


