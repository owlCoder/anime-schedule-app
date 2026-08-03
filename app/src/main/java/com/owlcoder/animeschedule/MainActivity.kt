package com.owlcoder.animeschedule

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import com.owlcoder.animeschedule.presentation.components.LocalNavBarHeight
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.ToastController
import com.owlcoder.animeschedule.presentation.components.ToastHost
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import com.owlcoder.animeschedule.presentation.navigation.Screen
import com.owlcoder.animeschedule.presentation.navigation.shouldShowBottomBar
import androidx.core.content.ContextCompat
import android.content.res.Configuration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.owlcoder.animeschedule.core.locale.LocaleHelper
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.presentation.navigation.AnimeBottomBar
import com.owlcoder.animeschedule.presentation.navigation.AnimeNavHost
import com.owlcoder.animeschedule.presentation.screens.onboarding.OnboardingScreen
import com.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import androidx.navigation.compose.currentBackStackEntryAsState
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var prefsDataStore: UserPreferencesDataStore

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result — no action needed, user decided */ }

    private var currentLanguage: com.owlcoder.animeschedule.data.local.datastore.AppLanguage? = null

    fun applyLocale(language: com.owlcoder.animeschedule.data.local.datastore.AppLanguage) {
        if (currentLanguage == language) return
        currentLanguage = language
        val locale = LocaleHelper.resolveLocale(language)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent?.let { handleOAuthIntent(it) }

        // Read prefs synchronously on main thread to avoid onboarding flash.
        // DataStore reads from disk only on first access; subsequent calls are instant from cache.
        val initialPrefs = runBlocking { prefsDataStore.userPreferencesFlow.first() }

        setContent {
            val prefs by prefsDataStore.userPreferencesFlow.collectAsState(initial = initialPrefs)
            val scope = rememberCoroutineScope()

            var pendingTheme by rememberSaveable { mutableStateOf(prefs.themeMode) }
            var pendingAccent by rememberSaveable { mutableStateOf(prefs.accentColor) }
            var pendingLanguage by rememberSaveable { mutableStateOf(prefs.appLanguage) }
            var pendingNotifEnabled by rememberSaveable { mutableStateOf(true) }
            var pendingNotifOffset by rememberSaveable { mutableStateOf(0) }

            val effectiveTheme = if (prefs.onboardingDone) prefs.themeMode else pendingTheme
            val effectiveAccent = if (prefs.onboardingDone) prefs.accentColor else pendingAccent
            val effectiveLanguage = if (prefs.onboardingDone) prefs.appLanguage else pendingLanguage

            applyLocale(effectiveLanguage)

            key(effectiveLanguage) {
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
                        selectedTheme = pendingTheme,
                        selectedAccent = pendingAccent,
                        selectedLanguage = pendingLanguage,
                        onThemeChange = { pendingTheme = it },
                        onAccentChange = { pendingAccent = it },
                        onLanguageChange = { lang ->
                            pendingLanguage = lang
                        },
                        onNotifSettingsChange = { enabled, offset ->
                            pendingNotifEnabled = enabled
                            pendingNotifOffset = offset
                            if (enabled) requestNotificationPermissionIfNeeded()
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    LaunchedEffect(navController) {
                        pendingDeepLinkAnimeId?.let { animeId ->
                            pendingDeepLinkAnimeId = null
                            navController.navigate(Screen.Detail.createRoute(animeId))
                        }
                    }
                    val toastController = androidx.compose.runtime.remember { ToastController() }
                    var searchKeyboardFocused by rememberSaveable { mutableStateOf(false) }
                    // Covers the whole app (incl. nav bar) with the animated splash until the
                    // Schedule screen reports its first data load has resolved.
                    var appLoading by rememberSaveable { mutableStateOf(true) }
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    val showBottomBar = shouldShowBottomBar(currentRoute) &&
                        !(currentRoute == Screen.Search.route && searchKeyboardFocused)
                    LaunchedEffect(currentRoute) {
                        if (currentRoute != Screen.Search.route) searchKeyboardFocused = false
                    }
                    CompositionLocalProvider(
                        // Screens still consume this local for their bottom spacers. The root
                        // Scaffold now owns the actual bar/insets, so keeping the compatibility
                        // local at zero avoids double-padding and preserves existing screens.
                        LocalNavBarHeight provides 0.dp,
                        LocalToast provides toastController
                    ) {
                    ToastHost(controller = toastController) {
                        Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            // Nested screens own their status/navigation-bar insets. The root
                            // contributes only horizontal safe-drawing insets and the measured
                            // Material NavigationBar height, avoiding double bottom padding.
                            contentWindowInsets = WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal
                            ),
                            bottomBar = {
                                if (showBottomBar) {
                                    AnimeBottomBar(
                                        navController = navController,
                                    )
                                }
                            }
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
                                onSearchFocusChanged = { searchKeyboardFocused = it },
                            )
                        }
                        if (appLoading) {
                            com.owlcoder.animeschedule.presentation.components.AnimatedSplashScreen(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        }
                    }
                    }
                }
            }
            } // key(effectiveLanguage)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // If the user dismissed the Chrome Custom Tab without completing the MAL OAuth
        // flow (no redirect intent ever arrives), clear the stuck loading state.
        authViewModel.cancelLoginIfPending()
    }

    var pendingDeepLinkAnimeId: Int? = null
        private set

    private fun handleOAuthIntent(intent: Intent) {
        val data = intent.data ?: return
        when {
            data.scheme == "com.owlcoder.animeschedule" && data.host == "oauth" -> {
                val code = data.getQueryParameter("code")
                val state = data.getQueryParameter("state")
                if (code == null) {
                    authViewModel.handleCallbackError()
                } else {
                    authViewModel.handleCallback(code, state)
                }
            }
            data.scheme == "com.owlcoder.animeschedule" && data.host == "detail" -> {
                val animeId = data.lastPathSegment?.toIntOrNull() ?: return
                pendingDeepLinkAnimeId = animeId
            }
        }
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
