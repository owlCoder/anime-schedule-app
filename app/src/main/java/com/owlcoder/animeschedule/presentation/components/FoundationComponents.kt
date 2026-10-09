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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
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
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    val shape = ContinuousRoundedShape(GlassTokens.controlRadius)
    val clearDescription = stringResource(R.string.search_clear_query)
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    val motion = LocalMotionPolicy.current
    val outline by animateColorAsState(
        if (focused && enabled) MaterialTheme.colorScheme.primary.copy(alpha = .65f)
        else MaterialTheme.colorScheme.outlineVariant,
        motion.iosTween(IosMotion.Quick), label = "search-focus-outline")
    Box(modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focused = it.isFocused; onFocusChanged(it.isFocused) }
                .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
                .border(0.5.dp, outline, shape)
                .semantics { contentDescription = placeholder }
                .padding(start = 11.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSearch?.invoke(); focus.clearFocus(force = true); keyboard?.hide() },
                onDone = { focus.clearFocus(force = true); keyboard?.hide() },
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
                                modifier = Modifier.clearAndSetSemantics {},
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                    if (onClear != null) {
                        // Reserve the trailing action so the text/cursor never jump when typing.
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.animation.AnimatedVisibility(value.isNotEmpty(),
                                enter = fadeIn(motion.iosTween(IosMotion.Quick)),
                                exit = fadeOut(motion.iosTween(IosMotion.PressIn))) {
                                IconButton(onClick = onClear, enabled = enabled, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Default.Close, clearDescription, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}
