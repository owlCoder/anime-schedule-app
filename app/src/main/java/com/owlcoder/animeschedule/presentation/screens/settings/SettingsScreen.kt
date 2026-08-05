package com.owlcoder.animeschedule.presentation.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.data.local.datastore.CacheRetentionPolicy
import com.owlcoder.animeschedule.data.local.datastore.ThemeMode
import com.owlcoder.animeschedule.presentation.components.AppButton
import com.owlcoder.animeschedule.presentation.components.AppButtonVariant
import com.owlcoder.animeschedule.presentation.components.AppLargeHeader
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.AppSwitch
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import java.time.ZoneId
import java.util.Locale

private val SettingsGroupShape = ContinuousRoundedShape(20.dp)

private enum class SettingsSheet {
    Theme,
    Notifications,
    Timezone,
    Language,
    WatchSources,
    CacheRetention,
    ClearCache,
    Changelog,
    About,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onRestartForLanguage: (AppLanguage) -> Unit = {},
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val isLoggingIn by authViewModel.isLoggingIn.collectAsState()
    val loginError by authViewModel.loginError.collectAsState()
    val cacheSizeBytes by settingsViewModel.cacheSizeBytes.collectAsState()
    val isClearingCache by settingsViewModel.isClearingCache.collectAsState()
    val cacheActionMessage by settingsViewModel.cacheActionMessage.collectAsState()
    val context = LocalContext.current
    var activeSheet by remember { mutableStateOf<SettingsSheet?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> settingsViewModel.setNotificationsEnabled(granted) }

    fun setNotificationsEnabled(enabled: Boolean) {
        if (!enabled) {
            settingsViewModel.setNotificationsEnabled(false)
        } else if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            settingsViewModel.setNotificationsEnabled(true)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 6.dp,
                bottom = 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            item(key = "settings-header") {
                AppLargeHeader(
                    title = stringResource(R.string.settings_title),
                    modifier = Modifier.padding(bottom = 1.dp),
                )
            }

            item(key = "account") {
                SettingsSection(stringResource(R.string.settings_section_account)) {
                    SettingsGroup {
                        AccountRow(
                            isLoggedIn = uiState.isLoggedIn,
                            username = uiState.username,
                            avatarUrl = uiState.avatarUrl,
                            isLoggingIn = isLoggingIn,
                            onClick = {
                                if (uiState.isLoggedIn) authViewModel.logout()
                                else authViewModel.launchMalLogin(context)
                            },
                        )
                    }
                    if (!loginError.isNullOrBlank()) {
                        Text(
                            text = loginError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 12.dp, top = 6.dp),
                        )
                    }
                }
            }

            item(key = "preferences") {
                SettingsSection(stringResource(R.string.settings_section_preferences)) {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.ColorLens,
                            title = stringResource(R.string.settings_appearance),
                            value = themeModeLabel(uiState.themeMode),
                            onClick = { activeSheet = SettingsSheet.Theme },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Notifications,
                            title = stringResource(R.string.settings_notifications),
                            value = if (uiState.notificationsEnabled) {
                                "${stringResource(R.string.settings_notifications_on)} · ${notificationOffsetLabel(uiState.notificationOffsetMinutes)}"
                            } else {
                                stringResource(R.string.settings_notifications_off)
                            },
                            onClick = { activeSheet = SettingsSheet.Notifications },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Schedule,
                            title = stringResource(R.string.settings_timezone),
                            value = uiState.timezoneId.ifEmpty {
                                stringResource(R.string.settings_timezone_system)
                            },
                            onClick = { activeSheet = SettingsSheet.Timezone },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Translate,
                            title = stringResource(R.string.settings_language),
                            value = languageLabel(uiState.appLanguage),
                            onClick = { activeSheet = SettingsSheet.Language },
                        )
                    }
                }
            }

            item(key = "data-sources") {
                SettingsSection(stringResource(R.string.settings_section_data_sources)) {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.PlayCircle,
                            title = stringResource(R.string.settings_watch_sources),
                            value = stringResource(R.string.settings_watch_sources_subtitle),
                            onClick = { activeSheet = SettingsSheet.WatchSources },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Storage,
                            title = stringResource(R.string.settings_cache),
                            value = stringResource(
                                R.string.settings_cache_value,
                                formatBytes(cacheSizeBytes),
                                uiState.cacheRetentionDays,
                            ),
                            onClick = { activeSheet = SettingsSheet.CacheRetention },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.DeleteSweep,
                            title = stringResource(R.string.settings_clear_cache),
                            value = cacheActionMessage
                                ?: stringResource(R.string.settings_clear_cache_subtitle),
                            onClick = { activeSheet = SettingsSheet.ClearCache },
                            iconColor = MaterialTheme.colorScheme.error,
                            titleColor = MaterialTheme.colorScheme.error,
                            enabled = !isClearingCache,
                            trailing = if (isClearingCache) {
                                {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(17.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            } else null,
                        )
                    }
                }
            }

            item(key = "about") {
                SettingsSection(stringResource(R.string.settings_section_about)) {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.Update,
                            title = stringResource(R.string.settings_changelog),
                            value = stringResource(R.string.settings_changelog_subtitle),
                            onClick = { activeSheet = SettingsSheet.Changelog },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.settings_about),
                            value = stringResource(R.string.settings_about_subtitle),
                            onClick = { activeSheet = SettingsSheet.About },
                        )
                    }
                }
            }
        }
    }

    when (activeSheet) {
        SettingsSheet.Theme -> SelectionSheet(
            title = stringResource(R.string.settings_appearance),
            options = ThemeMode.entries,
            selected = uiState.themeMode,
            label = { themeModeLabel(it) },
            onSelect = {
                settingsViewModel.setThemeMode(it)
                activeSheet = null
            },
            onDismiss = { activeSheet = null },
        )
        SettingsSheet.Notifications -> NotificationSettingsSheet(
            enabled = uiState.notificationsEnabled,
            offset = uiState.notificationOffsetMinutes,
            onEnabledChange = ::setNotificationsEnabled,
            onOffsetSelect = settingsViewModel::setNotificationOffset,
            onDismiss = { activeSheet = null },
        )
        SettingsSheet.Timezone -> TimezoneSheet(
            current = uiState.timezoneId,
            onSelect = {
                settingsViewModel.setTimezone(it)
                activeSheet = null
            },
            onDismiss = { activeSheet = null },
        )
        SettingsSheet.Language -> SelectionSheet(
            title = stringResource(R.string.settings_language),
            options = AppLanguage.entries,
            selected = uiState.appLanguage,
            label = { languageLabel(it) },
            onSelect = { language ->
                settingsViewModel.setAppLanguage(language)
                activeSheet = null
                onRestartForLanguage(language)
            },
            onDismiss = { activeSheet = null },
        )
        SettingsSheet.WatchSources -> WatchSourcesBottomSheet(
            onDismiss = { activeSheet = null },
        )
        SettingsSheet.CacheRetention -> CacheRetentionSheet(
            current = uiState.cacheRetentionDays,
            onSelect = {
                settingsViewModel.setCacheRetentionDays(it)
                activeSheet = null
            },
            onDismiss = { activeSheet = null },
        )
        SettingsSheet.ClearCache -> ClearCacheSheet(
            onDismiss = { activeSheet = null },
            onConfirm = {
                activeSheet = null
                settingsViewModel.clearCacheNow()
            },
        )
        SettingsSheet.Changelog -> ChangelogBottomSheet { activeSheet = null }
        SettingsSheet.About -> AboutBottomSheet { activeSheet = null }
        null -> Unit
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
        )
        content()
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    AppMaterialSurface(
        modifier = Modifier.fillMaxWidth(),
        material = AppMaterial.Grouped,
        shape = SettingsGroupShape,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun AccountRow(
    isLoggedIn: Boolean,
    username: String,
    avatarUrl: String,
    isLoggingIn: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 11.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (isLoggedIn && username.isNotBlank()) username else "MyAnimeList",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    if (isLoggedIn) R.string.profile_logged_in
                    else R.string.profile_not_logged_in,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            isLoggingIn -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            !isLoggedIn -> Row(
        modifier = Modifier
            .height(40.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Default.Key,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.profile_login),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
    else -> Row(
                modifier = Modifier
                    .height(40.dp)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.profile_logout),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tileColor = if (iconColor == MaterialTheme.colorScheme.error) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(ContinuousRoundedShape(10.dp))
                .background(tileColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = iconColor,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 11.dp, end = 7.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (value.isNotBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) trailing() else Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 57.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SelectionSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(onDismissRequest = onDismiss, title = title) {
        InsetGroup {
            options.forEachIndexed { index, option ->
                SelectionRow(
                    label = label(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                )
                if (index < options.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 14.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsSheet(
    enabled: Boolean,
    offset: Int,
    onEnabledChange: (Boolean) -> Unit,
    onOffsetSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val offsets = listOf(0, -5, -10, -15, -30, 10, 30, 60)
    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_notifications),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InsetGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_notifications),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.notif_offset_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AppSwitch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                    )
                }
            }
            if (enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.settings_notif_timing).uppercase(),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    InsetGroup {
                        offsets.forEachIndexed { index, option ->
                            SelectionRow(
                                label = notificationOffsetLabel(option),
                                selected = offset == option,
                                onClick = { onOffsetSelect(option) },
                            )
                            if (index < offsets.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 14.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimezoneSheet(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val systemId = ZoneId.systemDefault().id
    val zones = remember(query) {
        ZoneId.getAvailableZoneIds()
            .asSequence()
            .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
            .sorted()
            .take(250)
            .toList()
    }

    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_timezone),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppMaterialSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                material = AppMaterial.Interactive,
                shape = ContinuousRoundedShape(13.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 9.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.settings_timezone_search),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
            }

            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                item(key = "system-timezone") {
                    SelectionRow(
                        label = stringResource(R.string.settings_timezone_system),
                        subtitle = systemId,
                        selected = current.isBlank(),
                        onClick = { onSelect("") },
                    )
                }
                items(zones, key = { it }) { zone ->
                    SelectionRow(
                        label = zone,
                        selected = current == zone,
                        onClick = { onSelect(zone) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CacheRetentionSheet(
    current: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_cache_retention_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_cache_retention_subtitle),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            InsetGroup {
                CacheRetentionPolicy.supportedRetentionDays.forEachIndexed { index, days ->
                    SelectionRow(
                        label = stringResource(R.string.settings_cache_retention_days, days),
                        selected = current == days,
                        onClick = { onSelect(days) },
                    )
                    if (index < CacheRetentionPolicy.supportedRetentionDays.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 14.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClearCacheSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_clear_cache),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_clear_cache_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppButton(
                    label = stringResource(R.string.common_cancel),
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    variant = AppButtonVariant.Secondary,
                )
                AppButton(
                    label = stringResource(R.string.settings_clear_cache_confirm),
                    icon = Icons.Default.DeleteSweep,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    variant = AppButtonVariant.Destructive,
                )
            }
        }
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
    AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
    AppLanguage.SERBIAN_LATIN -> stringResource(R.string.settings_language_serbian)
}

@Composable
private fun notificationOffsetLabel(minutes: Int): String = when {
    minutes == 0 -> stringResource(R.string.notif_offset_immediate)
    minutes < 0 -> stringResource(R.string.notif_offset_before, -minutes)
    minutes < 60 -> stringResource(R.string.notif_offset_after_min, minutes)
    else -> stringResource(R.string.notif_offset_after_hour, minutes / 60)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1_024L) return "$bytes B"
    val kilobytes = bytes / 1_024.0
    if (kilobytes < 1_024.0) return String.format(Locale.ROOT, "%.1f KB", kilobytes)
    return String.format(Locale.ROOT, "%.1f MB", kilobytes / 1_024.0)
}
