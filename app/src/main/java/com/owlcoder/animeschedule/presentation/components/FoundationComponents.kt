package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.ui.theme.AppSpacing
import com.owlcoder.animeschedule.ui.theme.GlassTokens
import com.owlcoder.animeschedule.ui.theme.PillShape

@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        content = { content() },
    )
}

@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.screen),
            content = content,
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    leadingIcon: ImageVector? = null,
    onClear: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val shape = ContinuousRoundedShape(GlassTokens.controlRadius)
    val clearDescription = stringResource(R.string.search_clear_recent)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(appMaterialColor(AppMaterial.Interactive), shape)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(start = 11.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Default,
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSearch?.invoke() },
            ),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
            },
        )
        if (value.isNotEmpty() && onClear != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = clearDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun MediaRow(
    modifier: Modifier = Modifier,
    leadingContent: @Composable () -> Unit,
    title: String,
    supportingText: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    InsetListRow(
        label = title,
        supportingText = supportingText,
        modifier = modifier,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        onClick = onClick,
    )
}

@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = content,
    )
}

@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, shape = PillShape) { Text(actionLabel) }
        }
    }
}

@Composable
fun AppStatusPill(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .background(containerColor, PillShape)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(15.dp), tint = contentColor)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}

@Composable
fun AppPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled,
        shape = PillShape,
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.size(AppSpacing.xs))
        }
        Text(label)
    }
}

@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        enabled = enabled,
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
