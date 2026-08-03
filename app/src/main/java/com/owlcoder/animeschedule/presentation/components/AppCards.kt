package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun AppHalfCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit,
) = AppCardSurface(modifier, onClick, shape, 14.dp, content)

@Composable
fun AppFullCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit,
) = AppCardSurface(modifier, onClick, shape, 16.dp, content)

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit,
) = AppCardSurface(modifier, onClick, shape, 14.dp, content)

@Composable
private fun AppCardSurface(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    shape: Shape,
    contentPadding: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit,
) {
    AppMaterialSurface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick).semantics { role = Role.Button }
                } else Modifier
            ),
        material = AppMaterial.Elevated,
        shape = shape,
    ) {
        Column(Modifier.fillMaxWidth().padding(contentPadding)) { content() }
    }
}
