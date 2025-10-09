package com.noahjutz.gymroutines.ui.workout.viewer

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.WorkoutWithSetGroups
import com.noahjutz.gymroutines.data.domain.duration
import com.noahjutz.gymroutines.ui.components.SetTypeBadge
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.ui.components.WarmupIndicatorWidth
import com.noahjutz.gymroutines.util.formatSimple
import com.noahjutz.gymroutines.util.pretty
import com.noahjutz.gymroutines.util.toStringOrBlank
import kotlin.time.ExperimentalTime
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalTime::class)
@Composable
fun WorkoutViewer(
    workoutId: Int,
    viewModel: WorkoutViewerViewModel = getViewModel { parametersOf(workoutId) },
    popBackStack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_view_workout),
                navigationIcon = {
                    IconButton(onClick = popBackStack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { paddingValues ->
        val workout by viewModel.workout.collectAsState()
        Crossfade(workout == null, Modifier.padding(paddingValues)) { isNull ->
            if (isNull) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                workout?.let { workout ->
                    WorkoutViewerContent(workout, viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalTime
@Composable
fun WorkoutViewerContent(workout: WorkoutWithSetGroups, viewModel: WorkoutViewerViewModel) {
    val backgroundColor = colors.background
    val backgroundBrush = remember(backgroundColor) {
        Brush.verticalGradient(
            listOf(
                backgroundColor,
                backgroundColor.copy(alpha = 0.9f)
            )
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            item {
                val routineName by viewModel.routineName.collectAsState(initial = "")
                val totalExercises = workout.setGroups.size
                val totalSets = workout.setGroups.sumOf { it.sets.size }
                val completedSets = workout.setGroups.sumOf { setGroup -> setGroup.sets.count { it.complete } }
                WorkoutViewerHeader(
                    routineName = routineName,
                    completedOn = workout.workout.endTime.formatSimple(),
                    duration = workout.workout.duration.pretty(),
                    totalExercises = totalExercises,
                    completedSets = completedSets,
                    totalSets = totalSets
                )
            }

            items(workout.setGroups.sortedBy { it.group.position }) { setGroup ->
            val exercise by viewModel.getExercise(setGroup.group.exerciseId)
                .collectAsState(initial = null)

            val headerShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            Card(
                Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(colors.secondary, colors.secondaryVariant)
                                ),
                                headerShape
                            )
                            .padding(horizontal = 22.dp, vertical = 16.dp)
                    ) {
                        val exerciseName = exercise?.name?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.unnamed_exercise)
                        Text(
                            exerciseName,
                            style = typography.h6.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSecondary
                            )
                        )
                    }
                    Column(Modifier.padding(vertical = 16.dp)) {
                        Row(Modifier.padding(horizontal = 4.dp)) {
                            val headerTextStyle = TextStyle(
                                color = colors.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Box(
                                Modifier
                                    .padding(4.dp)
                                    .width(WarmupIndicatorWidth)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.onSurface.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.column_set),
                                    style = headerTextStyle
                                )
                            }
                            if (exercise?.logReps == true) Box(
                                Modifier
                                    .padding(4.dp)
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.onSurface.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.column_reps),
                                    style = headerTextStyle
                                )
                            }
                            if (exercise?.logWeight == true) Box(
                                Modifier
                                    .padding(4.dp)
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.onSurface.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.column_weight),
                                    style = headerTextStyle
                                )
                            }
                            if (exercise?.logTime == true) Box(
                                Modifier
                                    .padding(4.dp)
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.onSurface.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.column_time),
                                    style = headerTextStyle
                                )
                            }
                            if (exercise?.logDistance == true) Box(
                                Modifier
                                    .padding(4.dp)
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.onSurface.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.column_distance),
                                    style = headerTextStyle
                                )
                            }
                            Box(
                                Modifier
                                    .padding(4.dp)
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.onSurface.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null)
                            }
                        }
                        var workingSetIndex = 0
                        setGroup.sets.forEach { set ->
                            Row(
                                Modifier.padding(horizontal = 4.dp)
                            ) {
                                val TableCell: @Composable RowScope.(
                                    @Composable BoxScope.() -> Unit
                                ) -> Unit =
                                    {
                                        ProvideTextStyle(
                                            value = typography.body1.copy(
                                                textAlign = TextAlign.Center,
                                                color = colors.onSurface
                                            )
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .padding(4.dp)
                                                    .weight(1f),
                                                color = colors.onSurface.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(12.dp),
                                            ) {
                                                Box(
                                                    Modifier
                                                        .padding(horizontal = 4.dp)
                                                        .height(56.dp),
                                                    contentAlignment = Alignment.Center,
                                                    content = it
                                                )
                                            }
                                        }
                                    }
                                SetTypeBadge(
                                    isWarmup = set.isWarmup,
                                    index = if (set.isWarmup) workingSetIndex else workingSetIndex++,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .width(WarmupIndicatorWidth)
                                )
                                if (exercise?.logReps == true) {
                                    TableCell { Text(set.reps.toStringOrBlank()) }
                                }
                                if (exercise?.logWeight == true) {
                                    TableCell { Text(set.weight.formatSimple()) }
                                }
                                if (exercise?.logTime == true) {
                                    TableCell { Text(set.time.toStringOrBlank()) }
                                }
                                if (exercise?.logDistance == true) {
                                    TableCell { Text(set.distance.formatSimple()) }
                                }
                                Box(
                                    Modifier
                                        .padding(4.dp)
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (set.complete) colors.secondary.copy(alpha = 0.85f)
                                            else colors.onSurface.copy(
                                                alpha = 0.06f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (set.complete) {
                                        Icon(
                                            Icons.Default.Check,
                                            stringResource(R.string.column_set_complete),
                                            tint = colors.onSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun WorkoutViewerHeader(
    routineName: String,
    completedOn: String,
    duration: String,
    totalExercises: Int,
    completedSets: Int,
    totalSets: Int,
) {
    val palette = colors
    val displayName = routineName.takeIf { it.isNotBlank() } ?: stringResource(R.string.unnamed_routine)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(26.dp),
        elevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(palette.secondary, palette.secondaryVariant)
                        ),
                        RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = displayName,
                        style = typography.h5.copy(
                            color = palette.onSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = stringResource(R.string.workout_viewer_completed_on, completedOn),
                        style = typography.caption.copy(color = palette.onSecondary.copy(alpha = 0.8f))
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ViewerStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.header_stat_duration),
                    value = duration
                )
                ViewerStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.header_stat_exercises),
                    value = totalExercises.toString()
                )
                ViewerStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.header_stat_sets_done),
                    value = stringResource(R.string.header_stat_sets_done_value, completedSets, totalSets),
                    highlight = true
                )
            }
        }
    }
}

@Composable
private fun ViewerStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
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
                    color = if (highlight) colors.secondary else colors.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}
