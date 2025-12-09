package ac.sbmax002.eye_on.navigation

/**
 * 앱의 모든 화면 경로(Route)를 정의하는 객체
 * 
 * 사용 예시:
 * - navController.navigate(Routes.HOME)
 * - navController.navigate("${Routes.DETAIL}/$sessionId")
 */
object Routes {
    const val HOME = "home"
    const val STATISTICS = "statistics"
    const val DETAIL = "detail"
    const val SETTINGS = "settings"
    const val LEVEL1_ALERT = "level1_alert"
    
    /**
     * 상세 화면으로 이동하는 경로 생성
     * @param sessionId 세션 ID
     * @return 완성된 경로 문자열
     */
    fun detail(sessionId: String): String = "$DETAIL/$sessionId"
}

