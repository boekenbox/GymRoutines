package com.noahjutz.gymroutines.ui.exercises.library

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.library.LibraryExercise
import com.noahjutz.gymroutines.ui.components.SearchBar
import com.noahjutz.gymroutines.util.toDisplayCase
import java.util.Locale
import org.koin.androidx.compose.getViewModel

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun ExerciseLibraryScreen(
    viewModel: ExerciseLibraryViewModel = getViewModel(),
    onExerciseAdded: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            value = state.query,
            onValueChange = viewModel::setQuery
        )

        FilterRow(
            state = state,
            onSelectBodyPart = viewModel::setBodyPart,
            onSelectEquipment = viewModel::setEquipment,
            onSelectMuscle = viewModel::setMuscle,
            onClearFilters = viewModel::clearFilters
        )

        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading -> LibraryLoadingPlaceholder()
            state.items.isEmpty() -> EmptyLibraryState(query = state.query)
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(state.items, key = { it.exercise.id }) { item ->
                    LibraryExerciseCard(
                        item = item,
                        onViewDetails = { onOpenExerciseDetail(item.exercise.id) },
                        onAdd = {
                            viewModel.importExercise(item.exercise) {
                                onExerciseAdded()
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    state: ExerciseLibraryUiState,
    onSelectBodyPart: (String?) -> Unit,
    onSelectEquipment: (String?) -> Unit,
    onSelectMuscle: (String?) -> Unit,
    onClearFilters: () -> Unit,
) {
    var showBodyPartDialog by remember { mutableStateOf(false) }
    var showEquipmentDialog by remember { mutableStateOf(false) }
    var showMuscleDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = state.selectedBodyPart != null,
            onClick = { showBodyPartDialog = true },
            label = { Text(state.selectedBodyPart?.toDisplayCase() ?: stringResource(R.string.filter_body_part)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
            }
        )
        FilterChip(
            selected = state.selectedEquipment != null,
            onClick = { showEquipmentDialog = true },
            label = { Text(state.selectedEquipment?.toDisplayCase() ?: stringResource(R.string.filter_equipment)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
            }
        )
        FilterChip(
            selected = state.selectedMuscle != null,
            onClick = { showMuscleDialog = true },
            label = { Text(state.selectedMuscle?.toDisplayCase() ?: stringResource(R.string.filter_muscle)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
            }
        )
        if (state.selectedBodyPart != null || state.selectedEquipment != null || state.selectedMuscle != null) {
            IconButton(onClick = onClearFilters) {
                Icon(Icons.Default.Clear, stringResource(R.string.btn_clear_filters))
            }
        }
    }

    if (showBodyPartDialog) {
        SelectionDialog(
            title = stringResource(R.string.filter_body_part),
            options = state.bodyParts,
            selected = state.selectedBodyPart,
            onSelect = { selection ->
                onSelectBodyPart(selection)
                showBodyPartDialog = false
            },
            onDismiss = { showBodyPartDialog = false }
        )
    }

    if (showEquipmentDialog) {
        SelectionDialog(
            title = stringResource(R.string.filter_equipment),
            options = state.equipments,
            selected = state.selectedEquipment,
            onSelect = { selection ->
                onSelectEquipment(selection)
                showEquipmentDialog = false
            },
            onDismiss = { showEquipmentDialog = false }
        )
    }

    if (showMuscleDialog) {
        SelectionDialog(
            title = stringResource(R.string.filter_muscle),
            options = state.muscles,
            selected = state.selectedMuscle,
            onSelect = { selection ->
                onSelectMuscle(selection)
                showMuscleDialog = false
            },
            onDismiss = { showMuscleDialog = false }
        )
    }
}

@Composable
private fun LibraryExerciseCard(
    item: LibraryExerciseListItem,
    onViewDetails: () -> Unit,
    onAdd: () -> Unit,
) {
    val exercise = item.exercise
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.CenterVertically),
                    model = ImageRequest.Builder(context)
                        .data(exercise.gifUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(
                        R.string.cd_exercise_gif,
                        exercise.name.toDisplayCase()
                    ),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name.toDisplayCase(),
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    ExerciseTagRow(exercise)
                }
            }

            Divider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onViewDetails) {
                    Text(stringResource(R.string.btn_view_details))
                }

                if (item.isImported) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.label_already_added)) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledLabelColor = MaterialTheme.colors.primary,
                            disabledContainerColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                        )
                    )
                } else {
                    Button(onClick = onAdd) {
                        Text(stringResource(R.string.btn_add_exercise))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ExerciseTagRow(exercise: LibraryExercise) {
    val locale = remember { Locale.getDefault() }
    val tags = remember(exercise) {
        buildList {
            addAll(exercise.bodyParts)
            addAll(exercise.targetMuscles)
            addAll(exercise.equipments)
        }
            .distinctBy { it.lowercase(locale) }
            .take(5)
    }

    FlowRow {
        tags.forEach { tag ->
            AssistChip(
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
                onClick = {},
                enabled = false,
                label = { Text(tag.toDisplayCase()) },
                colors = AssistChipDefaults.assistChipColors(
                    disabledLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    disabledContainerColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
                )
            )
        }
    }
}

@Composable
private fun LibraryLoadingPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        androidx.compose.material.CircularProgressIndicator(color = MaterialTheme.colors.primary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.loading_library))
    }
}

@Composable
private fun EmptyLibraryState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.empty_library_results_title),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (query.isBlank()) {
                stringResource(R.string.empty_library_results_message)
            } else {
                stringResource(R.string.empty_library_search_message, query)
            },
            style = MaterialTheme.typography.body2,
            color = Color.Gray
        )
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            val locale = remember { Locale.getDefault() }
            LazyColumnWithSelection(
                options = options,
                selected = selected,
                onSelect = {
                    onSelect(it)
                },
                locale = locale
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun LazyColumnWithSelection(
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    locale: Locale,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        item {
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                text = { Text(stringResource(R.string.filter_option_all)) },
                trailing = {
                    RadioButton(selected = selected == null, onClick = { onSelect(null) })
                }
            )
        }
        items(options, key = { it }) { option ->
            val normalized = option.lowercase(locale)
            val isSelected = selected != null && selected.equals(normalized, ignoreCase = true)
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                text = { Text(option.toDisplayCase(locale)) },
                trailing = {
                    RadioButton(selected = isSelected, onClick = { onSelect(option) })
                }
            )
        }
    }
}
