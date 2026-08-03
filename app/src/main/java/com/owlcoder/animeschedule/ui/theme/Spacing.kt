package com.owlcoder.animeschedule.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The shared spacing scale for screens and reusable components. */
object AppSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val screen: Dp = 16.dp
    val compactScreen: Dp = 12.dp
    val section: Dp = 20.dp
    val control: Dp = 44.dp
    val iconButton: Dp = 48.dp
    val groupedRow: Dp = 52.dp
    val groupedRowTwoLine: Dp = 64.dp
    val mediaRow: Dp = 72.dp
}

/** Density tokens used by shared components and compact screen layouts. */
object AppDensity {
    val screenHorizontal: Dp = 16.dp
    val sectionGap: Dp = 20.dp
    val cardGap: Dp = 8.dp
    val contentGap: Dp = 8.dp
    val compactGap: Dp = 4.dp
    val minTouchTarget: Dp = 48.dp
    val groupedRowMinHeight: Dp = 52.dp
}

val AppScreenPadding = PaddingValues(horizontal = AppSpacing.screen)

fun Modifier.appScreenPadding(horizontal: Dp = AppSpacing.screen): Modifier =
    padding(horizontal = horizontal)
