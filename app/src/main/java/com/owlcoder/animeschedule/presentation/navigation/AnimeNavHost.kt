package com.owlcoder.animeschedule.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.owlcoder.animeschedule.presentation.screens.detail.AnimeDetailScreen
import com.owlcoder.animeschedule.presentation.screens.mylist.MyListScreen
import com.owlcoder.animeschedule.presentation.screens.schedule.ScheduleScreen
import com.owlcoder.animeschedule.presentation.screens.watch.WatchScreen
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.presentation.screens.settings.SettingsScreen
import com.owlcoder.animeschedule.presentation.screens.settings.WatchSourcesScreen

private val slideEnter = slideInHorizontally(initialOffsetX = { it }) + fadeIn()
private val slideExit = slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
private val slidePopEnter = slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()
private val slidePopExit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()

@Composable
fun AnimeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onRestartForLanguage: (AppLanguage) -> Unit = {},
    onScheduleInitialLoadChange: (Boolean) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Schedule.route,
        modifier = modifier,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        composable(Screen.Schedule.route) {
            ScheduleScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.Detail.createRoute(animeId))
                },
                onInitialLoadChange = onScheduleInitialLoadChange
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
        composable(Screen.WatchSources.route) {
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
