package ac.sbmax002.eye_on.repository

import ac.sbmax002.eye_on.ui.home.AppMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 앱 전역에서 공유되는 상태를 관리하는 Repository
 * 
 * 여러 화면에서 공유되어야 하는 상태들(appMode 등)을 중앙에서 관리
 * StateFlow를 사용하여 값이 변경되면 구독하는 모든 화면에 자동으로 반영
 * 
 * Singleton 패턴을 사용하여 앱 전체에서 하나의 인스턴스만 존재
 * 
 * ## 사용 방법
 * 
 * ### 1. 다른 화면에서 공유 상태 구독하기
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     // StateFlow를 collectAsStateWithLifecycle()로 구독하면
 *     // 값이 변경될 때마다 자동으로 리컴포지션 된다~
 *     val appMode by AppStateRepository.appMode.collectAsStateWithLifecycle()
 *     
 *     // appMode 값에 따라 UI를 변경할 수 있음
 *     when (appMode) {
 *         AppMode.DRIVING -> { /* 운전 모드 UI */ }
 *         AppMode.STUDY -> { /* 스터디 모드 UI */ }
 *     }
 * }
 * ```
 * 
 * ### 2. 공유 상태 변경하기
 * ```kotlin
 * // 어느 화면에서든 이렇게 호출하면 모든 화면에 자동 반영됨
 * AppStateRepository.setAppMode(AppMode.STUDY)
 * ```
 * 
 * ### 3. 현재 값 가져오기 (구독 없이)
 * ```kotlin
 * val currentMode = AppStateRepository.getCurrentAppMode()
 * ```
 */
object AppStateRepository {
    
    // 앱 모드 - 여러 화면에서 공유되는 상태
    private val _appMode = MutableStateFlow<AppMode>(AppMode.DRIVING)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    // 메모리상의 인증 정보 (Network Interceptor에서 동기적으로 접근하기 위함)
    var accessToken: String? = null
    var userId: Long? = null
    
    /**
     * 앱 모드 변경
     * 이 메서드를 호출하면 모든 화면에서 자동으로 반영 딘다
     * 
     * @param mode 변경할 앱 모드
     */
    fun setAppMode(mode: AppMode) {
        _appMode.value = mode
    }
    
    /**
     * 현재 앱 모드 get
     * StateFlow를 구독하지 않고 현재 값만 필요한 경우 사용한다
     * 
     * @return 현재 앱 모드
     */
    fun getCurrentAppMode(): AppMode = _appMode.value
    
    /**
     * 새로운 공유 상태를 추가하려면:
     * 1. private val _새상태 = MutableStateFlow<타입>(초기값) 추가
     * 2. val 새상태: StateFlow<타입> = _새상태.asStateFlow() 추가
     * 3. 필요시 setter 함수 추가
     */
}

