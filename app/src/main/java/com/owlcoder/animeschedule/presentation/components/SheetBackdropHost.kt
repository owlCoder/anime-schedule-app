package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Shared screen container. Modal windows own their scrim; Activity content stays sharp. */
@Composable
fun SheetBackdropHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier, content = content)
}
