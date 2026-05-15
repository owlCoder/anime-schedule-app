package rs.owlcoder.animeschedule.presentation.navigation

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
import rs.owlcoder.animeschedule.presentation.screens.settings.AboutScreen
import rs.owlcoder.animeschedule.presentation.screens.settings.SettingsScreen

@Composable
fun AnimeNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination = Screen.Schedule.route, modifier = modifier) {
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
            NotificationsScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateToAbout = {
                navController.navigate(Screen.About.route)
            })
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Detail.ROUTE,
            arguments = listOf(navArgument("animeId") { type = NavType.IntType })
        ) {
            AnimeDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
