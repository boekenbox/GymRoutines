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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.util.MAX_REST_TIMER_SECONDS
import com.noahjutz.gymroutines.util.REST_TIMER_STEP_SECONDS
import com.noahjutz.gymroutines.util.formatRestDuration

@Composable
fun RestTimerDialog(
    initialWarmupSeconds: Int,
    initialWorkingSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    onRemove: () -> Unit,
) {
    var warmupSeconds by rememberSaveable(initialWarmupSeconds) {
        mutableStateOf(initialWarmupSeconds.coerceIn(0, MAX_REST_TIMER_SECONDS))
    }
    var workingSeconds by rememberSaveable(initialWorkingSeconds) {
        mutableStateOf(
            initialWorkingSeconds.takeIf { it > 0 }
                ?.coerceIn(0, MAX_REST_TIMER_SECONDS)
                ?: 120
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_rest_timer)) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_body_rest_timer))
                Spacer(Modifier.height(16.dp))
                RestTimerDurationPicker(
                    label = stringResource(R.string.rest_timer_warmup_field),
                    seconds = warmupSeconds,
                    onSecondsChange = { warmupSeconds = it.coerceIn(0, MAX_REST_TIMER_SECONDS) },
                    showNonePlaceholder = true,
                )
                Spacer(Modifier.height(12.dp))
                RestTimerDurationPicker(
                    label = stringResource(R.string.rest_timer_working_field),
                    seconds = workingSeconds,
                    onSecondsChange = { workingSeconds = it.coerceIn(0, MAX_REST_TIMER_SECONDS) },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.rest_timer_hint_none),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
                if (initialWarmupSeconds > 0 || initialWorkingSeconds > 0) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onRemove) {
                        Text(stringResource(R.string.dialog_remove_rest_timer))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(warmupSeconds, workingSeconds) }
            ) {
                Text(stringResource(R.string.dialog_confirm_rest_timer))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun RestTimerDurationPicker(
    label: String,
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    showNonePlaceholder: Boolean = false,
) {
    val colors = MaterialTheme.colors
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.subtitle2,
            color = colors.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    onSecondsChange((seconds - REST_TIMER_STEP_SECONDS).coerceAtLeast(0))
                },
                enabled = seconds > 0
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.rest_timer_decrease)
                )
            }
            Surface(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .defaultMinSize(minWidth = 96.dp)
                    .heightIn(min = 44.dp),
                shape = MaterialTheme.shapes.medium,
                color = colors.onSurface.copy(alpha = 0.05f)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val labelText = when {
                        seconds > 0 -> formatRestDuration(seconds)
                        showNonePlaceholder -> stringResource(R.string.rest_timer_none)
                        else -> formatRestDuration(seconds)
                    }
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.subtitle1,
                        color = colors.onSurface
                    )
                }
            }
            IconButton(
                onClick = {
                    onSecondsChange((seconds + REST_TIMER_STEP_SECONDS).coerceAtMost(MAX_REST_TIMER_SECONDS))
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.rest_timer_increase)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = {
                    onSecondsChange((seconds - 30).coerceAtLeast(0))
                },
                enabled = seconds > 0
            ) {
                Text(stringResource(R.string.rest_timer_minus_30))
            }
            TextButton(
                onClick = {
                    onSecondsChange((seconds + 30).coerceAtMost(MAX_REST_TIMER_SECONDS))
                }
            ) {
                Text(stringResource(R.string.rest_timer_plus_30))
            }
        }
    }
}

@Composable
fun RestTimerIconButton(
    hasRestTimers: Boolean,
    warmupSeconds: Int,
    workingSeconds: Int,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colors
    val warmupText = if (warmupSeconds > 0) {
        formatRestDuration(warmupSeconds)
    } else {
        stringResource(R.string.rest_timer_none)
    }
    val workingText = if (workingSeconds > 0) {
        formatRestDuration(workingSeconds)
    } else {
        stringResource(R.string.rest_timer_none)
    }
    val tint = if (hasRestTimers) colors.primary else colors.onSurface.copy(alpha = 0.6f)
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = stringResource(
                R.string.rest_timer_icon_description,
                warmupText,
                workingText
            ),
            tint = tint
        )
    }
}
