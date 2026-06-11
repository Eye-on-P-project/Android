package ac.sbmax002.eye_on.ui.settings

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ac.sbmax002.eye_on.repository.StudyBlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 차단할 앱 선택 화면을 지원하는 데이터 구조
 */
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val isBlocked: Boolean
)

/**
 * 앱 선택 화면의 UI 상태를 정의하는 데이터 클래스
 */
data class BlockedAppsUiState(
    val isBlockingEnabled: Boolean = false,
    val installedApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val hasUsagePermission: Boolean = false
)

/**
 * 차단할 앱 목록 설정 관리 뷰모델
 */
@HiltViewModel
class BlockedAppsViewModel @Inject constructor(
    private val repository: StudyBlockRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedAppsUiState())
    val uiState: StateFlow<BlockedAppsUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            isBlockingEnabled = repository.isBlockingEnabled(),
            hasUsagePermission = hasUsageStatsPermission(context)
        )
        loadInstalledApps()
    }

    /**
     * 사용 정보 접근 권한 상태 체크 및 변경 사항 반영
     */
    fun checkPermission() {
        val hasPermission = hasUsageStatsPermission(context)
        _uiState.value = _uiState.value.copy(hasUsagePermission = hasPermission)
        
        // 사용자가 설정에서 권한을 해제했을 경우 차단 스위치도 오프 처리
        if (!hasPermission && _uiState.value.isBlockingEnabled) {
            repository.setBlockingEnabled(false)
            _uiState.value = _uiState.value.copy(isBlockingEnabled = false)
        }
    }

    /**
     * 앱 차단 마스터 기능 켜기/끄기
     */
    fun setBlockingEnabled(enabled: Boolean) {
        repository.setBlockingEnabled(enabled)
        _uiState.value = _uiState.value.copy(isBlockingEnabled = enabled)
    }

    /**
     * 특정 앱 차단 여부 토글
     */
    fun toggleAppBlocked(packageName: String) {
        val blocked = repository.getBlockedPackages().toMutableSet()
        val isCurrentlyBlocked = blocked.contains(packageName)
        
        if (isCurrentlyBlocked) {
            blocked.remove(packageName)
        } else {
            blocked.add(packageName)
        }
        repository.setBlockedPackages(blocked)
        
        // 상태 목록 즉시 갱신
        val updatedApps = _uiState.value.installedApps.map {
            if (it.packageName == packageName) {
                it.copy(isBlocked = !it.isBlocked)
            } else {
                it
            }
        }
        _uiState.value = _uiState.value.copy(
            installedApps = updatedApps,
            filteredApps = filterApps(updatedApps, _uiState.value.searchQuery)
        )
    }

    /**
     * 검색 쿼리 설정
     */
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredApps = filterApps(_uiState.value.installedApps, query)
        )
    }

    private fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return apps
        return apps.filter { it.name.contains(query, ignoreCase = true) }
    }

    /**
     * 기기에 설치된 실행 가능 앱 목록 로드
     */
    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val apps = withContext(Dispatchers.IO) {
                retrieveLaunchableApps(context, repository.getBlockedPackages())
            }
            _uiState.value = _uiState.value.copy(
                installedApps = apps,
                filteredApps = filterApps(apps, _uiState.value.searchQuery),
                isLoading = false
            )
        }
    }

    /**
     * 실행 가능한 런처 앱 목록 조회 (기본 홈 화면 및 본인 앱 예외처리)
     */
    private fun retrieveLaunchableApps(context: Context, blockedSet: Set<String>): List<AppInfo> {
        val pm = context.packageManager
        
        // 실행 가능한 런처 카테고리의 앱 쿼리
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        
        // 현재 기기의 기본 홈화면(런처) 패키지 식별
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = pm.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val launcherPackage = resolveInfo?.activityInfo?.packageName
        
        val appList = mutableListOf<AppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (info in resolveInfos) {
            val pkgName = info.activityInfo.packageName
            // 자사 앱, 기본 홈 런처, 중복 패키지는 차단 대상 목록에서 제외
            if (pkgName == context.packageName || pkgName == launcherPackage || seenPackages.contains(pkgName)) {
                continue
            }
            seenPackages.add(pkgName)
            
            try {
                val label = info.loadLabel(pm).toString()
                val icon = info.loadIcon(pm)
                appList.add(
                    AppInfo(
                        name = label,
                        packageName = pkgName,
                        icon = icon,
                        isBlocked = blockedSet.contains(pkgName)
                    )
                )
            } catch (e: Exception) {
                // 일부 시스템 패키지 로딩 실패 시 생략
            }
        }
        
        // 이름 기준 오름차순 정렬
        return appList.sortedBy { it.name }
    }

    /**
     * 사용 정보 접근 권한 승인 여부 검증
     */
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
