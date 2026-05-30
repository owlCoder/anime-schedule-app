package rs.owlcoder.animeschedule

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.owlcoder.animeschedule.core.locale.LocaleHelper
import rs.owlcoder.animeschedule.data.local.datastore.AppLanguage
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
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var prefsDataStore: UserPreferencesDataStore

    @Inject
    lateinit var getUnreadCountUseCase: GetUnreadCountUseCase

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result — no action needed, user decided */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        intent?.let { handleOAuthIntent(it) }
        setContent {
            val prefs by prefsDataStore.userPreferencesFlow.collectAsState(initial = UserPreferences())
            val unreadCount by getUnreadCountUseCase().collectAsState(initial = 0)
            val scope = rememberCoroutineScope()

            LaunchedEffect(prefs.appLanguage) {
                LocaleHelper.applyLanguage(prefs.appLanguage)
            }

            var pendingTheme by rememberSaveable { mutableStateOf(prefs.themeMode) }
            var pendingAccent by rememberSaveable { mutableStateOf(prefs.accentColor) }
            var pendingLanguage by rememberSaveable { mutableStateOf(prefs.appLanguage) }
            var pendingNotifEnabled by rememberSaveable { mutableStateOf(true) }
            var pendingNotifOffset by rememberSaveable { mutableStateOf(0) }

            // Sync pending values from prefs on first load (before onboarding is shown)
            val effectiveTheme = if (prefs.onboardingDone) prefs.themeMode else pendingTheme
            val effectiveAccent = if (prefs.onboardingDone) prefs.accentColor else pendingAccent

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
                            }
                        },
                        onLogin = { context -> authViewModel.launchMalLogin(context) },
                        selectedTheme = pendingTheme,
                        selectedAccent = pendingAccent,
                        selectedLanguage = pendingLanguage,
                        onThemeChange = { pendingTheme = it },
                        onAccentChange = { pendingAccent = it },
                        onLanguageChange = {
                            pendingLanguage = it
                            LocaleHelper.applyLanguage(it)
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
                            AnimeNavHost(navController, Modifier.fillMaxSize())
                            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                                AnimeBottomBar(navController, unreadCount = unreadCount)
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
