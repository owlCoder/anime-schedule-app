package rs.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import rs.owlcoder.animeschedule.R
import rs.owlcoder.animeschedule.data.local.datastore.AccentColor
import rs.owlcoder.animeschedule.data.local.datastore.AppLanguage
import rs.owlcoder.animeschedule.data.local.datastore.ThemeMode
import rs.owlcoder.animeschedule.ui.theme.accentPrimary
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToAbout: () -> Unit = {},
    onNavigateToChangelog: () -> Unit = {},
    onRestartForLanguage: (AppLanguage) -> Unit = {}
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showTimezonePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showNotifPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                ProfileCard(
                    isLoggedIn = uiState.isLoggedIn,
                    username = uiState.username,
                    avatarUrl = uiState.avatarUrl,
                    onLogin = { authViewModel.launchMalLogin(context) },
                    onLogout = { authViewModel.logout() }
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                SectionCard {
                    SettingsRow(
                        icon = Icons.Default.DarkMode,
                        iconColor = Color(0xFF5C6BC0),
                        title = stringResource(R.string.settings_theme),
                        subtitle = when (uiState.themeMode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                            ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                            ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                        },
                        onClick = { showThemePicker = true }
                    )
                    SettingsDivider()
                    val notifSubtitle = if (uiState.notificationsEnabled)
                        "${stringResource(R.string.settings_notifications_on)} · ${notifOffsetLabel(uiState.notificationOffsetMinutes)}"
                    else
                        stringResource(R.string.settings_notifications_off)
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        iconColor = Color(0xFFE53935),
                        title = stringResource(R.string.settings_notifications),
                        subtitle = notifSubtitle,
                        onClick = { showNotifPicker = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Translate,
                        iconColor = Color(0xFF1565C0),
                        title = stringResource(R.string.settings_language),
                        subtitle = when (uiState.appLanguage) {
                            AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
                            AppLanguage.SERBIAN_LATIN -> stringResource(R.string.settings_language_serbian)
                            AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                        },
                        onClick = { showLanguagePicker = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Schedule,
                        iconColor = Color(0xFF43A047),
                        title = stringResource(R.string.settings_timezone),
                        subtitle = uiState.timezoneId.ifEmpty { ZoneId.systemDefault().id },
                        onClick = { showTimezonePicker = true }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                SectionCard {
                    SettingsRow(
                        icon = Icons.Default.Update,
                        iconColor = Color(0xFF00897B),
                        title = stringResource(R.string.settings_changelog),
                        subtitle = stringResource(R.string.settings_changelog_subtitle),
                        onClick = onNavigateToChangelog
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFF039BE5),
                        title = stringResource(R.string.settings_about),
                        subtitle = stringResource(R.string.settings_about_subtitle),
                        onClick = onNavigateToAbout
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showLanguagePicker) {
        LanguageBottomSheet(
            currentLanguage = uiState.appLanguage,
            onSelect = { lang ->
                showLanguagePicker = false
                onRestartForLanguage(lang)
            },
            onDismiss = { showLanguagePicker = false }
        )
    }

    if (showNotifPicker) {
        NotifBottomSheet(
            enabled = uiState.notificationsEnabled,
            currentOffset = uiState.notificationOffsetMinutes,
            onEnabledChange = { settingsViewModel.setNotificationsEnabled(it) },
            onOffsetSelect = { settingsViewModel.setNotificationOffset(it) },
            onDismiss = { showNotifPicker = false }
        )
    }

    if (showTimezonePicker) {
        TimezonePickerDialog(
            currentTimezoneId = uiState.timezoneId,
            onDismiss = { showTimezonePicker = false },
            onConfirm = { settingsViewModel.setTimezone(it); showTimezonePicker = false }
        )
    }

    if (showThemePicker) {
        ThemeBottomSheet(
            currentTheme = uiState.themeMode,
            currentAccent = uiState.accentColor,
            onThemeChange = { settingsViewModel.setThemeMode(it) },
            onAccentChange = { settingsViewModel.setAccentColor(it) },
            onDismiss = { showThemePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeBottomSheet(
    currentTheme: ThemeMode,
    currentAccent: AccentColor,
    onThemeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Text(
                stringResource(R.string.appearance_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Text(
                stringResource(R.string.appearance_theme_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val themeOptions = listOf(
                Triple(ThemeMode.SYSTEM, stringResource(R.string.settings_theme_system), Icons.Default.AutoMode),
                Triple(ThemeMode.LIGHT,  stringResource(R.string.settings_theme_light),  Icons.Default.LightMode),
                Triple(ThemeMode.DARK,   stringResource(R.string.settings_theme_dark),   Icons.Default.DarkMode)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                themeOptions.forEach { (mode, label, icon) ->
                    val selected = currentTheme == mode
                    val borderColor by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        label = "borderColor"
                    )
                    val iconTint by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "iconTint"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onThemeChange(mode) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.appearance_accent_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.05f
            val accentOptions = listOf(
                AccentColor.TELEGRAM_BLUE to "Plava",
                AccentColor.PURPLE        to "Ljubičasta",
                AccentColor.GREEN         to "Zelena",
                AccentColor.ORANGE        to "Narandžasta",
                AccentColor.PINK          to "Roze",
                AccentColor.RED           to "Crvena",
                AccentColor.CYAN          to "Cijan",
                AccentColor.INDIGO        to "Indigo",
                AccentColor.TEAL         to "Teal",
                AccentColor.YELLOW        to "Žuta",
                AccentColor.DEEP_PURPLE   to "Duboka ljubičasta"
            )
            val rows = accentOptions.chunked(6)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowItems.forEach { (accent, name) ->
                            val selected = currentAccent == accent
                            val color = accentPrimary(accent, dark = isDark)
                            val size by animateDpAsState(
                                if (selected) 44.dp else 36.dp,
                                animationSpec = spring(),
                                label = "accentSize"
                            )
                            Box(
                                modifier = Modifier
                                    .size(size)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (selected) Modifier.border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                        else Modifier
                                    )
                                    .clickable { onAccentChange(accent) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = name,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        repeat(6 - rowItems.size) {
                            Spacer(Modifier.size(36.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileCard(
    isLoggedIn: Boolean,
    username: String,
    avatarUrl: String,
    onLogin: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isLoggedIn && avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = username,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isLoggedIn) username.ifEmpty { "MyAnimeList" } else "MyAnimeList",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (isLoggedIn) stringResource(R.string.profile_logged_in) else stringResource(R.string.profile_not_logged_in),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLoggedIn) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isLoggedIn) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable { onLogout() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.profile_logout),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onLogin() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Login,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.profile_login),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, color = iconColor)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimezonePickerDialog(
    currentTimezoneId: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val zones = remember {
        ZoneId.getAvailableZoneIds().sorted().filter { !it.startsWith("Etc/") || it == "Etc/UTC" }
    }
    var selected by remember { mutableStateOf(currentTimezoneId.ifEmpty { ZoneId.systemDefault().id }) }
    val sheetState = rememberModalBottomSheetState()
    val initialIndex = remember(zones, selected) { zones.indexOf(selected).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.timezone_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(R.string.timezone_confirm))
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                items(zones) { zone ->
                    val isSelected = selected == zone
                    val bgColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else Color.Transparent,
                        label = "zoneBg"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .clickable { selected = zone }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            zone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Notification helpers ──────────────────────────────────────────────────────

private val notifOffsetOptions = listOf(-30, -15, -5, 0, 5, 15, 30, 60)

@Composable
fun notifOffsetLabel(minutes: Int): String = when {
    minutes < 0  -> stringResource(R.string.notif_offset_before, -minutes)
    minutes == 0 -> stringResource(R.string.notif_offset_immediate)
    minutes < 60 -> stringResource(R.string.notif_offset_after_min, minutes)
    else         -> stringResource(R.string.notif_offset_after_hour, minutes / 60)
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
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Text(
                stringResource(R.string.settings_notifications),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.onboarding_notif_enable),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            if (enabled) {
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.notif_offset_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    stringResource(R.string.notif_offset_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                notifOffsetOptions.forEach { minutes ->
                    val isSelected = minutes == currentOffset
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                      else MaterialTheme.colorScheme.surface,
                        label = "offsetBg"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .clickable { onOffsetSelect(minutes) }
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            notifOffsetLabel(minutes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
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
    val sheetState = rememberModalBottomSheetState()
    val options = listOf(
        AppLanguage.SYSTEM to stringResource(R.string.settings_language_system),
        AppLanguage.ENGLISH to stringResource(R.string.settings_language_english),
        AppLanguage.SERBIAN_LATIN to stringResource(R.string.settings_language_serbian)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Text(
                stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            options.forEach { (language, label) ->
                val isSelected = currentLanguage == language
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                  else MaterialTheme.colorScheme.surface,
                    label = "langBg"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable { onSelect(language) }
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
