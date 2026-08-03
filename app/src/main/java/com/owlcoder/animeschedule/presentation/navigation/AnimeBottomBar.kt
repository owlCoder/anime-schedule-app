package com.owlcoder.animeschedule.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.owlcoder.animeschedule.presentation.components.AppNotificationBadge
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.GlassChrome
import com.owlcoder.animeschedule.presentation.components.GlassSurface
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone
import com.owlcoder.animeschedule.ui.theme.PillShape

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
)

private val items = listOf(
    BottomNavItem(Screen.Schedule, "Today", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.Search, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Screen.MyList, "My List", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    BottomNavItem(Screen.Settings, "Settings", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
)

private val DockShape = ContinuousRoundedShape(30.dp)
private val DockHeight = 62.dp

@Composable
fun AnimeBottomBar(
    navController: NavController,
    notificationCount: Int = 0,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        GlassChrome(
            modifier = Modifier.fillMaxWidth().height(DockHeight),
            shape = DockShape,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DockHeight)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    BottomNavItemView(
                        item = item,
                        selected = currentRoute == item.screen.route,
                        notificationCount = if (item.screen == Screen.Schedule) notificationCount else 0,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (currentRoute != item.screen.route) {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    selected: Boolean,
    notificationCount: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .semantics {
                contentDescription = item.label
                role = Role.Tab
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            GlassSurface(
                modifier = Modifier.size(width = 74.dp, height = 46.dp),
                shape = PillShape,
                tone = GlassTone.Accent,
                blur = GlassBlur.Soft,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                TabVisual(item, selected, notificationCount)
            }
        } else {
            Box(
                modifier = Modifier.size(width = 74.dp, height = 46.dp),
                contentAlignment = Alignment.Center,
            ) {
                TabVisual(item, selected, notificationCount)
            }
        }
    }
}

@Composable
private fun TabVisual(
    item: BottomNavItem,
    selected: Boolean,
    notificationCount: Int,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppNotificationBadge(
            count = notificationCount,
            contentDescription = if (notificationCount > 0) "${item.label}, $notificationCount unread" else null,
        ) {
            Icon(
                imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = item.label,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
            ),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}
