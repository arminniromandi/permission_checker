package ir.arminniromandi.permissionchecker.ui.navigation

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.arminniromandi.permissionchecker.R
import ir.arminniromandi.permissionchecker.data.source.AppIconLoader
import ir.arminniromandi.permissionchecker.ui.applist.AppListScreen
import ir.arminniromandi.permissionchecker.ui.applist.AppListViewModel
import ir.arminniromandi.permissionchecker.ui.detail.DetailScreen
import ir.arminniromandi.permissionchecker.ui.detail.DetailViewModel
import ir.arminniromandi.permissionchecker.ui.home.HomeScreen
import ir.arminniromandi.permissionchecker.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    iconLoader: AppIconLoader,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val view = LocalView.current
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val isDarkTheme = isSystemInDarkTheme()

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
                ?: (generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
                    .filterIsInstance<Activity>()
                    .firstOrNull()?.window)
            if (window != null) {
                window.statusBarColor = surfaceColor
                window.navigationBarColor = surfaceColor
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destination = navBackStackEntry?.destination

    val isHome = destination?.hasRoute<HomeRoute>() ?: true
    val isAppList = destination?.hasRoute<AppListRoute>() ?: false
    val isDetail = destination?.hasRoute<DetailRoute>() ?: false

    Scaffold(
        modifier = modifier.fillMaxSize().systemBarsPadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    when {
                        isAppList -> {
                            Text(
                                text = stringResource(R.string.app_list_title),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        isDetail -> {
                            Text(
                                text = stringResource(R.string.detail_title),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        else -> {}
                    }
                },
                navigationIcon = {
                    if (!isHome && navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
}
