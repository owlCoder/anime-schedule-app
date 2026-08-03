package com.owlcoder.animeschedule.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.owlcoder.animeschedule.presentation.screens.detail.AnimeDetailScreen
import com.owlcoder.animeschedule.presentation.screens.mylist.MyListScreen
import com.owlcoder.animeschedule.presentation.screens.schedule.AllTodayScreen
import com.owlcoder.animeschedule.presentation.screens.schedule.ScheduleScreen
import com.owlcoder.animeschedule.presentation.screens.schedule.ScheduleOverlay
import com.owlcoder.animeschedule.presentation.screens.schedule.ScheduleViewModel
import com.owlcoder.animeschedule.presentation.screens.search.SearchScreen
import com.owlcoder.animeschedule.presentation.screens.watch.WatchScreen
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.presentation.screens.settings.SettingsScreen
import com.owlcoder.animeschedule.presentation.screens.settings.WatchSourcesScreen

private val slideEnter = slideInHorizontally(
    animationSpec = tween(240),
    initialOffsetX = { it }
) + fadeIn(animationSpec = tween(180))
private val slideExit = slideOutHorizontally(
    animationSpec = tween(240),
    targetOffsetX = { -it / 3 }
) + fadeOut(animationSpec = tween(180))
private val slidePopEnter = slideInHorizontally(
    animationSpec = tween(240),
    initialOffsetX = { -it / 3 }
) + fadeIn(animationSpec = tween(180))
private val slidePopExit = slideOutHorizontally(
    animationSpec = tween(240),
    targetOffsetX = { it }
) + fadeOut(animationSpec = tween(180))

@Composable
fun AnimeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onRestartForLanguage: (AppLanguage) -> Unit = {},
    onScheduleInitialLoadChange: (Boolean) -> Unit = {},
    onSearchFocusChanged: (Boolean) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Schedule.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(180)) },
        exitTransition = { fadeOut(animationSpec = tween(140)) },
        popEnterTransition = { fadeIn(animationSpec = tween(180)) },
        popExitTransition = { fadeOut(animationSpec = tween(140)) }
    ) {
        composable(Screen.Schedule.route) {
            val scheduleViewModel: ScheduleViewModel = hiltViewModel()
            val openOverlay by scheduleViewModel.openOverlay.collectAsState()
            LaunchedEffect(openOverlay) {
                if (openOverlay is ScheduleOverlay.SeeAll) {
                    scheduleViewModel.setOpenOverlay(ScheduleOverlay.None)
                    navController.navigate(Screen.AllToday.route)
                }
            }
            ScheduleScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.Detail.createRoute(animeId))
                },
                onInitialLoadChange = onScheduleInitialLoadChange
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.Detail.createRoute(animeId))
                },
                onFocusChanged = onSearchFocusChanged,
                onCancel = { onSearchFocusChanged(false) },
            )
        }
        composable(Screen.MyList.route) {
            MyListScreen(onAnimeClick = { animeId ->
                navController.navigate(Screen.Detail.createRoute(animeId))
            })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onRestartForLanguage = onRestartForLanguage,
                onManageWatchSources = { navController.navigate(Screen.WatchSources.route) }
            )
        }
        composable(
            route = Screen.AllToday.route,
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit },
        ) {
            val scheduleViewModel: ScheduleViewModel = hiltViewModel()
            val uiState by scheduleViewModel.uiState.collectAsState()
            AllTodayScreen(
                episodes = uiState.todayEpisodes,
                isLoggedIn = uiState.isLoggedIn,
                pendingIncrementIds = uiState.pendingIncrementIds,
                onAnimeClick = { episode ->
                    navController.navigate(Screen.Detail.createRoute(episode.animeId))
                },
                onIncrementEpisode = { episode ->
                    episode.malId?.let(scheduleViewModel::incrementEpisode)
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.WatchSources.route,
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit }
        ) {
            WatchSourcesScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Detail.ROUTE,
            arguments = listOf(navArgument("animeId") { type = NavType.IntType }),
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit }
        ) {
            AnimeDetailScreen(
                onBack = { navController.popBackStack() },
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.Detail.createRoute(animeId))
                },
                onWatchSourceClick = { url ->
                    navController.navigate(Screen.Watch.createRoute(url))
                }
            )
        }
        composable(
            route = Screen.Watch.ROUTE,
            arguments = listOf(navArgument("url") { type = NavType.StringType }),
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit }
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
            val url = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
            WatchScreen(
                url = url,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
