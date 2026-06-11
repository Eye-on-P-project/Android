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
import ac.sbmax002.eye_on.service.MonitoringService
import ac.sbmax002.eye_on.ui.statistics.StatisticsScreen
import ac.sbmax002.eye_on.ui.statistics.DetailScreen
import ac.sbmax002.eye_on.ui.statistics.StatisticsViewModel
import ac.sbmax002.eye_on.ui.settings.SettingsScreen
import ac.sbmax002.eye_on.ui.settings.Level1AlertScreen
import ac.sbmax002.eye_on.ui.settings.Level2AlertScreen
import ac.sbmax002.eye_on.ui.settings.AccountScreen
import ac.sbmax002.eye_on.ui.settings.EditProfileScreen
import ac.sbmax002.eye_on.ui.settings.BlockedAppsScreen
import ac.sbmax002.eye_on.ui.subscription.SubscriptionStatusScreen
import ac.sbmax002.eye_on.ui.subscription.SubscriptionPlanScreen
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
    statisticsViewModel: StatisticsViewModel,
    monitoringService: MonitoringService?,
    startDestination: String = Routes.LOGIN
) {
    // 네비게이션 컨트롤러 생성 (화면 이동 관리자)
    val navController = rememberNavController()

    // 네비게이션 호스트 (모든 화면을 여기서 관리)
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 로그인 화면
        composable(Routes.LOGIN) {
            ac.sbmax002.eye_on.ui.login.LoginScreen(
                onNavigateToHome = {
                    // 로그인 완료 후 홈 화면으로 이동하며, 뒤로가기 시 로그인 화면이 나오지 않도록 스택에서 제거
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Routes.SIGN_UP)
                }
            )
        }

        // 회원가입 화면
        composable(Routes.SIGN_UP) {
            ac.sbmax002.eye_on.ui.login.SignUpScreen(
                onNavigateToHome = {
                    // 가입 완료 후 홈 화면으로 이동
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 1. 홈 화면
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToStatistics = {
                    navController.navigate(Routes.STATISTICS)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                monitoringService = monitoringService
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
                },
                onNavigateToLevel2Alert = {
                    navController.navigate(Routes.LEVEL2_ALERT)
                },
                onNavigateToAccount = {
                    navController.navigate(Routes.ACCOUNT)
                },
                onNavigateToSubscription = {
                    navController.navigate(Routes.SUBSCRIPTION_STATUS)
                },
                onNavigateToBlockedApps = {
                    navController.navigate(Routes.BLOCKED_APPS)
                }
            )
        }

        // 5. 1단계 알림음 설정 화면
        composable(Routes.LEVEL1_ALERT) {
            Level1AlertScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 6. 2단계 알림음 설정 화면
        composable(Routes.LEVEL2_ALERT) {
            Level2AlertScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 7. 계정 화면
        composable(Routes.ACCOUNT) {
            AccountScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditProfile = {
                    navController.navigate(Routes.EDIT_PROFILE)
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToSubscription = {
                    navController.navigate(Routes.SUBSCRIPTION_STATUS)
                }
            )
        }

        // 8. 회원 정보 수정 화면
        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 9. 구독 상태 화면
        composable(Routes.SUBSCRIPTION_STATUS) {
            SubscriptionStatusScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlan = {
                    navController.navigate(Routes.SUBSCRIPTION_PLAN)
                }
            )
        }

        // 10. 요금제 선택 화면
        composable(Routes.SUBSCRIPTION_PLAN) {
            SubscriptionPlanScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 11. 스터디 앱 차단 화면
        composable(Routes.BLOCKED_APPS) {
            BlockedAppsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

