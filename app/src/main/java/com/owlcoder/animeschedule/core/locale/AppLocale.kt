package com.owlcoder.animeschedule.core.locale

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.owlcoder.animeschedule.domain.model.AppLanguage

/** Keep dates and strings in the same language without replacing the Activity context. */
@Composable
fun ProvideAppLocale(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val deviceConfiguration = LocalConfiguration.current
    val configuration = remember(deviceConfiguration, language) {
        Configuration(deviceConfiguration).apply {
            val locale = LocaleHelper.resolveLocale(language)
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }
    val resources = remember(context, configuration) {
        context.createConfigurationContext(configuration).resources
    }
    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalResources provides resources,
        content = content,
    )
}
