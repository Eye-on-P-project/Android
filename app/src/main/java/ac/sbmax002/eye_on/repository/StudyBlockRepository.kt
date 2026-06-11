package ac.sbmax002.eye_on.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 스터디 모드 전용 앱 차단 설정을 관리하는 Repository
 * 다른 팀원의 코드 영향을 최소화하기 위해 독립된 SharedPreferences("study_block_prefs")를 사용합니다.
 */
@Singleton
class StudyBlockRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("study_block_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BLOCKING_ENABLED = "study_block_enabled"
        private const val KEY_BLOCKED_PACKAGES = "study_blocked_packages"
    }

    /**
     * 앱 차단 기능 활성화 여부 조회
     */
    fun isBlockingEnabled(): Boolean {
        return prefs.getBoolean(KEY_BLOCKING_ENABLED, false)
    }

    /**
     * 앱 차단 기능 활성화 여부 저장
     */
    fun setBlockingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCKING_ENABLED, enabled).apply()
    }

    /**
     * 차단 설정된 앱 패키지명 세트 조회
     */
    fun getBlockedPackages(): Set<String> {
        // SharedPreferences.getStringSet은 반환 객체를 직접 수정하면 안 되므로 새 Set으로 반환 처리
        val set = prefs.getStringSet(KEY_BLOCKED_PACKAGES, emptySet())
        return if (set != null) HashSet(set) else emptySet()
    }

    /**
     * 차단 설정할 앱 패키지명 세트 저장
     */
    fun setBlockedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, packages).apply()
    }

    /**
     * 특정 앱 차단 대상에 추가
     */
    fun addBlockedPackage(packageName: String) {
        val current = getBlockedPackages().toMutableSet()
        current.add(packageName)
        setBlockedPackages(current)
    }

    /**
     * 특정 앱 차단 대상에서 제거
     */
    fun removeBlockedPackage(packageName: String) {
        val current = getBlockedPackages().toMutableSet()
        current.remove(packageName)
        setBlockedPackages(current)
    }
}
