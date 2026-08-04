package com.owlcoder.animeschedule.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.iosDecelerate
import com.owlcoder.animeschedule.presentation.components.iosTween
import com.owlcoder.animeschedule.presentation.navigation.Screen.Detail
import com.owlcoder.animeschedule.presentation.screens.detail.AnimeDetailScreen
import com.owlcoder.animeschedule.presentation.screens.mylist.MyListScreen
import com.owlcoder.animeschedule.presentation.screens.schedule.AllTodayScreen
import com.owlcoder.animeschedule.presentation.screens.schedule.ScheduleScreen
import com.owlcoder.animeschedule.presentation.screens.schedule.ScheduleViewModel
import com.owlcoder.animeschedule.presentation.screens.search.SearchScreen
import com.owlcoder.animeschedule.presentation.screens.settings.SettingsScreen
import com.owlcoder.animeschedule.presentation.screens.settings.WatchSourcesScreen
import com.owlcoder.animeschedule.presentation.screens.watch.WatchScreen

@Composable
fun AnimeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onRestartForLanguage: (AppLanguage) -> Unit = {},
    onScheduleInitialLoadChange: (Boolean) -> Unit = {},
    onSearchFocusChanged: (Boolean) -> Unit = {},
) {
    val motion = LocalMotionPolicy.current

    val rootEnter = fadeIn(
        animationSpec = motion.iosDecelerate(IosMotion.Standard),
    ) + scaleIn(
        initialScale = if (motion.animationsEnabled) 0.985f else 1f,
        animationSpec = motion.iosTween(IosMotion.Standard),
    )
    val rootExit = fadeOut(
        animationSpec = motion.iosTween(IosMotion.Quick),
    ) + scaleOut(
        targetScale = if (motion.animationsEnabled) 0.995f else 1f,
        animationSpec = motion.iosTween(IosMotion.Quick),
    )

    val pushEnter = slideInHorizontally(
        animationSpec = motion.iosDecelerate(IosMotion.Navigation),
        initialOffsetX = { if (motion.animationsEnabled) it else 0 },
    ) + fadeIn(
        animationSpec = motion.iosDecelerate(IosMotion.Standard),
    )
    val pushExit = slideOutHorizontally(
        animationSpec = motion.iosTween(IosMotion.Navigation),
        targetOffsetX = { if (motion.animationsEnabled) -it / 4 else 0 },
    ) + fadeOut(
        animationSpec = motion.iosTween(IosMotion.Standard),
        targetAlpha = 0.82f,
    )
    val popEnter = slideInHorizontally(
        animationSpec = motion.iosDecelerate(IosMotion.Navigation),
        initialOffsetX = { if (motion.animationsEnabled) -it / 4 else 0 },
    ) + fadeIn(
        animationSpec = motion.iosDecelerate(IosMotion.Standard),
        initialAlpha = 0.82f,
    )
    val popExit = slideOutHorizontally(
        animationSpec = motion.iosTween(IosMotion.Navigation),
        targetOffsetX = { if (motion.animationsEnabled) it else 0 },
    ) + fadeOut(
        animationSpec = motion.iosTween(IosMotion.Standard),
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Schedule.route,
        modifier = modifier,
        enterTransition = { rootEnter },
        exitTransition = { rootExit },
        popEnterTransition = { rootEnter },
        popExitTransition = { rootExit },
    ) {
        composable(Screen.Schedule.route) {
            val scheduleViewModel: ScheduleViewModel = hiltViewModel()
            LaunchedEffect(scheduleViewModel, navController) {
                scheduleViewModel.navigationEvent.collect { event ->
                    when (event) {
                        is ScheduleViewModel.NavigationEvent.OpenSeeAll -> {
                            navController.navigate(Screen.AllToday.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
            ScheduleScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Detail.createRoute(animeId))
                },
                onInitialLoadChange = onScheduleInitialLoadChange,
                viewModel = scheduleViewModel,
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Detail.createRoute(animeId))
                },
                onFocusChanged = onSearchFocusChanged,
                onCancel = { onSearchFocusChanged(false) },
            )
        }
        composable(Screen.MyList.route) {
            MyListScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Detail.createRoute(animeId))
                },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onRestartForLanguage = onRestartForLanguage,
                onManageWatchSources = { navController.navigate(Screen.WatchSources.route) },
            )
        }
        composable(
            route = Screen.AllToday.route,
            enterTransition = { pushEnter },
            exitTransition = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit },
        ) {
            val scheduleViewModel: ScheduleViewModel = hiltViewModel()
            val uiState by scheduleViewModel.uiState.collectAsState()
            AllTodayScreen(
                episodes = uiState.todayEpisodes,
                isLoggedIn = uiState.isLoggedIn,
                pendingIncrementIds = uiState.pendingIncrementIds,
                onAnimeClick = { episode ->
                    navController.navigate(Detail.createRoute(episode.animeId))
                },
                onIncrementEpisode = { episode ->
                    episode.malId?.let(scheduleViewModel::incrementEpisode)
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.WatchSources.route,
            enterTransition = { pushEnter },
            exitTransition = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit },
        ) {
            WatchSourcesScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Detail.ROUTE,
            arguments = listOf(navArgument("animeId") { type = NavType.IntType }),
            enterTransition = { pushEnter },
            exitTransition = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit },
        ) {
            AnimeDetailScreen(
                onBack = { navController.popBackStack() },
                onAnimeClick = { animeId ->
                    navController.navigate(Detail.createRoute(animeId))
                },
                onWatchSourceClick = { url ->
                    navController.navigate(Screen.Watch.createRoute(url))
                },
            )
        }
        composable(
            route = Screen.Watch.ROUTE,
            arguments = listOf(navArgument("url") { type = NavType.StringType }),
            enterTransition = { pushEnter },
            exitTransition = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit },
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
            val url = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
            WatchScreen(
                url = url,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
