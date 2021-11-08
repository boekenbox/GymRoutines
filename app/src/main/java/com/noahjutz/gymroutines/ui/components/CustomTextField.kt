/*
 * Splitfit
 * Copyright (C) 2020  Noah Jutz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.noahjutz.gymroutines.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp

@Composable
fun AppBarTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
) {
    Box {
        BasicTextField(
            modifier = modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(if (isSystemInDarkTheme()) MaterialTheme.colors.onSurface else MaterialTheme.colors.onPrimary),
            textStyle = LocalTextStyle.current.copy(
                color = colors.contentColorFor(colors.primarySurface)
            ),
        )
        if (value.isBlank()) {
            Text(
                hint,
                modifier = Modifier.alpha(0.5f),
            )
        }
    }
}

@Composable
fun TableCellTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    hint: String? = null,
) {
    AutoSelectTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(color = colors.onSurface),
        cursorColor = colors.onSurface,
        maxLines = 1,
        decorationBox = { innerTextField ->
            Box(Modifier.height(48.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isBlank() && hint != null) Text(hint, modifier = Modifier.alpha(0.5f))
                innerTextField()
            }
        }
    )
}

@Composable
private fun AutoSelectTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = LocalTextStyle.current,
    cursorColor: Color = LocalContentColor.current,
    maxLines: Int = Int.MAX_VALUE,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() },
    singleLine: Boolean = false,
) {
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }

    fun selectText() {
        textFieldValue = textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length))
    }

    fun deselectText() {
        textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.text.length))
    }

    // onValueChange is called after onFocusChanged, overriding the selection in onFocusChanged.
    // see https://stackoverflow.com/questions/66262168
    var isValueChangeLocked = false

    BasicTextField(
        modifier = modifier.onFocusChanged {
            if (it.isFocused) {
                selectText()
                isValueChangeLocked = true
            }
        },
        value = textFieldValue,
        onValueChange = {
            if (!isValueChangeLocked) {
                onValueChange(it.text)
                deselectText()
            } else {
                isValueChangeLocked = false
            }
        },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        textStyle = textStyle,
        cursorBrush = SolidColor(cursorColor),
        maxLines = maxLines,
        decorationBox = decorationBox,
        singleLine = singleLine,
    )
}

/** Turns string of 0-4 digits to MM:SS format */
val durationVisualTransformation = object : VisualTransformation {
    val offsetMap = object : OffsetMapping {
        override fun originalToTransformed(offset: Int) = if (offset == 0) 0 else 5
        override fun transformedToOriginal(offset: Int) = 5 - offset
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val withZeroes = "0".repeat((4 - text.text.length).takeIf { it > 0 } ?: 0) + text.text
        val withColon = withZeroes.let { it.substring(0, 2) + ":" + it.substring(2, 4) }
        return TransformedText(
            AnnotatedString(if (text.text.isEmpty()) "" else withColon),
            offsetMap
        )
    }
}
