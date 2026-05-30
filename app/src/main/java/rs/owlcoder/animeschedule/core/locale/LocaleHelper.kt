package rs.owlcoder.animeschedule.core.locale

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import rs.owlcoder.animeschedule.data.local.datastore.AppLanguage
import java.util.Locale

private const val PREFS_NAME = "locale_prefs"
private const val KEY_LANGUAGE = "app_language"

object LocaleHelper {
    private fun localeFor(language: AppLanguage): Locale? = when (language) {
        AppLanguage.ENGLISH -> Locale.ENGLISH
        AppLanguage.SERBIAN_LATIN -> Locale.forLanguageTag("sr-Latn")
        AppLanguage.SYSTEM -> null
    }

    /**
     * Wraps [base] with the locale that matches [language].
     * Call from Activity.attachBaseContext so strings inflate in the right locale.
     */
    fun wrapContext(base: Context, language: AppLanguage): Context {
        val locale = localeFor(language) ?: return base
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    /**
     * Reads the persisted language from a plain SharedPreferences file.
     * Safe to call before Hilt injection (e.g. in attachBaseContext).
     */
    fun readLanguageSync(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LANGUAGE, null) ?: return AppLanguage.SYSTEM
        return runCatching { AppLanguage.valueOf(raw) }.getOrDefault(AppLanguage.SYSTEM)
    }

    /**
     * Persists the language choice to SharedPreferences so it is available
     * synchronously in the next attachBaseContext call after Activity restart.
     */
    fun saveLanguageSync(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, language.name).apply()
    }
}
