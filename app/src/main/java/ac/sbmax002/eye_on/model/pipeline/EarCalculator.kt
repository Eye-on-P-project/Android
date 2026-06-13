package ac.sbmax002.eye_on.model.pipeline

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.hypot

class EarCalculator {
    fun calculateEar(eyePoints: List<NormalizedLandmark>): Float {
        if (eyePoints.size < 6) return 0f

        fun dist(a: NormalizedLandmark, b: NormalizedLandmark): Float {
            val dx = a.x() - b.x()
            val dy = a.y() - b.y()
            return hypot(dx.toDouble(), dy.toDouble()).toFloat()
        }

        val horizontal = dist(eyePoints[0], eyePoints[3])
        if (horizontal <= 0f) return 0f

        val vertical1 = dist(eyePoints[1], eyePoints[5])
        val vertical2 = dist(eyePoints[2], eyePoints[4])
        return (vertical1 + vertical2) / (2f * horizontal)
    }
}
