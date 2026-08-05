package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.owlcoder.animeschedule.R

/** Nullable overload used by cards; filter options use the more specific String overload. */
@Composable
internal fun localizedFormatLabel(format: Any?): String {
    val value = format as? String ?: return ""
    val labelRes = when (value) {
        "TV" -> R.string.format_tv
        "TV_SHORT" -> R.string.format_tv_short
        "MOVIE" -> R.string.format_movie
        "SPECIAL" -> R.string.format_special
        "OVA" -> R.string.format_ova
        "ONA" -> R.string.format_ona
        "MUSIC" -> R.string.format_music
        else -> null
    }
    return labelRes?.let { stringResource(it) } ?: value
}
