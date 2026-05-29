package rs.owlcoder.animeschedule.presentation.screens.settings

import android.content.Context
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rs.owlcoder.animeschedule.data.local.secure.SecureTokenStore
import rs.owlcoder.animeschedule.domain.repository.AuthRepository
import rs.owlcoder.animeschedule.domain.usecase.HandleMalCallbackUseCase
import rs.owlcoder.animeschedule.domain.usecase.LoginWithMalUseCase
import rs.owlcoder.animeschedule.domain.usecase.RefreshMalListUseCase
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val secureTokenStore: SecureTokenStore,
    private val loginWithMalUseCase: LoginWithMalUseCase,
    private val handleMalCallbackUseCase: HandleMalCallbackUseCase,
    private val authRepository: AuthRepository,
    private val refreshMalListUseCase: RefreshMalListUseCase
) : ViewModel() {

    val isLoggedIn = authRepository.isLoggedIn
    val username = authRepository.username

    fun launchMalLogin(context: Context) {
        val (uri, verifier) = loginWithMalUseCase()
        // Store verifier in EncryptedSharedPreferences — survives Activity recreation
        // when Chrome Custom Tab returns (new Activity instance, SavedStateHandle is gone)
        secureTokenStore.savePkceVerifier(verifier)
        val customTab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTab.launchUrl(context, uri)
    }

    fun handleCallback(code: String) {
        val verifier = secureTokenStore.getPkceVerifier()
        if (verifier == null) {
            Log.e("AuthViewModel", "PKCE verifier missing — cannot complete OAuth flow")
            return
        }
        viewModelScope.launch {
            val success = handleMalCallbackUseCase(code, verifier)
            if (success) {
                launch { refreshMalListUseCase(force = true) }
            } else {
                Log.e("AuthViewModel", "Token exchange failed for code=$code")
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
