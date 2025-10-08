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

package com.noahjutz.gymroutines.ui.exercises.list

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.Exercise
import com.noahjutz.gymroutines.ui.components.SearchBar
import com.noahjutz.gymroutines.ui.components.SwipeToDeleteBackground
import com.noahjutz.gymroutines.ui.components.TopBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun ExerciseList(
    navToExerciseEditor: (Int) -> Unit,
    navToSettings: () -> Unit,
    viewModel: ExerciseListViewModel = getViewModel(),
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_exercise_list),
                actions = {
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.btn_more))
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(onClick = navToSettings) {
                                Text("Settings")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                onClick = { navToExerciseEditor(-1) },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.btn_new_exercise)) },
                shape = MaterialTheme.shapes.large,
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.onSecondary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            )
        },
    ) { paddingValues ->
        val exercises by viewModel.exercises.collectAsState()

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val exerciseList = exercises
            if (exerciseList == null) {
                ExerciseListPlaceholder()
            } else {
                ExerciseListContent(
                    navToExerciseEditor = navToExerciseEditor,
                    exercises = exerciseList,
                    viewModel = viewModel
                )
            }
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
private fun ExerciseListContent(
    exercises: List<Exercise>,
    navToExerciseEditor: (Int) -> Unit,
    viewModel: ExerciseListViewModel
) {
    val scope = rememberCoroutineScope()
    val searchQuery by viewModel.nameFilter.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            SearchBar(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                value = searchQuery,
                onValueChange = viewModel::setNameFilter
            )
        }

        items(
            items = exercises,
            key = { it.exerciseId },
            contentType = { "exercise" }
        ) { exercise ->
            val dismissState = rememberDismissState()

            SwipeToDismiss(
                modifier = Modifier
                    .animateItemPlacement()
                    .zIndex(if (dismissState.offset.value == 0f) 0f else 1f),
                state = dismissState,
                background = { SwipeToDeleteBackground(dismissState) }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.medium,
                    elevation = animateDpAsState(
                        if (dismissState.dismissDirection != null) 6.dp else 2.dp
                    ).value
                ) {
                    ListItem(
                        modifier = Modifier
                            .clickable { navToExerciseEditor(exercise.exerciseId) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        icon = null,
                        secondaryText = null,
                        overlineText = null,
                        singleLineSecondaryText = true,
                        text = {
                            Text(
                                text = exercise.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.subtitle1.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        },
                        trailing = {
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(Icons.Default.MoreVert, null)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        onClick = {
                                            expanded = false
                                            scope.launch {
                                                dismissState.dismiss(DismissDirection.StartToEnd)
                                            }
                                        }
                                    ) {
                                        Text(stringResource(R.string.btn_delete))
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (dismissState.targetValue != DismissValue.Default) {
                ConfirmDeleteExerciseDialog(
                    onDismiss = { scope.launch { dismissState.reset() } },
                    exerciseName = exercise.name,
                    onConfirm = { viewModel.delete(exercise) },
                )
            }
        }
        item {
            // Fix FAB overlap
            Spacer(Modifier.height(56.dp))
        }
    }
}

@Composable
private fun ConfirmDeleteExerciseDialog(
    exerciseName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(
                stringResource(R.string.dialog_title_delete, exerciseName)
            )
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
