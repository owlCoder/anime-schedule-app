package com.owlcoder.animeschedule.presentation.screens.onboarding

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.data.local.datastore.AccentColor
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.data.local.datastore.ThemeMode
import com.owlcoder.animeschedule.presentation.components.AppPrimaryButton
import com.owlcoder.animeschedule.presentation.components.GlassButton
import com.owlcoder.animeschedule.presentation.components.GlassSurface
import com.owlcoder.animeschedule.presentation.screens.settings.notifOffsetLabel
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone
import com.owlcoder.animeschedule.ui.theme.PillShape
import kotlinx.coroutines.launch

// Returns EN or SR text based on selected language (used during onboarding before locale is applied).
private fun AppLanguage.t(en: String, sr: String): String = if (this == AppLanguage.ENGLISH) en else sr

private val onboardingPageCount = 5

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
    // Keep the settings parameters in the public contract; onboarding intentionally does not
    // duplicate settings that are changed elsewhere in the app.
    @Suppress("UNUSED_VARIABLE")
    val retainedTheme = selectedTheme
    @Suppress("UNUSED_VARIABLE")
    val retainedAccent = selectedAccent

    val pagerState = rememberPagerState(pageCount = { onboardingPageCount })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val language = selectedLanguage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val distance = kotlin.math.abs(offset).coerceIn(0f, 1f)
                        alpha = 1f - distance * 0.18f
                        val scale = 1f - distance * 0.025f
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                when (page) {
                    0 -> LanguagePickerPage(
                        selectedLanguage = selectedLanguage,
                        onLanguageChange = onLanguageChange
                    )
                    1 -> WelcomePage(lang = language)
                    2 -> FeaturesPage(lang = language)
                    3 -> NotificationsPage(
                        onSettingsChange = onNotifSettingsChange,
                        lang = language
                    )
                    4 -> MalLoginPage(
                        onLogin = onLogin,
                        onComplete = onComplete,
                        lang = language
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PageIndicator(currentPage = currentPage, pageCount = onboardingPageCount)

            Spacer(Modifier.height(14.dp))

            GlassButton(
                onClick = {
                    if (currentPage == onboardingPageCount - 1) {
                        onComplete()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) { contentColor ->
                Text(
                    text = if (currentPage == onboardingPageCount - 1) {
                        language.t("Start using Anime Schedule", "Počni sa Anime Schedule")
                    } else {
                        language.t("Continue", "Nastavi")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
            }

            if (currentPage > 0) {
                TextButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(currentPage - 1) } },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = language.t("Back", "Nazad"),
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
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 20.dp else 6.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "onboardingIndicatorWidth"
            )
            val color by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                label = "onboardingIndicatorColor"
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(PillShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun OnboardingPage(
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        if (eyebrow != null) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp
            )
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Composable
private fun WelcomeLogo() {
    GlassSurface(
        modifier = Modifier.size(96.dp),
        shape = CircleShape,
        tone = GlassTone.Accent,
        blur = GlassBlur.Soft,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.about_logo_monogram),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun WelcomePage(lang: AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WelcomeLogo()
        Spacer(Modifier.height(28.dp))
        Text(
            text = lang.t("Welcome to Anime Schedule", "Dobrodošao u Anime Schedule"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = lang.t(
                "A calm place to see what airs next and keep your list in sync.",
                "Mirno mesto za pregled onoga što sledeće izlazi i čuvanje tvoje liste."
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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

    OnboardingPage(
        eyebrow = "Anime Schedule",
        title = "Jezik / Language",
        subtitle = "Uvek možeš promeniti u podešavanjima"
    ) {
        InsetGroup {
            options.forEachIndexed { index, (language, label) ->
                val selected = selectedLanguage == language
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(
                            role = Role.RadioButton,
                            onClick = { onLanguageChange(language) }
                        )
                        .semantics { role = Role.RadioButton }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (index < options.lastIndex) InsetDivider()
            }
        }
    }
}

@Composable
private fun FeaturesPage(lang: AppLanguage) {
    data class Feature(val icon: ImageVector, val title: String, val subtitle: String)

    val features = listOf(
        Feature(Icons.Default.CalendarMonth, lang.t("Airing schedule", "Raspored emitovanja"), lang.t("See what airs next in your time zone", "Vidi šta sledeće izlazi u tvojoj vremenskoj zoni")),
        Feature(Icons.Default.FormatListBulleted, lang.t("My MAL list", "Moja MAL lista"), lang.t("Browse and update your list", "Pregledaj i ažuriraj svoju listu")),
        Feature(Icons.Default.Notifications, lang.t("Notifications", "Obaveštenja"), lang.t("Know when a new episode airs", "Saznaj kada izađe nova epizoda")),
        Feature(Icons.Default.Search, lang.t("Search anime", "Pretraga animea"), lang.t("Find a show in a few taps", "Pronađi seriju u nekoliko dodira"))
    )

    OnboardingPage(
        eyebrow = lang.t("A quick tour", "Kratak pregled"),
        title = lang.t("Everything in one place", "Sve na jednom mestu"),
        subtitle = lang.t("A focused dashboard keeps the important part close.", "Fokusiran početni ekran drži ono važno nadohvat ruke.")
    ) {
        InsetGroup {
            features.forEachIndexed { index, feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeatureIconBadge(icon = feature.icon)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = feature.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = feature.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (index < features.lastIndex) InsetDivider()
            }
        }
    }
}

@Composable
private fun FeatureIconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(21.dp)
        )
    }
}

private val notifOffsetOptions = listOf(-30, -15, -5, 0, 5, 15, 30, 60)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationsPage(
    onSettingsChange: (enabled: Boolean, offsetMinutes: Int) -> Unit,
    lang: AppLanguage
) {
    var enabled by remember { mutableStateOf(true) }
    var selectedOffset by remember { mutableIntStateOf(0) }

    OnboardingPage(
        eyebrow = lang.t("Stay on time", "Budi u toku"),
        title = lang.t("Notifications", "Obaveštenja"),
        subtitle = lang.t("Choose how you want to hear about new episodes.", "Izaberi kako želiš da saznaš za nove epizode.")
    ) {
        InsetGroup {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lang.t("Episode alerts", "Obaveštenja za epizode"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = lang.t("When a new episode airs", "Kada izađe nova epizoda"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        onSettingsChange(it, selectedOffset)
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = lang.t("WHEN TO NOTIFY", "KADA DA STIGNE"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            notifOffsetOptions.forEach { minutes ->
                val selected = minutes == selectedOffset
                val background by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    label = "notificationOffsetBackground"
                )
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(PillShape)
                        .background(background)
                        .clickable(
                            role = Role.RadioButton,
                            onClick = {
                                selectedOffset = minutes
                                onSettingsChange(enabled, minutes)
                            }
                        )
                        .semantics { role = Role.RadioButton }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notifOffsetLabel(minutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun MalLoginPage(
    onLogin: (Context) -> Unit,
    onComplete: () -> Unit,
    lang: AppLanguage
) {
    val context = LocalContext.current

    OnboardingPage(
        modifier = Modifier,
        eyebrow = "MyAnimeList",
        title = lang.t("Connect your list", "Poveži svoju listu"),
        subtitle = lang.t(
            "Optional. You can sign in now or continue and connect it later in Settings.",
            "Opciono. Možeš se prijaviti sada ili nastaviti i povezati je kasnije u Podešavanjima."
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            AppPrimaryButton(
                label = lang.t("Sign in to MAL", "Prijavi se na MAL"),
                onClick = {
                    onLogin(context)
                    onComplete()
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Login
            )
        }
    }
}

@Composable
private fun InsetGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        content = content
    )
}

@Composable
private fun InsetDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    )
}
