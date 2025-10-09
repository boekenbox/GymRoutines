package com.noahjutz.gymroutines.ui.exercises.list

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Scaffold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.ui.components.Chip
import com.noahjutz.gymroutines.ui.components.SearchBar
import com.noahjutz.gymroutines.ui.components.SwipeToDeleteBackground
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.ui.exercises.detail.ExerciseDetailDialog
import com.noahjutz.gymroutines.ui.exercises.detail.toDetailData
import com.noahjutz.gymroutines.ui.exercises.components.ExerciseFilterPanel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun ExerciseList(
    navToExerciseEditor: (Int) -> Unit,
    navToSettings: () -> Unit,
    viewModel: ExerciseListViewModel = getViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var detailItem by remember { mutableStateOf<ExerciseListItem?>(null) }
    val coroutineScope = rememberCoroutineScope()

    detailItem?.let { item ->
        item.toDetailData()?.let { data ->
            ExerciseDetailDialog(
                data = data,
                onDismiss = { detailItem = null },
                onEdit = { exerciseId ->
                    detailItem = null
                    navToExerciseEditor(exerciseId)
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_exercise_list),
                actions = {
                    IconButton(onClick = navToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.screen_settings)
                    )
                    }
                }
            )
        },
        floatingActionButton = {
            androidx.compose.material.ExtendedFloatingActionButton(
                modifier = Modifier.padding(bottom = 8.dp),
                onClick = { navToExerciseEditor(-1) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(R.string.btn_new_exercise)) },
                backgroundColor = colors.secondary,
                contentColor = colors.onSecondary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                value = uiState.query,
                onValueChange = viewModel::setNameFilter
            )

            ExerciseFilterPanel(
                availableFilters = uiState.availableFilters,
                selectedFilters = uiState.selectedFilters,
                onToggle = viewModel::toggleFilter,
                onClear = viewModel::clearFilters,
                modifier = Modifier.fillMaxWidth(),
                initiallyExpanded = true
            )

            when {
                uiState.isLoading -> {
                    ExerciseListPlaceholder()
                }
                uiState.items.isEmpty() -> {
                    EmptyExerciseListState()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 72.dp)
                    ) {
                        items(uiState.items, key = { it.key }) { item ->
                            val exercise = item.exercise
                            val dismissState = rememberDismissState()

                            val canDelete = exercise != null
                            if (canDelete) {
                                SwipeToDismiss(
                                    state = dismissState,
                                    background = { SwipeToDeleteBackground(dismissState) },
                                    dismissThresholds = { androidx.compose.material.FractionalThreshold(0.4f) },
                                    dismissContent = {
                                        ExerciseListItemCard(
                                            item = item,
                                            onShowDetail = {
                                                detailItem = item
                                            },
                                            onEdit = {
                                                exercise?.let { navToExerciseEditor(it.exerciseId) }
                                                    ?: item.entry?.let { entry ->
                                                        viewModel.ensureExercise(entry) { navToExerciseEditor(it) }
                                                    }
                                            }
                                        )
                                    }
                                )

                                if (dismissState.targetValue != androidx.compose.material.DismissValue.Default) {
                                    ConfirmDeleteExerciseDialog(
                                        exerciseName = exercise?.name.orEmpty().ifBlank {
                                            stringResource(R.string.unnamed_exercise)
                                        },
                                        onDismiss = {
                                            coroutineScope.launch { dismissState.reset() }
                                        },
                                        onConfirm = {
                                            viewModel.delete(exercise!!)
                                        }
                                    )
                                }
                            } else {
                                ExerciseListItemCard(
                                    item = item,
                                    onShowDetail = {
                                        detailItem = item
                                    },
                                    onEdit = {
                                        item.entry?.let { entry ->
                                            viewModel.ensureExercise(entry) { navToExerciseEditor(it) }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseListItemCard(
    item: ExerciseListItem,
    onShowDetail: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onShowDetail),
        shape = MaterialTheme.shapes.medium,
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            RowWithActions(
                title = item.title.ifBlank { stringResource(R.string.unnamed_exercise) },
                onInfo = onShowDetail,
                onEdit = onEdit,
                canEdit = item.exerciseId != null || item.entry != null
            )
            item.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (item.chips.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.chips.forEach { chip ->
                        Chip(
                            text = chip,
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowWithActions(
    title: String,
    onInfo: () -> Unit,
    onEdit: () -> Unit,
    canEdit: Boolean,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onInfo) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.btn_view_details)
            )
        }
        if (canEdit) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.btn_edit_exercise)
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
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.dialog_title_delete, exerciseName))
        },
        confirmButton = {
            androidx.compose.material.Button(onClick = onConfirm) {
                Text(stringResource(R.string.btn_delete))
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
private fun EmptyExerciseListState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.hint_exercise_list_empty_title),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.hint_exercise_list_empty_body),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

