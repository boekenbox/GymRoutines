package com.noahjutz.gymroutines.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.util.formatRestDuration

private const val MAX_REST_SECONDS = 99 * 60 + 59

@Composable
fun RestTimerDialog(
    initialWarmupSeconds: Int,
    initialWorkingSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    var warmupSeconds by rememberSaveable(initialWarmupSeconds) {
        mutableStateOf(initialWarmupSeconds.coerceIn(0, MAX_REST_SECONDS))
    }
    var workingSeconds by rememberSaveable(initialWorkingSeconds) {
        mutableStateOf(initialWorkingSeconds.coerceIn(0, MAX_REST_SECONDS))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_rest_timer)) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_body_rest_timer))
                Spacer(Modifier.height(16.dp))
                RestTimerValueAdjuster(
                    label = stringResource(R.string.rest_timer_warmup_field),
                    seconds = warmupSeconds,
                    onSecondsChange = { warmupSeconds = it.coerceIn(0, MAX_REST_SECONDS) }
                )
                Spacer(Modifier.height(12.dp))
                RestTimerValueAdjuster(
                    label = stringResource(R.string.rest_timer_working_field),
                    seconds = workingSeconds,
                    onSecondsChange = { workingSeconds = it.coerceIn(0, MAX_REST_SECONDS) }
                )
                if (onRemove != null && (initialWarmupSeconds > 0 || initialWorkingSeconds > 0)) {
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
private fun RestTimerValueAdjuster(
    label: String,
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colors.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onSecondsChange((seconds - 5).coerceAtLeast(0)) },
                    enabled = seconds > 0
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                }
                Text(
                    text = formatRestDuration(seconds),
                    modifier = Modifier.widthIn(min = 72.dp),
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = { onSecondsChange((seconds + 5).coerceAtMost(MAX_REST_SECONDS)) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RestTimerQuickAdjustButton(
                label = stringResource(R.string.rest_timer_minus_30),
                enabled = seconds > 0,
                onClick = { onSecondsChange((seconds - 30).coerceAtLeast(0)) }
            )
            RestTimerQuickAdjustButton(
                label = stringResource(R.string.rest_timer_plus_30),
                onClick = { onSecondsChange((seconds + 30).coerceAtMost(MAX_REST_SECONDS)) }
            )
            RestTimerQuickAdjustButton(
                label = stringResource(R.string.rest_timer_plus_minute),
                onClick = { onSecondsChange((seconds + 60).coerceAtMost(MAX_REST_SECONDS)) }
            )
        }
    }
}

@Composable
private fun RestTimerQuickAdjustButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label)
    }
}
