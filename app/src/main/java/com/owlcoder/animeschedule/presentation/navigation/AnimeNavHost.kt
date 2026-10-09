package com.owlcoder.animeschedule.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.owlcoder.animeschedule.domain.model.ToolShortcut
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.owlcoder.animeschedule.domain.model.AppLanguage
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.navigation.Screen.Detail
import com.owlcoder.animeschedule.presentation.screens.detail.AnimeDetailScreen
import com.owlcoder.animeschedule.presentation.screens.mylist.MyListScreen
import com.owlcoder.animeschedule.presentation.screens.schedule.ScheduleScreen
import com.owlcoder.animeschedule.presentation.screens.schedule.ScheduleViewModel
import com.owlcoder.animeschedule.presentation.screens.search.SearchScreen
import com.owlcoder.animeschedule.presentation.screens.settings.SettingsScreen
import com.owlcoder.animeschedule.presentation.screens.watch.WatchScreen
import com.owlcoder.animeschedule.presentation.components.iosAccelerate
import com.owlcoder.animeschedule.presentation.components.iosDecelerate

@Composable
fun AnimeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onRestartForLanguage: (AppLanguage) -> Unit = {},
    onScheduleInitialLoadChange: (Boolean) -> Unit = {},
    onSearchFocusChanged: (Boolean) -> Unit = {},
) {
    val motion = LocalMotionPolicy.current

    // Top-level destinations should feel like switching panes, not pushing a new screen.
    val rootEnter = fadeIn(
        animationSpec = motion.iosDecelerate(IosMotion.Standard - IosMotion.PressIn, IosMotion.PressIn),
    )
    val rootExit = fadeOut(
        animationSpec = motion.iosAccelerate(IosMotion.PressIn),
    )

    // A short directional translation keeps the destination readable during navigation.
    val pushEnter = slideInHorizontally(
        animationSpec = motion.iosDecelerate(IosMotion.Navigation),
        initialOffsetX = { if (motion.animationsEnabled) it / 8 else 0 },
    ) + fadeIn(
        animationSpec = motion.iosDecelerate(IosMotion.Navigation - IosMotion.PressIn, IosMotion.PressIn),
    )
    val pushExit = slideOutHorizontally(
        animationSpec = motion.iosAccelerate(IosMotion.Navigation),
        targetOffsetX = { if (motion.animationsEnabled) -it / 16 else 0 },
    ) + fadeOut(
        animationSpec = motion.iosAccelerate(IosMotion.PressIn),
    )
    val popEnter = slideInHorizontally(
        animationSpec = motion.iosDecelerate(IosMotion.Navigation),
        initialOffsetX = { if (motion.animationsEnabled) -it / 16 else 0 },
    ) + fadeIn(
        animationSpec = motion.iosDecelerate(IosMotion.Navigation - IosMotion.PressIn, IosMotion.PressIn),
    )
    val popExit = slideOutHorizontally(
        animationSpec = motion.iosAccelerate(IosMotion.Navigation),
        targetOffsetX = { if (motion.animationsEnabled) it / 8 else 0 },
    ) + fadeOut(
        animationSpec = motion.iosAccelerate(IosMotion.PressIn),
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
            ScheduleScreen(
                onAnimeClick = { animeId ->
                    navController.openDetail(animeId)
                },
                onInitialLoadChange = onScheduleInitialLoadChange,
                viewModel = scheduleViewModel,
                onShortcut = { shortcut ->
                    navController.navigate(Screen.MyList.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    navController.currentBackStackEntry?.savedStateHandle?.set("tool-shortcut", shortcut)
                },
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onAnimeClick = { animeId ->
                    navController.openDetail(animeId)
                },
                onFocusChanged = onSearchFocusChanged,
                onCancel = { onSearchFocusChanged(false) },
                requestFocus = true,
            )
        }
        composable(Screen.MyList.route) { entry ->
            val shortcut by entry.savedStateHandle.getStateFlow<ToolShortcut?>("tool-shortcut", null).collectAsStateWithLifecycle()
            MyListScreen(
                initialTool = shortcut,
                onToolOpened = { entry.savedStateHandle.set<ToolShortcut?>("tool-shortcut", null) },
                onAnimeClick = { animeId ->
                    navController.openDetail(animeId)
                },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onRestartForLanguage = onRestartForLanguage)
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
                onBack = { navController.popToTopLevelDestination() },
                onAnimeClick = { animeId ->
                    navController.openDetail(animeId, replaceTransientStack = true)
                },
                onWatchSourceClick = { url ->
                    navController.navigate(Screen.Watch.createRoute(url)) {
                        launchSingleTop = true
                    }
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

private fun NavHostController.openDetail(
    animeId: Int,
    replaceTransientStack: Boolean = false,
) {
    if (replaceTransientStack) popToTopLevelDestination()
    navigate(Detail.createRoute(animeId)) {
        launchSingleTop = true
    }
}

private fun NavHostController.popToTopLevelDestination() {
    var route = currentDestination?.route
    while (!shouldShowBottomBar(route)) {
        if (!popBackStack()) return
        route = currentDestination?.route
    }
}
