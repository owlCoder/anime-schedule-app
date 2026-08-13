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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.owlcoder.animeschedule.core.locale.LocaleHelper
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.presentation.components.AppSystemBarAppearance
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.LocalChromeHazeState
import com.owlcoder.animeschedule.presentation.components.LocalNavBarHeight
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.ToastController
import com.owlcoder.animeschedule.presentation.components.ToastHost
import com.owlcoder.animeschedule.presentation.components.iosTween
import com.owlcoder.animeschedule.presentation.navigation.AnimeBottomBar
import com.owlcoder.animeschedule.presentation.navigation.AnimeNavHost
import com.owlcoder.animeschedule.presentation.navigation.Screen
import com.owlcoder.animeschedule.presentation.navigation.shouldShowBottomBar
import com.owlcoder.animeschedule.presentation.screens.onboarding.OnboardingScreen
import com.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent?.let { handleOAuthIntent(it) }

        val initialPrefs = runBlocking { prefsDataStore.userPreferencesFlow.first() }

        setContent {
            val prefs by prefsDataStore.userPreferencesFlow.collectAsState(initial = initialPrefs)
            val isMalConnected by authViewModel.isLoggedIn.collectAsState(initial = initialPrefs.malLoggedIn)
            val malUsername by authViewModel.username.collectAsState(initial = initialPrefs.malUsername)
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
                        LaunchedEffect(navController) {
                            pendingDeepLinkAnimeId?.let { animeId ->
                                pendingDeepLinkAnimeId = null
                                navController.navigate(Screen.Detail.createRoute(animeId))
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
        handleOAuthIntent(intent)
    }

    override fun onResume() {
        super.onResume()
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
                if (code == null) authViewModel.handleCallbackError()
                else authViewModel.handleCallback(code, state)
            }

            data.scheme == "com.owlcoder.animeschedule" && data.host == "detail" -> {
                val animeId = data.lastPathSegment?.toIntOrNull() ?: return
                pendingDeepLinkAnimeId = animeId
            }
        }
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
