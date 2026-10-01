package com.owlcoder.animeschedule.data.repository

import com.owlcoder.animeschedule.data.api.mal.auth.MalAuthManager
import com.owlcoder.animeschedule.data.api.mal.auth.PkceGenerator
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.data.local.secure.SecureTokenStore
import com.owlcoder.animeschedule.domain.model.LoginFailure
import com.owlcoder.animeschedule.domain.model.LoginState
import com.owlcoder.animeschedule.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Owns the sign-in session. It is a singleton on purpose: the Activity that receives the OAuth
 * redirect and the screens that start sign-in live in different ViewModel scopes, and they all
 * have to observe the same [loginState].
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val malAuthManager: MalAuthManager,
    private val tokenStore: SecureTokenStore,
    prefsDataStore: UserPreferencesDataStore
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> =
        prefsDataStore.userPreferencesFlow.map { it.malLoggedIn }.distinctUntilChanged()

    override val username: Flow<String> =
        prefsDataStore.userPreferencesFlow.map { it.malUsername }.distinctUntilChanged()

    override val avatarUrl: Flow<String> =
        prefsDataStore.userPreferencesFlow.map { it.malAvatarUrl }.distinctUntilChanged()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    override val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // The redirect is delivered (onNewIntent) right before the Activity resumes. That resume must
    // not be mistaken for the user closing the browser.
    private var redirectJustReceived = false
    private var exchangingCode = false

    override fun beginLogin(): String {
        val verifier = PkceGenerator.generateCodeVerifier()
        val state = PkceGenerator.generateState()
        // Persisted: the Activity may be recreated while the browser is in the foreground.
        tokenStore.savePkceVerifier(verifier)
        tokenStore.saveOAuthState(state)
        redirectJustReceived = false
        _loginState.value = LoginState.InProgress
        return malAuthManager.buildAuthorizationUri(verifier, state)
    }

    override suspend fun completeLogin(code: String, state: String?): Boolean {
        redirectJustReceived = true
        val verifier = tokenStore.getPkceVerifier()
        if (verifier == null || state == null || state != tokenStore.getOAuthState()) {
            tokenStore.clearPkceVerifier()
            _loginState.value = LoginState.Failed(LoginFailure.INVALID_SESSION)
            return false
        }
        exchangingCode = true
        _loginState.value = LoginState.InProgress
        try {
            val success = malAuthManager.handleCallback(code, verifier)
            _loginState.value =
                if (success) LoginState.Idle else LoginState.Failed(LoginFailure.EXCHANGE_FAILED)
            return success
        } finally {
            exchangingCode = false
            tokenStore.clearPkceVerifier()
        }
    }

    override fun loginDenied() {
        redirectJustReceived = true
        tokenStore.clearPkceVerifier()
        _loginState.value = LoginState.Failed(LoginFailure.DENIED)
    }

    override fun loginAbandoned() {
        if (redirectJustReceived) {
            redirectJustReceived = false
            return
        }
        if (_loginState.value == LoginState.InProgress && !exchangingCode) {
            _loginState.value = LoginState.Idle
        }
    }

    override suspend fun logout() {
        malAuthManager.logout()
        tokenStore.clearPkceVerifier()
        _loginState.value = LoginState.Idle
    }
}
