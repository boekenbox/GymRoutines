package com.noahjutz.gymroutines.ui.workout.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.ui.components.SimpleLineChart
import org.koin.androidx.compose.getViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun WorkoutInsights(
    viewModel: WorkoutInsightsViewModel = getViewModel(),
    navToSettings: () -> Unit,
    navToWorkout: (Int) -> Unit,
    navToPrHistory: () -> Unit = {},
    navToWeeklyVolume: () -> Unit = {},
    navToExerciseDetail: (Int) -> Unit = {},
    navToConsistencyDetail: () -> Unit = {},
    navToRoutineHistory: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.screen_insights)) },
                actions = {
                    IconButton(onClick = navToSettings) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.btn_more))
                    }
                }
            )
        }
    ) { padding ->
            val durationChart = state.durationChart

            LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp, top = padding.calculateTopPadding() + 16.dp)
        ) {
            item {
                InsightCard(
                    title = stringResource(R.string.chart_workout_duration),
                    onClick = {},
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (durationChart == null) {
                        EmptyStateText(text = stringResource(R.string.insights_duration_empty))
                    } else {
                        Box(Modifier.fillMaxWidth().height(180.dp)) {
                            SimpleLineChart(
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                data = durationChart.aggregated,
                                secondaryData = durationChart.raw
                            )
                        }
                    }
                }
            }

            state.lastSessionSummary?.let { summary ->
                item {
                    InsightCard(
                        title = stringResource(R.string.insights_last_session_title),
                        subtitle = summary.routineName,
                        onClick = { navToWorkout(summary.workoutId) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        SessionSummaryContent(summary)
                    }
                }
            }

            item {
                InsightCard(
                    title = stringResource(R.string.insights_session_comparison_title),
                    onClick = {
                        state.sessionComparison?.latestWorkoutId?.let(navToWorkout)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val comparison = state.sessionComparison
                    if (comparison == null) {
                        EmptyStateText(text = stringResource(R.string.insights_session_comparison_empty))
                    } else if (comparison.isFirstTime) {
                        EmptyStateText(text = stringResource(R.string.insights_session_comparison_first))
                    } else {
                        SessionComparisonContent(comparison)
                    }
                }
            }

            item {
                InsightCard(
                    title = stringResource(R.string.insights_prs_title),
                    action = {
                        TextButton(onClick = navToPrHistory) {
                            Text(text = stringResource(R.string.insights_prs_view_all))
                        }
                    },
                    onClick = navToPrHistory,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (state.prs.isEmpty()) {
                        EmptyStateText(text = stringResource(R.string.insights_prs_empty))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.prs.take(3).forEach { pr ->
                                PrRow(pr = pr, onClick = { navToWorkout(pr.workoutId) })
                            }
                        }
                    }
                }
            }

            item {
                val weekly = state.weeklyVolume
                val weeklySubtitle = when {
                    weekly == null -> null
                    weekly.comparisonPercent == null -> stringResource(R.string.insights_volume_need_more_data)
                    weekly.comparisonPercent > 0 -> stringResource(
                        R.string.insights_volume_increase,
                        weekly.comparisonPercent
                    )
                    weekly.comparisonPercent < 0 -> stringResource(
                        R.string.insights_volume_decrease,
                        weekly.comparisonPercent.absoluteValue
                    )
                    else -> stringResource(R.string.insights_volume_no_change)
                }
                InsightCard(
                    title = stringResource(R.string.insights_weekly_volume_title),
                    subtitle = weeklySubtitle,
                    onClick = navToWeeklyVolume,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (weekly == null || weekly.points.isEmpty()) {
                        EmptyStateText(text = stringResource(R.string.insights_volume_empty))
                    } else {
                        WeeklyVolumeChart(points = weekly.points)
                    }
                }
            }

            item {
                InsightCard(
                    title = stringResource(R.string.insights_exercise_progress_title),
                    action = {
                        ProgressToggle(
                            metric = state.exerciseProgress.metric,
                            onMetricChange = viewModel::onProgressMetricChanged
                        )
                    },
                    onClick = {
                        state.exerciseProgress.selectedExerciseId?.let(navToExerciseDetail)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ExerciseProgressContent(
                        progress = state.exerciseProgress,
                        onExerciseSelected = viewModel::onExerciseSelected
                    )
                }
            }

            item {
                InsightCard(
                    title = stringResource(R.string.insights_consistency_title),
                    onClick = navToConsistencyDetail,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val consistency = state.consistency
                    if (consistency == null) {
                        EmptyStateText(text = stringResource(R.string.insights_consistency_empty))
                    } else {
                        ConsistencyContent(consistency)
                    }
                }
            }

            item {
                InsightCard(
                    title = stringResource(R.string.insights_routine_utilization_title),
                    onClick = {
                        state.routineUtilization?.routines?.firstOrNull()?.routineName?.let(navToRoutineHistory)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val utilization = state.routineUtilization
                    if (utilization == null || utilization.routines.isEmpty()) {
                        EmptyStateText(text = stringResource(R.string.insights_routines_empty))
                    } else {
                        RoutineUtilizationContent(utilization, navToRoutineHistory)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryContent(summary: SessionSummaryUi) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = summary.date.format(DateTimeFormatter.ofPattern("MMM d")),
                style = typography.subtitle1,
                color = colors.onSurface.copy(alpha = 0.7f)
            )
            if (summary.prCount > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = stringResource(R.string.insights_prs_count, summary.prCount),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = typography.caption.copy(color = colors.primary)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            SummaryMetric(label = stringResource(R.string.insights_metric_volume), value = formatWeight(summary.totalVolume))
            SummaryMetric(label = stringResource(R.string.insights_metric_total_reps), value = summary.totalReps.toString())
            SummaryMetric(label = stringResource(R.string.insights_metric_avg_load), value = formatWeight(summary.avgLoadPerRep))
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = typography.h6, fontWeight = FontWeight.Bold)
        Text(text = label, style = typography.caption, color = colors.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun SessionComparisonContent(comparison: SessionComparisonUi) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        comparison.comparisonDate?.let { date ->
            Text(
                text = stringResource(
                    R.string.insights_session_comparison_against,
                    date.format(DateTimeFormatter.ofPattern("MMM d"))
                ),
                style = typography.caption,
                color = colors.onSurface.copy(alpha = 0.7f)
            )
        }
        Text(
            text = stringResource(
                R.string.insights_session_comparison_volume_delta,
                formatWeight(comparison.sessionVolumeDelta)
            ),
            style = typography.subtitle2,
            fontWeight = FontWeight.SemiBold
        )
        comparison.rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = row.exerciseName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = typography.subtitle1)
                }
                DeltaValue(value = row.loadDelta, suffix = stringResource(R.string.insights_unit_weight))
                Spacer(modifier = Modifier.size(12.dp))
                DeltaValue(value = row.repsDelta.toDouble(), suffix = stringResource(R.string.insights_unit_reps), showDecimals = false)
                Spacer(modifier = Modifier.size(12.dp))
                DeltaValue(value = row.volumeDelta, suffix = stringResource(R.string.insights_unit_weight))
            }
        }
    }
}

@Composable
private fun DeltaValue(value: Double, suffix: String, showDecimals: Boolean = true) {
    val positive = value > 0
    val icon = if (positive) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown
    val formatted = if (showDecimals) String.format(Locale.getDefault(), "%+.1f %s", value, suffix) else String.format(Locale.getDefault(), "%+d %s", value.roundToInt(), suffix)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = if (positive) colors.primary else colors.error)
        Text(text = formatted, style = typography.body2)
    }
}

@Composable
fun PrRow(pr: PrEventUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = pr.exerciseName, style = typography.subtitle1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = pr.type.displayName(), style = typography.caption, color = colors.onSurface.copy(alpha = 0.6f))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = pr.displayValue(), style = typography.body1, fontWeight = FontWeight.Bold)
            val occurrenceDate = pr.occurredAt.atZone(ZoneId.systemDefault()).toLocalDate()
            Text(text = DateTimeFormatter.ofPattern("MMM d").format(occurrenceDate), style = typography.caption)
        }
        IconButton(onClick = onClick) {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun WeeklyVolumeChart(points: List<WeeklyVolumePoint>) {
    val maxVolume = points.maxOfOrNull { it.totalVolume } ?: 1.0
    val barColor = colors.primary
    val averageLineColor = colors.onSurface.copy(alpha = 0.5f)
    Column(horizontalAlignment = Alignment.Start) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val maxHeight = size.height
            val step = if (points.size <= 1) size.width else size.width / (points.size - 1)
            val barWidthPx = step * 0.5f

            points.forEachIndexed { index, point ->
                val normalized = if (maxVolume == 0.0) 0.0 else point.totalVolume / maxVolume
                val barHeight = (normalized * maxHeight).toFloat()
                val x = index * step - barWidthPx / 2f
                drawRect(
                    color = barColor,
                    topLeft = androidx.compose.ui.geometry.Offset(x.coerceAtLeast(0f), maxHeight - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight)
                )
            }

            if (points.any { it.rollingAverage != null }) {
                val path = Path()
                points.forEachIndexed { index, point ->
                    val average = point.rollingAverage ?: return@forEachIndexed
                    val normalized = if (maxVolume == 0.0) 0.0 else average / maxVolume
                    val x = index * step
                    val y = maxHeight - (normalized * maxHeight).toFloat()
                    if (path.isEmpty) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = averageLineColor,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            points.forEach { point ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = point.weekKey.substringAfter('-'), style = typography.caption)
                    Text(text = point.workoutsCount.toString(), style = typography.caption, color = colors.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun ProgressToggle(metric: ExerciseProgressMetric, onMetricChange: (ExerciseProgressMetric) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectableChip(
            selected = metric == ExerciseProgressMetric.Load,
            label = stringResource(R.string.insights_progress_load),
            onClick = { onMetricChange(ExerciseProgressMetric.Load) }
        )
        SelectableChip(
            selected = metric == ExerciseProgressMetric.EstimatedOneRm,
            label = stringResource(R.string.insights_progress_est_1rm),
            onClick = { onMetricChange(ExerciseProgressMetric.EstimatedOneRm) }
        )
    }
}

@Composable
private fun ExerciseProgressContent(
    progress: ExerciseProgressUi,
    onExerciseSelected: (Int) -> Unit,
) {
    if (progress.exercises.isEmpty()) {
        EmptyStateText(text = stringResource(R.string.insights_exercise_progress_empty))
        return
    }

    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        progress.exercises.forEach { series ->
            SelectableChip(
                label = series.exerciseName,
                selected = progress.selectedExerciseId == series.exerciseId,
                onClick = { onExerciseSelected(series.exerciseId) }
            )
        }
    }

    val selected = progress.exercises.firstOrNull { it.exerciseId == progress.selectedExerciseId }
        ?: progress.exercises.first()

    val data = if (progress.metric == ExerciseProgressMetric.Load) selected.secondarySamples else selected.samples
    if (data.isEmpty()) {
        EmptyStateText(text = stringResource(R.string.insights_exercise_progress_empty))
    } else {
        Sparkline(data.takeLast(12))
    }
}

@Composable
private fun SelectableChip(selected: Boolean, label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) colors.primary.copy(alpha = 0.12f) else colors.onSurface.copy(alpha = 0.05f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = typography.body2,
            color = if (selected) colors.primary else colors.onSurface
        )
    }
}

@Composable
private fun Sparkline(points: List<Pair<LocalDate, Double>>) {
    if (points.isEmpty()) {
        EmptyStateText(text = stringResource(R.string.insights_exercise_progress_empty))
        return
    }
    val chartData = points.mapIndexed { index, pair -> index.toFloat() to pair.second.toFloat() }
    SimpleLineChart(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        data = chartData
    )
}

@Composable
private fun ConsistencyContent(consistency: ConsistencyUi) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.insights_consistency_workouts, consistency.workoutsPerWeekAverage),
            style = typography.body1
        )
        val days = consistency.daysSinceLastWorkout?.let { it.toString() } ?: "--"
        Text(
            text = stringResource(R.string.insights_consistency_days_since, days),
            style = typography.body1
        )
        Text(
            text = stringResource(R.string.insights_consistency_streak, consistency.currentStreak),
            style = typography.body1
        )
    }
}

@Composable
private fun RoutineUtilizationContent(utilization: RoutineUtilizationUi, onRoutineClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        utilization.routines.forEach { routine ->
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = routine.routineName, style = typography.subtitle1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = stringResource(
                            R.string.insights_routines_average,
                            routine.usageCount,
                            routine.averageDaysBetween?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "--"
                        ),
                        style = typography.caption,
                        color = colors.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = { onRoutineClick(routine.routineName) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
        if (utilization.exerciseUsage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.insights_routines_top_exercises),
                style = typography.subtitle2,
                color = colors.onSurface.copy(alpha = 0.7f)
            )
            utilization.exerciseUsage.take(3).forEach { (name, count) ->
                Text(
                    text = stringResource(R.string.insights_routines_exercise_row, name, count),
                    style = typography.caption
                )
            }
        }
    }
}

@Composable
private fun EmptyStateText(text: String) {
    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = typography.body2, color = colors.onSurface.copy(alpha = 0.6f))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun InsightCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 4.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(text = title, style = typography.h6)
                    subtitle?.let {
                        Text(text = it, style = typography.caption, color = colors.onSurface.copy(alpha = 0.6f))
                    }
                }
                action?.invoke()
            }
            content()
        }
    }
}
