package ir.arminniromandi.permissionchecker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ir.arminniromandi.permissionchecker.data.source.AppIconLoader
import ir.arminniromandi.permissionchecker.ui.applist.AppListScreen
import ir.arminniromandi.permissionchecker.ui.applist.AppListViewModel
import ir.arminniromandi.permissionchecker.ui.detail.DetailScreen
import ir.arminniromandi.permissionchecker.ui.detail.DetailViewModel
import ir.arminniromandi.permissionchecker.ui.home.HomeScreen
import ir.arminniromandi.permissionchecker.ui.home.HomeViewModel

@Composable
fun AppNavHost(
    iconLoader: AppIconLoader,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToAppList = {
                    navController.navigate(AppListRoute)
                }
            )
        }

        composable<AppListRoute> {
            val viewModel: AppListViewModel = hiltViewModel()
            AppListScreen(
                viewModel = viewModel,
                iconLoader = iconLoader,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { packageName ->
                    navController.navigate(DetailRoute(packageName))
                }
            )
        }

        composable<DetailRoute> {
            val viewModel: DetailViewModel = hiltViewModel()
            DetailScreen(
                viewModel = viewModel,
                iconLoader = iconLoader,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
