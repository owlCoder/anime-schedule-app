package com.owlcoder.animeschedule.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.screens.settings.SettingsViewModel
import com.owlcoder.animeschedule.ui.theme.PillShape

data class BottomNavItem(
    val screen: Screen,
    @StringRes val labelRes: Int,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

private val items = listOf(
    BottomNavItem(
        Screen.Schedule,
        R.string.nav_schedule,
        Icons.Filled.CalendarMonth,
        Icons.Outlined.CalendarMonth
    ),
    BottomNavItem(
        Screen.Search,
        R.string.nav_search,
        Icons.Filled.Search,
        Icons.Outlined.Search
    ),
    BottomNavItem(
        Screen.MyList,
        R.string.nav_mylist,
        Icons.Filled.Bookmark,
        Icons.Outlined.BookmarkBorder
    ),
    BottomNavItem(
        Screen.Settings,
        R.string.nav_settings,
        Icons.Filled.AccountCircle,
        Icons.Outlined.AccountCircle
    )
)

/**
 * Full-width dock bar anchored flush to the bottom of the screen. Its solid [surface] fill
 * extends all the way to the very bottom edge (the navigation-bar inset is applied as inner
 * bottom padding, not an outer margin), so there is never a bare strip of the pure-black
 * app background peeking below the bar — the black-strip issue of the old floating pill is
 * gone. Only the top corners are rounded and a single hairline `outlineVariant` divider sits
 * along the top edge, reading as a clean modern dock rather than a floating glass pill.
 *
 * The active tab morphs into a soft `primaryContainer` pill that expands to reveal its text
 * label (animated), with the icon switching to its filled variant and tinting `primary`.
 * Inactive tabs stay as muted outlined `onSurfaceVariant` icons.
 */
@Composable
fun AnimeBottomBar(
    navController: NavController,
    onSearchClick: () -> Unit = {}
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Settings tab shows the MAL profile avatar when signed in, or a plain account icon
    // otherwise — reuses SettingsViewModel (Activity-scoped here, same instance the Settings
    // screen itself uses) instead of duplicating the auth-state read.
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    val navBarInset = WindowInsets.navigationBars.asPaddingValues()

    // Clean OPAQUE dock with SQUARE top edge (flush, full-bleed). Rounded top corners used to
    // reveal the black canvas behind them, and the elevation/AA on that reveal read as a grey
    // "peel" strip above the bar. A square-top solid surface sits flush against the content with
    // no reveal at all — just a single top hairline separates it. (Real blur-behind would need
    // the haze library, which isn't present.)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top hairline divider.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .padding(top = 12.dp, bottom = 12.dp)
                .padding(bottom = navBarInset.calculateBottomPadding()),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                // Search is a popup overlay (not a nav destination), so its item just
                // triggers the overlay and never appears "selected".
                val isSearch = item.screen == Screen.Search
                val avatarUrl = if (item.screen == Screen.Settings && settingsUiState.isLoggedIn)
                    settingsUiState.avatarUrl
                else null
                BottomNavItemView(
                    item = item,
                    selected = !isSearch && currentRoute == item.screen.route,
                    avatarUrl = avatarUrl,
                    onClick = {
                        if (isSearch) {
                            onSearchClick()
                        } else if (currentRoute != item.screen.route) {
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.Schedule.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    val pillColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
                      else Color.Transparent,
        animationSpec = spring(),
        label = "navPillColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(),
        label = "navContentColor"
    )
    val labelColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = spring(),
        label = "navLabelColor"
    )

    Row(
        modifier = modifier
            .clip(PillShape)
            .background(pillColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = if (selected) 16.dp else 14.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (selected) 8.dp else 0.dp)
    ) {
        if (avatarUrl != null && avatarUrl.isNotEmpty()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = stringResource(item.labelRes),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, contentColor, CircleShape)
            )
        } else {
            Icon(
                imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                contentDescription = stringResource(item.labelRes),
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
        }
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(spring()) + expandHorizontally(spring(), expandFrom = Alignment.Start),
            exit = fadeOut(spring()) + shrinkHorizontally(spring(), shrinkTowards = Alignment.Start)
        ) {
            Text(
                text = stringResource(item.labelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
                maxLines = 1
            )
        }
    }
}
