package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

/** Each modal owns its registration; changing content cannot leave a stale window effect. */
@Stable
internal class SheetBackdropState {
    private val owners = mutableStateMapOf<Any, Unit>()
    val active: Boolean get() = owners.isNotEmpty()
    fun attach(owner: Any) { owners[owner] = Unit }
    fun detach(owner: Any) { owners.remove(owner) }
}

internal val LocalSheetBackdrop = staticCompositionLocalOf<SheetBackdropState?> { null }

/** Blur the Activity content. Modal sheets render in their own window and stay sharp. */
@Composable
fun SheetBackdropHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val backdrop = remember { SheetBackdropState() }
    val motion = LocalMotionPolicy.current
    val radius by animateDpAsState(
        if (backdrop.active) 12.dp else 0.dp,
        animationSpec = if (backdrop.active) motion.iosTween(IosMotion.Quick) else snap(),
        label = "sheet-backdrop-blur",
    )
    CompositionLocalProvider(LocalSheetBackdrop provides backdrop) {
        Box(modifier.then(if (radius > 0.dp) Modifier.blur(radius, BlurredEdgeTreatment.Unbounded) else Modifier), content = content)
    }
}
