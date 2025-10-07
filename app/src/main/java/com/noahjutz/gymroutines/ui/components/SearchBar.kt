package com.noahjutz.gymroutines.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
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
    BasicTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colors.onSurface),
        decorationBox = { innerTextField ->
            Surface(
                modifier = Modifier.height(48.dp),
                color = MaterialTheme.colors.surface,
                shape = MaterialTheme.shapes.large,
                elevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .padding(vertical = 2.dp)
                            .weight(1f)
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                stringResource(R.string.hint_search),
                                style = MaterialTheme.typography.subtitle1.copy(
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.38f)
                                )
                            )
                        }
                        innerTextField()
                    }
                    AnimatedVisibility(
                        value.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(
                                Icons.Default.Clear,
                                stringResource(R.string.btn_clear_text),
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    )
}
