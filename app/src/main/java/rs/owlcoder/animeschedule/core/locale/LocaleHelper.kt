package rs.owlcoder.animeschedule.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import rs.owlcoder.animeschedule.data.local.datastore.AppLanguage

object LocaleHelper {
    fun applyLanguage(language: AppLanguage) {
        val localeList = when (language) {
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
            AppLanguage.SERBIAN_LATIN -> LocaleListCompat.forLanguageTags("sr-Latn")
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
