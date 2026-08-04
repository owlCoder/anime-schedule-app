package com.owlcoder.animeschedule.presentation.screens.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSwitch
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.GlassChrome
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                    3 -> MalPreviewPage(selectedLanguage) { onLogin(context) }
                    else -> PersonalizePage(
                        language = selectedLanguage,
                        selectedTheme = selectedTheme,
                        selectedLanguage = selectedLanguage,
                        onThemeChange = onThemeChange,
                        onLanguageChange = onLanguageChange,
                    )
                }
            }

            GlassChrome(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                shape = ContinuousRoundedShape(28.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    PageIndicator(currentPage, OnboardingPageCount)
                    AppButton(
                        label = if (currentPage == OnboardingPageCount - 1) {
                            selectedLanguage.t("Start using AnimeSchedule", "Počni da koristiš AnimeSchedule")
                        } else selectedLanguage.t("Continue", "Nastavi"),
                        onClick = {
                            if (currentPage == OnboardingPageCount - 1) onComplete()
                            else scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = AppButtonVariant.Primary,
                    )
                    TextButton(
                        onClick = {
                            if (currentPage > 0) scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                            else onComplete()
                        },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                    ) {
                        Text(
                            text = if (currentPage > 0) selectedLanguage.t("Back", "Nazad")
                            else selectedLanguage.t("Set up later", "Podesi kasnije"),
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
                    colors = listOf(light.copy(alpha = 0.065f), light.copy(alpha = 0.018f), Color.Transparent),
                    radius = 980f,
                ),
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.36f), MaterialTheme.colorScheme.background),
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
                    .width(if (index == currentPage) 18.dp else 6.dp)
                    .height(6.dp)
                    .clip(PillShape)
                    .background(if (index == currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)),
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
        Surface(
            modifier = Modifier.size(128.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CalendarMonth, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
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
        AppMaterialSurface(
            modifier = Modifier.fillMaxWidth(),
            material = AppMaterial.Elevated,
            shape = ContinuousRoundedShape(22.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
            AppMaterialSurface(
                modifier = Modifier.fillMaxWidth(),
                material = AppMaterial.Elevated,
                shape = ContinuousRoundedShape(20.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsNone, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(language.t("Episode reminders", "Podsetnici za epizode"), style = MaterialTheme.typography.titleMedium)
                        Text(language.t("Only for titles you follow", "Samo za naslove koje pratiš"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AppSwitch(checked = enabled, onCheckedChange = onEnabledChange)
                }
            }
            if (enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(-15, 0, 15).forEach { minutes ->
                        SelectionRow(
                            label = when (minutes) {
                                -15 -> language.t("15 min before", "15 min ranije")
                                0 -> language.t("At air time", "U vreme emitovanja")
                                else -> language.t("15 min after", "15 min kasnije")
                            },
                            selected = offsetMinutes == minutes,
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
        AppMaterialSurface(
            modifier = Modifier.fillMaxWidth(),
            material = AppMaterial.Elevated,
            shape = ContinuousRoundedShape(22.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountCircle, null, Modifier.size(27.dp))
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 11.dp)) {
                        Text("MyAnimeList", style = MaterialTheme.typography.titleMedium)
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
            "Liquid Glass stays neutral. Choose appearance and language.",
            "Liquid Glass ostaje neutralan. Izaberi izgled i jezik.",
        ),
    ) {
        AppMaterialSurface(
            modifier = Modifier.fillMaxWidth(),
            material = AppMaterial.Elevated,
            shape = ContinuousRoundedShape(22.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsChoiceTitle(language.t("Appearance", "Izgled"))
                ThemeMode.entries.forEachIndexed { index, mode ->
                    ChoiceRow(
                        label = when (mode) {
                            ThemeMode.SYSTEM -> language.t("System", "Sistemski")
                            ThemeMode.LIGHT -> language.t("Light", "Svetli")
                            ThemeMode.DARK -> language.t("Dark", "Tamni")
                        },
                        selected = selectedTheme == mode,
                        onClick = { onThemeChange(mode) },
                    )
                    if (index < ThemeMode.entries.lastIndex) ChoiceDivider()
                }
                SettingsChoiceTitle(language.t("Language", "Jezik"))
                listOf(
                    AppLanguage.SYSTEM to language.t("System", "Sistemski"),
                    AppLanguage.ENGLISH to "English",
                    AppLanguage.SERBIAN_LATIN to "Srpski",
                ).forEachIndexed { index, (option, label) ->
                    ChoiceRow(label, selectedLanguage == option) { onLanguageChange(option) }
                    if (index < 2) ChoiceDivider()
                }
            }
        }
    }
}

@Composable
private fun ProductPage(eyebrow: String, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.45f))
        Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(title, Modifier.padding(top = 7.dp), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(subtitle, Modifier.padding(top = 8.dp, bottom = 24.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        Spacer(Modifier.weight(0.55f))
    }
}

@Composable
private fun FeatureOrb(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp))
            }
        }
        Text(label, Modifier.padding(top = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DatePreviewCell(day: String, date: String, selected: Boolean) {
    Surface(
        modifier = Modifier.size(width = 48.dp, height = 52.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(date, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PreviewScheduleRow(time: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(time, Modifier.width(48.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelectionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(44.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.11f) else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (selected) Icon(Icons.Default.Check, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SettingsChoiceTitle(text: String) {
    Text(
        text.uppercase(),
        Modifier.fillMaxWidth().padding(start = 13.dp, top = 11.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp).clickable(onClick = onClick).padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (selected) Icon(Icons.Default.Check, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ChoiceDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 13.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f),
    )
}
