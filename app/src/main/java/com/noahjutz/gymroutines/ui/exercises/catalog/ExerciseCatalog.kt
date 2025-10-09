package com.noahjutz.gymroutines.ui.exercises.catalog

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryEntry
import com.noahjutz.gymroutines.ui.components.SearchBar
import com.noahjutz.gymroutines.ui.components.TopBar
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun ExerciseCatalog(
    onBack: () -> Unit,
    onOpenMyExercises: () -> Unit,
    viewModel: ExerciseCatalogViewModel = getViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var preview by remember { mutableStateOf<ExerciseLibraryEntry?>(null) }
    val scaffoldState = rememberScaffoldState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExerciseCatalogEvent.ExerciseImported -> {
                    val result = scaffoldState.snackbarHostState.showSnackbar(
                        message = context.getString(R.string.msg_exercise_imported, event.name),
                        actionLabel = context.getString(R.string.action_view_my_exercises)
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onOpenMyExercises()
                    }
                }
                is ExerciseCatalogEvent.ExerciseAlreadyInLibrary -> {
                    val result = scaffoldState.snackbarHostState.showSnackbar(
                        message = context.getString(R.string.msg_exercise_already_exists, event.name),
                        actionLabel = context.getString(R.string.action_view_my_exercises)
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onOpenMyExercises()
                    }
                }
            }
        }
    }

    preview?.let { entry ->
        ExerciseDetailsDialog(
            entry = entry,
            onDismiss = { preview = null }
        )
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_exercise_catalog),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        ExerciseCatalogContent(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            onQueryChange = viewModel::onQueryChanged,
            onSuggestionClicked = viewModel::onSuggestionSelected,
            onPreview = { preview = it }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ExerciseCatalogContent(
    state: ExerciseCatalogUiState,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit,
    onSuggestionClicked: (String) -> Unit,
    onPreview: (ExerciseLibraryEntry) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                CircularProgressIndicator(modifier = Modifier.height(36.dp))
            }
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            text = stringResource(R.string.hint_exercise_catalog),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
        )
        SearchBar(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (state.suggestions.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                text = stringResource(R.string.label_search_suggestions),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.suggestions.take(12)) { suggestion ->
                    SuggestionChip(
                        label = suggestion,
                        onClick = { onSuggestionClicked(suggestion) }
                    )
                }
            }
        }

        if (state.exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.hint_exercise_catalog_empty),
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(state.exercises, key = { it.id }) { entry ->
                ExerciseCatalogItem(
                    entry = entry,
                    onPreview = { onPreview(entry) }
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    label: String,
    onClick: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
        elevation = 0.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.LibraryBooks,
                contentDescription = null,
                tint = MaterialTheme.colors.primary.copy(alpha = 0.9f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
private fun ExerciseCatalogItem(
    entry: ExerciseLibraryEntry,
    onPreview: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onPreview),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold)
            )
            val subtitle = listOfNotNull(
                entry.targetMuscles.takeIf { it.isNotEmpty() }?.joinToString(),
                entry.equipments.takeIf { it.isNotEmpty() }?.joinToString(),
            ).joinToString(" • ")
            if (subtitle.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(top = 6.dp),
                    text = subtitle,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.instructions.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = entry.instructions.first(),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ExerciseDetailsDialog(
    entry: ExerciseLibraryEntry,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (entry.instructions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.label_instructions_header),
                        style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    entry.instructions.forEach { instruction ->
                        Text("• ${instruction.trim()}")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (entry.tips.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.label_tips_header),
                        style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    entry.tips.forEach { tip ->
                        Text("• ${tip.trim()}")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                val metadata = listOfNotNull(
                    entry.equipments.takeIf { it.isNotEmpty() }?.let {
                        stringResource(R.string.label_equipment, it.joinToString())
                    },
                    entry.targetMuscles.takeIf { it.isNotEmpty() }?.let {
                        stringResource(R.string.label_primary_muscles, it.joinToString())
                    },
                    entry.secondaryMuscles.takeIf { it.isNotEmpty() }?.let {
                        stringResource(R.string.label_secondary_muscles, it.joinToString())
                    },
                    entry.mechanic?.takeIf { it.isNotBlank() }?.let {
                        stringResource(R.string.label_mechanic, it.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) })
                    },
                    entry.force?.takeIf { it.isNotBlank() }?.let {
                        stringResource(R.string.label_force, it.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) })
                    },
                    entry.difficulty?.takeIf { it.isNotBlank() }?.let {
                        stringResource(R.string.label_difficulty, it.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) })
                    }
                )
                metadata.forEach { line ->
                    Text(line)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}
