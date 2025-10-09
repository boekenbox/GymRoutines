package com.noahjutz.gymroutines.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R

@ExperimentalAnimationApi
@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colors
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.subtitle1.copy(color = colors.onSurface),
        shape = MaterialTheme.shapes.large,
        placeholder = {
            Text(
                text = stringResource(R.string.hint_search),
                style = MaterialTheme.typography.subtitle1.copy(
                    color = colors.onSurface.copy(alpha = 0.38f)
                )
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.onSurface.copy(alpha = 0.65f)
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = value.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.btn_clear_text),
                        tint = colors.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
        },
        colors = TextFieldDefaults.textFieldColors(
            textColor = colors.onSurface,
            cursorColor = colors.onSurface,
            backgroundColor = colors.surface,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            leadingIconColor = colors.onSurface.copy(alpha = 0.65f),
            trailingIconColor = colors.onSurface.copy(alpha = 0.65f),
            placeholderColor = colors.onSurface.copy(alpha = 0.38f)
        )
    )
}
