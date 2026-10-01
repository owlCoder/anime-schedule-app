package com.owlcoder.animeschedule.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        try {
            openEncryptedPrefs()
        } catch (e: GeneralSecurityException) {
            resetAfterUnreadableStore(e)
        } catch (e: IOException) {
            resetAfterUnreadableStore(e)
        }
    }

    private fun openEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * The Keystore master key can disappear (device restore, security reset) while the encrypted
     * file survives, after which it can never be decrypted again. The tokens are unrecoverable
     * either way, so start from an empty store: the user is signed out instead of the app
     * crashing on every network request.
     */
    private fun resetAfterUnreadableStore(cause: Exception): SharedPreferences {
        Log.w(TAG, "Secure token store was unreadable; resetting it", cause)
        context.deleteSharedPreferences(FILE_NAME)
        return openEncryptedPrefs()
    }

    fun saveMalTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        prefs.edit {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
            putLong(KEY_EXPIRES_AT, expiresAt)
        }
    }

    fun getMalAccessToken(): String? = prefs.getString(KEY_ACCESS, null)
    fun getMalRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)
    fun getMalTokenExpiresAt(): Long = prefs.getLong(KEY_EXPIRES_AT, 0L)

    fun clearMalTokens() {
        prefs.edit {
            remove(KEY_ACCESS)
            remove(KEY_REFRESH)
            remove(KEY_EXPIRES_AT)
            remove(KEY_PKCE_VERIFIER)
            remove(KEY_OAUTH_STATE)
        }
    }

    // PKCE verifier and OAuth state must survive Activity recreation
    fun savePkceVerifier(verifier: String) {
        prefs.edit { putString(KEY_PKCE_VERIFIER, verifier) }
    }

    fun getPkceVerifier(): String? = prefs.getString(KEY_PKCE_VERIFIER, null)

    fun clearPkceVerifier() {
        prefs.edit {
            remove(KEY_PKCE_VERIFIER)
            remove(KEY_OAUTH_STATE)
        }
    }

    fun saveOAuthState(state: String) {
        prefs.edit { putString(KEY_OAUTH_STATE, state) }
    }

    fun getOAuthState(): String? = prefs.getString(KEY_OAUTH_STATE, null)

    companion object {
        private const val TAG = "SecureTokenStore"
        private const val FILE_NAME = "secure_tokens"
        private const val KEY_ACCESS = "mal_access_token"
        private const val KEY_REFRESH = "mal_refresh_token"
        private const val KEY_EXPIRES_AT = "mal_expires_at"
        private const val KEY_PKCE_VERIFIER = "mal_pkce_verifier"
        private const val KEY_OAUTH_STATE = "mal_oauth_state"
    }
}
