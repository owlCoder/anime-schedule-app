package com.owlcoder.animeschedule.data.api.mal.auth

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction
import retrofit2.HttpException
import com.owlcoder.animeschedule.BuildConfig
import com.owlcoder.animeschedule.data.api.mal.MalApiService
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.db.PendingListUpdateDao
import com.owlcoder.animeschedule.data.local.secure.SecureTokenStore
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class MalAuthManager @Inject constructor(
    private val authService: MalAuthService,
    private val malApiService: MalApiService,
    private val tokenStore: SecureTokenStore,
    private val prefsDataStore: UserPreferencesDataStore,
    private val malListEntryDao: MalListEntryDao,
    private val pendingListUpdateDao: PendingListUpdateDao,
    private val session: MalSession = MalSession(),
    private val notificationActionDao: com.owlcoder.animeschedule.data.local.db.NotificationActionDao? = null,
    private val workScheduler: com.owlcoder.animeschedule.domain.repository.WorkScheduler? = null,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context? = null,
    private val database: com.owlcoder.animeschedule.data.local.db.AnimeScheduleDatabase? = null,
) {
    private val refreshMutex = Mutex()

    /**
     * MAL's OAuth only supports the "plain" PKCE method, so the challenge is the verifier itself.
     * Security therefore rests on the verifier staying private to this device and on [state]
     * being validated when the redirect returns.
     */
    fun buildAuthorizationUri(verifier: String, state: String): String =
        "https://myanimelist.net/v1/oauth2/authorize".toUri().buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", BuildConfig.MAL_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.MAL_REDIRECT_URI)
            .appendQueryParameter("code_challenge", verifier)
            .appendQueryParameter("code_challenge_method", "plain")
            .appendQueryParameter("state", state)
            .build()
            .toString()

    suspend fun handleCallback(code: String, verifier: String): Boolean = session.mutex.withLock {
        try {
            // redirect_uri is passed pre-encoded (@Field(encoded = true)) so Retrofit does not
            // encode the "://" a second time.
            val encodedRedirect = URLEncoder.encode(BuildConfig.MAL_REDIRECT_URI, "UTF-8")
            val response = authService.exchangeToken(
                clientId = BuildConfig.MAL_CLIENT_ID,
                code = code,
                codeVerifier = verifier,
                grantType = "authorization_code",
                redirectUri = encodedRedirect
            )
            val expiresAt = Instant.now().epochSecond + response.expiresIn
            // A process restart during profile verification must not replay the previous
            // account's queue using these newly exchanged credentials.
            prefsDataStore.expireMalSession()
            refreshMutex.withLock {
                tokenStore.saveMalTokens(response.accessToken, response.refreshToken, expiresAt)
            }
            tokenStore.clearPkceVerifier()

            // Verify the owner before exposing a session or replaying account-scoped edits.
            val me = try {
                malApiService.getMe()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Log.w(TAG, "Fetching the profile after sign-in failed", e)
                null
            }
            val previous = prefsDataStore.userPreferencesFlow.first()
            // A queued edit belongs to its original account. Without a verified profile, do not
            // risk delivering it to a different account after signing in again.
            if (me == null || me.name.isBlank()) {
                tokenStore.clearMalTokens()
                return@withLock false
            }
            if (!previous.malUsername.equals(me.name, true)) {
                clearAccountCache()
                context?.let { androidx.core.app.NotificationManagerCompat.from(it).cancelAll() }
                prefsDataStore.setLastMalListSyncEpochMs(0)
                prefsDataStore.setLastMalSyncSuccess(0)
            }
            prefsDataStore.setMalLoggedIn(true, me.name, me.picture ?: "")
            session.changedAccount()
            workScheduler?.scheduleFlushPendingUpdates()
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: HttpException) {
            Log.e(TAG, "Token exchange rejected: HTTP ${e.code()}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange failed: ${e.javaClass.simpleName}")
            false
        }
    }

    enum class RefreshResult {
        /** The token was refreshed (or a concurrent caller already refreshed it). */
        REFRESHED,

        /** The refresh token is invalid or expired: the session is really dead. */
        INVALID,

        /** Temporary network/server trouble: do NOT sign the user out. */
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

    suspend fun refreshAccessToken(force: Boolean = false): RefreshResult = refreshMutex.withLock {
        val refresh = tokenStore.getMalRefreshToken() ?: return@withLock RefreshResult.INVALID
        val expiresAt = tokenStore.getMalTokenExpiresAt()
        if (!force && expiresAt - Instant.now().epochSecond > TOKEN_REFRESH_SKEW_SECONDS) {
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
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: HttpException) {
            Log.w(TAG, "Token refresh rejected: HTTP ${e.code()}")
            // 400/401 = invalid_grant (revoked/expired refresh token); anything else
            // (5xx, rate limit) is the server's problem, not a dead session.
            if (e.code() == 400 || e.code() == 401) RefreshResult.INVALID else RefreshResult.TRANSIENT
        } catch (e: Exception) {
            Log.w(TAG, "Token refresh failed: ${e.javaClass.simpleName}")
            RefreshResult.TRANSIENT
        }
    }

    suspend fun logout() = session.mutex.withLock {
        session.changedAccount()
        refreshMutex.withLock { tokenStore.clearMalTokens() }
        // Cached list rows belong to the logged-out account; without clearing them the schedule
        // home "recently changed" section and detail badges would keep showing the old user's list.
        clearAccountCache()
        prefsDataStore.setMalLoggedIn(false, "")
        context?.let { androidx.core.app.NotificationManagerCompat.from(it).cancelAll() }
        prefsDataStore.setLastMalListSyncEpochMs(0L)
        prefsDataStore.setLastMalSyncSuccess(0L)
    }

    private suspend fun clearAccountCache() {
        suspend fun clear() {
            malListEntryDao.deleteAll()
            pendingListUpdateDao.deleteAll()
            notificationActionDao?.deleteAll()
        }
        if (database == null) clear() else database.withTransaction { clear() }
    }

    private companion object {
        const val TAG = "MalAuthManager"
        const val TOKEN_REFRESH_SKEW_SECONDS = 60L
    }
}
