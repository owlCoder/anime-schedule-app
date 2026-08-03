package com.owlcoder.animeschedule.presentation.screens.onboarding

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.data.local.datastore.AccentColor
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.data.local.datastore.ThemeMode
import com.owlcoder.animeschedule.presentation.components.AppButton
import com.owlcoder.animeschedule.presentation.components.AppButtonVariant
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSwitch
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.GlassSurface
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone
import com.owlcoder.animeschedule.ui.theme.PillShape
import com.owlcoder.animeschedule.ui.theme.accentPrimary
import kotlinx.coroutines.launch

private const val OnboardingPageCount = 5

private fun AppLanguage.t(en: String, sr: String): String =
    if (this == AppLanguage.SERBIAN_LATIN) sr else en

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
        OnboardingBackdrop()

        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .graphicsLayer {
                            val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val distance = kotlin.math.abs(offset).coerceIn(0f, 1f)
                            alpha = 1f - distance * 0.22f
                            val scale = 1f - distance * 0.035f
                            scaleX = scale
                            scaleY = scale
                            translationX = offset * size.width * 0.05f
                        },
                ) {
                    when (page) {
                        0 -> WelcomePage(selectedLanguage)
                        1 -> SchedulePage(selectedLanguage)
                        2 -> NotificationsPage(
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
                        3 -> MalSyncPage(
                            language = selectedLanguage,
                            onLogin = { onLogin(context) },
                        )
                        4 -> PersonalizePage(
                            language = selectedLanguage,
                            selectedTheme = selectedTheme,
                            selectedAccent = selectedAccent,
                            selectedLanguage = selectedLanguage,
                            onThemeChange = onThemeChange,
                            onAccentChange = onAccentChange,
                            onLanguageChange = onLanguageChange,
                        )
                    }
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    PageIndicator(currentPage = currentPage, pageCount = OnboardingPageCount)
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
                        modifier = Modifier.height(36.dp),
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
private fun OnboardingBackdrop() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.045f),
                        Color.Transparent,
                    ),
                    radius = 1100f,
                ),
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    )
}

@Composable
private fun PageIndicator(currentPage: Int, pageCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 24.dp else 6.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "onboardingIndicatorWidth",
            )
            val color by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
                label = "onboardingIndicatorColor",
            )
            Box(
                modifier = Modifier.height(6.dp).width(width).clip(PillShape).background(color),
            )
        }
    }
}

@Composable
private fun OnboardingPageLayout(
    eyebrow: String,
    title: String,
    subtitle: String,
    visual: @Composable () -> Unit,
    content: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 18.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            visual()
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.15.sp,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content?.invoke()
        }
    }
}

@Composable
private fun WelcomePage(language: AppLanguage) {
    OnboardingPageLayout(
        eyebrow = "AnimeSchedule",
        title = language.t("Your anime week, beautifully organized.", "Tvoja anime nedelja, lepo organizovana."),
        subtitle = language.t(
            "See what airs next, discover seasonal shows and keep every episode within reach.",
            "Vidi šta sledeće izlazi, otkrij sezonske naslove i drži svaku epizodu nadohvat ruke.",
        ),
        visual = { WelcomeVisual() },
    )
}

@Composable
private fun WelcomeVisual() {
    Box(modifier = Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(248.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        GlassSurface(
            modifier = Modifier.size(146.dp),
            shape = ContinuousRoundedShape(42.dp),
            tone = GlassTone.Accent,
            blur = GlassBlur.Medium,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.about_logo_monogram),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        FloatingFeatureChip(
            icon = Icons.Default.Search,
            label = "Discover",
            modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 42.dp),
        )
        FloatingFeatureChip(
            icon = Icons.Default.Notifications,
            label = "Reminders",
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 36.dp),
        )
        FloatingFeatureChip(
            icon = Icons.Default.Bookmark,
            label = "My List",
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 64.dp),
        )
    }
}

@Composable
private fun FloatingFeatureChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier = modifier,
        shape = PillShape,
        tone = GlassTone.Neutral,
        blur = GlassBlur.Soft,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun SchedulePage(language: AppLanguage) {
    OnboardingPageLayout(
        eyebrow = language.t("At a glance", "Na prvi pogled"),
        title = language.t("Know exactly what airs next.", "Uvek znaš šta sledeće izlazi."),
        subtitle = language.t(
            "A focused Today view adapts from upcoming episodes to the latest broadcasts from your day.",
            "Fokusirani Today ekran prelazi sa narednih epizoda na poslednja današnja emitovanja.",
        ),
        visual = { SchedulePreview() },
    )
}

@Composable
private fun SchedulePreview() {
    GlassSurface(
        modifier = Modifier.fillMaxWidth().height(322.dp).padding(horizontal = 8.dp),
        shape = ContinuousRoundedShape(30.dp),
        tone = GlassTone.Neutral,
        blur = GlassBlur.Medium,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Monday, 3 August", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                GlassSurface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    tone = GlassTone.Accent,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            AppMaterialSurface(
                modifier = Modifier.fillMaxWidth(),
                material = AppMaterial.Grouped,
                shape = MaterialTheme.shapes.large,
            ) {
                Column {
                    PreviewScheduleRow("19:30", "New episode tonight", "Ep. 8/12")
                    PreviewDivider()
                    PreviewScheduleRow("21:00", "Season finale", "Ep. 12/12")
                    PreviewDivider()
                    PreviewScheduleRow("22:15", "Late broadcast", "Ep. 4")
                }
            }
        }
    }
}

@Composable
private fun PreviewScheduleRow(time: String, title: String, episode: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(time, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.70f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        ),
                    ),
                ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(episode, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PreviewDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 112.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
    )
}

@Composable
private fun NotificationsPage(
    language: AppLanguage,
    enabled: Boolean,
    offsetMinutes: Int,
    onEnabledChange: (Boolean) -> Unit,
    onOffsetChange: (Int) -> Unit,
) {
    OnboardingPageLayout(
        eyebrow = language.t("Stay on time", "Budi u toku"),
        title = language.t("Gentle reminders, right when you need them.", "Nenametljivi podsetnici baš kada ti trebaju."),
        subtitle = language.t(
            "Choose when AnimeSchedule should remind you. You can change this at any time.",
            "Izaberi kada AnimeSchedule treba da te podseti. Ovo možeš promeniti u bilo kom trenutku.",
        ),
        visual = { NotificationPreview() },
        content = {
            AppMaterialSurface(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                material = AppMaterial.Grouped,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(language.t("Episode reminders", "Podsetnici za epizode"), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (enabled) offsetLabel(offsetMinutes, language) else language.t("Off", "Isključeno"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AppSwitch(checked = enabled, onCheckedChange = onEnabledChange)
                    }
                    if (enabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(-15, -5, 0, 15).forEach { offset ->
                                val selected = offset == offsetMinutes
                                GlassSurface(
                                    modifier = Modifier.clickable { onOffsetChange(offset) },
                                    shape = PillShape,
                                    tone = if (selected) GlassTone.Accent else GlassTone.Neutral,
                                ) {
                                    Text(
                                        text = offsetLabel(offset, language),
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun NotificationPreview() {
    Box(modifier = Modifier.fillMaxWidth().height(292.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width = 250.dp, height = 260.dp)
                .clip(ContinuousRoundedShape(42.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.62f)),
        )
        Column(
            modifier = Modifier.width(286.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PreviewNotification("Keep your streak going", "A new episode airs in 15 minutes", "19:15")
            PreviewNotification("Now airing", "Your saved anime just started", "21:00")
            PreviewNotification("Tomorrow", "Three shows are on your schedule", "09:41")
        }
    }
}

@Composable
private fun PreviewNotification(title: String, subtitle: String, time: String) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = ContinuousRoundedShape(18.dp),
        tone = GlassTone.Neutral,
        blur = GlassBlur.Soft,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MalSyncPage(language: AppLanguage, onLogin: () -> Unit) {
    OnboardingPageLayout(
        eyebrow = "MyAnimeList",
        title = language.t("Your list stays with you.", "Tvoja lista ostaje uz tebe."),
        subtitle = language.t(
            "Sign in to update status, episodes and scores without leaving AnimeSchedule.",
            "Prijavi se da ažuriraš status, epizode i ocene bez napuštanja AnimeSchedule aplikacije.",
        ),
        visual = { MalPreview() },
        content = {
            AppButton(
                label = language.t("Sign in with MyAnimeList", "Prijavi se putem MyAnimeList"),
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                icon = Icons.Default.Login,
                variant = AppButtonVariant.Secondary,
            )
        },
    )
}

@Composable
private fun MalPreview() {
    GlassSurface(
        modifier = Modifier.fillMaxWidth().height(310.dp).padding(horizontal = 10.dp),
        shape = ContinuousRoundedShape(30.dp),
        tone = GlassTone.Neutral,
        blur = GlassBlur.Medium,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(58.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text("MyAnimeList", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Connected list preview", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AppMaterialSurface(
                modifier = Modifier.fillMaxWidth(),
                material = AppMaterial.Grouped,
                shape = MaterialTheme.shapes.large,
            ) {
                Column {
                    PreviewListRow(Icons.Default.Bookmark, "Plan to watch", "24")
                    PreviewDivider()
                    PreviewListRow(Icons.Default.Check, "Completed", "108")
                    PreviewDivider()
                    PreviewListRow(Icons.Default.CalendarMonth, "Watching", "7")
                }
            }
        }
    }
}

@Composable
private fun PreviewListRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalizePage(
    language: AppLanguage,
    selectedTheme: ThemeMode,
    selectedAccent: AccentColor,
    selectedLanguage: AppLanguage,
    onThemeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    OnboardingPageLayout(
        eyebrow = language.t("Make it yours", "Prilagodi sebi"),
        title = language.t("A finish that feels personal.", "Završni izgled koji je tvoj."),
        subtitle = language.t(
            "Choose the appearance you prefer. Every option remains available in Settings.",
            "Izaberi izgled koji ti odgovara. Sve opcije ostaju dostupne u podešavanjima.",
        ),
        visual = {
            GlassSurface(
                modifier = Modifier.fillMaxWidth().heightIn(min = 330.dp).padding(horizontal = 6.dp),
                shape = ContinuousRoundedShape(30.dp),
                tone = GlassTone.Neutral,
                blur = GlassBlur.Medium,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PreferenceTitle(language.t("Theme", "Tema"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { theme ->
                            PreferencePill(
                                label = theme.name.lowercase().replaceFirstChar { it.uppercase() },
                                selected = theme == selectedTheme,
                                onClick = { onThemeChange(theme) },
                            )
                        }
                    }

                    PreferenceTitle(language.t("Accent", "Akcentna boja"))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AccentColor.entries.forEach { accent ->
                            val color = accentPrimary(accent, dark)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { onAccentChange(accent) }
                                    .semantics { role = Role.RadioButton },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (accent == selectedAccent) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                }
                            }
                        }
                    }

                    PreferenceTitle(language.t("Language", "Jezik"))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AppLanguage.SYSTEM to language.t("System", "Sistemski"),
                            AppLanguage.ENGLISH to "English",
                            AppLanguage.SERBIAN_LATIN to "Srpski (latinica)",
                        ).forEach { (option, label) ->
                            PreferencePill(
                                label = label,
                                selected = option == selectedLanguage,
                                onClick = { onLanguageChange(option) },
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun PreferenceTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 3.dp),
    )
}

@Composable
private fun PreferencePill(label: String, selected: Boolean, onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier
            .height(38.dp)
            .clickable(onClick = onClick)
            .semantics { role = Role.RadioButton },
        shape = PillShape,
        tone = if (selected) GlassTone.Accent else GlassTone.Neutral,
        blur = if (selected) GlassBlur.Soft else GlassBlur.None,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private fun offsetLabel(minutes: Int, language: AppLanguage): String = when {
    minutes < 0 -> language.t("${-minutes} min before", "${-minutes} min ranije")
    minutes == 0 -> language.t("At air time", "U vreme emitovanja")
    minutes < 60 -> language.t("$minutes min after", "$minutes min kasnije")
    else -> language.t("${minutes / 60} h after", "${minutes / 60} h kasnije")
}
