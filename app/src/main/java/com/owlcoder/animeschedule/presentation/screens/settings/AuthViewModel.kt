package com.owlcoder.animeschedule.presentation.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owlcoder.animeschedule.domain.model.LoginState
import com.owlcoder.animeschedule.domain.repository.AuthRepository
import com.owlcoder.animeschedule.domain.repository.MalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlinx.coroutines.launch
import androidx.core.net.toUri

/**
 * Thin view of the sign-in session. The session itself lives in [AuthRepository], so the
 * instance owned by MainActivity (which receives the OAuth redirect) and the instances owned by
 * individual screens all see the same [loginState].
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val malRepository: MalRepository
) : ViewModel() {

    val isLoggedIn: Flow<Boolean> = authRepository.isLoggedIn
    val username: Flow<String> = authRepository.username
    val loginState: StateFlow<LoginState> = authRepository.loginState

    fun launchMalLogin(context: Context) {
        val authorizationUrl = authRepository.beginLogin()
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, authorizationUrl.toUri())
        } catch (_: ActivityNotFoundException) {
            // No browser can handle the sign-in page.
            authRepository.loginAbandoned()
        }
    }

    /** Handles the redirect back from the MyAnimeList consent page. */
    fun onOAuthRedirect(code: String?, state: String?) {
        if (code == null) {
            authRepository.loginDenied()
            return
        }
        viewModelScope.launch {
            if (authRepository.completeLogin(code, state)) {
                malRepository.refreshUserList(force = true)
            }
        }
    }

    /** Called when the app returns to the foreground without an OAuth redirect. */
    fun onReturnedFromBrowser() = authRepository.loginAbandoned()

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
