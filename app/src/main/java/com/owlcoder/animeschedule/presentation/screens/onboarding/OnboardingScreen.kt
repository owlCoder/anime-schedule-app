package com.owlcoder.animeschedule.presentation.screens.onboarding

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.data.local.datastore.AccentColor
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.data.local.datastore.ThemeMode
import com.owlcoder.animeschedule.presentation.components.AppButton
import com.owlcoder.animeschedule.presentation.components.AppButtonVariant
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSwitch
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.iosPressScale
import com.owlcoder.animeschedule.ui.theme.PillShape
import com.owlcoder.animeschedule.ui.theme.accentPrimary
import kotlinx.coroutines.launch

private const val OnboardingPageCount = 6

private fun AppLanguage.t(en: String, sr: String): String =
    if (this == AppLanguage.SERBIAN_LATIN) sr else en

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onLogin: (Context) -> Unit,
    isMalConnected: Boolean,
    malUsername: String,
    selectedTheme: ThemeMode,
    selectedAccent: AccentColor,
    selectedLanguage: AppLanguage,
    onThemeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onNotifSettingsChange: (enabled: Boolean, offsetMinutes: Int) -> Unit = { _, _ -> },
) {
    val pagerState = rememberPagerState(pageCount = { OnboardingPageCount })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(true) }
    var notificationOffset by remember { mutableIntStateOf(0) }
    val currentPage = pagerState.currentPage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NeutralOnboardingBackdrop()
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) { page ->
                when (page) {
                    0 -> LanguageSelectionPage(
                        selectedLanguage = selectedLanguage,
                        onLanguageChange = onLanguageChange,
                    )
                    1 -> WelcomePage(selectedLanguage)
                    2 -> SchedulePreviewPage(selectedLanguage)
                    3 -> NotificationPreviewPage(
                        language = selectedLanguage,
                        enabled = notificationsEnabled,
                        offsetMinutes = notificationOffset,
                        onEnabledChange = {
                            notificationsEnabled = it
                            onNotifSettingsChange(it, notificationOffset)
                        },
                        onOffsetChange = {
                            notificationOffset = it
                            onNotifSettingsChange(notificationsEnabled, it)
                        },
                    )
                    4 -> MalPreviewPage(
                        language = selectedLanguage,
                        isConnected = isMalConnected,
                        username = malUsername,
                        onLogin = { onLogin(context) },
                    )
                    else -> PersonalizePage(
                        language = selectedLanguage,
                        selectedTheme = selectedTheme,
                        selectedAccent = selectedAccent,
                        onThemeChange = onThemeChange,
                        onAccentChange = onAccentChange,
                    )
                }
            }

            OnboardingActions(
                currentPage = currentPage,
                pageCount = OnboardingPageCount,
                language = selectedLanguage,
                onBackOrLater = {
                    if (currentPage > 0) {
                        scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                    } else {
                        onComplete()
                    }
                },
                onContinue = {
                    if (currentPage == OnboardingPageCount - 1) {
                        onComplete()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                    }
                },
            )
        }
    }
}

@Composable
private fun OnboardingActions(
    currentPage: Int,
    pageCount: Int,
    language: AppLanguage,
    onBackOrLater: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PageIndicator(currentPage, pageCount)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (currentPage > 0) {
                AppButton(
                    label = language.t("Back", "Nazad"),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBackOrLater,
                    modifier = Modifier.weight(0.42f),
                    variant = AppButtonVariant.Secondary,
                )
            }
            AppButton(
                label = if (currentPage == pageCount - 1) {
                    language.t("Get started", "Započni")
                } else {
                    language.t("Continue", "Nastavi")
                },
                icon = if (currentPage == pageCount - 1) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.AutoMirrored.Filled.ArrowForward
                },
                onClick = onContinue,
                modifier = Modifier.weight(if (currentPage > 0) 0.58f else 1f),
                variant = AppButtonVariant.Primary,
            )
        }
    }
}

@Composable
private fun LanguageSelectionPage(
    selectedLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    ProductPage(
        eyebrow = selectedLanguage.t("WELCOME", "DOBRODOŠAO"),
        title = selectedLanguage.t("Choose your language", "Izaberi jezik"),
        subtitle = selectedLanguage.t(
            "You can change the app language later in Settings.",
            "Jezik aplikacije možeš kasnije promeniti u Podešavanjima.",
        ),
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = ContinuousRoundedShape(20.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LanguageChoiceCard(
                code = "EN",
                label = "English",
                selected = selectedLanguage == AppLanguage.ENGLISH,
                onClick = { onLanguageChange(AppLanguage.ENGLISH) },
                modifier = Modifier.weight(1f),
            )
            LanguageChoiceCard(
                code = "SR",
                label = "Srpski",
                selected = selectedLanguage == AppLanguage.SERBIAN_LATIN,
                onClick = { onLanguageChange(AppLanguage.SERBIAN_LATIN) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LanguageChoiceCard(
    code: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "language-card-background",
    )
    Surface(
        modifier = modifier
            .height(112.dp)
            .iosPressScale()
            .onboardingClickable(onClick),
        shape = ContinuousRoundedShape(24.dp),
        color = background,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.dp else 0.7.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun NeutralOnboardingBackdrop() {
    val tint = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.09f),
                        tint.copy(alpha = 0.025f),
                        Color.Transparent,
                    ),
                    radius = 920f,
                ),
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.30f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    )
}

@Composable
private fun PageIndicator(currentPage: Int, pageCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(pageCount) { index ->
            val width by animateDpAsState(
                targetValue = if (index == currentPage) 22.dp else 7.dp,
                label = "onboarding-indicator-width",
            )
            val color by animateColorAsState(
                targetValue = if (index == currentPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)
                },
                label = "onboarding-indicator-color",
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(7.dp)
                    .clip(PillShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun WelcomePage(language: AppLanguage) {
    var selectedFeature by remember { mutableIntStateOf(0) }
    val features = listOf(
        Triple(Icons.Default.CalendarMonth, language.t("Schedule", "Raspored"), language.t("See every broadcast in your local time.", "Vidi svako emitovanje u svom vremenu.")),
        Triple(Icons.Default.Search, language.t("Discover", "Pronađi"), language.t("Find titles and seasonal releases quickly.", "Brzo pronađi naslove i sezonska izdanja.")),
        Triple(Icons.Default.Bookmark, language.t("My List", "Moja lista"), language.t("Keep progress and scores in sync.", "Sinhronizuj napredak i ocene.")),
    )

    ProductPage(
        eyebrow = language.t("ANIME, ON TIME", "ANIME, NA VREME"),
        title = language.t("Your week in anime", "Tvoja anime nedelja"),
        subtitle = language.t(
            "A calm schedule, upcoming releases and your list in one focused app.",
            "Miran raspored, nove epizode i tvoja lista u jednoj fokusiranoj aplikaciji.",
        ),
    ) {
        AppMark()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            features.forEachIndexed { index, feature ->
                FeatureTile(
                    icon = feature.first,
                    label = feature.second,
                    selected = selectedFeature == index,
                    onClick = { selectedFeature = index },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        AppMaterialSurface(
            modifier = Modifier.fillMaxWidth(),
            material = AppMaterial.Interactive,
            shape = ContinuousRoundedShape(18.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = features[selectedFeature].first,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = features[selectedFeature].third,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SchedulePreviewPage(language: AppLanguage) {
    var selectedDay by remember { mutableIntStateOf(0) }
    val days = listOf("Mon" to "3", "Tue" to "4", "Wed" to "5", "Thu" to "6", "Fri" to "7")
    val times = listOf("17:00", "18:30", "19:15", "20:00", "21:00")

    ProductPage(
        eyebrow = language.t("SEVEN DAYS AHEAD", "SEDAM DANA UNAPRED"),
        title = language.t("See what airs next", "Vidi šta sledi"),
        subtitle = language.t(
            "Tap a day to preview its schedule. Everything follows your selected time zone.",
            "Dodirni dan da pregledaš raspored. Sve prati izabranu vremensku zonu.",
        ),
    ) {
        AppMaterialSurface(
            modifier = Modifier.fillMaxWidth(),
            material = AppMaterial.Elevated,
            shape = ContinuousRoundedShape(22.dp),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    days.forEachIndexed { index, pair ->
                        DatePreviewCell(
                            day = pair.first,
                            date = pair.second,
                            selected = selectedDay == index,
                            onClick = { selectedDay = index },
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                )
                PreviewScheduleRow(times[selectedDay], "Grand Blue Season 3", "Ep. ${selectedDay + 5}/12")
                PreviewScheduleRow("18:${30 + selectedDay}", "Summer Pockets", "Ep. ${17 + selectedDay}")
                PreviewScheduleRow("21:00", "The Fragrant Flower", "Ep. ${6 + selectedDay}/13")
            }
        }
        Text(
            text = language.t("Try selecting another day", "Probaj da izabereš drugi dan"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun NotificationPreviewPage(
    language: AppLanguage,
    enabled: Boolean,
    offsetMinutes: Int,
    onEnabledChange: (Boolean) -> Unit,
    onOffsetChange: (Int) -> Unit,
) {
    ProductPage(
        eyebrow = language.t("GENTLE REMINDERS", "DISKRETNI PODSETNICI"),
        title = language.t("Never miss an episode", "Ne propusti epizodu"),
        subtitle = language.t(
            "Turn reminders on and choose exactly when they arrive.",
            "Uključi podsetnike i izaberi tačno kada stižu.",
        ),
    ) {
        AppMaterialSurface(
            modifier = Modifier.fillMaxWidth(),
            material = AppMaterial.Elevated,
            shape = ContinuousRoundedShape(20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = ContinuousRoundedShape(13.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (enabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        text = language.t("Episode reminders", "Podsetnici za epizode"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (enabled) language.t("Enabled", "Uključeni")
                        else language.t("Disabled", "Isključeni"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AppSwitch(checked = enabled, onCheckedChange = onEnabledChange)
            }
        }
        if (enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(-15, 0, 15).forEach { minutes ->
                    SelectionChip(
                        label = when (minutes) {
                            -15 -> language.t("15 min before", "15 min ranije")
                            0 -> language.t("At air time", "Pri emitovanju")
                            else -> language.t("15 min after", "15 min kasnije")
                        },
                        selected = offsetMinutes == minutes,
                        onClick = { onOffsetChange(minutes) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            AppMaterialSurface(
                modifier = Modifier.fillMaxWidth(),
                material = AppMaterial.Interactive,
                shape = ContinuousRoundedShape(18.dp),
            ) {
                Text(
                    text = language.t(
                        "You can enable reminders later in Settings.",
                        "Podsetnike možeš uključiti kasnije u Podešavanjima.",
                    ),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MalPreviewPage(
    language: AppLanguage,
    isConnected: Boolean,
    username: String,
    onLogin: () -> Unit,
) {
    ProductPage(
        eyebrow = "MYANIMELIST",
        title = language.t("Your progress, kept in sync", "Tvoj napredak, uvek sinhronizovan"),
        subtitle = language.t(
            "Connect your account to update episodes, status and scores without leaving the app.",
            "Poveži nalog da ažuriraš epizode, status i ocene bez napuštanja aplikacije.",
        ),
    ) {
        AppMaterialSurface(
            modifier = Modifier.fillMaxWidth(),
            material = AppMaterial.Elevated,
            shape = ContinuousRoundedShape(22.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(30.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 11.dp)) {
                        Text("MyAnimeList", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                isConnected && username.isNotBlank() -> language.t(
                                    "Connected as @$username",
                                    "Povezano kao @$username",
                                )
                                isConnected -> language.t("Connected", "Povezano")
                                else -> language.t("Not connected", "Nije povezano")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConnected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                )
                PreviewScheduleRow("12/12", language.t("Completed", "Završeno"), language.t("Your score 8/10", "Tvoja ocena 8/10"))
                AppButton(
                    label = if (isConnected) {
                        language.t("Reconnect account", "Ponovo poveži nalog")
                    } else {
                        language.t("Connect account", "Poveži nalog")
                    },
                    icon = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Login,
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth(),
                    variant = AppButtonVariant.Secondary,
                )
            }
        }
        Text(
            text = language.t("Optional — you can connect later", "Opciono — možeš povezati kasnije"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PersonalizePage(
    language: AppLanguage,
    selectedTheme: ThemeMode,
    selectedAccent: AccentColor,
    onThemeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
) {
    ProductPage(
        eyebrow = language.t("FINISH SETUP", "ZAVRŠI PODEŠAVANJE"),
        title = language.t("Make it yours", "Podesi po svom ukusu"),
        subtitle = language.t(
            "Choose appearance and accent. Changes are applied immediately.",
            "Izaberi izgled i boju. Promene se primenjuju odmah.",
        ),
    ) {
        CompactPreferenceGroup(
            icon = Icons.Default.Palette,
            title = language.t("Appearance", "Izgled"),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    SelectionChip(
                        label = when (mode) {
                            ThemeMode.SYSTEM -> language.t("System", "Sistem")
                            ThemeMode.LIGHT -> language.t("Light", "Svetlo")
                            ThemeMode.DARK -> language.t("Dark", "Tamno")
                        },
                        selected = selectedTheme == mode,
                        onClick = { onThemeChange(mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            AccentPicker(selectedAccent, onAccentChange)
        }

    }
}

@Composable
private fun ProductPage(
    eyebrow: String,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.16f))
        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .padding(horizontal = 11.dp, vertical = 5.dp),
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = 11.dp),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(11.dp),
            content = content,
        )
        Spacer(Modifier.weight(0.84f))
    }
}

@Composable
private fun AppMark() {
    Surface(
        modifier = Modifier.size(120.dp),
        shape = ContinuousRoundedShape(34.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = ContinuousRoundedShape(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.onboardingClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
}

@Composable
private fun FeatureTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "feature-tile-color",
    )
    Surface(
        modifier = modifier
            .height(72.dp)
            .iosPressScale()
            .onboardingClickable(onClick),
        shape = ContinuousRoundedShape(18.dp),
        color = color,
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
            )
        } else null,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(21.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DatePreviewCell(
    day: String,
    date: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        } else {
            Color.Transparent
        },
        label = "date-preview-color",
    )
    Surface(
        modifier = Modifier
            .size(width = 48.dp, height = 54.dp)
            .iosPressScale()
            .onboardingClickable(onClick),
        shape = ContinuousRoundedShape(16.dp),
        color = color,
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                0.8.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
            )
        } else null,
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = date,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PreviewScheduleRow(time: String, title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = time,
            modifier = Modifier.width(48.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "selection-chip-background",
    )
    Surface(
        modifier = modifier
            .height(46.dp)
            .iosPressScale()
            .onboardingClickable(onClick),
        shape = ContinuousRoundedShape(16.dp),
        color = background,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.7.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompactPreferenceGroup(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppMaterialSurface(
        modifier = Modifier.fillMaxWidth(),
        material = AppMaterial.Elevated,
        shape = ContinuousRoundedShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            content()
        }
    }
}

@Composable
private fun AccentPicker(
    selectedAccent: AccentColor,
    onAccentChange: (AccentColor) -> Unit,
) {
    val options = listOf(
        AccentColor.TELEGRAM_BLUE,
        AccentColor.PURPLE,
        AccentColor.GREEN,
        AccentColor.ORANGE,
        AccentColor.PINK,
        AccentColor.RED,
        AccentColor.CYAN,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { accent ->
            val selected = selectedAccent == accent
            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .iosPressScale()
                    .onboardingClickable { onAccentChange(accent) },
                shape = CircleShape,
                color = accent.swatch(),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.surface,
                ),
            ) {
                if (selected) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = if (accent.swatch().luminance() > 0.58f) {
                                Color(0xFF10131A)
                            } else {
                                Color.White
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccentColor.swatch(): Color = accentPrimary(
    accent = this,
    dark = MaterialTheme.colorScheme.background.luminance() < 0.35f,
)
