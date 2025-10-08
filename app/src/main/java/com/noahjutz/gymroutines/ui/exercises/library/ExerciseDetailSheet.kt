package com.noahjutz.gymroutines.ui.exercises.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.noahjutz.gymroutines.util.toDisplayCase
import java.util.Locale
import org.koin.androidx.compose.getViewModel
import kotlinx.coroutines.launch

@Composable
fun ExerciseDetailSheet(
    libraryId: String,
    onExerciseImported: (Int) -> Unit,
    viewModel: ExerciseLibraryViewModel = getViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val exercise = remember(libraryId, state) { viewModel.getExercise(libraryId) }
    val scope = rememberCoroutineScope()

    if (exercise == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material.CircularProgressIndicator(color = MaterialTheme.colors.primary)
            Text(stringResource(R.string.loading_library))
        }
        return
    }

    val isImported = viewModel.isExerciseImported(libraryId)
    DetailContent(
        exercise = exercise,
        isImported = isImported,
        onImport = {
            scope.launch {
                viewModel.importExercise(exercise) { onExerciseImported(it) }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    exercise: LibraryExercise,
    isImported: Boolean,
    onImport: () -> Unit,
) {
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        item {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
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
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = exercise.name.toDisplayCase(),
                    style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                FlowRow {
                    buildList {
                        addAll(exercise.bodyParts)
                        addAll(exercise.targetMuscles)
                        addAll(exercise.equipments)
                    }
                        .distinctBy { it.lowercase(locale) }
                        .forEach { tag ->
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
        }
        item {
            DetailSection(
                title = stringResource(R.string.section_primary_muscles),
                values = exercise.targetMuscles
            )
        }
        if (exercise.secondaryMuscles.isNotEmpty()) {
            item {
                DetailSection(
                    title = stringResource(R.string.section_secondary_muscles),
                    values = exercise.secondaryMuscles
                )
            }
        }
        item {
            DetailSection(
                title = stringResource(R.string.section_body_parts),
                values = exercise.bodyParts
            )
        }
        item {
            DetailSection(
                title = stringResource(R.string.section_equipment),
                values = exercise.equipments
            )
        }
        if (exercise.instructions.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.section_instructions),
                    style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
            itemsIndexed(exercise.instructions) { index, instruction ->
                InstructionItem(step = index + 1, instruction = instruction)
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isImported) {
                    Text(
                        text = stringResource(R.string.label_already_added),
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Button(onClick = onImport) {
                        Text(stringResource(R.string.btn_add_exercise))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    values: List<String>,
) {
    if (values.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = values.joinToString(separator = ", ") { it.toDisplayCase() },
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun InstructionItem(step: Int, instruction: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text(
            text = stringResource(R.string.instruction_step_number, step),
            style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = instruction,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
        )
    }
}
