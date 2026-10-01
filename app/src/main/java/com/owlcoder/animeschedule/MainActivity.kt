package com.owlcoder.animeschedule

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.owlcoder.animeschedule.core.locale.LocaleHelper
import com.owlcoder.animeschedule.domain.model.AppLanguage
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.presentation.components.AppSystemBarAppearance
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.LocalChromeHazeState
import com.owlcoder.animeschedule.presentation.components.LocalNavBarHeight
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.ToastController
import com.owlcoder.animeschedule.presentation.components.ToastHost
import com.owlcoder.animeschedule.presentation.navigation.AnimeBottomBar
import com.owlcoder.animeschedule.presentation.navigation.AnimeNavHost
import com.owlcoder.animeschedule.presentation.navigation.Screen
import com.owlcoder.animeschedule.presentation.navigation.shouldShowBottomBar
import com.owlcoder.animeschedule.presentation.screens.onboarding.OnboardingScreen
import com.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.rememberHazeState
import javax.inject.Inject
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.owlcoder.animeschedule.presentation.components.iosTween
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.first
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var prefsDataStore: UserPreferencesDataStore

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* The user made an explicit choice. */ }

    private var currentLanguage: AppLanguage? = null

    fun applyLocale(language: AppLanguage) {
        if (currentLanguage == language) return
        currentLanguage = language
        val locale = LocaleHelper.resolveLocale(language)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    // Flipped once the first real frame has been composed, which releases the splash screen.
    @Volatile
    private var contentReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The preferences decide theme and language, so nothing real can be drawn before they
        // load. Holding the splash avoids blocking the main thread on DataStore.
        splashScreen.setKeepOnScreenCondition { !contentReady }
        // A recreated Activity gets the same launch intent back; it was already handled once.
        if (savedInstanceState == null) intent?.let { handleIncomingIntent(it) }

        setContent {
            val loadedPrefs by prefsDataStore.userPreferencesFlow.collectAsStateWithLifecycle(initialValue = null)
            val prefs = loadedPrefs ?: return@setContent
            SideEffect { contentReady = true }

            val isMalConnected by authViewModel.isLoggedIn.collectAsStateWithLifecycle(initialValue = prefs.malLoggedIn)
            val malUsername by authViewModel.username.collectAsStateWithLifecycle(initialValue = prefs.malUsername)
            val scope = rememberCoroutineScope()

            var pendingTheme by rememberSaveable { mutableStateOf(prefs.themeMode) }
            var pendingAccent by rememberSaveable { mutableStateOf(prefs.accentColor) }
            var pendingLanguage by rememberSaveable { mutableStateOf(prefs.appLanguage) }
            var pendingNotifEnabled by rememberSaveable { mutableStateOf(true) }
            var pendingNotifOffset by rememberSaveable { mutableIntStateOf(0) }

            val effectiveTheme = if (prefs.onboardingDone) prefs.themeMode else pendingTheme
            val effectiveAccent = if (prefs.onboardingDone) prefs.accentColor else pendingAccent
            val effectiveLanguage = if (prefs.onboardingDone) prefs.appLanguage else pendingLanguage

            applyLocale(effectiveLanguage)

            AnimeScheduleTheme(themeMode = effectiveTheme, accentColor = effectiveAccent) {
                if (!prefs.onboardingDone) {
                        OnboardingScreen(
                            onComplete = {
                                scope.launch {
                                    prefsDataStore.setThemeMode(pendingTheme)
                                    prefsDataStore.setAccentColor(pendingAccent)
                                    prefsDataStore.setAppLanguage(pendingLanguage)
                                    prefsDataStore.setNotificationsEnabled(pendingNotifEnabled)
                                    prefsDataStore.setNotificationOffset(pendingNotifOffset)
                                    prefsDataStore.setOnboardingDone()
                                    LocaleHelper.applyLanguage(pendingLanguage)
                                }
                            },
                            onLogin = { context -> authViewModel.launchMalLogin(context) },
                            isMalConnected = isMalConnected,
                            malUsername = malUsername,
                            selectedTheme = pendingTheme,
                            selectedAccent = pendingAccent,
                            selectedLanguage = pendingLanguage,
                            onThemeChange = { pendingTheme = it },
                            onAccentChange = { pendingAccent = it },
                            onLanguageChange = { pendingLanguage = it },
                            onNotifSettingsChange = { enabled, offset ->
                                pendingNotifEnabled = enabled
                                pendingNotifOffset = offset
                                if (enabled) requestNotificationPermissionIfNeeded()
                            },
                        )
                    } else {
                        val navController = rememberNavController()
                        val hazeState = rememberHazeState()
                        val motion = LocalMotionPolicy.current
                        LaunchedEffect(navController, pendingDeepLinkAnimeId) {
                            pendingDeepLinkAnimeId?.let { animeId ->
                                pendingDeepLinkAnimeId = null
                                navController.navigate(Screen.Detail.createRoute(animeId)) {
                                    launchSingleTop = true
                                }
                            }
                        }

                        val toastController = remember { ToastController() }
                        var appLoading by rememberSaveable { mutableStateOf(true) }
                        var isSearchFocused by rememberSaveable { mutableStateOf(false) }
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = backStackEntry?.destination?.route
                        val showBottomBar = shouldShowBottomBar(currentRoute) && !isSearchFocused
                        val showInitialScheduleLoading = appLoading &&
                            (currentRoute == null || currentRoute == Screen.Schedule.route)

                        AppSystemBarAppearance(
                            statusBarOnImagery = currentRoute == Screen.Detail.ROUTE,
                        )

                        CompositionLocalProvider(
                            LocalChromeHazeState provides hazeState,
                            LocalNavBarHeight provides if (showBottomBar) 84.dp else 0.dp,
                            LocalToast provides toastController,
                        ) {
                            ToastHost(controller = toastController) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Scaffold(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .hazeSource(hazeState),
                                        contentWindowInsets = WindowInsets.safeDrawing.only(
                                            WindowInsetsSides.Horizontal,
                                        ),
                                    ) { innerPadding ->
                                        AnimeNavHost(
                                            navController = navController,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(innerPadding),
                                            onRestartForLanguage = { lang ->
                                                scope.launch {
                                                    prefsDataStore.setAppLanguage(lang)
                                                    LocaleHelper.applyLanguage(lang)
                                                }
                                            },
                                            onScheduleInitialLoadChange = { appLoading = it },
                                            onSearchFocusChanged = { isSearchFocused = it },
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = showBottomBar,
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        enter = slideInVertically(
                                            animationSpec = motion.iosTween(IosMotion.Standard),
                                            initialOffsetY = { if (motion.animationsEnabled) it / 2 else 0 },
                                        ) + fadeIn(
                                            animationSpec = motion.iosTween(IosMotion.Quick),
                                        ),
                                        exit = slideOutVertically(
                                            animationSpec = motion.iosTween(IosMotion.Standard),
                                            targetOffsetY = { if (motion.animationsEnabled) it / 2 else 0 },
                                        ) + fadeOut(
                                            animationSpec = motion.iosTween(IosMotion.Quick),
                                        ),
                                    ) {
                                        AnimeBottomBar(
                                            navController = navController,
                                            hazeState = hazeState,
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = showInitialScheduleLoading,
                                        enter = fadeIn(animationSpec = motion.iosTween(IosMotion.Quick)),
                                        exit = fadeOut(animationSpec = motion.iosTween(IosMotion.Standard)),
                                    ) {
                                        com.owlcoder.animeschedule.presentation.components.AnimatedSplashScreen(
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            }
                        }
                }
                }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        authViewModel.onReturnedFromBrowser()
    }

    // Observable so a link delivered to an already-running app (onNewIntent) still navigates.
    private var pendingDeepLinkAnimeId by mutableStateOf<Int?>(null)

    /** Routes the app's own `com.owlcoder.animeschedule://` links: OAuth redirect and detail. */
    private fun handleIncomingIntent(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme != APP_SCHEME) return
        when (data.host) {
            "oauth" -> authViewModel.onOAuthRedirect(
                code = data.getQueryParameter("code"),
                state = data.getQueryParameter("state"),
            )
            "detail" -> pendingDeepLinkAnimeId = data.lastPathSegment?.toIntOrNull()?.takeIf { it > 0 }
        }
    }

    /** POST_NOTIFICATIONS is a runtime permission only from Android 13; earlier it is implicit. */
    fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private companion object {
        const val APP_SCHEME = "com.owlcoder.animeschedule"
    }
}
