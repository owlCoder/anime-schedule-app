package rs.owlcoder.animeschedule

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.res.Configuration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import rs.owlcoder.animeschedule.core.locale.LocaleHelper
import rs.owlcoder.animeschedule.data.local.datastore.UserPreferences
import rs.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import rs.owlcoder.animeschedule.domain.usecase.GetUnreadCountUseCase
import rs.owlcoder.animeschedule.presentation.navigation.AnimeBottomBar
import rs.owlcoder.animeschedule.presentation.navigation.AnimeNavHost
import rs.owlcoder.animeschedule.presentation.screens.onboarding.OnboardingScreen
import rs.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import rs.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var prefsDataStore: UserPreferencesDataStore

    @Inject
    lateinit var getUnreadCountUseCase: GetUnreadCountUseCase

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result — no action needed, user decided */ }

    private var currentLanguage: rs.owlcoder.animeschedule.data.local.datastore.AppLanguage? = null

    fun applyLocale(language: rs.owlcoder.animeschedule.data.local.datastore.AppLanguage) {
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
        requestNotificationPermissionIfNeeded()
        intent?.let { handleOAuthIntent(it) }

        // Read prefs synchronously on main thread to avoid onboarding flash.
        // DataStore reads from disk only on first access; subsequent calls are instant from cache.
        val initialPrefs = runBlocking { prefsDataStore.userPreferencesFlow.first() }

        setContent {
            val prefs by prefsDataStore.userPreferencesFlow.collectAsState(initial = initialPrefs)
            val unreadCount by getUnreadCountUseCase().collectAsState(initial = 0)
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
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    Scaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            AnimeNavHost(
                                navController = navController,
                                modifier = Modifier.fillMaxSize(),
                                onRestartForLanguage = { lang ->
                                    scope.launch {
                                        prefsDataStore.setAppLanguage(lang)
                                        // LocalContext is re-wrapped via effectiveLanguage → no Activity restart needed
                                    }
                                }
                            )
                            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                                AnimeBottomBar(navController, unreadCount = unreadCount)
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

    private fun handleOAuthIntent(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme == "rs.owlcoder.animeschedule" && data.host == "oauth") {
            val code = data.getQueryParameter("code") ?: return
            val state = data.getQueryParameter("state")
            authViewModel.handleCallback(code, state)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
