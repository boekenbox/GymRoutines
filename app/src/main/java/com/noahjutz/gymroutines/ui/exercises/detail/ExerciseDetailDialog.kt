package com.noahjutz.gymroutines.ui.exercises.detail

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.Exercise
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryEntry
import com.noahjutz.gymroutines.data.exerciselibrary.displayName
import com.noahjutz.gymroutines.ui.components.Chip
import com.noahjutz.gymroutines.ui.exercises.list.formatTag
import com.noahjutz.gymroutines.ui.exercises.list.ExerciseListItem
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryRepository
import java.util.Locale

sealed class ExerciseDetailData {
    data class Library(val entry: ExerciseLibraryEntry, val exercise: Exercise? = null) : ExerciseDetailData()
    data class Custom(val exercise: Exercise) : ExerciseDetailData()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailDialog(
    data: ExerciseDetailData,
    onDismiss: () -> Unit,
    onEdit: ((Int) -> Unit)? = null,
) {
    val locale = remember { Locale.getDefault() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    when (data) {
                        is ExerciseDetailData.Library -> LibraryExerciseDetail(
                            data = data,
                            locale = locale,
                            onEdit = onEdit,
                        )
                        is ExerciseDetailData.Custom -> CustomExerciseDetail(
                            exercise = data.exercise,
                            onEdit = onEdit
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    onClick = onDismiss
                ) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryExerciseDetail(
    data: ExerciseDetailData.Library,
    locale: Locale,
    onEdit: ((Int) -> Unit)?,
) {
    val entry = data.entry
    val exercise = data.exercise
    val displayName = entry.displayName(locale)
    Column {
        Text(
            text = displayName,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )
        val heroPath = entry.heroAsset?.let { "file:///android_asset/exercise_index/$it" }
        val context = LocalContext.current
        heroPath?.let { path ->
            val imageRequest = remember(path) {
                ImageRequest.Builder(context)
                    .data(path)
                    .crossfade(true)
                    .decoderFactory(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoderDecoder.Factory()
                        } else {
                            GifDecoder.Factory()
                        }
                    )
                    .build()
            }
            Spacer(modifier = Modifier.height(12.dp))
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(bottom = 4.dp),
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entry.bodyParts.forEach { part ->
                Chip(
                    text = part.formatTag(locale),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            entry.equipments.forEach { equipment ->
                Chip(
                    text = equipment.formatTag(locale),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (entry.instructions.isNotEmpty()) {
            SectionHeader(stringResource(R.string.label_instructions_header))
            Spacer(modifier = Modifier.height(4.dp))
            entry.instructions.forEachIndexed { index, instruction ->
                Text("${index + 1}. ${instruction.trim()}", style = MaterialTheme.typography.body2)
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (entry.tips.isNotEmpty()) {
            SectionHeader(stringResource(R.string.label_tips_header))
            Spacer(modifier = Modifier.height(4.dp))
            entry.tips.forEach { tip ->
                Text("• ${tip.trim()}", style = MaterialTheme.typography.body2)
                Spacer(modifier = Modifier.height(2.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        val metadata = listOfNotNull(
            entry.targetMuscles.takeIf { it.isNotEmpty() }?.let {
                stringResource(
                    R.string.label_primary_muscles,
                    it.joinToString { muscle -> muscle.formatTag(locale) }
                )
            },
            entry.secondaryMuscles.takeIf { it.isNotEmpty() }?.let {
                stringResource(
                    R.string.label_secondary_muscles,
                    it.joinToString { muscle -> muscle.formatTag(locale) }
                )
            },
            entry.mechanic?.takeIf { it.isNotBlank() }?.let {
                stringResource(R.string.label_mechanic, it.formatTag(locale))
            },
            entry.force?.takeIf { it.isNotBlank() }?.let {
                stringResource(R.string.label_force, it.formatTag(locale))
            },
            entry.difficulty?.takeIf { it.isNotBlank() }?.let {
                stringResource(R.string.label_difficulty, it.formatTag(locale))
            }
        )
        if (metadata.isNotEmpty()) {
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            metadata.forEach { line ->
                Text(line, style = MaterialTheme.typography.body2)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (onEdit != null) {
                exercise?.let {
                    TextButton(onClick = { onEdit(it.exerciseId) }) {
                        Text(stringResource(R.string.btn_edit_exercise))
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomExerciseDetail(
    exercise: Exercise,
    onEdit: ((Int) -> Unit)?
) {
    Column {
        Text(
            text = exercise.name.ifBlank { stringResource(R.string.unnamed_exercise) },
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        val trimmedNotes = remember(exercise.notes, exercise.libraryNotes) {
            if (exercise.notes == exercise.libraryNotes) "" else exercise.notes
        }
        if (trimmedNotes.isNotBlank()) {
            SectionHeader(stringResource(R.string.label_exercise_notes))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = trimmedNotes,
                style = MaterialTheme.typography.body2
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (exercise.libraryNotes.isNotBlank()) {
            SectionHeader(stringResource(R.string.label_library_details))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exercise.libraryNotes,
                style = MaterialTheme.typography.body2
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        SectionHeader(stringResource(R.string.label_tracking_options))
        Spacer(modifier = Modifier.height(4.dp))
        val tracking = buildList {
            if (exercise.logReps) add(stringResource(R.string.checkbox_log_reps))
            if (exercise.logWeight) add(stringResource(R.string.checkbox_log_weight))
            if (exercise.logTime) add(stringResource(R.string.checkbox_log_time))
            if (exercise.logDistance) add(stringResource(R.string.checkbox_log_distance))
        }
        if (tracking.isNotEmpty()) {
            tracking.forEach { option ->
                Text("• $option", style = MaterialTheme.typography.body2)
            }
        } else {
            Text(
                text = stringResource(R.string.hint_exercise_tracking_none),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        onEdit?.let { edit ->
            TextButton(
                modifier = Modifier.align(Alignment.End),
                onClick = { edit(exercise.exerciseId) }
            ) {
                Text(stringResource(R.string.btn_edit_exercise))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.SemiBold)
    )
}

fun ExerciseListItem.toDetailData(): ExerciseDetailData? {
    return when {
        entry != null -> ExerciseDetailData.Library(entry, exercise)
        exercise != null -> ExerciseDetailData.Custom(exercise)
        else -> null
    }
}

suspend fun resolveExerciseDetail(
    exercise: Exercise,
    libraryRepository: ExerciseLibraryRepository
): ExerciseDetailData {
    return if (exercise.tags.startsWith("library:")) {
        val entry = libraryRepository.getExerciseByTag(exercise.tags)
        if (entry != null) {
            ExerciseDetailData.Library(entry, exercise)
        } else {
            ExerciseDetailData.Custom(exercise)
        }
    } else {
        ExerciseDetailData.Custom(exercise)
    }
}
