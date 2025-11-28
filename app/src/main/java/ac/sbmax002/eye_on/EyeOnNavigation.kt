package ac.sbmax002.eye_on

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

// 화면 경로(Route) 정의
object EyeOnDestinations {
    const val HOME = "home"
    const val STATISTICS = "statistics"
    const val DETAIL = "detail"
    const val SETTINGS = "settings"
}

@Composable
fun EyeOnApp(
    homeViewModel: HomeViewModel,
    statisticsViewModel: ac.sbmax002.eye_on.ui.statistics.StatisticsViewModel = androidx.lifecycle.viewmodel.compose.viewModel() // ViewModel 주입 위치 변경 추천
){
    // 네비게이션 컨트롤러 생성 (화면 이동 관리자)
    val navController = rememberNavController()

    // 네비게이션 호스트 (여기서 화면을 갈아끼워줍니다)
    NavHost(
        navController = navController,
        startDestination = EyeOnDestinations.HOME
    ) {
        // 1. 홈 화면
        composable(EyeOnDestinations.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToStatistics = {
                    navController.navigate(EyeOnDestinations.STATISTICS)
                },
                onNavigateToSettings = {
                    navController.navigate(EyeOnDestinations.SETTINGS)
                }
            )
        }

        // 2. 통계 화면 (새로 만든 기능)
        composable(EyeOnDestinations.STATISTICS) {
            ac.sbmax002.eye_on.ui.statistics.StatisticsScreen(
                viewModel = statisticsViewModel, // 뷰모델 전달
                onNavigateBack = { navController.popBackStack() },
                // [신규] 리스트 클릭 시 상세 화면으로 이동
                onNavigateToDetail = { sessionId ->
                    navController.navigate("${EyeOnDestinations.DETAIL}/$sessionId")
                }
            )
        }

        // 통계 상세 화면 (Argument 처리)
        composable(
            route = "${EyeOnDestinations.DETAIL}/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable

            DetailScreen(
                sessionId = sessionId,
                viewModel = statisticsViewModel, // 데이터 공유를 위해 동일한 뷰모델 사용
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 3. 설정 화면 (나중에 구현)
        composable(EyeOnDestinations.SETTINGS) {
            // SettingsScreen(...)
        }
    }
}