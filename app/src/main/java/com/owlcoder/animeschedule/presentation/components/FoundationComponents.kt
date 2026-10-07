package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.ui.theme.GlassTokens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.unit.dp

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
    val clearDescription = stringResource(R.string.search_clear_query)
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    Box(modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(appMaterialColor(AppMaterial.Interactive), shape)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .semantics { contentDescription = placeholder }
                .padding(start = 11.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSearch?.invoke(); focus.clearFocus(); keyboard?.hide() },
                onDone = { focus.clearFocus(); keyboard?.hide() },
            ),
            // Keep decoration inside the text field so tapping its padding or leading icon
            // focuses the input as well. The clear action still has its own accessible target.
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                    if (value.isNotEmpty() && onClear != null) {
                        IconButton(onClick = onClear, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.Close, clearDescription, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
        )
    }
}
