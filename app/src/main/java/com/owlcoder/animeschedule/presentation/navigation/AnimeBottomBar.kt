package com.owlcoder.animeschedule.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.components.AppNotificationBadge
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.GlassChrome
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.iosPressScale
import com.owlcoder.animeschedule.presentation.components.iosSpring
import com.owlcoder.animeschedule.presentation.components.iosTween

private data class BottomNavItem(
    val screen: Screen,
    @StringRes val labelRes: Int,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
)

private val items = listOf(
    BottomNavItem(
        Screen.Schedule,
        R.string.schedule_tab_today,
        Icons.Filled.CalendarMonth,
        Icons.Outlined.CalendarMonth,
    ),
    BottomNavItem(Screen.Search, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Screen.MyList, R.string.nav_mylist, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    BottomNavItem(Screen.Settings, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

private val DockShape = ContinuousRoundedShape(23.dp)
private val DockHeight = 50.dp

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
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        GlassChrome(
            modifier = Modifier
                .fillMaxWidth()
                .height(DockHeight),
            shape = DockShape,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DockHeight)
                    .padding(horizontal = 3.dp, vertical = 3.dp),
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
    val label = stringResource(item.labelRes)
    val interactionSource = remember { MutableInteractionSource() }
    val motion = LocalMotionPolicy.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.35f
    val selectedFill = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.52f)
    val fill by animateColorAsState(
        targetValue = if (selected) selectedFill else Color.Transparent,
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "bottom-tab-fill",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            if (dark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.58f)
        } else {
            Color.Transparent
        },
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "bottom-tab-border",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = motion.iosSpring(),
        label = "bottom-tab-scale",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .iosPressScale(interactionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                role = Role.Tab
                this.selected = selected
            }
            .padding(horizontal = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .fillMaxHeight(),
            shape = ContinuousRoundedShape(17.dp),
            color = fill,
            border = BorderStroke(0.5.dp, borderColor),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            TabVisual(
                item = item,
                label = label,
                selected = selected,
                notificationCount = notificationCount,
            )
        }
    }
}

@Composable
private fun TabVisual(
    item: BottomNavItem,
    label: String,
    selected: Boolean,
    notificationCount: Int,
) {
    val motion = LocalMotionPolicy.current
    val color by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "bottom-tab-color",
    )
    val inactiveAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.80f,
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "bottom-tab-alpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .alpha(inactiveAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppNotificationBadge(
            count = notificationCount,
            contentDescription = if (notificationCount > 0) {
                "$label, $notificationCount"
            } else {
                null
            },
        ) {
            Crossfade(
                targetState = selected,
                animationSpec = motion.iosTween(IosMotion.Quick),
                label = "bottom-tab-icon",
            ) { active ->
                Icon(
                    imageVector = if (active) item.activeIcon else item.inactiveIcon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                lineHeight = 10.5.sp,
            ),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}
