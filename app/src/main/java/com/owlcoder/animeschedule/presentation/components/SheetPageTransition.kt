package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection

/** Nested pages share one panel and retain their input/scroll state when returning. */
@Composable
fun <T : Any> SheetPageTransition(
    page: T,
    isRoot: (T) -> Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    val motion = LocalMotionPolicy.current
    val state = rememberSaveableStateHolder()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedContent(page, modifier.fillMaxWidth(), transitionSpec = {
        val direction = (if (isRoot(targetState)) -1 else 1) * if (rtl) -1 else 1
        motion.contentTransform(
            enter = slideInHorizontally(motion.iosDecelerate()) {
                if (motion.animationsEnabled) direction * it / 24 else 0
            } + androidx.compose.animation.fadeIn(motion.iosDecelerate(IosMotion.Standard - IosMotion.PressIn, IosMotion.PressIn)),
            exit = slideOutHorizontally(motion.iosAccelerate(IosMotion.PressIn)) {
                if (motion.animationsEnabled) -direction * it / 24 else 0
            } + androidx.compose.animation.fadeOut(motion.iosAccelerate(IosMotion.PressIn)),
        )
    }, label = "sheet-page") { displayedPage ->
        state.SaveableStateProvider(displayedPage) {
            Column(Modifier.fillMaxWidth()) { content(displayedPage) }
        }
    }
}
