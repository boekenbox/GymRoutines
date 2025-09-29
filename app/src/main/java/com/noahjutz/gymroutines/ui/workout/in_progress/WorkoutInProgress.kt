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

package com.noahjutz.gymroutines.ui.workout.in_progress

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DismissValue
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Surface
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberDismissState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.WorkoutSet
import com.noahjutz.gymroutines.data.domain.WorkoutSetGroupWithSets
import com.noahjutz.gymroutines.data.domain.WorkoutWithSetGroups
import com.noahjutz.gymroutines.data.domain.duration
import com.noahjutz.gymroutines.ui.components.AutoSelectTextField
import com.noahjutz.gymroutines.ui.components.SwipeToDeleteBackground
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.ui.components.durationVisualTransformation
import com.noahjutz.gymroutines.util.RegexPatterns
import com.noahjutz.gymroutines.util.formatSimple
import com.noahjutz.gymroutines.util.toStringOrBlank
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf
import java.util.Locale
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun WorkoutInProgress(
    navToExercisePicker: () -> Unit,
    navToWorkoutCompleted: (Int, Int) -> Unit,
    popBackStack: () -> Unit,
    workoutId: Int,
    exerciseIdsToAdd: List<Int>,
    viewModel: WorkoutInProgressViewModel = getViewModel { parametersOf(workoutId) },
) {
    LaunchedEffect(exerciseIdsToAdd) {
        viewModel.addExercises(exerciseIdsToAdd)
    }

    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_perform_workout),
                navigationIcon = {
                    IconButton(onClick = popBackStack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
    ) { paddingValues ->
        val workout by viewModel.workout.collectAsState(initial = null)

        Crossfade(workout == null, Modifier.padding(paddingValues)) { isNull ->
            if (isNull) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                workout?.let { workout ->
                    WorkoutInProgressContent(
                        workout = workout,
                        viewModel = viewModel,
                        popBackStack = popBackStack,
                        navToExercisePicker = navToExercisePicker,
                        navToWorkoutCompleted = navToWorkoutCompleted,
                        scaffoldState = scaffoldState,
                    )
                }
            }
        }
    }
}
@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun WorkoutInProgressContent(
    workout: WorkoutWithSetGroups,
    viewModel: WorkoutInProgressViewModel,
    popBackStack: () -> Unit,
    navToExercisePicker: () -> Unit,
    navToWorkoutCompleted: (Int, Int) -> Unit,
    scaffoldState: ScaffoldState,
) {
    var showFinishWorkoutDialog by remember { mutableStateOf(false) }
    if (showFinishWorkoutDialog) FinishWorkoutDialog(
        onDismiss = { showFinishWorkoutDialog = false },
        finishWorkout = {
            viewModel.finishWorkout {
                navToWorkoutCompleted(workout.workout.workoutId, workout.workout.routineId)
            }
        }
    )

    var showCancelWorkoutDialog by remember { mutableStateOf(false) }
    if (showCancelWorkoutDialog) CancelWorkoutDialog(
        onDismiss = { showCancelWorkoutDialog = false },
        cancelWorkout = {
            viewModel.cancelWorkout(popBackStack)
        },
        confirmButtonBackground = colors.error,
    )

    val coroutineScope = rememberCoroutineScope()
    var setPendingDeletion by remember { mutableStateOf<WorkoutSet?>(null) }
    var pendingSetGroup by remember { mutableStateOf<WorkoutSetGroupWithSets?>(null) }
    val deleteMessage = stringResource(R.string.snackbar_set_removed)
    val undoLabel = stringResource(R.string.action_undo)

    if (setPendingDeletion != null) {
        ConfirmDeleteSetDialog(
            onDismiss = {
                setPendingDeletion = null
                pendingSetGroup = null
            },
            onConfirm = {
                val setToDelete = setPendingDeletion
                val groupToRestore = pendingSetGroup?.group
                if (setToDelete != null) {
                    viewModel.deleteSet(setToDelete)
                    coroutineScope.launch {
                        val result = scaffoldState.snackbarHostState.showSnackbar(
                            message = deleteMessage,
                            actionLabel = undoLabel,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreSet(setToDelete, groupToRestore)
                        }
                    }
                }
                setPendingDeletion = null
                pendingSetGroup = null
            },
            confirmButtonBackground = colors.error,
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                color = colors.surface,
                shape = RoundedCornerShape(16.dp)
            ) {
                val routineName by viewModel.routineName.collectAsState("")
                Text(
                    text = routineName,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    style = typography.h5,
                )
            }
            val duration = workout.workout.duration
            val totalSeconds = duration.inWholeSeconds
            if (totalSeconds > 0) {
                val minutes = (totalSeconds / 60).toInt()
                val seconds = (totalSeconds % 60).toInt()
                Text(
                    String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    style = typography.body2.copy(
                        textAlign = TextAlign.Center,
                        color = colors.onSurface.copy(alpha = 0.7f),
                    )
                )
            }
        }

        items(workout.setGroups.sortedBy { it.group.position }, key = { it.group.id }) { setGroup ->
            val exercise by viewModel.getExercise(setGroup.group.exerciseId)
                .collectAsState(initial = null)
            Card(
                Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val exerciseName = exercise?.name.orEmpty()
                        Text(
                            exerciseName,
                            style = typography.h6,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = exerciseName }
                                .padding(end = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    Icons.Default.DragHandle,
                                    stringResource(R.string.drag_handle)
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        expanded = false
                                        val id = setGroup.group.id
                                        val toId = workout.setGroups
                                            .find { it.group.position == setGroup.group.position - 1 }
                                            ?.group
                                            ?.id
                                        if (toId != null) {
                                            viewModel.swapSetGroups(id, toId)
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.btn_move_up))
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        expanded = false
                                        val id = setGroup.group.id
                                        val toId = workout.setGroups
                                            .find { it.group.position == setGroup.group.position + 1 }
                                            ?.group
                                            ?.id
                                        if (toId != null) {
                                            viewModel.swapSetGroups(id, toId)
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.btn_move_down))
                                }
                            }
                        }
                    }
                    Divider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val headerTextStyle = TextStyle(
                            color = colors.onSurface.copy(alpha = 0.72f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End
                        )
                        if (exercise?.logReps == true) {
                            Text(
                                stringResource(R.string.column_reps),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                style = headerTextStyle
                            )
                        }
                        if (exercise?.logWeight == true) {
                            Text(
                                stringResource(R.string.column_weight_with_unit),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                style = headerTextStyle
                            )
                        }
                        if (exercise?.logTime == true) {
                            Text(
                                stringResource(R.string.column_time),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                style = headerTextStyle
                            )
                        }
                        if (exercise?.logDistance == true) {
                            Text(
                                stringResource(R.string.column_distance),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                style = headerTextStyle
                            )
                        }
                        Box(
                            modifier = Modifier
                                .widthIn(min = 48.dp)
                                .padding(start = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                stringResource(R.string.column_set_complete),
                                tint = colors.onSurface.copy(alpha = 0.72f)
                            )
                        }
                    }
                    Divider()
                    Column {
                        val textFieldStyle = typography.body1.copy(
                            textAlign = TextAlign.End,
                            color = colors.onSurface,
                            fontFeatureSettings = "tnum"
                        )
                        val cellDecoration: @Composable (@Composable () -> Unit) -> Unit = { innerTextField ->
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .heightIn(min = 48.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                innerTextField()
                            }
                        }
                        setGroup.sets.forEachIndexed { index, set ->
                            key(set.workoutSetId) {
                                val dismissState = rememberDismissState(
                                    confirmStateChange = { value ->
                                        if (value != DismissValue.Default) {
                                            // Ask for confirmation before removing the set via swipe.
                                            setPendingDeletion = set
                                            pendingSetGroup = setGroup
                                            false
                                        } else {
                                            true
                                        }
                                    }
                                )
                                SwipeToDismiss(
                                    state = dismissState,
                                    background = { SwipeToDeleteBackground(dismissState) },
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (exercise?.logReps == true) {
                                            val (reps, setReps) = remember { mutableStateOf(set.reps.toStringOrBlank()) }
                                            LaunchedEffect(reps) {
                                                val repsInt = reps.toIntOrNull()
                                                viewModel.updateReps(set, repsInt)
                                            }
                                            AutoSelectTextField(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 8.dp)
                                                    .heightIn(min = 48.dp),
                                                value = reps,
                                                onValueChange = {
                                                    if (it.matches(RegexPatterns.integer))
                                                        setReps(it)
                                                },
                                                textStyle = textFieldStyle,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                cursorColor = colors.onSurface,
                                                decorationBox = cellDecoration,
                                            )
                                        }
                                        if (exercise?.logWeight == true) {
                                            val (weight, setWeight) = remember {
                                                mutableStateOf(
                                                    set.weight.formatSimple()
                                                )
                                            }
                                            LaunchedEffect(weight) {
                                                val weightDouble = weight.toDoubleOrNull()
                                                viewModel.updateWeight(set, weightDouble)
                                            }
                                            AutoSelectTextField(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 8.dp)
                                                    .heightIn(min = 48.dp),
                                                value = weight,
                                                onValueChange = {
                                                    if (it.matches(RegexPatterns.float))
                                                        setWeight(it)
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                textStyle = textFieldStyle,
                                                cursorColor = colors.onSurface,
                                                decorationBox = cellDecoration
                                            )
                                        }
                                        if (exercise?.logTime == true) {
                                            val (time, setTime) = remember { mutableStateOf(set.time.toStringOrBlank()) }
                                            LaunchedEffect(time) {
                                                val timeInt = time.toIntOrNull()
                                                viewModel.updateTime(set, timeInt)
                                            }
                                            AutoSelectTextField(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 8.dp)
                                                    .heightIn(min = 48.dp),
                                                value = time,
                                                onValueChange = {
                                                    if (it.matches(RegexPatterns.duration))
                                                        setTime(it)
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                textStyle = textFieldStyle,
                                                visualTransformation = durationVisualTransformation,
                                                cursorColor = colors.onSurface,
                                                decorationBox = cellDecoration
                                            )
                                        }
                                        if (exercise?.logDistance == true) {
                                            val (distance, setDistance) = remember {
                                                mutableStateOf(
                                                    set.distance.formatSimple()
                                                )
                                            }
                                            LaunchedEffect(distance) {
                                                val distanceDouble = distance.toDoubleOrNull()
                                                viewModel.updateDistance(set, distanceDouble)
                                            }
                                            AutoSelectTextField(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 8.dp)
                                                    .heightIn(min = 48.dp),
                                                value = distance,
                                                onValueChange = {
                                                    if (it.matches(RegexPatterns.float))
                                                        setDistance(it)
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                textStyle = textFieldStyle,
                                                cursorColor = colors.onSurface,
                                                decorationBox = cellDecoration
                                            )
                                        }
                                        val toggleColor by animateColorAsState(
                                            targetValue = if (set.complete) colors.secondary else colors.onSurface.copy(
                                                alpha = 0.08f
                                            ),
                                            label = "SetCompleteToggle"
                                        )
                                        Box(
                                            Modifier
                                                .padding(start = 8.dp)
                                                .widthIn(min = 48.dp)
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .toggleable(
                                                    value = set.complete,
                                                    onValueChange = {
                                                        viewModel.updateChecked(set, it)
                                                    },
                                                )
                                                .background(toggleColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = set.complete,
                                                enter = fadeIn(),
                                                exit = fadeOut()
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    stringResource(R.string.column_set_complete),
                                                    tint = colors.onSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (index < setGroup.sets.lastIndex) {
                                Divider(Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                    TextButton(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .defaultMinSize(minHeight = 48.dp),
                        onClick = { viewModel.addSet(setGroup) },
                    ) {
                        Icon(Icons.Default.Add, stringResource(R.string.btn_add_set))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_add_set))
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    onClick = navToExercisePicker
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.btn_add_exercise))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_add_exercise))
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(percent = 100),
                        onClick = { showCancelWorkoutDialog = true },
                    ) {
                        Text(stringResource(R.string.btn_discard_workout))
                    }
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(percent = 100),
                        onClick = { showFinishWorkoutDialog = true }
                    ) {
                        Text(stringResource(R.string.btn_finish_workout))
                    }
                }
            }
        }
    }
}


@Composable
private fun ConfirmDeleteSetDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmButtonBackground: Color? = null,
) {
    val confirmButtonColors = confirmButtonBackground?.let { background ->
        ButtonDefaults.buttonColors(backgroundColor = background)
    } ?: ButtonDefaults.buttonColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_delete_set)) },
        text = { Text(stringResource(R.string.dialog_body_delete_set)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = confirmButtonColors,
            ) {
                Text(stringResource(R.string.dialog_confirm_delete_set))
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
private fun CancelWorkoutDialog(
    onDismiss: () -> Unit,
    cancelWorkout: () -> Unit,
    confirmButtonBackground: Color? = null,
) {
    val confirmButtonColors = confirmButtonBackground?.let { background ->
        ButtonDefaults.buttonColors(backgroundColor = background)
    } ?: ButtonDefaults.buttonColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_discard_workout)) },
        confirmButton = {
            Button(
                onClick = cancelWorkout,
                colors = confirmButtonColors,
            ) {
                Text(stringResource(R.string.btn_delete))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } },
    )
}

@Composable
private fun FinishWorkoutDialog(
    onDismiss: () -> Unit,
    finishWorkout: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_finish_workout)) },
        confirmButton = { Button(onClick = finishWorkout) { Text(stringResource(R.string.dialog_confirm_finish_workout)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } },
    )
}