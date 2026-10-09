package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.ui.theme.LocalCompactLayout
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.StringRes
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AppLanguage
import com.owlcoder.animeschedule.domain.model.CacheRetentionPolicy
import com.owlcoder.animeschedule.domain.model.LoginFailure
import com.owlcoder.animeschedule.domain.model.LoginState
import com.owlcoder.animeschedule.domain.model.ThemeMode
import com.owlcoder.animeschedule.presentation.components.AppButton
import com.owlcoder.animeschedule.presentation.components.AppButtonVariant
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.AppSwitch
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.LocalNavBarHeight
import java.time.ZoneId
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.owlcoder.animeschedule.presentation.components.iosPressScale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val SettingsGroupShape = ContinuousRoundedShape(18.dp)

private enum class SettingsSheet {
    Theme,
    Display,
    Notifications,
    Timezone,
    Language,
    WatchSources,
    CacheRetention,
    ClearCache,
    Changelog,
    About,
    Backup,
    EpisodeLength,
    Sync,
    Shortcuts,
    Logout,
}

private data class SettingsItem(
    val icon: ImageVector, val title: String, val value: String,
    val sheet: SettingsSheet, val keywords: String = "", val destructive: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onRestartForLanguage: (AppLanguage) -> Unit = {},
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val loginState by authViewModel.loginState.collectAsStateWithLifecycle()
    val cacheSizeBytes by settingsViewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val isClearingCache by settingsViewModel.isClearingCache.collectAsStateWithLifecycle()
    val cacheActionMessageRes by settingsViewModel.cacheActionMessageRes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val tools = LocalWatchTools.current
    val navBarHeight = LocalNavBarHeight.current
    var activeSheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }
    val sync = LocalSyncCenter.current
    var query by rememberSaveable { mutableStateOf("") }
    var permissionGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        settingsViewModel.setNotificationsEnabled(granted)
    }
    fun enableNotifications(enabled: Boolean) {
        if (enabled && !permissionGranted && Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else settingsViewModel.setNotificationsEnabled(enabled)
    }
    val groups = listOf(
        stringResource(R.string.settings_section_preferences) to listOf(
            SettingsItem(Icons.Default.DashboardCustomize, stringResource(R.string.shortcuts_title), stringResource(R.string.shortcuts_settings_hint), SettingsSheet.Shortcuts, "shortcuts precice prečice tools alatke"),
            SettingsItem(Icons.Default.Palette, stringResource(R.string.settings_appearance),
                if (uiState.themeOptions.scheduled) stringResource(R.string.scheduled_theme) else "${themeModeLabel(uiState.themeMode)} · ${stringResource(uiState.themeOptions.palette.labelRes())}",
                SettingsSheet.Theme, "theme tema izgled colors boje palettes palete color"),
            SettingsItem(Icons.Default.Tune, stringResource(R.string.appearance_display), stringResource(R.string.display_settings_hint), SettingsSheet.Display, "display prikaz amoled contrast kontrast animation animacije compact raspored schedule automatska"),
            SettingsItem(Icons.Default.Notifications, stringResource(R.string.settings_notifications),
                if (uiState.notificationsEnabled && !permissionGranted) stringResource(R.string.notification_permission_needed) else if (uiState.notificationsEnabled) "${stringResource(R.string.settings_notifications_on)} · ${notificationOffsetLabel(uiState.notificationOffsetMinutes)}" else stringResource(R.string.settings_notifications_off), SettingsSheet.Notifications, "reminder podsetnik"),
            SettingsItem(Icons.Default.Public, stringResource(R.string.settings_timezone), uiState.timezoneId.ifEmpty { stringResource(R.string.settings_timezone_system) }, SettingsSheet.Timezone),
            SettingsItem(Icons.Default.Translate, stringResource(R.string.settings_language), languageLabel(uiState.appLanguage), SettingsSheet.Language),
            SettingsItem(Icons.Default.Timer, stringResource(R.string.episode_length), stringResource(R.string.episode_length_value, tools.data.episodeMinutes), SettingsSheet.EpisodeLength, "duration vreme minutes minuti"),
        ),
        stringResource(R.string.settings_section_data_sources) to listOf(
            SettingsItem(Icons.Default.CloudSync, stringResource(R.string.sync_center), syncHeadline(sync.state), SettingsSheet.Sync, "sync synchronization sinhronizacija internet pending čekanje cekanje"),
            SettingsItem(Icons.Default.PlayCircle, stringResource(R.string.settings_watch_sources), stringResource(R.string.settings_watch_sources_subtitle), SettingsSheet.WatchSources),
            SettingsItem(Icons.Default.Backup, stringResource(R.string.personal_backup), stringResource(R.string.personal_backup_subtitle), SettingsSheet.Backup, "restore export import vrati izvoz uvoz"),
            SettingsItem(Icons.Default.Storage, stringResource(R.string.settings_cache), stringResource(R.string.settings_cache_value, formatBytes(cacheSizeBytes), uiState.cacheRetentionDays), SettingsSheet.CacheRetention),
            SettingsItem(Icons.Default.DeleteSweep, stringResource(R.string.settings_clear_cache), stringResource(cacheActionMessageRes ?: R.string.settings_clear_cache_subtitle), SettingsSheet.ClearCache, destructive = true),
        ),
        stringResource(R.string.settings_section_about) to listOf(
            SettingsItem(Icons.Default.Update, stringResource(R.string.settings_changelog), stringResource(R.string.settings_changelog_subtitle), SettingsSheet.Changelog),
            SettingsItem(Icons.Default.Info, stringResource(R.string.settings_about), stringResource(R.string.settings_about_subtitle), SettingsSheet.About),
        ),
    )
    val accountSection = stringResource(R.string.settings_section_account)
    val search = query.trim()
    val filtered = groups.map { (title, rows) -> title to rows.filter { search.isBlank() || title.contains(search, true) || "${it.title} ${it.value} ${it.keywords}".contains(search, true) } }.filter { it.second.isNotEmpty() }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).statusBarsPadding(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = navBarHeight + 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item("header") { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
            item("search") { AppSearchField(query, { query = it }, Modifier.testTag("settings-search"), stringResource(R.string.settings_search), Icons.Default.Search, onClear = { query = "" }) }
            if (search.isBlank() || "MyAnimeList ${uiState.username} ${accountSection}".contains(search, true)) item("account") {
                SettingsSection(stringResource(R.string.settings_section_account)) {
                    SettingsGroup { AccountRow(uiState.isLoggedIn, uiState.username, uiState.avatarUrl, loginState is LoginState.InProgress) {
                        if (uiState.isLoggedIn) {
                            if (sync.state.pendingCount > 0) activeSheet = SettingsSheet.Logout else authViewModel.logout()
                        } else authViewModel.launchMalLogin(context)
                    } }
                    Spacer(Modifier.height(10.dp))
                    SyncSummary(sync.state) { activeSheet = SettingsSheet.Sync }
                    (loginState as? LoginState.Failed)?.let { Text(stringResource(it.reason.messageRes()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
            filtered.forEach { (title, rows) -> item(title) {
                SettingsSection(title) {
                    SettingsGroup {
                        rows.forEachIndexed { index, row ->
                            SettingsRow(row.icon, row.title, row.value, {
                                focus.clearFocus(force = true)
                                keyboard?.hide()
                                activeSheet = row.sheet
                            }, enabled = !(row.sheet == SettingsSheet.ClearCache && isClearingCache),
                                titleColor = if (row.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            if (index < rows.lastIndex) SettingsDivider()
                        }
                    }
                }
            } }
            if (filtered.isEmpty() && search.isNotBlank()) item("no-matches") {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.SearchOff, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.settings_search_empty), style = MaterialTheme.typography.bodyLarge)
                    AppButton(stringResource(R.string.search_clear_query), { query = "" }, variant = AppButtonVariant.Secondary, icon = Icons.Default.RestartAlt)
                }
            }
        }
    }
    when (activeSheet) {
        SettingsSheet.Sync -> SyncCenterSheet(sync.state, sync.retry, sync.login, { activeSheet = null }, sync.zone)
        SettingsSheet.Shortcuts -> ToolShortcutsSheet(tools.data.shortcuts, tools.setShortcuts) { activeSheet = null }
        SettingsSheet.Logout -> AppSheet(onDismissRequest = { activeSheet = null }, title = stringResource(R.string.logout_pending_title)) {
            Text(pluralStringResource(R.plurals.logout_pending_hint, sync.state.pendingCount, sync.state.pendingCount), style = MaterialTheme.typography.bodyMedium)
            AppButton(stringResource(R.string.sync_retry), { activeSheet = SettingsSheet.Sync }, Modifier.fillMaxWidth().padding(top = 16.dp), icon = Icons.Default.CloudSync)
            AppButton(stringResource(R.string.profile_logout), { activeSheet = null; authViewModel.logout() }, Modifier.fillMaxWidth().padding(top = 8.dp), variant = AppButtonVariant.Destructive, icon = Icons.AutoMirrored.Filled.ExitToApp)
        }
        SettingsSheet.Theme -> AppearanceSheet(uiState.themeMode, uiState.themeOptions,
            settingsViewModel::setThemeMode, settingsViewModel::setThemePalette,
            settingsViewModel::resetAppearance, { activeSheet = null })
        SettingsSheet.Display -> DisplaySheet(uiState.themeOptions, settingsViewModel::setThemeOptions) { activeSheet = null }
        SettingsSheet.Notifications -> NotificationSettingsSheet(uiState.notificationsEnabled, uiState.notificationOffsetMinutes, ::enableNotifications, settingsViewModel::setNotificationOffset, { activeSheet = null }, permissionGranted, { enableNotifications(true) }, uiState.quietHours, settingsViewModel::setQuietHours)
        SettingsSheet.Timezone -> TimezoneSheet(uiState.timezoneId, { settingsViewModel.setTimezone(it); activeSheet = null }, { activeSheet = null })
        SettingsSheet.Language -> SelectionSheet(stringResource(R.string.settings_language), listOf(AppLanguage.ENGLISH, AppLanguage.SERBIAN_LATIN), uiState.appLanguage, { languageLabel(it) }, {
            settingsViewModel.setAppLanguage(it); activeSheet = null; onRestartForLanguage(it)
        }, { activeSheet = null })
        SettingsSheet.WatchSources -> WatchSourcesBottomSheet(onDismiss = { activeSheet = null })
        SettingsSheet.CacheRetention -> CacheRetentionSheet(uiState.cacheRetentionDays, { settingsViewModel.setCacheRetentionDays(it); activeSheet = null }, { activeSheet = null })
        SettingsSheet.ClearCache -> ClearCacheSheet({ activeSheet = null }, { activeSheet = null; settingsViewModel.clearCacheNow() })
        SettingsSheet.EpisodeLength -> EpisodeLengthSheet(tools.data.episodeMinutes, tools.setEpisodeMinutes, { activeSheet = null })
        SettingsSheet.Backup -> PersonalBackupSheet(settingsViewModel.personalBackupStore, uiState.username, { activeSheet = null })
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
            modifier = Modifier.padding(start = 10.dp, bottom = 5.dp),
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
            .heightIn(min = 80.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 0.dp,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(29.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
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

        if (isLoggingIn) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        else AppButton(stringResource(if (isLoggedIn) R.string.profile_logout else R.string.profile_login), onClick,
            variant = AppButtonVariant.Plain, icon = if (isLoggedIn) Icons.AutoMirrored.Filled.ExitToApp else Icons.AutoMirrored.Filled.Login)

    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = null,
) {

    val source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (LocalCompactLayout.current) 56.dp else 66.dp)
            .iosPressScale(source, pressedScale = .99f)
            .clickable(source, indication = null, enabled = enabled, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = if (LocalCompactLayout.current) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(icon, destructive = titleColor == MaterialTheme.colorScheme.error)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 7.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                maxLines = 2,
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
        modifier = Modifier.padding(start = 60.dp, end = 14.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SelectionSheet(title: String, options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, onDismiss: () -> Unit) {
    AppSheet(onDismissRequest = onDismiss, title = title) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option -> SelectionRow(label(option), option == selected, { onSelect(option) }, icon = Icons.Default.Translate) }
        }
    }
}

@Composable
private fun SelectionRow(label: String, selected: Boolean, onClick: () -> Unit, subtitle: String? = null, modifier: Modifier = Modifier, icon: ImageVector = Icons.Default.CheckCircle) {
    AppChoiceRow(label, icon, selected, onClick, modifier, subtitle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationSettingsSheet(
    enabled: Boolean,
    offset: Int,
    onEnabledChange: (Boolean) -> Unit,
    onOffsetSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    permissionGranted: Boolean = true,
    onRequestPermission: () -> Unit = {},
    quietHours: com.owlcoder.animeschedule.domain.model.QuietHours = com.owlcoder.animeschedule.domain.model.QuietHours(),
    onQuietChange: (com.owlcoder.animeschedule.domain.model.QuietHours) -> Unit = {},
) {
    val offsets = listOf(0, -5, -10, -15, -30, 10, 30, 60)
    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_notifications),
    ) {
        Column(Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InsetGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Notifications, null, Modifier.size(22.dp).padding(end = 4.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
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
            if (enabled && !permissionGranted) AppButton(stringResource(R.string.notification_allow), onRequestPermission, Modifier.fillMaxWidth(), icon = Icons.Default.NotificationsActive)
            if (enabled) {
                NotificationPreferencesControls(quietHours, onQuietChange)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.settings_notif_timing).uppercase(),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        offsets.forEach { option ->
                            SelectionRow(notificationOffsetLabel(option), offset == option,
                                { onOffsetSelect(option) }, icon = Icons.Default.Schedule)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimezoneSheet(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val zones = remember(query) { ZoneId.getAvailableZoneIds().filter { query.isBlank() || it.contains(query.trim(), ignoreCase = true) }.sorted() }
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.settings_timezone)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppSearchField(query, { query = it }, placeholder = stringResource(R.string.settings_timezone_search), leadingIcon = Icons.Default.Search, onClear = { query = "" })
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { SelectionRow(stringResource(R.string.settings_timezone_system), current.isBlank(), { onSelect("") }, subtitle = ZoneId.systemDefault().id, icon = Icons.Default.PhoneAndroid) }
                items(zones, key = { it }) { zone -> SelectionRow(zone, current == zone, { onSelect(zone) }, icon = Icons.Default.Public) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CacheRetentionSheet(
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CacheRetentionPolicy.supportedRetentionDays.forEach { days ->
                    SelectionRow(stringResource(R.string.settings_cache_retention_days, days),
                        current == days, { onSelect(days) }, icon = Icons.Default.Storage)
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
    AppLanguage.SYSTEM -> stringResource(R.string.settings_language_english)
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

@StringRes
private fun LoginFailure.messageRes(): Int = when (this) {
    LoginFailure.DENIED -> R.string.login_error_denied
    LoginFailure.INVALID_SESSION -> R.string.login_error_invalid_session
    LoginFailure.EXCHANGE_FAILED -> R.string.login_error_exchange_failed
}
