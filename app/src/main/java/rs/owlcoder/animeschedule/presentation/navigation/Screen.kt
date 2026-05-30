package rs.owlcoder.animeschedule.presentation.navigation

sealed class Screen(val route: String) {
    data object Schedule : Screen("schedule")
    data object Search : Screen("search")
    data object MyList : Screen("mylist")
    data object Seasonal : Screen("seasonal")
    data object Notifications : Screen("notifications")
    data object Settings : Screen("settings")
    data object About : Screen("about")
    data object Changelog : Screen("changelog")
    data class Detail(val animeId: Int = 0) : Screen("detail/{animeId}") {
        companion object {
            const val ROUTE = "detail/{animeId}"
            fun createRoute(animeId: Int) = "detail/$animeId"
        }
    }
}
