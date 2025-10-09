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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DismissValue
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.WorkoutSet
import com.noahjutz.gymroutines.data.domain.WorkoutSetGroupWithSets
import com.noahjutz.gymroutines.data.domain.WorkoutWithSetGroups
import com.noahjutz.gymroutines.data.domain.duration
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryRepository
import com.noahjutz.gymroutines.ui.components.AutoSelectTextField
import com.noahjutz.gymroutines.ui.components.EditExerciseNotesDialog
import com.noahjutz.gymroutines.ui.components.RestTimerDialog
import com.noahjutz.gymroutines.ui.components.SetTypeBadge
import com.noahjutz.gymroutines.ui.components.SwipeToDeleteBackground
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.ui.components.durationVisualTransformation
import com.noahjutz.gymroutines.ui.components.WarmupIndicatorWidth
import com.noahjutz.gymroutines.ui.exercises.detail.ExerciseDetailData
import com.noahjutz.gymroutines.ui.exercises.detail.ExerciseDetailDialog
import com.noahjutz.gymroutines.ui.exercises.detail.resolveExerciseDetail
import com.noahjutz.gymroutines.util.RegexPatterns
import com.noahjutz.gymroutines.util.formatRestDuration
import com.noahjutz.gymroutines.util.formatSimple
import com.noahjutz.gymroutines.util.pretty
import com.noahjutz.gymroutines.util.toStringOrBlank
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.compose.get
import org.koin.core.parameter.parametersOf
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

    Scaffold(
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
                    )
                }
            }
        }
    }
}



private data class ExerciseNotesDialogState(
    val exerciseId: Int,
    val name: String,
    val notes: String,
)

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
        }
    )

    var pendingSetTypeChange by remember { mutableStateOf<Pair<WorkoutSet, Boolean>?>(null) }
    pendingSetTypeChange?.let { (set, makeWarmup) ->
        SetTypeChangeDialog(
            isWarmup = makeWarmup,
            onConfirm = {
                viewModel.updateWarmup(set, makeWarmup)
                pendingSetTypeChange = null
            },
            onDismiss = { pendingSetTypeChange = null }
        )
    }

    val restTimerState by viewModel.restTimerState.collectAsState()
    var pendingSetDeletion by remember { mutableStateOf<WorkoutSet?>(null) }
    pendingSetDeletion?.let { set ->
        ConfirmDeleteWorkoutSetDialog(
            onDismiss = { pendingSetDeletion = null },
            onConfirm = {
                viewModel.deleteSet(set)
                pendingSetDeletion = null
            }
        )
    }

    var restTimerEditorGroup by remember { mutableStateOf<WorkoutSetGroupWithSets?>(null) }
    restTimerEditorGroup?.let { group ->
        RestTimerDialog(
            initialWarmupSeconds = group.group.restTimerWarmupSeconds,
            initialWorkingSeconds = group.group.restTimerWorkingSeconds,
            onDismiss = { restTimerEditorGroup = null },
            onConfirm = { warmupSeconds, workingSeconds ->
                viewModel.updateRestTimers(group.group.id, warmupSeconds, workingSeconds)
                restTimerEditorGroup = null
            },
            onRemove = if (group.group.restTimerWarmupSeconds > 0 || group.group.restTimerWorkingSeconds > 0) {
                {
                    viewModel.updateRestTimers(group.group.id, 0, 0)
                    restTimerEditorGroup = null
                }
            } else null
        )
    }

    val libraryRepository: ExerciseLibraryRepository = get()
    val coroutineScope = rememberCoroutineScope()
    var detailDialog by remember { mutableStateOf<ExerciseDetailData?>(null) }
    detailDialog?.let { data ->
        ExerciseDetailDialog(data = data, onDismiss = { detailDialog = null })
    }

    val sortedSetGroups by remember(workout.setGroups) {
        derivedStateOf { workout.setGroups.sortedBy { it.group.position } }
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

    val backgroundColor = colors.background
    val primaryColor = colors.primary
    val headerBrush = remember(backgroundColor, primaryColor) {
        Brush.verticalGradient(
            listOf(
                backgroundColor,
                lerp(backgroundColor, primaryColor.copy(alpha = 0.08f), 0.9f)
            )
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(headerBrush)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            item {
                val routineName by viewModel.routineName.collectAsState("")
                val totalExercises = remember(sortedSetGroups) { sortedSetGroups.size }
                val totalSets = remember(sortedSetGroups) { sortedSetGroups.sumOf { it.sets.size } }
                val completedSets = remember(sortedSetGroups) {
                    sortedSetGroups.sumOf { setGroup -> setGroup.sets.count { it.complete } }
                }
                WorkoutInProgressHeader(
                    routineName = routineName,
                    duration = workout.workout.duration.pretty(),
                    totalExercises = totalExercises,
                    totalSets = totalSets,
                    completedSets = completedSets
                )
            }

        items(sortedSetGroups, key = { it.group.id }) { setGroup ->
            val exerciseState = viewModel.getExercise(setGroup.group.exerciseId)
                .collectAsState(initial = null)
            val exercise = exerciseState.value
            val groupPrimary = colors.primary
            val groupPrimaryVariant = colors.primaryVariant
            val headerBrush = remember(groupPrimary, groupPrimaryVariant) {
                Brush.horizontalGradient(
                    listOf(groupPrimary, groupPrimaryVariant)
                )
            }
            val headerShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            Card(
                Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .padding(top = 14.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBrush, headerShape)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val exerciseName = when {
                                exercise?.name.isNullOrBlank() -> stringResource(R.string.unnamed_exercise)
                                else -> exercise!!.name
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    exerciseName,
                                    style = typography.h6.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colors.onPrimary
                                    )
                                )
                                exercise?.libraryNotes?.takeIf { it.isNotBlank() }?.let { libraryNotes ->
                                    Text(
                                        text = libraryNotes,
                                        style = typography.caption.copy(color = MaterialTheme.colors.onPrimary.copy(alpha = 0.8f)),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    exercise?.let { current ->
                                        coroutineScope.launch {
                                            detailDialog = resolveExerciseDetail(current, libraryRepository)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = stringResource(R.string.btn_view_details),
                                    tint = MaterialTheme.colors.onPrimary
                                )
                            }

                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        stringResource(R.string.drag_handle),
                                        tint = MaterialTheme.colors.onPrimary
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

                    if (exercise != null) {
                        val userNotes = remember(exercise.notes, exercise.libraryNotes) {
                            if (exercise.notes == exercise.libraryNotes) "" else exercise.notes
                        }
                        val trimmedNotes = remember(userNotes) { userNotes.trim() }
                        val hasRestTimers = setGroup.group.restTimerWarmupSeconds > 0 ||
                            setGroup.group.restTimerWorkingSeconds > 0
                        val timerTint = if (hasRestTimers) colors.primary else colors.onSurface.copy(alpha = 0.6f)
                        val actionBackground = colors.onSurface.copy(alpha = 0.06f)
                        if (trimmedNotes.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .fillMaxWidth(),
                                color = colors.onSurface.copy(alpha = 0.04f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = trimmedNotes,
                                        style = typography.body2,
                                        color = colors.onSurface.copy(alpha = 0.9f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    RoundedIconButton(
                                        onClick = { restTimerEditorGroup = setGroup },
                                        icon = Icons.Default.Timer,
                                        contentDescription = stringResource(R.string.rest_timer_edit),
                                        tint = timerTint,
                                        backgroundColor = actionBackground
                                    )
                                    RoundedIconButton(
                                        onClick = {
                                            notesEditorState = ExerciseNotesDialogState(
                                                exerciseId = exercise.exerciseId,
                                                name = exercise.name,
                                                notes = userNotes
                                            )
                                        },
                                        icon = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.btn_edit_notes),
                                        tint = colors.primary,
                                        backgroundColor = actionBackground
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        notesEditorState = ExerciseNotesDialogState(
                                            exerciseId = exercise.exerciseId,
                                            name = exercise.name,
                                            notes = userNotes
                                        )
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.4f)),
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = colors.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.btn_add_notes), color = colors.primary)
                                }
                                Spacer(Modifier.width(8.dp))
                                RoundedIconButton(
                                    onClick = { restTimerEditorGroup = setGroup },
                                    icon = Icons.Default.Timer,
                                    contentDescription = stringResource(R.string.rest_timer_add),
                                    tint = timerTint,
                                    backgroundColor = actionBackground
                                )
                            }
                        }
                    }

                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(Modifier.padding(horizontal = 2.dp)) {
                            val headerTextStyle = typography.caption.copy(
                                color = colors.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            val headerBackground = colors.onSurface.copy(alpha = 0.06f)
                            Box(
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .width(WarmupIndicatorWidth)
                                    .height(40.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(headerBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.column_set),
                                    style = headerTextStyle
                                )
                            }
                            if (exercise?.logReps == true) Box(
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(headerBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.column_reps),
                                    style = headerTextStyle
                                )
                            }
                            if (exercise?.logWeight == true) Box(
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(headerBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.column_weight),
                                    style = headerTextStyle
                                )
                            }
                            if (exercise?.logTime == true) Box(
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(headerBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.column_time),
                                    style = headerTextStyle
                                )
                            }
                            if (exercise?.logDistance == true) Box(
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(headerBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.column_distance),
                                    style = headerTextStyle
                                )
                            }
                            Box(
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .size(40.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(headerBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    stringResource(R.string.column_set_complete),
                                    tint = colors.primary
                                )
                            }
                        }
                        setGroup.sets.forEachIndexed { index, set ->
                            key(set.workoutSetId) {
                                val dismissState = rememberDismissState(
                                    confirmStateChange = { value ->
                                        if (value != DismissValue.Default) {
                                            pendingSetDeletion = set
                                        }
                                        false
                                    }
                                )
                                SwipeToDismiss(
                                    state = dismissState,
                                    background = { SwipeToDeleteBackground(dismissState) },
                                ) {
                                    Surface(color = colors.surface) {
                                        Column(
                                            Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth()
                                            ) {
                                                val workingSetNumber = remember(setGroup.sets, index, set.isWarmup) {
                                                    if (set.isWarmup) {
                                                        0
                                                    } else {
                                                        setGroup.sets
                                                            .take(index + 1)
                                                            .count { !it.isWarmup }
                                                            .let { (it - 1).coerceAtLeast(0) }
                                                    }
                                                }
                                                SetTypeBadge(
                                                    isWarmup = set.isWarmup,
                                                    index = workingSetNumber,
                                                    modifier = Modifier
                                                        .padding(3.dp)
                                                        .width(WarmupIndicatorWidth),
                                                    onToggle = {
                                                        val makeWarmup = !set.isWarmup
                                                        if (!makeWarmup || setGroup.sets.take(index).all { it.isWarmup }) {
                                                            pendingSetTypeChange = set to makeWarmup
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
                                                            color = colors.surface,
                                                            shape = MaterialTheme.shapes.small,
                                                            border = BorderStroke(
                                                                1.dp,
                                                                colors.onSurface.copy(alpha = 0.06f)
                                                            )
                                                        ) {
                                                            Box(
                                                                Modifier
                                                                    .heightIn(min = 44.dp)
                                                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                innerTextField()
                                                            }
                                                        }
                                                    }
                                                if (exercise?.logReps == true) {
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
                                                if (exercise?.logTime == true) {
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
                                                val completionBackground by animateColorAsState(
                                                    if (set.complete) lerp(
                                                        colors.surface,
                                                        colors.secondary,
                                                        0.45f
                                                    ) else colors.onSurface.copy(alpha = 0.05f)
                                                )
                                                val completionContent = if (set.complete) {
                                                    contentColorFor(completionBackground)
                                                } else colors.onSurface
                                                Box(
                                                    Modifier
                                                        .padding(3.dp)
                                                        .size(44.dp)
                                                        .clip(MaterialTheme.shapes.small)
                                                        .toggleable(
                                                            value = set.complete,
                                                            onValueChange = {
                                                                viewModel.updateChecked(set, it)
                                                            },
                                                        )
                                                        .background(completionBackground),
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
                                                            tint = completionContent
                                                        )
                                                    }
                                                }
                                            }
                                            val restDuration = if (set.isWarmup) {
                                                setGroup.group.restTimerWarmupSeconds
                                            } else {
                                                setGroup.group.restTimerWorkingSeconds
                                            }
                                            if (restDuration > 0) {
                                                val isActive = restTimerState?.setId == set.workoutSetId
                                                val remaining = if (isActive) {
                                                    restTimerState?.remainingSeconds ?: restDuration
                                                } else {
                                                    restDuration
                                                }
                                                RestTimerIndicator(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
                                                    isActive = isActive,
                                                    isWarmup = set.isWarmup,
                                                    remainingSeconds = remaining,
                                                    onAdd = { viewModel.adjustRestTimer(30) },
                                                    onSubtract = { viewModel.adjustRestTimer(-30) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium,
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
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(60.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary, contentColor = colors.onPrimary),
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 4.dp),
                onClick = navToExercisePicker
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_add_exercise))
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                OutlinedButton(
                    modifier = Modifier.height(46.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.2f)),
                    onClick = { showCancelWorkoutDialog = true },
                ) {
                    Text(stringResource(R.string.btn_discard_workout))
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(backgroundColor = colors.secondary, contentColor = colors.onSecondary),
                    elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 4.dp),
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
private fun WorkoutInProgressHeader(
    routineName: String,
    duration: String,
    totalExercises: Int,
    totalSets: Int,
    completedSets: Int,
) {
    val colors = MaterialTheme.colors
    val typography = MaterialTheme.typography
    val displayName = routineName.takeIf { it.isNotBlank() } ?: stringResource(R.string.unnamed_routine)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = MaterialTheme.shapes.large,
        elevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(colors.primary, colors.primaryVariant)
                        ),
                        RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = displayName,
                        style = typography.h5.copy(
                            color = colors.onPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = stringResource(R.string.sheet_workout_in_progress),
                        style = typography.caption.copy(color = colors.onPrimary.copy(alpha = 0.78f))
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WorkoutHeaderStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.header_stat_duration),
                    value = duration
                )
                WorkoutHeaderStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.header_stat_exercises),
                    value = totalExercises.toString()
                )
                WorkoutHeaderStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.header_stat_sets_done),
                    value = stringResource(R.string.header_stat_sets_done_value, completedSets, totalSets),
                    highlight = completedSets > 0
                )
            }
        }
    }
}

@Composable
private fun WorkoutHeaderStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    val colors = MaterialTheme.colors
    val typography = MaterialTheme.typography
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = typography.overline,
                color = colors.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = typography.h6.copy(
                    color = if (highlight) colors.primary else colors.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun RoundedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    backgroundColor: Color,
) {
    IconButton(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(backgroundColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription, tint = tint)
        }
    }
}

@Composable
private fun ConfirmDeleteWorkoutSetDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.dialog_title_delete, stringResource(R.string.dialog_item_set)))
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.btn_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } },
    )
}

@Composable
private fun CancelWorkoutDialog(
    onDismiss: () -> Unit,
    cancelWorkout: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_discard_workout)) },
        confirmButton = { Button(onClick = cancelWorkout) { Text(stringResource(R.string.btn_delete)) } },
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

@Composable
private fun SetTypeChangeDialog(
    isWarmup: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_change_set_type)) },
        text = {
            val message = if (isWarmup) {
                stringResource(R.string.dialog_body_change_set_type_warmup)
            } else {
                stringResource(R.string.dialog_body_change_set_type_working)
            }
            Text(message)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.dialog_confirm_change_set_type))
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
private fun RestTimerIndicator(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    isWarmup: Boolean,
    remainingSeconds: Int,
    onAdd: () -> Unit,
    onSubtract: () -> Unit,
) {
    val primary = MaterialTheme.colors.primary
    val indicatorColor = primary.copy(alpha = if (isActive) 0.28f else 0.16f)
    if (!isActive) {
        Box(
            modifier = modifier
                .height(3.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(indicatorColor)
        )
    } else {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = indicatorColor
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val label = if (isWarmup) {
                    stringResource(R.string.rest_timer_indicator_warmup)
                } else {
                    stringResource(R.string.rest_timer_indicator_working)
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatRestDuration(remainingSeconds),
                    style = typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colors.onSurface
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = onSubtract,
                    enabled = isActive && remainingSeconds > 0,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.rest_timer_minus_30), color = MaterialTheme.colors.onSurface)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(
                    onClick = onAdd,
                    enabled = isActive,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.rest_timer_plus_30), color = MaterialTheme.colors.onSurface)
                }
            }
        }
    }
}
