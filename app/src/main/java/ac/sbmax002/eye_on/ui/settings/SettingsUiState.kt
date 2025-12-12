package ac.sbmax002.eye_on.ui.settings

/**
 * 설정 화면의 UI 상태를 관리하는 데이터 클래스
 */
data class SettingsUiState(
    // 졸음 경고 1단계 설정
    val level1AlarmSound: AlarmSound = AlarmSound.BELL_NOTIFICATION,
    val level1Volume: Int = 70, // 0-100
    
    // 졸음 감지 민감도
    val drowsinessSensitivity: DrowsinessSensitivity = DrowsinessSensitivity.LOW,
    
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
enum class AlarmSound(
    val displayName: String,
    val fileName: String
) {
    BELL_NOTIFICATION("안내 벨소리", "bell_notification"),
    FIRE_ALARM("화재 경보음", "fire_alarm_test"),
    MEGA_HORN("경적 소리", "mega_horn"),
    SCHOOL_BELL("학교 종소리", "school_bell"),
    SECURITY_ALARM("보안 경보음", "security_alarm"),
    SIREN("사이렌", "siren_alarm"),
    
    // 하위 호환성을 위한 기존 enum 값들 (deprecated)
    @Deprecated("Use BELL_NOTIFICATION instead", ReplaceWith("BELL_NOTIFICATION"))
    CHIME("안내 벨소리", "bell_notification"),
    
    @Deprecated("Use SIREN instead", ReplaceWith("SIREN"))
    SIREN_OLD("사이렌", "siren_alarm")
}

/**
 * 졸음 감지 민감도
 */
enum class DrowsinessSensitivity(
    val displayName: String,
    val drowsyDurationMs: Long
) {
    HIGH("높음", 500L),
    LOW("낮음", 700L)
}

/**
 * 플로팅 아이콘 크기
 */
enum class FloatingIconSize(val displayName: String) {
    SMALL("작게"),
    MEDIUM("중간"),
    LARGE("크게")
}

