package rs.owlcoder.animeschedule.data.local.secure

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveMalTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun getMalAccessToken(): String? = prefs.getString(KEY_ACCESS, null)
    fun getMalRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)
    fun getMalTokenExpiresAt(): Long = prefs.getLong(KEY_EXPIRES_AT, 0L)

    fun clearMalTokens() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_PKCE_VERIFIER)
            .apply()
    }

    // PKCE verifier and OAuth state must survive Activity recreation
    fun savePkceVerifier(verifier: String) {
        prefs.edit().putString(KEY_PKCE_VERIFIER, verifier).apply()
    }

    fun getPkceVerifier(): String? = prefs.getString(KEY_PKCE_VERIFIER, null)

    fun clearPkceVerifier() {
        prefs.edit().remove(KEY_PKCE_VERIFIER).remove(KEY_OAUTH_STATE).apply()
    }

    fun saveOAuthState(state: String) {
        prefs.edit().putString(KEY_OAUTH_STATE, state).apply()
    }

    fun getOAuthState(): String? = prefs.getString(KEY_OAUTH_STATE, null)

    companion object {
        private const val KEY_ACCESS = "mal_access_token"
        private const val KEY_REFRESH = "mal_refresh_token"
        private const val KEY_EXPIRES_AT = "mal_expires_at"
        private const val KEY_PKCE_VERIFIER = "mal_pkce_verifier"
        private const val KEY_OAUTH_STATE = "mal_oauth_state"
    }
}
