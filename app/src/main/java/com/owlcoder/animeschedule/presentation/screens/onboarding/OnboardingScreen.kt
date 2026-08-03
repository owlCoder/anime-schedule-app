package com.owlcoder.animeschedule.presentation.screens.onboarding

import android.content.Context
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.owlcoder.animeschedule.presentation.components.AppSwitch
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.GlassSurface
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone
import com.owlcoder.animeschedule.ui.theme.PillShape
import kotlinx.coroutines.launch

private const val OnboardingPageCount = 5

private fun AppLanguage.t(en: String, sr: String): String =
    if (this == AppLanguage.SERBIAN_LATIN) sr else en

@Suppress("UNUSED_PARAMETER")
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onLogin: (Context) -> Unit,
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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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
                    0 -> WelcomePage(selectedLanguage)
                    1 -> SchedulePreviewPage(selectedLanguage)
                    2 -> NotificationPreviewPage(
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
                    3 -> MalPreviewPage(
                        language = selectedLanguage,
                        onLogin = { onLogin(context) },
                    )
                    else -> PersonalizePage(
                        language = selectedLanguage,
                        selectedTheme = selectedTheme,
                        selectedLanguage = selectedLanguage,
                        onThemeChange = onThemeChange,
                        onLanguageChange = onLanguageChange,
                    )
                }
            }

            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = ContinuousRoundedShape(30.dp),
                tone = GlassTone.Neutral,
                blur = GlassBlur.Medium,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PageIndicator(currentPage, OnboardingPageCount)
                    AppButton(
                        label = if (currentPage == OnboardingPageCount - 1) {
                            selectedLanguage.t("Start using AnimeSchedule", "Počni da koristiš AnimeSchedule")
                        } else {
                            selectedLanguage.t("Continue", "Nastavi")
                        },
                        onClick = {
                            if (currentPage == OnboardingPageCount - 1) {
                                onComplete()
                            } else {
                                scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = AppButtonVariant.Primary,
                    )
                    TextButton(
                        onClick = {
                            if (currentPage > 0) {
                                scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                            } else {
                                onComplete()
                            }
                        },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                    ) {
                        Text(
                            text = if (currentPage > 0) {
                                selectedLanguage.t("Back", "Nazad")
                            } else {
                                selectedLanguage.t("Set up later", "Podesi kasnije")
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeutralOnboardingBackdrop() {
    val light = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        light.copy(alpha = 0.09f),
                        light.copy(alpha = 0.025f),
                        Color.Transparent,
                    ),
                    radius = 1050f,
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
            Box(
                modifier = Modifier
                    .width(if (index == currentPage) 20.dp else 6.dp)
                    .height(6.dp)
                    .clip(PillShape)
                    .background(
                        if (index == currentPage) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                    ),
            )
        }
    }
}

@Composable
private fun WelcomePage(language: AppLanguage) {
    ProductPage(
        eyebrow = language.t("ANIME, ON TIME", "ANIME, NA VREME"),
        title = language.t("Your week in anime", "Tvoja anime nedelja"),
        subtitle = language.t(
            "A calm schedule, upcoming releases and your list in one focused app.",
            "Miran raspored, nove epizode i tvoja lista u jednoj fokusiranoj aplikaciji.",
        ),
    ) {
        GlassSurface(
            modifier = Modifier.size(154.dp),
            shape = CircleShape,
            tone = GlassTone.Accent,
            blur = GlassBlur.Medium,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("AS", style = MaterialTheme.typography.headlineLarge)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureOrb(Icons.Default.CalendarMonth, language.t("Schedule", "Raspored"))
            FeatureOrb(Icons.Default.Search, language.t("Discover", "Pronađi"))
            FeatureOrb(Icons.Default.Bookmark, language.t("My List", "Moja lista"))
        }
    }
}

@Composable
private fun SchedulePreviewPage(language: AppLanguage) {
    ProductPage(
        eyebrow = language.t("SEVEN DAYS AHEAD", "SEDAM DANA UNAPRED"),
        title = language.t("See what airs next", "Vidi šta sledi"),
        subtitle = language.t(
            "Move through the week and open the complete schedule for any day.",
            "Prođi kroz nedelju i otvori kompletan raspored za bilo koji dan.",
        ),
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = ContinuousRoundedShape(24.dp),
            tone = GlassTone.Neutral,
            blur = GlassBlur.Medium,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("Mon" to "3", "Tue" to "4", "Wed" to "5", "Thu" to "6", "Fri" to "7").forEachIndexed { index, pair ->
                        DatePreviewCell(pair.first, pair.second, selected = index == 0)
                    }
                }
                PreviewScheduleRow("17:00", "Grand Blue Season 3", "Ep. 5/12")
                PreviewScheduleRow("18:30", "Summer Pockets", "Ep. 17")
                PreviewScheduleRow("21:00", "The Fragrant Flower", "Ep. 6/13")
            }
        }
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
            "Choose when reminders arrive. You can change this at any time.",
            "Izaberi kada stižu podsetnici. Ovo možeš promeniti bilo kada.",
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedShape(22.dp),
                tone = GlassTone.Neutral,
                blur = GlassBlur.Medium,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(language.t("Episode reminders", "Podsetnici za epizode"), style = MaterialTheme.typography.titleMedium)
                        Text(language.t("Only for titles you follow", "Samo za naslove koje pratiš"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        SelectionPill(
                            label = when (minutes) {
                                -15 -> language.t("15 min before", "15 min ranije")
                                0 -> language.t("At air time", "U vreme emitovanja")
                                else -> language.t("15 min after", "15 min kasnije")
                            },
                            selected = offsetMinutes == minutes,
                            modifier = Modifier.weight(1f),
                            onClick = { onOffsetChange(minutes) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MalPreviewPage(language: AppLanguage, onLogin: () -> Unit) {
    ProductPage(
        eyebrow = "MYANIMELIST",
        title = language.t("Your progress, kept in sync", "Tvoj napredak, uvek sinhronizovan"),
        subtitle = language.t(
            "Update episodes and status without leaving your schedule.",
            "Ažuriraj epizode i status bez napuštanja rasporeda.",
        ),
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = ContinuousRoundedShape(24.dp),
            tone = GlassTone.Neutral,
            blur = GlassBlur.Medium,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassSurface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        tone = GlassTone.Accent,
                        blur = GlassBlur.Soft,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("MyAnimeList", style = MaterialTheme.typography.titleLarge)
                        Text(language.t("Not connected", "Nije povezano"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                PreviewScheduleRow("12/12", language.t("Completed", "Završeno"), "Score 8/10")
                AppButton(
                    label = language.t("Connect MyAnimeList", "Poveži MyAnimeList"),
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth(),
                    variant = AppButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
private fun PersonalizePage(
    language: AppLanguage,
    selectedTheme: ThemeMode,
    selectedLanguage: AppLanguage,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    ProductPage(
        eyebrow = language.t("FINISH SETUP", "ZAVRŠI PODEŠAVANJE"),
        title = language.t("Make it comfortable", "Podesi kako ti odgovara"),
        subtitle = language.t(
            "Liquid glass stays neutral. Choose only appearance and language.",
            "Liquid glass ostaje neutralan. Izaberi samo izgled i jezik.",
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ChoiceGroup(
                title = language.t("Appearance", "Izgled"),
                options = ThemeMode.entries,
                selected = selectedTheme,
                label = {
                    when (it) {
                        ThemeMode.SYSTEM -> language.t("System", "Sistem")
                        ThemeMode.LIGHT -> language.t("Light", "Svetlo")
                        ThemeMode.DARK -> language.t("Dark", "Tamno")
                    }
                },
                onSelect = onThemeChange,
            )
            ChoiceGroup(
                title = language.t("Language", "Jezik"),
                options = listOf(AppLanguage.SYSTEM, AppLanguage.ENGLISH, AppLanguage.SERBIAN_LATIN),
                selected = selectedLanguage,
                label = {
                    when (it) {
                        AppLanguage.SYSTEM -> language.t("System", "Sistem")
                        AppLanguage.ENGLISH -> "English"
                        AppLanguage.SERBIAN_LATIN -> "Srpski (latinica)"
                    }
                },
                onSelect = onLanguageChange,
            )
        }
    }
}

@Composable
private fun ProductPage(
    eyebrow: String,
    title: String,
    subtitle: String,
    content: @Composable Column.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.90f),
        )
        Spacer(Modifier.height(26.dp))
        content()
    }
}

@Composable
private fun FeatureOrb(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GlassSurface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            tone = GlassTone.Neutral,
            blur = GlassBlur.Soft,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DatePreviewCell(day: String, date: String, selected: Boolean) {
    val modifier = Modifier.size(width = 48.dp, height = 54.dp)
    if (selected) {
        GlassSurface(
            modifier = modifier,
            shape = PillShape,
            tone = GlassTone.Accent,
            blur = GlassBlur.Soft,
        ) {
            DatePreviewContent(day, date)
        }
    } else {
        DatePreviewContent(day, date, modifier)
    }
}

@Composable
private fun DatePreviewContent(day: String, date: String, modifier: Modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(date, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PreviewScheduleRow(time: String, title: String, metadata: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time, modifier = Modifier.width(52.dp), style = MaterialTheme.typography.labelMedium)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(metadata, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelectionPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassSurface(
        modifier = modifier.height(44.dp).clickable(onClick = onClick),
        shape = PillShape,
        tone = if (selected) GlassTone.Accent else GlassTone.Neutral,
        blur = GlassBlur.Soft,
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun <T> ChoiceGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = ContinuousRoundedShape(20.dp),
            tone = GlassTone.Neutral,
            blur = GlassBlur.Soft,
        ) {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onSelect(option) }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label(option), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        if (option == selected) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (index < options.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp)
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
                        )
                    }
                }
            }
        }
    }
}
