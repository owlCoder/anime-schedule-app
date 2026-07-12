package com.owlcoder.animeschedule.presentation.navigation

sealed class Screen(val route: String) {
    data object Schedule : Screen("schedule")
    data object Search : Screen("search")
    data object MyList : Screen("mylist")
    data object Settings : Screen("settings")
    data object WatchSources : Screen("watch_sources")
    data class Detail(val animeId: Int = 0) : Screen("detail/{animeId}") {
        companion object {
            const val ROUTE = "detail/{animeId}"
            fun createRoute(animeId: Int) = "detail/$animeId"
        }
    }
    data class Watch(val url: String = "") : Screen("watch/{url}") {
        companion object {
            const val ROUTE = "watch/{url}"
            fun createRoute(url: String) =
                "watch/${java.net.URLEncoder.encode(url, "UTF-8")}"
        }
    }
}
