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

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.Exercise
import com.noahjutz.gymroutines.data.domain.Routine
import com.noahjutz.gymroutines.data.domain.RoutineSetGroupWithSets
import com.noahjutz.gymroutines.ui.components.AutoSelectTextField
import com.noahjutz.gymroutines.ui.components.EditExerciseNotesDialog
import com.noahjutz.gymroutines.ui.components.RestTimerDialog
import com.noahjutz.gymroutines.ui.components.SetTypeBadge
import com.noahjutz.gymroutines.ui.components.SwipeToDeleteBackground
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.ui.components.WarmupIndicatorWidth
import com.noahjutz.gymroutines.ui.components.durationVisualTransformation
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryRepository
import com.noahjutz.gymroutines.ui.exercises.detail.ExerciseDetailData
import com.noahjutz.gymroutines.ui.exercises.detail.ExerciseDetailDialog
import com.noahjutz.gymroutines.ui.exercises.detail.resolveExerciseDetail
import com.noahjutz.gymroutines.util.RegexPatterns
import com.noahjutz.gymroutines.util.formatRestDuration
import com.noahjutz.gymroutines.util.formatSimple
import com.noahjutz.gymroutines.util.toStringOrBlank
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.compose.get
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun RoutineEditor(
    navToExercisePicker: () -> Unit,
    navToWorkout: (Long) -> Unit,
    popBackStack: () -> Unit,
    routineId: Int,
    exerciseIdsToAdd: List<Int>,
    viewModel: RoutineEditorViewModel = getViewModel { parametersOf(routineId) },
) {
    val scaffoldState = rememberScaffoldState()

    LaunchedEffect(exerciseIdsToAdd) {
        viewModel.addExercises(exerciseIdsToAdd)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        floatingActionButton = {
            val isWorkoutRunning by viewModel.isWorkoutInProgress.collectAsState(initial = false)
            if (!isWorkoutRunning) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.startWorkout { id ->
                            navToWorkout(id)
                        }
                    },
                    icon = { Icon(Icons.Default.PlayArrow, null) },
                    text = { Text(stringResource(R.string.btn_start_workout)) },
                    backgroundColor = colors.secondary,
                    contentColor = colors.onSecondary
                )
            }
        },
        topBar = {
            TopBar(
                navigationIcon = {
                    IconButton(onClick = popBackStack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.btn_pop_back))
                    }
                },
                title = stringResource(R.string.screen_edit_routine),
            )
        }
    ) { paddingValues ->
        val routine by viewModel.routine.collectAsState(initial = null)
        Crossfade(routine != null, Modifier.padding(paddingValues)) { isReady ->
            if (!isReady) {
                RoutineEditorPlaceholder()
            } else {
                routine?.let { routine ->
                    RoutineEditorContent(
                        routine = routine.routine,
                        setGroups = routine.setGroups,
                        viewModel = viewModel,
                        navToExercisePicker = navToExercisePicker
                    )
                }
            }
        }
    }
}

@Composable
private fun RestTimerSummary(
    modifier: Modifier = Modifier,
    warmupSeconds: Int,
    workingSeconds: Int,
    onEdit: () -> Unit,
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

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = colors.primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = colors.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.rest_timer_label_warmup, warmupText),
                    style = typography.body2,
                    color = colors.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.rest_timer_label_working, workingText),
                    style = typography.body2,
                    color = colors.onSurface
                )
            }
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.rest_timer_edit))
            }
        }
    }
}

@Composable
private fun ConfirmDeleteRoutineSetDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(stringResource(R.string.dialog_title_delete, stringResource(R.string.dialog_item_set)))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                content = { Text(stringResource(R.string.btn_delete)) }
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                content = { Text(stringResource(R.string.btn_cancel)) }
            )
        },
        onDismissRequest = onDismiss,
    )
}

private data class ExerciseNotesDialogState(
    val exerciseId: Int,
    val name: String,
    val notes: String,
)


@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun RoutineEditorContent(
    routine: Routine,
    setGroups: List<RoutineSetGroupWithSets>,
    viewModel: RoutineEditorViewModel,
    navToExercisePicker: () -> Unit
) {
    val sortedSetGroups by remember(setGroups) {
        derivedStateOf { setGroups.sortedBy { it.group.position } }
    }
    var notesEditorState by remember { mutableStateOf<ExerciseNotesDialogState?>(null) }
    notesEditorState?.let { state ->
        val exerciseName = state.name.ifBlank { stringResource(R.string.unnamed_exercise) }
        EditExerciseNotesDialog(
            exerciseName = exerciseName,
            initialNotes = state.notes,
            onDismiss = { notesEditorState = null },
            onConfirm = { updatedNotes ->
                viewModel.updateExerciseNotes(state.exerciseId, updatedNotes)
                notesEditorState = null
            }
        )
    }

    var restTimerEditorGroup by remember { mutableStateOf<RoutineSetGroupWithSets?>(null) }
    restTimerEditorGroup?.let { group ->
        RestTimerDialog(
            initialWarmupSeconds = group.group.restTimerWarmupSeconds,
            initialWorkingSeconds = group.group.restTimerWorkingSeconds,
            onDismiss = { restTimerEditorGroup = null },
            onConfirm = { warmupSeconds, workingSeconds ->
                viewModel.updateRestTimers(group.group.id, warmupSeconds, workingSeconds)
                restTimerEditorGroup = null
            },
            onRemove = {
                viewModel.updateRestTimers(group.group.id, 0, 0)
                restTimerEditorGroup = null
            }
        )
    }

    val libraryRepository: ExerciseLibraryRepository = get()
    val coroutineScope = rememberCoroutineScope()
    var detailDialog by remember { mutableStateOf<ExerciseDetailData?>(null) }
    detailDialog?.let { data ->
        ExerciseDetailDialog(data = data, onDismiss = { detailDialog = null })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 64.dp)
    ) {

        item {
            val (name, setName) = remember { mutableStateOf(routine.name) }
            LaunchedEffect(name) {
                viewModel.updateName(name)
            }
            val (nameLineCount, setNameLineCount) = remember { mutableStateOf(0) }
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = setName,
                onTextLayout = { setNameLineCount(it.lineCount) },
                textStyle = typography.h6.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.onSurface),
                decorationBox = { innerTextField ->
                    Surface(
                        modifier = if (nameLineCount <= 1) Modifier.height(48.dp) else Modifier,
                        color = colors.onSurface.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            Modifier.padding(start = 20.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .padding(vertical = 10.dp)
                                    .weight(1f)
                            ) {
                                if (routine.name.isEmpty()) {
                                    Text(
                                        stringResource(R.string.unnamed_routine),
                                        style = typography.h6.copy(
                                            color = colors.onSurface.copy(alpha = 0.12f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                            AnimatedVisibility(
                                name.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Spacer(Modifier.width(4.dp))
                                IconButton(onClick = { setName("") }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        stringResource(R.string.btn_clear_text)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        items(sortedSetGroups, key = { it.group.id }) { setGroup ->
            val exerciseState = viewModel.getExercise(setGroup.group.exerciseId)
                .collectAsState(initial = null)
            val exercise = exerciseState.value ?: return@items
            val headerColor = lerp(colors.surface, colors.primary, 0.18f)
            Card(
                Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = headerColor,
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val exerciseName = if (exercise.name.isBlank()) {
                                stringResource(R.string.unnamed_exercise)
                            } else {
                                exercise.name
                            }
                            Text(
                                exerciseName,
                                style = typography.h6,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .weight(1f)
                            )

                            IconButton(onClick = {
                                coroutineScope.launch {
                                    detailDialog = resolveExerciseDetail(exercise, libraryRepository)
                                }
                            }) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = stringResource(R.string.btn_view_details)
                                )
                            }

                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(
                                    modifier = Modifier.padding(end = 6.dp),
                                    onClick = { expanded = !expanded }
                                ) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        stringResource(R.string.btn_more)
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
                                            val toId = sortedSetGroups
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
                                            val toId = sortedSetGroups
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
                    }
                    val trimmedNotes = remember(exercise.notes) { exercise.notes.trim() }
                        if (trimmedNotes.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .fillMaxWidth(),
                            color = colors.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.label_exercise_notes),
                                    tint = colors.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = trimmedNotes,
                                    style = typography.body2,
                                    color = colors.onSurface.copy(alpha = 0.9f),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        notesEditorState = ExerciseNotesDialogState(
                                            exerciseId = exercise.exerciseId,
                                            name = exercise.name,
                                            notes = exercise.notes
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.btn_edit_notes),
                                        tint = colors.primary
                                    )
                                }
                            }
                        }
                    } else {
                        TextButton(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            onClick = {
                                notesEditorState = ExerciseNotesDialogState(
                                    exerciseId = exercise.exerciseId,
                                    name = exercise.name,
                                    notes = exercise.notes
                                )
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_add_notes))
                        }
                    }
                    Column(Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
                        val warmupRest = setGroup.group.restTimerWarmupSeconds
                        val workingRest = setGroup.group.restTimerWorkingSeconds
                        val hasRestTimers = warmupRest > 0 || workingRest > 0

                        if (hasRestTimers) {
                            RestTimerSummary(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                warmupSeconds = warmupRest,
                                workingSeconds = workingRest,
                                onEdit = { restTimerEditorGroup = setGroup }
                            )
                        } else {
                            TextButton(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                onClick = { restTimerEditorGroup = setGroup }
                            ) {
                                Icon(imageVector = Icons.Default.Timer, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.rest_timer_add))
                            }
                        }

                        Divider(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = colors.onSurface.copy(alpha = 0.08f)
                        )
                        Row(Modifier.padding(horizontal = 2.dp)) {
                            val headerTextStyle = TextStyle(
                                color = colors.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Surface(
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .width(WarmupIndicatorWidth),
                            ) {
                                Text(
                                    stringResource(R.string.column_set),
                                    style = headerTextStyle
                                )
                            }
                            if (exercise.logReps) Surface(
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .weight(1f),
                            ) {
                                Text(
                                    stringResource(R.string.column_reps),
                                    style = headerTextStyle
                                )
                            }
                            if (exercise.logWeight) Surface(
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .weight(1f),
                            ) {
                                Text(
                                    stringResource(R.string.column_weight),
                                    style = headerTextStyle
                                )
                            }
                            if (exercise.logTime) Surface(
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .weight(1f),
                            ) {
                                Text(
                                    stringResource(R.string.column_time),
                                    style = headerTextStyle
                                )
                            }
                            if (exercise.logDistance) Surface(
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .weight(1f),
                            ) {
                                Text(
                                    stringResource(R.string.column_distance),
                                    style = headerTextStyle
                                )
                            }
                        }
                        var workingSetIndex = 0
                        setGroup.sets.forEachIndexed { index, set ->
                            key(set.routineSetId) {
                                val dismissState = rememberDismissState()
                                val scope = rememberCoroutineScope()
                                SwipeToDismiss(
                                    state = dismissState,
                                    background = { SwipeToDeleteBackground(dismissState) },
                                ) {
                                    Surface {
                                        Row(
                                            Modifier.padding(horizontal = 2.dp)
                                        ) {
                                            SetTypeBadge(
                                                isWarmup = set.isWarmup,
                                                index = if (set.isWarmup) workingSetIndex else workingSetIndex++,
                                                modifier = Modifier
                                                    .padding(3.dp)
                                                    .width(WarmupIndicatorWidth),
                                                onToggle = {
                                                    val makeWarmup = !set.isWarmup
                                                    if (!makeWarmup || setGroup.sets.take(index).all { it.isWarmup }) {
                                                        viewModel.updateWarmup(set, makeWarmup)
                                                    }
                                                }
                                            )
                                            val textFieldStyle = typography.body1.copy(
                                                textAlign = TextAlign.Center,
                                                color = colors.onSurface
                                            )
                                            val decorationBox: @Composable (@Composable () -> Unit) -> Unit =
                                                { innerTextField ->
                                                    Surface(
                                                        color = colors.onSurface.copy(alpha = 0.08f),
                                                        shape = RoundedCornerShape(10.dp),
                                                    ) {
                                                        Box(
                                                            Modifier.padding(
                                                                vertical = 10.dp,
                                                                horizontal = 6.dp
                                                            ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            innerTextField()
                                                        }
                                                    }
                                                }
                                            if (exercise.logReps) {
                                                val (reps, setReps) = remember { mutableStateOf(set.reps.toStringOrBlank()) }
                                                LaunchedEffect(reps) {
                                                    val repsInt = reps.toIntOrNull()
                                                    viewModel.updateReps(set, repsInt)
                                                }
                                                AutoSelectTextField(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(3.dp),
                                                    value = reps,
                                                    onValueChange = {
                                                        if (it.matches(RegexPatterns.integer))
                                                            setReps(it)
                                                    },
                                                    textStyle = textFieldStyle,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    cursorColor = colors.onSurface,
                                                    decorationBox = decorationBox,
                                                )
                                            }
                                            if (exercise.logWeight) {
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
                                                        .padding(3.dp),
                                                    value = weight,
                                                    onValueChange = {
                                                        if (it.matches(RegexPatterns.float))
                                                            setWeight(it)
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    textStyle = textFieldStyle,
                                                    cursorColor = colors.onSurface,
                                                    decorationBox = decorationBox
                                                )
                                            }
                                            if (exercise.logTime) {
                                                val (time, setTime) = remember { mutableStateOf(set.time.toStringOrBlank()) }
                                                LaunchedEffect(time) {
                                                    val timeInt = time.toIntOrNull()
                                                    viewModel.updateTime(set, timeInt)
                                                }
                                                AutoSelectTextField(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(3.dp),
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
                                                    decorationBox = decorationBox
                                                )
                                            }
                                            if (exercise.logDistance) {
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
                                                        .padding(3.dp),
                                                    value = distance,
                                                    onValueChange = {
                                                        if (it.matches(RegexPatterns.float))
                                                            setDistance(it)
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    textStyle = textFieldStyle,
                                                    cursorColor = colors.onSurface,
                                                    decorationBox = decorationBox
                                                )
                                            }
                                        }
                                    }
                                }
                                if (dismissState.targetValue != DismissValue.Default) {
                                    ConfirmDeleteRoutineSetDialog(
                                        onDismiss = { scope.launch { dismissState.reset() } },
                                        onConfirm = {
                                            viewModel.deleteSet(set)
                                            scope.launch { dismissState.reset() }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = { viewModel.addSet(setGroup) },
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_add_set))
                    }
                }
            }
        }

        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(72.dp),
                shape = RoundedCornerShape(24.dp),
                onClick = navToExercisePicker
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_add_exercise))
            }
        }
    }
}
