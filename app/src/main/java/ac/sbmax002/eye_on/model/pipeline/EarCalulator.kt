package ac.sbmax002.eye_on.model.pipeline

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.hypot

/**
 * 눈 랜드마크 좌표들에서 EAR 값을 계산하는 클래스.
 */
class EarCalculator {

    /**
     * eyePoints 리스트는 p1~p6 순서로 들어옴
     * EAR = (||p2 - p6|| + ||p3 - p5||) / (2 * ||p1 - p4||)
     */
    fun calculateEar(eyePoints: List<NormalizedLandmark>): Float {
        if (eyePoints.size < 6) return 0f

        fun dist(a: NormalizedLandmark, b: NormalizedLandmark): Float {
            val dx = a.x() - b.x()
            val dy = a.y() - b.y()
            return hypot(dx.toDouble(), dy.toDouble()).toFloat()
        }

        val p1 = eyePoints[0]
        val p2 = eyePoints[1]
        val p3 = eyePoints[2]
        val p4 = eyePoints[3]
        val p5 = eyePoints[4]
        val p6 = eyePoints[5]

        val A = dist(p2, p6)
        val B = dist(p3, p5)
        val C = dist(p1, p4)

        if (C == 0f) return 0f
        return (A + B) / (2f * C)
    }
}