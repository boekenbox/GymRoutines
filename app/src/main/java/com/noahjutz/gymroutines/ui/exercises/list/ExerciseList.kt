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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberDismissState
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.ui.components.SearchBar
import com.noahjutz.gymroutines.ui.components.SwipeToDeleteBackground
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.ui.exercises.library.ExerciseLibraryScreen
import com.noahjutz.gymroutines.util.toDisplayCase
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun ExerciseList(
    navToExerciseEditor: (Int) -> Unit,
    navToSettings: () -> Unit,
    navToExerciseDetail: (String) -> Unit,
    startWithLibrary: Boolean = false,
    viewModel: ExerciseListViewModel = getViewModel(),
) {
    val libraryViewModel: com.noahjutz.gymroutines.ui.exercises.library.ExerciseLibraryViewModel =
        getViewModel()
    var selectedTab by rememberSaveable {
        mutableStateOf(if (startWithLibrary) ExerciseTab.Library else ExerciseTab.MyExercises)
    }

    LaunchedEffect(startWithLibrary) {
        if (startWithLibrary) {
            selectedTab = ExerciseTab.Library
        }
    }

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
            if (selectedTab == ExerciseTab.MyExercises) {
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
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                ExerciseTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab.ordinal == index,
                        onClick = { selectedTab = tab },
                        text = { Text(stringResource(tab.title)) }
                    )
                }
            }

            when (selectedTab) {
                ExerciseTab.MyExercises -> MyExercisesTab(
                    viewModel = viewModel,
                    navToExerciseEditor = navToExerciseEditor,
                    navToExerciseDetail = navToExerciseDetail,
                )
                ExerciseTab.Library -> ExerciseLibraryScreen(
                    viewModel = libraryViewModel,
                    onExerciseAdded = { selectedTab = ExerciseTab.MyExercises },
                    onOpenExerciseDetail = navToExerciseDetail,
                )
            }
        }
    }
}

private enum class ExerciseTab(val title: Int) {
    MyExercises(R.string.tab_my_exercises),
    Library(R.string.tab_library)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MyExercisesTab(
    viewModel: ExerciseListViewModel,
    navToExerciseEditor: (Int) -> Unit,
    navToExerciseDetail: (String) -> Unit,
) {
    val exercises by viewModel.exercises.collectAsState()

    Box(Modifier.fillMaxSize()) {
        val exerciseList = exercises
        if (exerciseList == null) {
            ExerciseListPlaceholder()
        } else {
            ExerciseListContent(
                navToExerciseEditor = navToExerciseEditor,
                exercises = exerciseList,
                viewModel = viewModel,
                navToExerciseDetail = navToExerciseDetail,
            )
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class
)
@Composable
private fun ExerciseListContent(
    exercises: List<ExerciseListItem>,
    navToExerciseEditor: (Int) -> Unit,
    viewModel: ExerciseListViewModel,
    navToExerciseDetail: (String) -> Unit,
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
            key = { it.exercise.exerciseId },
            contentType = { "exercise" }
        ) { item ->
            val exercise = item.exercise
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
                            Column {
                                Text(
                                    text = exercise.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.subtitle1.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                ExerciseMetadataRow(item)
                            }
                        },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.libraryExercise != null) {
                                    IconButton(onClick = { navToExerciseDetail(item.libraryExercise.id) }) {
                                        Icon(Icons.Default.Info, stringResource(R.string.btn_exercise_info))
                                    }
                                }
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseMetadataRow(item: ExerciseListItem) {
    FlowRow(
        modifier = Modifier.padding(top = 6.dp)
    ) {
        val labelColor = MaterialTheme.colors.primary
        val backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f)

        if (item.exercise.isCustom) {
            AssistChip(
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
                onClick = {},
                label = { Text(stringResource(R.string.tag_custom)) },
                enabled = false,
                colors = AssistChipDefaults.assistChipColors(
                    disabledLabelColor = labelColor,
                    disabledContainerColor = backgroundColor
                )
            )
        } else {
            AssistChip(
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
                onClick = {},
                label = { Text(stringResource(R.string.tag_library)) },
                enabled = false,
                colors = AssistChipDefaults.assistChipColors(
                    disabledLabelColor = labelColor,
                    disabledContainerColor = backgroundColor
                )
            )
        }

        val libraryExercise = item.libraryExercise
        if (libraryExercise != null) {
            val locale = remember { Locale.getDefault() }
            val tags = buildList {
                addAll(libraryExercise.bodyParts)
                addAll(libraryExercise.targetMuscles)
                addAll(libraryExercise.equipments)
            }
            tags.distinctBy { it.lowercase(locale) }
                .take(3)
                .forEach { tag ->
                    AssistChip(
                        modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
                        onClick = {},
                        label = { Text(tag.toDisplayCase()) },
                        enabled = false,
                        colors = AssistChipDefaults.assistChipColors(
                            disabledLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                            disabledContainerColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
                        )
                    )
                }
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
