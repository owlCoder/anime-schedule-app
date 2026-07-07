package com.owlcoder.animeschedule.core.locale

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import java.util.Locale

object LocaleHelper {

    // Called once at app start so the system locale is set for non-Compose resources.
    fun applyLanguage(language: AppLanguage) {
        val localeList = when (language) {
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
            AppLanguage.SERBIAN_LATIN -> LocaleListCompat.forLanguageTags("sr-Latn")
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    // Wraps a Context with the chosen locale so Compose stringResource() resolves correctly
    // without restarting the Activity.
    fun wrap(context: Context, language: AppLanguage): Context {
        val locale = resolveLocale(language)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun resolveLocale(language: AppLanguage): Locale = when (language) {
        AppLanguage.ENGLISH -> Locale.ENGLISH
        AppLanguage.SERBIAN_LATIN -> Locale.forLanguageTag("sr-Latn")
        AppLanguage.SYSTEM -> Locale.getDefault()
    }
}
