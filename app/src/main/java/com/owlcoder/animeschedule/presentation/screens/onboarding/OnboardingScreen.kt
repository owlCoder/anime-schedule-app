package com.owlcoder.animeschedule.presentation.screens.onboarding

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.data.local.datastore.AccentColor
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.data.local.datastore.ThemeMode
import com.owlcoder.animeschedule.presentation.components.GlassButton
import com.owlcoder.animeschedule.presentation.screens.settings.notifOffsetLabel
import com.owlcoder.animeschedule.ui.theme.PillShape

// Returns EN or SR text based on selected language (used during onboarding before locale is applied)
private fun AppLanguage.t(en: String, sr: String): String = if (this == AppLanguage.ENGLISH) en else sr

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
    onNotifSettingsChange: (enabled: Boolean, offsetMinutes: Int) -> Unit = { _, _ -> }
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val isLastPage = currentPage == 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Parallax: pages fade + shrink slightly as they slide out of focus
                        // during a swipe, instead of a flat cut — makes the pager feel alive.
                        val pageOffset =
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val distance = pageOffset.let { if (it < 0f) -it else it }.coerceIn(0f, 1f)
                        alpha = 1f - distance * 0.7f
                        val scale = 1f - distance * 0.1f
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                when (page) {
                    0 -> LanguagePickerPage(
                        selectedLanguage = selectedLanguage,
                        onLanguageChange = onLanguageChange
                    )
                    1 -> WelcomePage(lang = selectedLanguage)
                    2 -> FeaturesPage(lang = selectedLanguage)
                    3 -> NotificationsPage(onSettingsChange = onNotifSettingsChange, lang = selectedLanguage)
                    4 -> MalLoginPage(
                        onLogin = onLogin,
                        onComplete = onComplete,
                        lang = selectedLanguage
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PageIndicator(currentPage = currentPage, pageCount = 5)

            Spacer(Modifier.height(20.dp))

            GlassButton(
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { contentColor ->
                Text(
                    text = if (isLastPage) selectedLanguage.t("Start", "Počni") else selectedLanguage.t("Next", "Dalje"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }

            Spacer(Modifier.height(4.dp))

            if (currentPage > 0) {
                TextButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(currentPage - 1) } }
                ) {
                    Text(
                        text = selectedLanguage.t("Back", "Nazad"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun PageIndicator(currentPage: Int, pageCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "indicatorWidth"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.surfaceVariant,
                label = "indicatorColor"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(PillShape)
                    .background(color)
            )
        }
    }
}

// Minimal "AS" wordmark (same lockup as the About screen) with a slow breathing
// scale and a pulsing halo ring behind it, so the welcome page reads as alive
// rather than a static logo tile.
@Composable
private fun WelcomeLogo() {
    val transition = rememberInfiniteTransition(label = "welcomeLogo")
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(1f + pulse * 0.55f)
                .border(
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f * (1f - pulse))),
                    CircleShape
                )
        )
        Text(
            text = stringResource(R.string.about_logo_monogram),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.scale(breathe)
        )
    }
}

// ── Page 0: Welcome ──────────────────────────────────────────────────────────

@Composable
private fun WelcomePage(lang: AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WelcomeLogo()

        Spacer(Modifier.height(32.dp))

        Text(
            text = lang.t("Welcome to\nAnime Schedule", "Dobrodošao u\nAnime Schedule"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = lang.t(
                "Track anime airing schedules in real time and manage your MyAnimeList without ads.",
                "Prati raspored emitovanja anime serija u realnom vremenu i upravljaj MyAnimeList listom bez reklama."
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Page 1: Language Picker ──────────────────────────────────────────────────

@Composable
private fun LanguagePickerPage(
    selectedLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val options = listOf(
        AppLanguage.SYSTEM to "Sistemski / System",
        AppLanguage.ENGLISH to "English",
        AppLanguage.SERBIAN_LATIN to "Srpski (latinica)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Jezik / Language",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Uvek možeš promeniti u podešavanjima",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEachIndexed { index, (language, label) ->
                val isSelected = selectedLanguage == language
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                  else MaterialTheme.colorScheme.surface,
                    label = "langBg"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .clickable { onLanguageChange(language) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
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
                if (index < options.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }
    }
}

// ── Page 2: Features ─────────────────────────────────────────────────────────

@Composable
private fun FeaturesPage(lang: AppLanguage) {
    data class Feature(
        val icon: ImageVector,
        val title: String,
        val subtitle: String
    )

    val features = listOf(
        Feature(
            icon = Icons.Default.CalendarMonth,
            title = lang.t("Airing schedule", "Raspored emitovanja"),
            subtitle = lang.t("Today, tomorrow and this week in your time zone", "Danas, sutra i ova nedelja u tvojoj vremenskoj zoni")
        ),
        Feature(
            icon = Icons.Default.FormatListBulleted,
            title = lang.t("My MAL list", "Moja MAL lista"),
            subtitle = lang.t("Browse and update your MyAnimeList list", "Pregledaj i ažuriraj svoju MyAnimeList listu")
        ),
        Feature(
            icon = Icons.Default.Notifications,
            title = lang.t("Notifications", "Obaveštenja"),
            subtitle = lang.t("Automatic notifications when a new episode airs", "Automatska obaveštenja kada nova epizoda izađe")
        ),
        Feature(
            icon = Icons.Default.Search,
            title = lang.t("Search anime", "Pretraga anime"),
            subtitle = lang.t("Search any anime and add it to your list", "Pretraži bilo koji anime i dodaj ga na listu")
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = lang.t("What you can do", "Šta sve možeš"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(28.dp))

        features.forEachIndexed { index, feature ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                FeatureIconBadge(icon = feature.icon, phaseOffset = index * 0.25f)

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = feature.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (index < features.lastIndex) {
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// Feature icon badge with a slow, gently phase-offset bob — each row floats independently
// so the list doesn't look perfectly static while still reading as calm, not busy.
@Composable
private fun FeatureIconBadge(icon: ImageVector, phaseOffset: Float) {
    val transition = rememberInfiniteTransition(label = "featureBob")
    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = androidx.compose.animation.core.StartOffset(
                (2200 * phaseOffset).toInt()
            )
        ),
        label = "bob"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer { translationY = bob * 4.dp.toPx() }
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ── Page 3: Notifications ────────────────────────────────────────────────────

private val notifOffsetOptions = listOf(-30, -15, -5, 0, 5, 15, 30, 60)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationsPage(
    onSettingsChange: (enabled: Boolean, offsetMinutes: Int) -> Unit,
    lang: AppLanguage
) {
    var enabled by remember { mutableStateOf(true) }
    var selectedOffset by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            lang.t("Notifications", "Obaveštenja"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(4.dp))

        Text(
            lang.t("Notify me when a new episode airs", "Obavesti me kada nova epizoda izađe"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // Enable toggle row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                lang.t("Enable notifications", "Uključi obaveštenja"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    onSettingsChange(it, selectedOffset)
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            lang.t("WHEN TO NOTIFY", "KADA DA STIGNE"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            notifOffsetOptions.forEach { minutes ->
                val isSelected = minutes == selectedOffset
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                  else MaterialTheme.colorScheme.surfaceVariant,
                    label = "offsetChipBg"
                )
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(bgColor)
                        .clickable {
                            selectedOffset = minutes
                            onSettingsChange(enabled, minutes)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        notifOffsetLabel(minutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Page 4: MAL Login ─────────────────────────────────────────────────────────

@Composable
private fun MalLoginPage(
    onLogin: (Context) -> Unit,
    onComplete: () -> Unit,
    lang: AppLanguage
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = lang.t("Sign in to MyAnimeList", "Prijavi se na MyAnimeList"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = lang.t("Optional — you can sign in later in Settings", "Opciono — možeš se prijaviti i kasnije u podešavanjima"),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(36.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(36.dp))

        GlassButton(
            onClick = {
                onLogin(context)
                onComplete()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) { contentColor ->
            Icon(
                imageVector = Icons.Default.Login,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Text(
                text = lang.t("Sign in to MAL", "Prijavi se na MAL"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onComplete) {
            Text(
                text = lang.t("Skip, sign in later", "Preskoči, prijavi se kasnije"),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
