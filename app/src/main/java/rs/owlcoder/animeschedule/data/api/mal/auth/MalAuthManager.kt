package rs.owlcoder.animeschedule.data.api.mal.auth

import android.net.Uri
import android.util.Log
import retrofit2.HttpException
import rs.owlcoder.animeschedule.BuildConfig
import rs.owlcoder.animeschedule.data.api.mal.MalApiService
import rs.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import rs.owlcoder.animeschedule.data.local.secure.SecureTokenStore
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MalAuthManager @Inject constructor(
    private val authService: MalAuthService,
    private val malApiService: MalApiService,
    private val tokenStore: SecureTokenStore,
    private val prefsDataStore: UserPreferencesDataStore
) {
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

    suspend fun refreshAccessToken(): Boolean {
        val refresh = tokenStore.getMalRefreshToken() ?: return false
        return try {
            val response = authService.refreshToken(
                clientId = BuildConfig.MAL_CLIENT_ID,
                refreshToken = refresh,
                grantType = "refresh_token"
            )
            val expiresAt = Instant.now().epochSecond + response.expiresIn
            tokenStore.saveMalTokens(response.accessToken, response.refreshToken, expiresAt)
            true
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string() ?: "(no body)"
            Log.e("MalAuthManager", "HTTP ${e.code()} on token refresh: $body")
            false
        } catch (e: Exception) {
            Log.e("MalAuthManager", "Token refresh failed", e)
            false
        }
    }

    suspend fun logout() {
        tokenStore.clearMalTokens()
        prefsDataStore.setMalLoggedIn(false, "")
    }
}
