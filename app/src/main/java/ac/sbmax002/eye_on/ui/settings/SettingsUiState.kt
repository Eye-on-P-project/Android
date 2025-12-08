package ac.sbmax002.eye_on.ui.settings

/**
 * 설정 화면의 UI 상태를 관리하는 데이터 클래스
 */
data class SettingsUiState(
    // 졸음 경고 1단계 설정
    val level1AlarmSound: AlarmSound = AlarmSound.CHIME,
    val level1Volume: Int = 70, // 0-100
    
    // 수면 경고 2단계 설정
    val level2AlarmSound: AlarmSound = AlarmSound.SIREN,
    val level2Volume: Int = 100, // 0-100
    
    // 플로팅 아이콘 크기
    val floatingIconSize: FloatingIconSize = FloatingIconSize.MEDIUM,
    
    // 기타 설정
    val vibrationEnabled: Boolean = true,
    val darkModeEnabled: Boolean = true
)

/**
 * 알림음 종류
 */
enum class AlarmSound(val displayName: String) {
    CHIME("띵동 (Chime)"),
    SIREN("사이렌 (Siren)")
}

/**
 * 플로팅 아이콘 크기
 */
enum class FloatingIconSize(val displayName: String) {
    SMALL("작게"),
    MEDIUM("중간"),
    LARGE("크게")
}

