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

package com.noahjutz.gymroutines.ui.routines.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.AppPrefs
import com.noahjutz.gymroutines.ui.components.SetGroupCard
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.ui.exercises.picker.ExercisePickerSheet
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun CreateRoutineScreen(
    startWorkout: (Int) -> Unit,
    popBackStack: () -> Unit,
    routineId: Int,
    viewModel: RoutineEditorViewModel = getViewModel { parametersOf(routineId) },
    preferences: DataStore<Preferences> = get(),
    navToExerciseEditor: () -> Unit,
) {
    val preferencesData by preferences.data.collectAsState(null)
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    BackHandler(enabled = sheetState.isVisible) {
        scope.launch {
            sheetState.hide()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        sheetContent = {
            ExercisePickerSheet(
                onExercisesSelected = {
                    scope.launch {
                        viewModel.editor.addExercises(it)
                        sheetState.hide()
                    }
                },
                navToExerciseEditor = navToExerciseEditor
            )
        },
        sheetElevation = 0.dp,
    ) {
        Scaffold(
            scaffoldState = scaffoldState,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        val currentWorkout =
                            preferencesData?.get(AppPrefs.CurrentWorkout.key)
                        if (currentWorkout == null || currentWorkout < 0) {
                            startWorkout(viewModel.presenter.routine.value.routineId)
                        } else {
                            scope.launch {
                                scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                                val snackbarResult =
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        "A workout is already in progress.",
                                        "Stop current"
                                    )
                                if (snackbarResult == SnackbarResult.ActionPerformed) {
                                    preferences.edit {
                                        it[AppPrefs.CurrentWorkout.key] = -1
                                    }
                                    scaffoldState.snackbarHostState.showSnackbar("Current workout finished.")
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.PlayArrow, null) },
                    text = { Text("Start Workout") },
                )
            },
            topBar = {
                TopBar(
                    navigationIcon = {
                        IconButton(onClick = popBackStack) {
                            Icon(Icons.Default.ArrowBack, stringResource(R.string.pop_back))
                        }
                    },
                    title = "Edit Routine",
                )
            }
        ) {
            val routine by viewModel.presenter.routine.collectAsState()
            LazyColumn(Modifier.fillMaxHeight(), contentPadding = PaddingValues(bottom = 70.dp)) {

                item {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        value = routine.name,
                        onValueChange = viewModel.editor::setName,
                        label = { Text("Routine Name") },
                        placeholder = { Text(stringResource(R.string.unnamed_routine)) },
                        singleLine = true,
                    )
                }

                itemsIndexed(routine.sets) { setGroupIndex, setGroup ->
                    val exercise = viewModel.presenter.getExercise(setGroup.exerciseId)!!
                    SetGroupCard(
                        name = exercise.name.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.unnamed_exercise),
                        sets = setGroup.sets,
                        onMoveDown = {
                            viewModel.editor.swapSetGroups(
                                setGroupIndex,
                                setGroupIndex + 1
                            )
                        },
                        onMoveUp = {
                            viewModel.editor.swapSetGroups(
                                setGroupIndex,
                                setGroupIndex - 1
                            )
                        },
                        onAddSet = { viewModel.editor.addSetTo(setGroup) },
                        onDeleteSet = { viewModel.editor.deleteSetFrom(setGroup, it) },
                        logReps = exercise.logReps,
                        logWeight = exercise.logWeight,
                        logTime = exercise.logTime,
                        logDistance = exercise.logDistance,
                        showCheckbox = false,
                        onDistanceChange = { setIndex, distance ->
                            viewModel.editor.updateSet(
                                setGroupIndex, setIndex,
                                distance = distance.toDoubleOrNull()
                            )
                        },
                        onRepsChange = { setIndex, reps ->
                            viewModel.editor.updateSet(
                                setGroupIndex, setIndex,
                                reps = reps.toIntOrNull()
                            )
                        },
                        onTimeChange = { setIndex, time ->
                            viewModel.editor.updateSet(
                                setGroupIndex, setIndex,
                                time = time.toIntOrNull()
                            )
                        },
                        onWeightChange = { setIndex, weight ->
                            viewModel.editor.updateSet(
                                setGroupIndex, setIndex,
                                weight = weight.toDoubleOrNull()
                            )
                        }
                    )
                }

                item {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
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
                }
            }
        }
    }
}
