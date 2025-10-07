package com.noahjutz.gymroutines.ui.components

import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextFieldValue.Companion.Saver
import androidx.compose.ui.text.style.TextAlign
import com.noahjutz.gymroutines.R

@Composable
fun EditExerciseNotesDialog(
    exerciseName: String,
    initialNotes: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var notesValue by rememberSaveable(exerciseName, stateSaver = Saver) {
        mutableStateOf(TextFieldValue(initialNotes))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_title_edit_notes, exerciseName),
                textAlign = TextAlign.Start,
            )
        },
        text = {
            OutlinedTextField(
                value = notesValue,
                onValueChange = { notesValue = it },
                label = { Text(stringResource(R.string.label_exercise_notes)) },
                singleLine = false,
                maxLines = 6,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(notesValue.text)
                }
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
