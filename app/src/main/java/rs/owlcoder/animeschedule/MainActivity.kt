package rs.owlcoder.animeschedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import rs.owlcoder.animeschedule.data.local.datastore.UserPreferences
import rs.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import rs.owlcoder.animeschedule.presentation.navigation.AnimeBottomBar
import rs.owlcoder.animeschedule.presentation.navigation.AnimeNavHost
import rs.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import rs.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var prefsDataStore: UserPreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent?.let { handleOAuthIntent(it) }
        setContent {
            val prefs by prefsDataStore.userPreferencesFlow.collectAsState(initial = UserPreferences())
            AnimeScheduleTheme(themeMode = prefs.themeMode) {
                val navController = rememberNavController()
                // Scaffold bez contentWindowInsets — svaki ekran sam upravlja statusBar paddingom
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = { AnimeBottomBar(navController) }
                ) { innerPadding ->
                    AnimeNavHost(navController, Modifier.padding(innerPadding))
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
            authViewModel.handleCallback(code)
        }
    }
}
