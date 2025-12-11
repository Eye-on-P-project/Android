package ac.sbmax002.eye_on.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import ac.sbmax002.eye_on.ui.home.HomeScreen
import ac.sbmax002.eye_on.ui.home.HomeViewModel
import ac.sbmax002.eye_on.ui.statistics.StatisticsScreen
import ac.sbmax002.eye_on.ui.statistics.DetailScreen
import ac.sbmax002.eye_on.ui.statistics.StatisticsViewModel
import ac.sbmax002.eye_on.ui.settings.SettingsScreen
import ac.sbmax002.eye_on.ui.settings.Level1AlertScreen
import ac.sbmax002.eye_on.navigation.Routes

/**
 * 앱의 메인 네비게이션 컴포저블
 * 
 * 모든 화면 전환을 관리하는 NavHost를 포함합니다.
 * 
 * @param homeViewModel 홈 화면의 ViewModel
 * @param statisticsViewModel 통계 화면의 ViewModel (기본값으로 자동 생성)
 */
@Composable
fun EyeOnApp(
    homeViewModel: HomeViewModel,
    // ★ 수정: "= viewModel()" 기본값 제거 (반드시 주입받도록 강제)
    statisticsViewModel: StatisticsViewModel
) {
    // 네비게이션 컨트롤러 생성 (화면 이동 관리자)
    val navController = rememberNavController()

    // 네비게이션 호스트 (모든 화면을 여기서 관리)
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        // 1. 홈 화면
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToStatistics = {
                    navController.navigate(Routes.STATISTICS)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        // 2. 통계 화면
        composable(Routes.STATISTICS) {
            StatisticsScreen(
                homeViewModel = homeViewModel,
                viewModel = statisticsViewModel,
// 버그 해결 현재 통계화면 일 때만 popBackStack 실행 (중복 방지)
                onNavigateBack = {
                    if (navController.currentDestination?.route == Routes.STATISTICS) {
                        navController.popBackStack()
                    }
                },
                onNavigateToDetail = { sessionId ->
                    navController.navigate(Routes.detail(sessionId))
                }
            )
        }

        // 3. 통계 상세 화면 (Argument 처리)
        composable(
            route = "${Routes.DETAIL}/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable

            DetailScreen(
                sessionId = sessionId,
                viewModel = statisticsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 4. 설정 화면
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLevel1Alert = {
                    navController.navigate(Routes.LEVEL1_ALERT)
                }
            )
        }

        // 5. 1단계 알림음 설정 화면
        composable(Routes.LEVEL1_ALERT) {
            Level1AlertScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

