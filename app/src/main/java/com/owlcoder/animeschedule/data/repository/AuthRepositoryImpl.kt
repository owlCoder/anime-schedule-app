package com.owlcoder.animeschedule.data.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.owlcoder.animeschedule.data.api.mal.auth.MalAuthManager
import com.owlcoder.animeschedule.data.api.mal.auth.PkceGenerator
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val malAuthManager: MalAuthManager,
    private val prefsDataStore: UserPreferencesDataStore
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> =
        prefsDataStore.userPreferencesFlow.map { it.malLoggedIn }

    override val username: Flow<String> =
        prefsDataStore.userPreferencesFlow.map { it.malUsername }

    override val avatarUrl: Flow<String> =
        prefsDataStore.userPreferencesFlow.map { it.malAvatarUrl }

    override fun buildAuthUri(): Triple<Uri, String, String> {
        val verifier = PkceGenerator.generateCodeVerifier()
        val state = PkceGenerator.generateState()
        return Triple(malAuthManager.buildAuthorizationUri(verifier, state), verifier, state)
    }

    override suspend fun handleOAuthCallback(code: String, verifier: String): Boolean =
        malAuthManager.handleCallback(code, verifier)

    override suspend fun logout() = malAuthManager.logout()
}
