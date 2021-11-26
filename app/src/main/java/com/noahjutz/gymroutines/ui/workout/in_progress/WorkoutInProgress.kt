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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.ui.components.NormalDialog
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.ui.exercises.picker.ExercisePickerSheet
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun WorkoutInProgress(
    navToExerciseEditor: () -> Unit,
    navToCompleted: (routineId: Int, workoutId: Int) -> Unit,
    popBackStack: () -> Unit,
    workoutId: Int,
    routineId: Int,
    viewModel: WorkoutInProgressViewModel = getViewModel { parametersOf(workoutId, routineId) },
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    val workout by viewModel.presenter.workout.collectAsState()

    BackHandler(enabled = sheetState.isVisible) {
        scope.launch {
            sheetState.hide()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        sheetElevation = 0.dp,
        sheetContent = {
            ExercisePickerSheet(
                onExercisesSelected = {
                    scope.launch {
                        // viewModel.editor.addExercises(it)
                        sheetState.hide()
                    }
                },
                navToExerciseEditor = navToExerciseEditor
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    title = workout.workout.name,
                    navigationIcon = {
                        IconButton(onClick = popBackStack) { Icon(Icons.Default.ArrowBack, null) }
                    }
                )
            },
        ) {
            var showFinishWorkoutDialog by remember { mutableStateOf(false) }
            if (showFinishWorkoutDialog) FinishWorkoutDialog(
                onDismiss = {
                    showFinishWorkoutDialog = false
                },
                finishWorkout = {
                    scope.launch {
                        viewModel.editor.finishWorkout()
                    }.invokeOnCompletion {
                        navToCompleted(
                            routineId,
                            workout.workout.workoutId
                        ) // TODO not always passed to WorkoutInProgress
                    }
                }
            )
            var showCancelWorkoutDialog by remember { mutableStateOf(false) }
            if (showCancelWorkoutDialog) CancelWorkoutDialog(
                onDismiss = {
                    showCancelWorkoutDialog = false
                },
                cancelWorkout = {
                    scope.launch {
                        viewModel.editor.cancelWorkout()
                        popBackStack()
                    }
                }
            )

            LazyColumn(Modifier.fillMaxHeight()) {
                item {
                    val duration by viewModel.presenter.durationString.collectAsState("00:00:00")
                    Text(
                        text = duration,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    Divider()
                    Spacer(Modifier.height(8.dp))
                }

                // TODO
                // items(workout.sets.groupBy { it.exerciseId }.toList()) { (exerciseId, sets) ->
                //    val exercise = viewModel.presenter.getExercise(exerciseId)!!
                //    SetGroupCard(
                //        name = exercise.name.takeIf { it.isNotBlank() }
                //            ?: stringResource(R.string.unnamed_exercise),
                //        sets = sets.map { (workoutId, exerciseId, position, reps, weight, time, distance, complete, setId) ->
                //            ExerciseSetLegacy(
                //                exerciseId,
                //                reps,
                //                weight,
                //                time,
                //                distance,
                //                complete,
                //                position,
                //                setId,
                //            )
                //        },
                //        onMoveDown = { },
                //        onMoveUp = { },
                //        onAddSet = { },
                //        onDeleteSet = { },
                //        logReps = exercise.logReps,
                //        logWeight = exercise.logWeight,
                //        logTime = exercise.logTime,
                //        logDistance = exercise.logDistance,
                //        showCheckbox = true,
                //        onWeightChange = { _, _ -> },
                //        onTimeChange = { _, _ -> },
                //        onRepsChange = { _, _ -> },
                //        onDistanceChange = { _, _ -> },
                //        onCheckboxChange = { _, _ -> }
                //    )
                // }

                item {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .height(120.dp),
                        onClick = {
                            scope.launch {
                                sheetState.show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Add Exercise")
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { showCancelWorkoutDialog = true },
                        ) {
                            Icon(Icons.Default.Delete, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Workout")
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showFinishWorkoutDialog = true }
                        ) {
                            Icon(Icons.Default.Done, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Finish Workout")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CancelWorkoutDialog(
    onDismiss: () -> Unit,
    cancelWorkout: () -> Unit,
) {
    NormalDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Workout?") },
        text = { Text("Do you really want to delete this workout?") },
        confirmButton = { Button(onClick = cancelWorkout) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun FinishWorkoutDialog(
    onDismiss: () -> Unit,
    finishWorkout: () -> Unit,
) {
    NormalDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finish Workout?") },
        text = { Text("Do you want to finish the workout?") },
        confirmButton = { Button(onClick = finishWorkout) { Text("Finish") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
