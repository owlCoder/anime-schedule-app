package rs.owlcoder.animeschedule.presentation.navigation

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
import rs.owlcoder.animeschedule.presentation.screens.detail.AnimeDetailScreen
import rs.owlcoder.animeschedule.presentation.screens.mylist.MyListScreen
import rs.owlcoder.animeschedule.presentation.screens.notifications.NotificationsScreen
import rs.owlcoder.animeschedule.presentation.screens.schedule.ScheduleScreen
import rs.owlcoder.animeschedule.presentation.screens.search.SearchScreen
import rs.owlcoder.animeschedule.data.local.datastore.AppLanguage
import rs.owlcoder.animeschedule.presentation.screens.settings.AboutScreen
import rs.owlcoder.animeschedule.presentation.screens.settings.ChangelogScreen
import rs.owlcoder.animeschedule.presentation.screens.settings.SettingsScreen

private val slideEnter = slideInHorizontally(initialOffsetX = { it }) + fadeIn()
private val slideExit = slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
private val slidePopEnter = slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()
private val slidePopExit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()

@Composable
fun AnimeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onRestartForLanguage: (AppLanguage) -> Unit = {}
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
            ScheduleScreen(onAnimeClick = { animeId ->
                navController.navigate(Screen.Detail.createRoute(animeId))
            })
        }
        composable(Screen.Search.route) {
            SearchScreen(onAnimeClick = { animeId ->
                navController.navigate(Screen.Detail.createRoute(animeId))
            })
        }
        composable(Screen.MyList.route) {
            MyListScreen(onAnimeClick = { animeId ->
                navController.navigate(Screen.Detail.createRoute(animeId))
            })
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(onAnimeClick = { animeId ->
                navController.navigate(Screen.Detail.createRoute(animeId))
            })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToChangelog = { navController.navigate(Screen.Changelog.route) },
                onRestartForLanguage = onRestartForLanguage
            )
        }
        composable(
            route = Screen.About.route,
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit }
        ) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Changelog.route,
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit }
        ) {
            ChangelogScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Detail.ROUTE,
            arguments = listOf(navArgument("animeId") { type = NavType.IntType }),
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit }
        ) {
            AnimeDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
