package com.owlcoder.animeschedule.data.api.mal.auth

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import com.owlcoder.animeschedule.BuildConfig
import com.owlcoder.animeschedule.data.api.mal.MalApiService
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.db.PendingListUpdateDao
import com.owlcoder.animeschedule.data.local.secure.SecureTokenStore
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MalAuthManager @Inject constructor(
    private val authService: MalAuthService,
    private val malApiService: MalApiService,
    private val tokenStore: SecureTokenStore,
    private val prefsDataStore: UserPreferencesDataStore,
    private val malListEntryDao: MalListEntryDao,
    private val pendingListUpdateDao: PendingListUpdateDao
) {
    private val refreshMutex = Mutex()
    fun buildAuthorizationUri(verifier: String, state: String): Uri {
        // MAL API uses plain PKCE: code_challenge == code_verifier (no SHA-256 hashing)
        Log.d("MalAuthManager", "Auth URI — client_id=${BuildConfig.MAL_CLIENT_ID}")
        Log.d("MalAuthManager", "redirect_uri=${BuildConfig.MAL_REDIRECT_URI}")
        Log.d("MalAuthManager", "verifier_len=${verifier.length}")
        return Uri.parse("https://myanimelist.net/v1/oauth2/authorize").buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", BuildConfig.MAL_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.MAL_REDIRECT_URI)
            .appendQueryParameter("code_challenge", verifier)
            .appendQueryParameter("code_challenge_method", "plain")
            .appendQueryParameter("state", state)
            .build()
    }

    suspend fun handleCallback(code: String, verifier: String): Boolean {
        Log.d("MalAuthManager", "handleCallback: code_len=${code.length} verifier_len=${verifier.length}")
        Log.d("MalAuthManager", "client_id=${BuildConfig.MAL_CLIENT_ID}")
        Log.d("MalAuthManager", "redirect_uri=${BuildConfig.MAL_REDIRECT_URI}")
        return try {
            // redirect_uri mora biti URL-enkodovano jer koristimo @Field(encoded=true)
            // da Retrofit ne bi dvostruko enkodovao ://
            val encodedRedirect = java.net.URLEncoder.encode(BuildConfig.MAL_REDIRECT_URI, "UTF-8")
            Log.d("MalAuthManager", "encoded_redirect=$encodedRedirect")
            val response = authService.exchangeToken(
                clientId = BuildConfig.MAL_CLIENT_ID,
                code = code,
                codeVerifier = verifier,
                grantType = "authorization_code",
                redirectUri = encodedRedirect
            )
            Log.d("MalAuthManager", "Token exchange OK: token_type=${response.tokenType} expires_in=${response.expiresIn}")
            val expiresAt = Instant.now().epochSecond + response.expiresIn
            tokenStore.saveMalTokens(response.accessToken, response.refreshToken, expiresAt)
            tokenStore.clearPkceVerifier()

            val me = try {
                malApiService.getMe()
            } catch (e: Exception) {
                Log.w("MalAuthManager", "getMe() failed after login", e)
                null
            }
            prefsDataStore.setMalLoggedIn(true, me?.name ?: "", me?.picture ?: "")
            Log.d("MalAuthManager", "Login complete, username=${me?.name}")
            true
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string() ?: "(no body)"
            Log.e("MalAuthManager", "HTTP ${e.code()} on token exchange: $body")
            false
        } catch (e: Exception) {
            Log.e("MalAuthManager", "Token exchange failed", e)
            false
        }
    }

    enum class RefreshResult {
        /** Token je uspešno osvežen (ili ga je već osvežio paralelni pozivalac). */
        REFRESHED,
        /** Refresh token je nevažeći/istekao — sesija je stvarno mrtva, treba logout. */
        INVALID,
        /** Prolazna mrežna/serverska greška — NE odjavljivati korisnika. */
        TRANSIENT
    }

    /**
     * Proactively refreshes the access token if it is expired or about to expire, so requests
     * don't have to burn a round-trip on a guaranteed 401 first. Reactive 401 handling stays
     * as the safety net (e.g. token revoked server-side while still "valid" locally).
     */
    suspend fun ensureFreshToken() {
        if (tokenStore.getMalRefreshToken() == null) return
        val expiresAt = tokenStore.getMalTokenExpiresAt()
        if (expiresAt - Instant.now().epochSecond <= TOKEN_REFRESH_SKEW_SECONDS) {
            refreshAccessToken()
        }
    }

    suspend fun refreshAccessToken(): RefreshResult = refreshMutex.withLock {
        val refresh = tokenStore.getMalRefreshToken() ?: return@withLock RefreshResult.INVALID
        val expiresAt = tokenStore.getMalTokenExpiresAt()
        if (expiresAt - Instant.now().epochSecond > TOKEN_REFRESH_SKEW_SECONDS) {
            // Another caller already refreshed the token while we were waiting for the lock.
            return@withLock RefreshResult.REFRESHED
        }
        try {
            val response = authService.refreshToken(
                clientId = BuildConfig.MAL_CLIENT_ID,
                refreshToken = refresh,
                grantType = "refresh_token"
            )
            val newExpiresAt = Instant.now().epochSecond + response.expiresIn
            tokenStore.saveMalTokens(response.accessToken, response.refreshToken, newExpiresAt)
            RefreshResult.REFRESHED
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string() ?: "(no body)"
            Log.e("MalAuthManager", "HTTP ${e.code()} on token refresh: $body")
            // 400/401 = invalid_grant (revoked/expired refresh token); anything else
            // (5xx, rate limit) is the server's problem, not a dead session.
            if (e.code() == 400 || e.code() == 401) RefreshResult.INVALID else RefreshResult.TRANSIENT
        } catch (e: Exception) {
            Log.e("MalAuthManager", "Token refresh failed", e)
            RefreshResult.TRANSIENT
        }
    }

    suspend fun logout() {
        tokenStore.clearMalTokens()
        prefsDataStore.setMalLoggedIn(false, "")
        // Cached list rows belong to the logged-out account — without this, the Schedule home
        // "recently changed" section and detail badges kept showing the old user's list.
        malListEntryDao.deleteAll()
        pendingListUpdateDao.deleteAll()
        prefsDataStore.setLastMalListSyncEpochMs(0L)
    }

    private companion object {
        const val TOKEN_REFRESH_SKEW_SECONDS = 60L
    }
}
