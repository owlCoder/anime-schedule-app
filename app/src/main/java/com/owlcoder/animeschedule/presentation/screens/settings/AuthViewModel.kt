package com.owlcoder.animeschedule.presentation.screens.settings

import android.content.Context
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.owlcoder.animeschedule.data.local.secure.SecureTokenStore
import com.owlcoder.animeschedule.domain.repository.AuthRepository
import com.owlcoder.animeschedule.domain.usecase.HandleMalCallbackUseCase
import com.owlcoder.animeschedule.domain.usecase.LoginWithMalUseCase
import com.owlcoder.animeschedule.domain.usecase.RefreshMalListUseCase
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

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Set synchronously whenever an OAuth redirect intent is dispatched this resume cycle,
    // so onResume's cancel-if-pending check doesn't race with an in-flight callback.
    private var callbackHandledThisResume = false

    fun launchMalLogin(context: Context) {
        _loginError.value = null
        _isLoggingIn.value = true
        val (uri, verifier, state) = loginWithMalUseCase()
        // Store verifier and state in EncryptedSharedPreferences — survives Activity recreation
        // when Chrome Custom Tab returns (new Activity instance, SavedStateHandle is gone)
        secureTokenStore.savePkceVerifier(verifier)
        secureTokenStore.saveOAuthState(state)
        val customTab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTab.launchUrl(context, uri)
    }

    fun handleCallback(code: String, returnedState: String?) {
        callbackHandledThisResume = true
        val verifier = secureTokenStore.getPkceVerifier()
        val expectedState = secureTokenStore.getOAuthState()
        if (verifier == null) {
            Log.e("AuthViewModel", "PKCE verifier missing — cannot complete OAuth flow")
            _isLoggingIn.value = false
            _loginError.value = "Prijava nije uspela, pokušaj ponovo"
            return
        }
        if (returnedState == null || returnedState != expectedState) {
            Log.e("AuthViewModel", "OAuth state mismatch — possible CSRF attack, aborting")
            secureTokenStore.clearPkceVerifier()
            _isLoggingIn.value = false
            _loginError.value = "Prijava nije uspela, pokušaj ponovo"
            return
        }
        viewModelScope.launch {
            val success = handleMalCallbackUseCase(code, verifier)
            secureTokenStore.clearPkceVerifier()
            _isLoggingIn.value = false
            if (success) {
                _loginError.value = null
                launch { refreshMalListUseCase(force = true) }
            } else {
                Log.e("AuthViewModel", "Token exchange failed for code=$code")
                _loginError.value = "Prijava na MyAnimeList nije uspela. Proveri internet konekciju."
            }
        }
    }

    fun consumeLoginError() {
        _loginError.value = null
    }

    fun handleCallbackError() {
        callbackHandledThisResume = true
        Log.e("AuthViewModel", "OAuth redirect missing 'code' — user likely denied access")
        secureTokenStore.clearPkceVerifier()
        _isLoggingIn.value = false
        _loginError.value = "Prijava otkazana"
    }

    fun cancelLoginIfPending() {
        if (callbackHandledThisResume) {
            callbackHandledThisResume = false
            return
        }
        if (_isLoggingIn.value) {
            _isLoggingIn.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            secureTokenStore.clearPkceVerifier()
        }
    }
}
