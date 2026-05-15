package rs.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import coil3.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import rs.owlcoder.animeschedule.data.local.datastore.ThemeMode
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToAbout: () -> Unit = {}
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showTimezonePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Podešavanja", style = MaterialTheme.typography.titleLarge) },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // --- Profile header card ---
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

            // --- Izgled ---
            item {
                SectionCard {
                    SettingsRow(
                        icon = Icons.Default.DarkMode,
                        iconColor = Color(0xFF5C6BC0),
                        title = "Tema",
                        subtitle = when (uiState.themeMode) {
                            ThemeMode.SYSTEM -> "Sistemska"
                            ThemeMode.LIGHT -> "Svetla"
                            ThemeMode.DARK -> "Tamna"
                        },
                        onClick = { showThemePicker = true }
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon = Icons.Default.Notifications,
                        iconColor = Color(0xFFE53935),
                        title = "Notifikacije",
                        subtitle = if (uiState.notificationsEnabled) "Uključene" else "Isključene",
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { settingsViewModel.setNotificationsEnabled(it) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Language,
                        iconColor = Color(0xFF43A047),
                        title = "Vremenska zona",
                        subtitle = uiState.timezoneId.ifEmpty { ZoneId.systemDefault().id },
                        onClick = { showTimezonePicker = true }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // --- Informacije ---
            item {
                SectionCard {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFF039BE5),
                        title = "O aplikaciji",
                        subtitle = "Verzija, podaci, kontakt",
                        onClick = onNavigateToAbout
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showTimezonePicker) {
        TimezonePickerDialog(
            currentTimezoneId = uiState.timezoneId,
            onDismiss = { showTimezonePicker = false },
            onConfirm = { settingsViewModel.setTimezone(it); showTimezonePicker = false }
        )
    }

    if (showThemePicker) {
        ThemePickerDialog(
            current = uiState.themeMode,
            onDismiss = { showThemePicker = false },
            onConfirm = { settingsViewModel.setThemeMode(it); showThemePicker = false }
        )
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
                        tint = if (isLoggedIn) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
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
                    if (isLoggedIn) "Ulogovan" else "Nije ulogovan",
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
                            "Odjavi",
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
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Prijavi se",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 68.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
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
private fun SettingsRowSwitch(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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

@Composable
private fun ThemePickerDialog(
    current: ThemeMode,
    onDismiss: () -> Unit,
    onConfirm: (ThemeMode) -> Unit
) {
    var selected by remember { mutableStateOf(current) }
    val options = listOf(
        ThemeMode.SYSTEM to "Sistemska",
        ThemeMode.LIGHT to "Svetla",
        ThemeMode.DARK to "Tamna"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Izaberi temu") },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = mode }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == mode, onClick = { selected = mode })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Potvrdi") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkaži") } }
    )
}

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Izaberi vremensku zonu") },
        text = {
            LazyColumn(Modifier.height(400.dp)) {
                items(zones) { zone ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = zone }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == zone, onClick = { selected = zone })
                        Text(
                            zone,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Potvrdi") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkaži") } }
    )
}
