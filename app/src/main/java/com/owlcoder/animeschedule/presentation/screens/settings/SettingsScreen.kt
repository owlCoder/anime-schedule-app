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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.data.local.datastore.AccentColor
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.data.local.datastore.CacheRetentionPolicy
import com.owlcoder.animeschedule.data.local.datastore.ThemeMode
import com.owlcoder.animeschedule.presentation.components.LocalNavBarHeight
import com.owlcoder.animeschedule.ui.theme.accentPrimary
import java.time.ZoneId

private val SettingsGroupShape = RoundedCornerShape(16.dp)
private val SettingsAccentRed = Color(0xFFFF3B30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onRestartForLanguage: (AppLanguage) -> Unit = {},
    onManageWatchSources: () -> Unit = {}
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val isLoggingIn by authViewModel.isLoggingIn.collectAsState()
    val loginError by authViewModel.loginError.collectAsState()
    val cacheSizeBytes by settingsViewModel.cacheSizeBytes.collectAsState()
    val isClearingCache by settingsViewModel.isClearingCache.collectAsState()
    val cacheActionMessage by settingsViewModel.cacheActionMessage.collectAsState()
    val context = LocalContext.current

    var showThemePicker by remember { mutableStateOf(false) }
    var showAccentPicker by remember { mutableStateOf(false) }
    var showTimezonePicker by remember { mutableStateOf(false) }
    var showNotifPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showCacheRetentionPicker by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
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
                top = 10.dp,
                bottom = LocalNavBarHeight.current + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                SettingsSection(
                    title = stringResource(R.string.settings_section_account)
                ) {
                    SettingsGroup {
                        AccountRow(
                            isLoggedIn = uiState.isLoggedIn,
                            username = uiState.username,
                            avatarUrl = uiState.avatarUrl,
                            isLoggingIn = isLoggingIn,
                            onClick = {
                                if (uiState.isLoggedIn) authViewModel.logout()
                                else authViewModel.launchMalLogin(context)
                            }
                        )
                    }
                    if (!loginError.isNullOrBlank()) {
                        Text(
                            text = loginError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Appearance") {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.ColorLens,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Theme",
                            value = themeModeLabel(uiState.themeMode),
                            onClick = { showThemePicker = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.ColorLens,
                            iconTint = accentPrimary(uiState.accentColor, dark = isDarkTheme()),
                            title = "Accent Color",
                            value = uiState.accentColor.displayName(),
                            onClick = { showAccentPicker = true },
                            trailing = {
                                AccentSwatch(
                                    color = accentPrimary(uiState.accentColor, dark = isDarkTheme())
                                )
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_notifications)) {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.Notifications,
                            iconTint = if (uiState.notificationsEnabled) MaterialTheme.colorScheme.primary else SettingsAccentRed,
                            title = stringResource(R.string.settings_notifications),
                            value = if (uiState.notificationsEnabled) {
                                "${stringResource(R.string.settings_notifications_on)} · ${notifOffsetLabel(uiState.notificationOffsetMinutes)}"
                            } else {
                                stringResource(R.string.settings_notifications_off)
                            },
                            onClick = { showNotifPicker = true }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Schedule") {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.Schedule,
                            iconTint = accentPrimary(AccentColor.GREEN, dark = isDarkTheme()),
                            title = stringResource(R.string.settings_timezone),
                            value = uiState.timezoneId.ifEmpty { ZoneId.systemDefault().id },
                            onClick = { showTimezonePicker = true }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Sources") {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.PlayCircle,
                            iconTint = accentPrimary(AccentColor.PURPLE, dark = isDarkTheme()),
                            title = stringResource(R.string.settings_watch_sources),
                            value = stringResource(R.string.settings_watch_sources_subtitle),
                            onClick = onManageWatchSources
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Storage") {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.Storage,
                            iconTint = accentPrimary(AccentColor.TEAL, dark = isDarkTheme()),
                            title = "Cache",
                            value = "${formatBytes(cacheSizeBytes)} · ${uiState.cacheRetentionDays} days",
                            onClick = { showCacheRetentionPicker = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Storage,
                            iconTint = SettingsAccentRed,
                            title = "Clear Cache",
                            value = cacheActionMessage ?: "Remove temporary images and stale data",
                            onClick = { showClearCacheConfirm = true },
                            enabled = !isClearingCache,
                            trailing = {
                                if (isClearingCache) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Language") {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.Translate,
                            iconTint = accentPrimary(AccentColor.TELEGRAM_BLUE, dark = isDarkTheme()),
                            title = stringResource(R.string.settings_language),
                            value = languageLabel(uiState.appLanguage),
                            onClick = { showLanguagePicker = true }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "About") {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.Update,
                            iconTint = accentPrimary(AccentColor.TEAL, dark = isDarkTheme()),
                            title = stringResource(R.string.settings_changelog),
                            value = stringResource(R.string.settings_changelog_subtitle),
                            onClick = { showChangelog = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Info,
                            iconTint = accentPrimary(AccentColor.TELEGRAM_BLUE, dark = isDarkTheme()),
                            title = stringResource(R.string.settings_about),
                            value = stringResource(R.string.settings_about_subtitle),
                            onClick = { showAbout = true }
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
            onDismiss = { showThemePicker = false }
        )
    }
    if (showAccentPicker) {
        AccentBottomSheet(
            current = uiState.accentColor,
            onSelect = { settingsViewModel.setAccentColor(it); showAccentPicker = false },
            onDismiss = { showAccentPicker = false }
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
            onDismiss = { showLanguagePicker = false }
        )
    }
    if (showNotifPicker) {
        NotifBottomSheet(
            enabled = uiState.notificationsEnabled,
            currentOffset = uiState.notificationOffsetMinutes,
            onEnabledChange = settingsViewModel::setNotificationsEnabled,
            onOffsetSelect = settingsViewModel::setNotificationOffset,
            onDismiss = { showNotifPicker = false }
        )
    }
    if (showCacheRetentionPicker) {
        CacheRetentionBottomSheet(
            currentRetentionDays = uiState.cacheRetentionDays,
            onSelect = { settingsViewModel.setCacheRetentionDays(it); showCacheRetentionPicker = false },
            onDismiss = { showCacheRetentionPicker = false }
        )
    }
    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear cache?") },
            text = { Text("Temporary images and stale cached data will be removed. Your MAL list, tokens and settings stay safe.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheConfirm = false
                    settingsViewModel.clearCacheNow()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
    if (showTimezonePicker) {
        TimezonePickerDialog(
            currentTimezoneId = uiState.timezoneId,
            onDismiss = { showTimezonePicker = false },
            onConfirm = { settingsViewModel.setTimezone(it); showTimezonePicker = false }
        )
    }
    if (showChangelog) ChangelogBottomSheet(onDismiss = { showChangelog = false })
    if (showAbout) AboutBottomSheet(onDismiss = { showAbout = false })
}

@Composable
private fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.2f

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 7.dp)
        )
        content()
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsGroupShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun AccountRow(
    isLoggedIn: Boolean,
    username: String,
    avatarUrl: String,
    isLoggingIn: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(enabled = !isLoggingIn, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(7.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isLoggedIn && username.isNotBlank()) username else "MyAnimeList",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Text(
                text = if (isLoggedIn) "Signed in" else "Not signed in",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (isLoggingIn) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(
                text = if (isLoggedIn) "Sign out" else "Sign in",
                style = MaterialTheme.typography.labelLarge,
                color = if (isLoggedIn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (isLoggedIn) Icons.AutoMirrored.Filled.ExitToApp else Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                tint = if (isLoggedIn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp).size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp, max = 64.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(8.dp),
            color = iconTint.copy(alpha = 0.14f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.padding(5.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (value.isNotBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
        if (trailing == null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    )
}

@Composable
private fun AccentSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
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

private fun AccentColor.displayName(): String = name.lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeBottomSheet(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    PickerSheet(title = "Theme", onDismiss = onDismiss) {
        ThemeMode.entries.forEach { mode ->
            PickerRow(
                label = themeModeLabel(mode),
                selected = current == mode,
                onClick = { onSelect(mode) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccentBottomSheet(
    current: AccentColor,
    onSelect: (AccentColor) -> Unit,
    onDismiss: () -> Unit
) {
    PickerSheet(title = "Accent Color", onDismiss = onDismiss) {
        AccentColor.entries.forEach { accent ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(accent) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccentSwatch(accentPrimary(accent, dark = isDarkTheme()))
                Text(
                    text = accent.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    color = if (current == accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (current == accent) FontWeight.SemiBold else FontWeight.Normal
                )
                if (current == accent) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets.navigationBars }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PickerRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifBottomSheet(
    enabled: Boolean,
    currentOffset: Int,
    onEnabledChange: (Boolean) -> Unit,
    onOffsetSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun toggle(value: Boolean) {
        onEnabledChange(value)
        if (value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    PickerSheet(title = stringResource(R.string.settings_notifications), onDismiss = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.onboarding_notif_enable), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = enabled, onCheckedChange = ::toggle)
        }
        if (enabled) {
            Text(
                stringResource(R.string.notif_offset_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 6.dp)
            )
            notifOffsetOptions.forEach { minutes ->
                PickerRow(
                    label = notifOffsetLabel(minutes),
                    selected = minutes == currentOffset,
                    onClick = { onOffsetSelect(minutes) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CacheRetentionBottomSheet(
    currentRetentionDays: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    PickerSheet(title = "Cache Retention", onDismiss = onDismiss) {
        Text(
            "Older temporary data is removed automatically during daily maintenance.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CacheRetentionPolicy.supportedRetentionDays.forEach { days ->
            PickerRow(
                label = "$days days",
                selected = currentRetentionDays == days,
                onClick = { onSelect(days) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageBottomSheet(
    currentLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    PickerSheet(title = stringResource(R.string.settings_language), onDismiss = onDismiss) {
        listOf(
            AppLanguage.SYSTEM to stringResource(R.string.settings_language_system),
            AppLanguage.ENGLISH to stringResource(R.string.settings_language_english),
            AppLanguage.SERBIAN_LATIN to stringResource(R.string.settings_language_serbian)
        ).forEach { (language, label) ->
            PickerRow(label, currentLanguage == language, onClick = { onSelect(language) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimezonePickerDialog(
    currentTimezoneId: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val zones = remember { ZoneId.getAvailableZoneIds().sorted() }
    var selected by remember(currentTimezoneId) { mutableStateOf(currentTimezoneId.ifEmpty { ZoneId.systemDefault().id }) }
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets.navigationBars }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.timezone_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { onConfirm(selected) }) { Text(stringResource(R.string.timezone_confirm)) }
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().height(420.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(zones) { zone ->
                    PickerRow(zone, selected == zone, onClick = { selected = zone })
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> "${bytes / (1024L * 1024L)} MB"
}
