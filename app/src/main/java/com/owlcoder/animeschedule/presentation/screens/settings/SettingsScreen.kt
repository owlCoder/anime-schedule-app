package com.owlcoder.animeschedule.presentation.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.owlcoder.animeschedule.BuildConfig
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
import com.owlcoder.animeschedule.presentation.components.GlassSurface
import com.owlcoder.animeschedule.presentation.components.LocalNavBarHeight
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone
import com.owlcoder.animeschedule.ui.theme.PillShape
import java.time.ZoneId

private val SettingsGroupShape = ContinuousRoundedShape(15.dp)
private val SettingsIconShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onRestartForLanguage: (AppLanguage) -> Unit = {},
    onManageWatchSources: () -> Unit = {},
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val isLoggingIn by authViewModel.isLoggingIn.collectAsState()
    val loginError by authViewModel.loginError.collectAsState()
    val cacheSizeBytes by settingsViewModel.cacheSizeBytes.collectAsState()
    val isClearingCache by settingsViewModel.isClearingCache.collectAsState()
    val cacheActionMessage by settingsViewModel.cacheActionMessage.collectAsState()
    val context = LocalContext.current

    var showThemePicker by remember { mutableStateOf(false) }
    var showTimezonePicker by remember { mutableStateOf(false) }
    var showNotifPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showCacheRetentionPicker by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 6.dp,
                bottom = LocalNavBarHeight.current + 34.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppLargeHeader(
                    title = stringResource(R.string.settings_title),
                    modifier = Modifier.padding(bottom = 1.dp),
                )
            }

            item {
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
                            modifier = Modifier.padding(start = 11.dp, top = 4.dp),
                        )
                    }
                }
            }

            item {
                SettingsSection("Appearance") {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.ColorLens,
                            title = "Theme",
                            value = themeModeLabel(uiState.themeMode),
                            onClick = { showThemePicker = true },
                        )
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.settings_notifications)) {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.Notifications,
                            title = stringResource(R.string.settings_notifications),
                            value = if (uiState.notificationsEnabled) {
                                "${stringResource(R.string.settings_notifications_on)} · ${notifOffsetLabel(uiState.notificationOffsetMinutes)}"
                            } else stringResource(R.string.settings_notifications_off),
                            onClick = { showNotifPicker = true },
                        )
                    }
                }
            }

            item {
                SettingsSection("Schedule") {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.Schedule,
                            title = stringResource(R.string.settings_timezone),
                            value = uiState.timezoneId.ifEmpty { ZoneId.systemDefault().id },
                            onClick = { showTimezonePicker = true },
                        )
                    }
                }
            }

            item {
                SettingsSection("Sources") {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.PlayCircle,
                            title = stringResource(R.string.settings_watch_sources),
                            value = stringResource(R.string.settings_watch_sources_subtitle),
                            onClick = onManageWatchSources,
                        )
                    }
                }
            }

            item {
                SettingsSection("Storage") {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.Storage,
                            title = "Cache",
                            value = "${formatBytes(cacheSizeBytes)} · ${uiState.cacheRetentionDays} days",
                            onClick = { showCacheRetentionPicker = true },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.DeleteSweep,
                            iconColor = MaterialTheme.colorScheme.error,
                            title = "Clear Cache",
                            value = cacheActionMessage ?: "Remove temporary images and stale data",
                            onClick = { showClearCacheConfirm = true },
                            enabled = !isClearingCache,
                            trailing = {
                                if (isClearingCache) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                }
                            },
                        )
                    }
                }
            }

            item {
                SettingsSection("Language") {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.Translate,
                            title = stringResource(R.string.settings_language),
                            value = languageLabel(uiState.appLanguage),
                            onClick = { showLanguagePicker = true },
                        )
                    }
                }
            }

            item {
                SettingsSection("About") {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.Update,
                            title = stringResource(R.string.settings_changelog),
                            value = stringResource(R.string.settings_changelog_subtitle),
                            onClick = { showChangelog = true },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.settings_about),
                            value = stringResource(R.string.settings_about_subtitle),
                            onClick = { showAbout = true },
                        )
                    }
                }
            }
        }
    }

    if (showThemePicker) {
        ThemeBottomSheet(
            current = uiState.themeMode,
            onSelect = { settingsViewModel.setThemeMode(it); showThemePicker = false },
            onDismiss = { showThemePicker = false },
        )
    }
    if (showLanguagePicker) {
        LanguageBottomSheet(
            currentLanguage = uiState.appLanguage,
            onSelect = { language ->
                showLanguagePicker = false
                settingsViewModel.setAppLanguage(language)
                onRestartForLanguage(language)
            },
            onDismiss = { showLanguagePicker = false },
        )
    }
    if (showNotifPicker) {
        NotifBottomSheet(
            enabled = uiState.notificationsEnabled,
            currentOffset = uiState.notificationOffsetMinutes,
            onEnabledChange = settingsViewModel::setNotificationsEnabled,
            onOffsetSelect = settingsViewModel::setNotificationOffset,
            onDismiss = { showNotifPicker = false },
        )
    }
    if (showCacheRetentionPicker) {
        CacheRetentionBottomSheet(
            currentRetentionDays = uiState.cacheRetentionDays,
            onSelect = { settingsViewModel.setCacheRetentionDays(it); showCacheRetentionPicker = false },
            onDismiss = { showCacheRetentionPicker = false },
        )
    }
    if (showClearCacheConfirm) {
        ClearCacheSheet(
            onDismiss = { showClearCacheConfirm = false },
            onConfirm = {
                showClearCacheConfirm = false
                settingsViewModel.clearCacheNow()
            },
        )
    }
    if (showTimezonePicker) {
        TimezonePickerDialog(
            currentTimezoneId = uiState.timezoneId,
            onDismiss = { showTimezonePicker = false },
            onConfirm = { settingsViewModel.setTimezone(it); showTimezonePicker = false },
        )
    }
    if (showChangelog) ChangelogBottomSheet(onDismiss = { showChangelog = false })
    if (showAbout) AboutBottomSheet(onDismiss = { showAbout = false })
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            modifier = Modifier.padding(start = 11.dp, bottom = 4.dp),
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
            .height(58.dp)
            .clickable(enabled = !isLoggingIn, onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(34.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            GlassSurface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                tone = GlassTone.Neutral,
                blur = GlassBlur.None,
            ) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AccountCircle, null, Modifier.size(22.dp))
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (isLoggedIn && username.isNotBlank()) username else "MyAnimeList",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (isLoggedIn) "Signed in" else "Not signed in",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isLoggingIn) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            val actionColor = if (isLoggedIn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            Text(
                text = if (isLoggedIn) "Sign out" else "Sign in",
                style = MaterialTheme.typography.labelMedium,
                color = actionColor,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = if (isLoggedIn) Icons.AutoMirrored.Filled.ExitToApp else Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                tint = actionColor,
                modifier = Modifier.padding(start = 4.dp).size(16.dp),
            )
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
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassSurface(
            modifier = Modifier.size(30.dp),
            shape = SettingsIconShape,
            tone = GlassTone.Neutral,
            blur = GlassBlur.None,
        ) {
            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(17.dp), tint = iconColor)
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(start = 10.dp, end = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (value.isNotBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.84f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
        if (trailing == null) {
            Icon(
                Icons.Default.ChevronRight,
                null,
                Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 51.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f),
    )
}

private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
    AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
    AppLanguage.SERBIAN_LATIN -> stringResource(R.string.settings_language_serbian)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = title,
    ) {
        AppMaterialSurface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
            material = AppMaterial.Grouped,
            shape = SettingsGroupShape,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                content = content,
            )
        }
    }
}

@Composable
private fun PickerRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).clickable(onClick = onClick).padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) Icon(Icons.Default.Check, null, Modifier.size(17.dp))
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 13.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f),
        )
    }
}

@Composable
private fun ThemeBottomSheet(current: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    PickerSheet("Theme", onDismiss) {
        ThemeMode.entries.forEach { mode ->
            PickerRow(themeModeLabel(mode), current == mode) { onSelect(mode) }
        }
    }
}

private val notifOffsetOptions = listOf(-30, -15, -5, 0, 5, 15, 30, 60)

@Composable
fun notifOffsetLabel(minutes: Int): String = when {
    minutes < 0 -> stringResource(R.string.notif_offset_before, -minutes)
    minutes == 0 -> stringResource(R.string.notif_offset_immediate)
    minutes < 60 -> stringResource(R.string.notif_offset_after_min, minutes)
    else -> stringResource(R.string.notif_offset_after_hour, minutes / 60)
}

@Composable
private fun NotifBottomSheet(
    enabled: Boolean,
    currentOffset: Int,
    onEnabledChange: (Boolean) -> Unit,
    onOffsetSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun toggle(value: Boolean) {
        onEnabledChange(value)
        if (
            value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    PickerSheet(stringResource(R.string.settings_notifications), onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.onboarding_notif_enable), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            AppSwitch(checked = enabled, onCheckedChange = ::toggle)
        }
        if (enabled) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
            Text(
                text = stringResource(R.string.notif_offset_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 13.dp, top = 10.dp, bottom = 3.dp),
            )
            notifOffsetOptions.forEach { minutes ->
                PickerRow(notifOffsetLabel(minutes), minutes == currentOffset) { onOffsetSelect(minutes) }
            }
        }
    }
}

@Composable
private fun CacheRetentionBottomSheet(
    currentRetentionDays: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerSheet("Cache Retention", onDismiss) {
        Text(
            text = "Older temporary data is removed automatically during daily maintenance.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(13.dp),
        )
        CacheRetentionPolicy.supportedRetentionDays.forEach { days ->
            PickerRow("$days days", currentRetentionDays == days) { onSelect(days) }
        }
    }
}

@Composable
fun LanguageBottomSheet(
    currentLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerSheet(stringResource(R.string.settings_language), onDismiss) {
        listOf(
            AppLanguage.SYSTEM to stringResource(R.string.settings_language_system),
            AppLanguage.ENGLISH to stringResource(R.string.settings_language_english),
            AppLanguage.SERBIAN_LATIN to stringResource(R.string.settings_language_serbian),
        ).forEach { (language, label) ->
            PickerRow(label, currentLanguage == language) { onSelect(language) }
        }
    }
}

@Composable
private fun TimezonePickerDialog(
    currentTimezoneId: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val zones = remember { ZoneId.getAvailableZoneIds().sorted() }
    var selected by remember(currentTimezoneId) {
        mutableStateOf(currentTimezoneId.ifEmpty { ZoneId.systemDefault().id })
    }
    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = stringResource(R.string.timezone_title),
        trailingContent = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(R.string.timezone_confirm), fontWeight = FontWeight.SemiBold)
            }
        },
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
        ) {
            items(zones) { zone ->
                PickerRow(zone, selected == zone) { selected = zone }
            }
        }
    }
}

@Composable
private fun ClearCacheSheet(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = "Clear cache?",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Temporary images and stale cached data will be removed. Your MAL list, tokens and settings stay safe.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppButton(
                    label = stringResource(R.string.common_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    variant = AppButtonVariant.Secondary,
                )
                AppButton(
                    label = "Clear",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    variant = AppButtonVariant.Destructive,
                )
            }
        }
    }
}

@Composable
private fun ChangelogBottomSheet(onDismiss: () -> Unit) {
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.settings_changelog)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            listOf(
                "Neutral liquid-glass navigation and controls",
                "Seven-day schedule selector",
                "Compact status editor, filters and settings",
                "Improved detail and search layouts",
            ).forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AboutBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = stringResource(R.string.settings_about),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppMaterialSurface(
                modifier = Modifier.size(56.dp),
                material = AppMaterial.Elevated,
                shape = ContinuousRoundedShape(16.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("AS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Text("AnimeSchedule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "Track anime airing schedules, all in one place",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            GlassSurface(shape = PillShape, tone = GlassTone.Neutral, blur = GlassBlur.None) {
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            AppMaterialSurface(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                material = AppMaterial.Grouped,
                shape = SettingsGroupShape,
            ) {
                Column {
                    AboutRow("Version", BuildConfig.VERSION_NAME)
                    SettingsDivider()
                    AboutRow("Build", BuildConfig.VERSION_CODE.toString())
                    SettingsDivider()
                    AboutRow("Platform", "Android ${Build.VERSION.RELEASE}+")
                    SettingsDivider()
                    AboutRow("Package", context.packageName)
                }
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> "${bytes / (1024L * 1024L)} MB"
}
