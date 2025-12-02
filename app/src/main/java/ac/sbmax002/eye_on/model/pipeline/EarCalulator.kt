package ac.sbmax002.eye_on.model.pipeline

/**
 * 눈 랜드마크 좌표들에서 EAR 값을 계산하는 클래스.
 */
class EarCalculator {

    /**
     * @param eyePoints 눈 주변 랜드마크 좌표들
     *        (나중에 인덱스/순서 정의해서 전달)
     * @return EAR 값
     */
    fun calculateEar(eyePoints: List<Pair<Float, Float>>): Float {
        // TODO: 나중에 EAR 공식으로 실제 계산 로직 채우기
        return 0f
    }
}