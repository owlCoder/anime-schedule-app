package rs.owlcoder.animeschedule.presentation.screens.onboarding

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import rs.owlcoder.animeschedule.R
import rs.owlcoder.animeschedule.data.local.datastore.AccentColor
import rs.owlcoder.animeschedule.data.local.datastore.AppLanguage
import rs.owlcoder.animeschedule.data.local.datastore.ThemeMode
import rs.owlcoder.animeschedule.presentation.screens.settings.notifOffsetLabel
import rs.owlcoder.animeschedule.ui.theme.accentPrimary

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
    val pagerState = rememberPagerState(pageCount = { 6 })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val isLastPage = currentPage == 5

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> LanguagePickerPage(
                    selectedLanguage = selectedLanguage,
                    onLanguageChange = onLanguageChange
                )
                1 -> WelcomePage(lang = selectedLanguage)
                2 -> ThemePickerPage(
                    selectedTheme = selectedTheme,
                    selectedAccent = selectedAccent,
                    onThemeChange = onThemeChange,
                    onAccentChange = onAccentChange,
                    lang = selectedLanguage
                )
                3 -> FeaturesPage(lang = selectedLanguage)
                4 -> NotificationsPage(onSettingsChange = onNotifSettingsChange, lang = selectedLanguage)
                5 -> MalLoginPage(
                    onLogin = onLogin,
                    onComplete = onComplete,
                    lang = selectedLanguage
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PageIndicator(currentPage = currentPage, pageCount = 6)

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isLastPage) selectedLanguage.t("Start", "Počni") else selectedLanguage.t("Next", "Dalje"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
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
                animationSpec = spring(),
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
                    .clip(CircleShape)
                    .background(color)
            )
        }
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
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.ic_app_icon),
                contentDescription = "App icon",
                modifier = Modifier.size(72.dp)
            )
        }

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
                .clip(RoundedCornerShape(12.dp))
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
                        .padding(horizontal = 16.dp, vertical = 14.dp),
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

// ── Page 2: Theme Picker ─────────────────────────────────────────────────────

@Composable
private fun ThemePickerPage(
    selectedTheme: ThemeMode,
    selectedAccent: AccentColor,
    onThemeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    lang: AppLanguage
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = lang.t("Choose a theme", "Izaberi temu"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = lang.t("You can always change this in Settings", "Uvek možeš promeniti u podešavanjima"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        val themeOptions = listOf(
            Triple(ThemeMode.SYSTEM, lang.t("System", "Sistemska"), Icons.Default.AutoMode),
            Triple(ThemeMode.LIGHT,  lang.t("Light", "Svetla"),     Icons.Default.LightMode),
            Triple(ThemeMode.DARK,   lang.t("Dark", "Tamna"),       Icons.Default.DarkMode)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            themeOptions.forEach { (mode, label, icon) ->
                val selected = selectedTheme == mode
                val borderColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.outlineVariant,
                    label = "themeBorder"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "themeIcon"
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
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = lang.t("Accent color", "Boja akcenta"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        val accentOptions = listOf(
            AccentColor.TELEGRAM_BLUE,
            AccentColor.PURPLE,
            AccentColor.GREEN,
            AccentColor.ORANGE,
            AccentColor.PINK,
            AccentColor.RED
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            accentOptions.forEach { accent ->
                val selected = selectedAccent == accent
                val color = accentPrimary(accent, dark = isDark)
                val size by animateDpAsState(
                    targetValue = if (selected) 44.dp else 36.dp,
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
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

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

// ── Page 3: Notifications ────────────────────────────────────────────────────

private val notifOffsetOptions = listOf(-30, -15, -5, 0, 5, 15, 30, 60)

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
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

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

        Spacer(Modifier.height(24.dp))

        // Enable toggle row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 4.dp),
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            notifOffsetOptions.forEachIndexed { index, minutes ->
                val isSelected = minutes == selectedOffset
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                  else MaterialTheme.colorScheme.surface,
                    label = "offsetBg"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .clickable {
                            selectedOffset = minutes
                            onSettingsChange(enabled, minutes)
                        }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
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
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (index < notifOffsetOptions.lastIndex) {
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
                .clip(RoundedCornerShape(20.dp))
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

        Button(
            onClick = {
                onLogin(context)
                onComplete()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Login,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = lang.t("Sign in to MAL", "Prijavi se na MAL"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp)
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
