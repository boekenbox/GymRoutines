package com.noahjutz.gymroutines.ui.workout.insights

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.AppPrefs
import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.RoutineRepository
import com.noahjutz.gymroutines.data.WorkoutRepository
import com.noahjutz.gymroutines.data.domain.Workout
import com.noahjutz.gymroutines.data.domain.WorkoutSet
import com.noahjutz.gymroutines.data.domain.WorkoutWithSetGroups
import com.noahjutz.gymroutines.data.domain.isPerformanceSet
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutInsightsViewModel(
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    private val preferences: DataStore<Preferences>,
) : ViewModel() {

    init {
        viewModelScope.launch {
            preferences.edit { prefs ->
                if (prefs[AppPrefs.InsightsDataStartEpochMillis.key] == null) {
                    prefs[AppPrefs.InsightsDataStartEpochMillis.key] = System.currentTimeMillis()
                }
            }
        }
    }

    private val zoneId = ZoneId.systemDefault()
    private val weekFields = WeekFields.ISO
    private val defaultLocale = Locale.getDefault()

    private val progressMetricFlow = preferences.data
        .map { prefs ->
            when (prefs[AppPrefs.InsightsProgressMetric.key] ?: AppPrefs.InsightsProgressMetric.defaultValue) {
                ExerciseProgressMetric.Load.name -> ExerciseProgressMetric.Load
                else -> ExerciseProgressMetric.EstimatedOneRm
            }
        }
        .distinctUntilChanged()

    private val selectedExerciseFlow = preferences.data
        .map { prefs ->
            val id = prefs[AppPrefs.InsightsSelectedExercise.key] ?: AppPrefs.InsightsSelectedExercise.defaultValue
            if (id <= 0) null else id
        }
        .distinctUntilChanged()

    private val bodyWeightFlow = preferences.data
        .map { prefs -> prefs[AppPrefs.BodyWeight.key] ?: AppPrefs.BodyWeight.defaultValue }
        .distinctUntilChanged()

    private val currentWorkoutFlow = preferences.data
        .map { prefs -> prefs[AppPrefs.CurrentWorkout.key] ?: AppPrefs.CurrentWorkout.defaultValue }
        .distinctUntilChanged()

    private val insightsDataStartFlow = preferences.data
        .map { prefs ->
            prefs[AppPrefs.InsightsDataStartEpochMillis.key]
                ?: Long.MAX_VALUE
        }
        .distinctUntilChanged()

    private val routineNameFlow = routineRepository.routines
        .map { routines -> routines.associate { it.routineId to it.name } }
        .distinctUntilChanged()

    private val exerciseNameCacheFlow = exerciseRepository.exercises
        .map { exercises ->
            val names = exercises.associate { it.exerciseId to it.name }
            val normalized = exercises.associate { it.exerciseId to it.name.trim().lowercase(defaultLocale) }
            ExerciseNameCache(names = names, normalizedNames = normalized)
        }
        .distinctUntilChanged()

    private val insightsSourceFlow = combine(
        workoutRepository.workoutsWithSetGroups,
        routineNameFlow,
        exerciseNameCacheFlow,
    ) { workouts, routineNames, exerciseCache ->
        InsightsSource(workouts = workouts, routineNames = routineNames, exerciseNameCache = exerciseCache)
    }

    private val preferenceSnapshotFlow = combine(
        progressMetricFlow,
        selectedExerciseFlow,
        bodyWeightFlow,
        currentWorkoutFlow,
        insightsDataStartFlow,
    ) { progressMetric, selectedExerciseId, bodyWeight, currentWorkoutId, insightsDataStart ->
        PreferenceSnapshot(
            progressMetric = progressMetric,
            selectedExerciseId = selectedExerciseId,
            bodyWeight = bodyWeight,
            currentWorkoutId = currentWorkoutId,
            insightsDataStartEpochMillis = insightsDataStart,
        )
    }

    val uiState: StateFlow<WorkoutInsightsUiState> =
        combine(insightsSourceFlow, preferenceSnapshotFlow) { source, preferences ->
            val activeWorkoutId = preferences.currentWorkoutId.takeIf { it > 0 }
            val effectiveBodyWeight = max(preferences.bodyWeight.toDouble(), 0.0)
            val sessions = source.workouts
                .filter { activeWorkoutId == null || it.workout.workoutId != activeWorkoutId }
                .sortedBy { it.workout.startTime }
                .map { session ->
                    buildSession(
                        session = session,
                        routineNames = source.routineNames,
                        exerciseNameCache = source.exerciseNameCache,
                        bodyWeight = effectiveBodyWeight,
                    )
                }

            val exerciseProgressData = buildExerciseProgress(sessions)
            val selectedExercise = when {
                exerciseProgressData.exercises.isEmpty() -> null
                preferences.selectedExerciseId != null &&
                    exerciseProgressData.exercises.any { it.exerciseId == preferences.selectedExerciseId } ->
                    preferences.selectedExerciseId
                else -> exerciseProgressData.exercises.firstOrNull()?.exerciseId
            }

            val exerciseProgress = exerciseProgressData.copy(
                selectedExerciseId = selectedExercise,
                metric = preferences.progressMetric,
            )

            val prSessions = sessions.filter {
                it.startInstant.toEpochMilli() >= preferences.insightsDataStartEpochMillis
            }
            val prComputation = computePersonalRecords(prSessions)
            val weeklyVolume = buildWeeklyVolume(sessions)
            val sessionComparison = buildSessionComparison(sessions)
            val consistency = buildConsistency(sessions, weeklyVolume)
            val routineUtilization = buildRoutineUtilization(sessions)
            val durationChart = buildDurationChart(sessions)
            val summary = sessions.lastOrNull()?.let { session ->
                val sessionEvents = prComputation.eventsByWorkout[session.workout.workoutId].orEmpty()
                SessionSummaryUi(
                    workoutId = session.workout.workoutId,
                    routineName = session.routineName,
                    date = session.date,
                    totalVolume = session.totalVolume,
                    totalReps = session.totalReps,
                    avgLoadPerRep = session.avgLoadPerRep,
                    durationMinutes = session.durationMinutes,
                    prCount = sessionEvents.map { it.exerciseId }.distinct().size,
                    completedWorkingSets = session.sets.size,
                    plannedWorkingSets = session.plannedWorkingSets,
                    topSets = session.exerciseSummaries.values
                        .mapNotNull { it.topSet }
                        .sortedWith(
                            compareByDescending<TopSetUi> { it.estimatedOneRm ?: it.load ?: 0.0 }
                                .thenByDescending { it.reps ?: 0 }
                        )
                        .take(6),
                )
            }

            WorkoutInsightsUiState(
                isLoading = false,
                durationChart = durationChart,
                lastSessionSummary = summary,
                weeklyVolume = weeklyVolume,
                sessionComparison = sessionComparison,
                prs = prComputation.events,
                currentPrs = CurrentPrsUi(prComputation.currentByType),
                exerciseProgress = exerciseProgress,
                consistency = consistency,
                routineUtilization = routineUtilization,
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), WorkoutInsightsUiState())

    fun onProgressMetricChanged(metric: ExerciseProgressMetric) {
        viewModelScope.launch {
            preferences.edit { prefs ->
                prefs[AppPrefs.InsightsProgressMetric.key] = metric.name
            }
        }
    }

    fun onExerciseSelected(exerciseId: Int) {
        viewModelScope.launch {
            preferences.edit { prefs ->
                prefs[AppPrefs.InsightsSelectedExercise.key] = exerciseId
            }
        }
    }

    private fun buildDurationChart(sessions: List<SessionComputation>): WorkoutDurationChartData? {
        if (sessions.size < 3) return null
        val raw = sessions.mapIndexed { index, session ->
            index.toFloat() to (session.durationMinutes ?: 0.0).toFloat()
        }
        val aggregated = raw.chunked(3).mapIndexed { index, chunk ->
            val avg = if (chunk.isEmpty()) 0f else chunk.map { it.second }.average().toFloat()
            (chunk.firstOrNull()?.first ?: index.toFloat()) to avg
        }
        return WorkoutDurationChartData(aggregated = aggregated, raw = raw)
    }

    private fun buildRoutineUtilization(sessions: List<SessionComputation>): RoutineUtilizationUi? {
        if (sessions.isEmpty()) return null
        val byRoutine = sessions.groupBy { it.workout.routineId }
        val routines = byRoutine.entries.map { (routineId, sessionList) ->
            val dates = sessionList.map { it.date }.sorted()
            val diffs = dates.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toDouble() }
            RoutineUsageUi(
                routineName = sessionList.first().routineName.ifBlank { "Routine #$routineId" },
                usageCount = dates.size,
                averageDaysBetween = if (diffs.isNotEmpty()) diffs.average() else null
            )
        }.sortedByDescending { it.usageCount }

        val exerciseFrequency = sessions
            .flatMap { session -> session.exerciseSummaries.values }
            .groupBy { it.exerciseId }
            .map { (_, entries) ->
                val name = entries.first().exerciseName
                name to entries.size
            }
            .sortedByDescending { it.second }

        return RoutineUtilizationUi(
            routines = routines.take(3),
            exerciseUsage = exerciseFrequency
        )
    }

    private fun buildConsistency(
        sessions: List<SessionComputation>,
        weeklyVolume: WeeklyVolumeOverview?
    ): ConsistencyUi? {
        if (sessions.isEmpty()) return null
        val workoutDates = sessions.map { it.date }.toSet()
        var streak = 0
        var cursor = LocalDate.now()
        while (workoutDates.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }

        val daysSinceLast = sessions.lastOrNull()?.let { ChronoUnit.DAYS.between(it.date, LocalDate.now()) }
        val rollingAvg = weeklyVolume?.points?.takeLast(4)?.map { it.workoutsCount }?.average() ?: 0.0

        return ConsistencyUi(
            workoutsPerWeekAverage = rollingAvg,
            daysSinceLastWorkout = daysSinceLast,
            currentStreak = streak
        )
    }

    private fun buildSessionComparison(sessions: List<SessionComputation>): SessionComparisonUi? {
        if (sessions.isEmpty()) return null
        val latest = sessions.last()
        val previous = sessions.dropLast(1).lastOrNull { it.workout.routineId == latest.workout.routineId }
            ?: return SessionComparisonUi(
                latestWorkoutId = latest.workout.workoutId,
                routineName = latest.routineName,
                comparisonDate = null,
                rows = emptyList(),
                sessionVolumeDelta = latest.totalVolume,
                isFirstTime = true,
            )

        val exerciseNames = (latest.exerciseSummaries.keys + previous.exerciseSummaries.keys)
            .associateWith { id -> latest.exerciseSummaries[id]?.exerciseName ?: previous.exerciseSummaries[id]?.exerciseName ?: "Exercise #$id" }

        val rows = exerciseNames.entries.map { (exerciseId, name) ->
            val current = latest.exerciseSummaries[exerciseId]
            val past = previous.exerciseSummaries[exerciseId]
            SessionComparisonRow(
                exerciseName = name,
                loadDelta = (current?.averageLoad ?: 0.0) - (past?.averageLoad ?: 0.0),
                repsDelta = (current?.totalReps ?: 0) - (past?.totalReps ?: 0),
                volumeDelta = (current?.totalVolume ?: 0.0) - (past?.totalVolume ?: 0.0),
            )
        }.sortedByDescending { abs(it.volumeDelta) }

        return SessionComparisonUi(
            latestWorkoutId = latest.workout.workoutId,
            routineName = latest.routineName,
            comparisonDate = previous.date,
            rows = rows.take(5),
            sessionVolumeDelta = latest.totalVolume - previous.totalVolume,
            isFirstTime = false,
        )
    }

    private fun buildWeeklyVolume(sessions: List<SessionComputation>): WeeklyVolumeOverview? {
        if (sessions.isEmpty()) return null
        val weekly = sessions.groupBy { weekKey(it.date) }
            .mapValues { entry ->
                val totalVolume = entry.value.sumOf { it.totalVolume }
                val workouts = entry.value.size
                totalVolume to workouts
            }
            .toList()
            .sortedBy { it.first }

        val points = weekly.mapIndexed { index, (week, data) ->
            val rollingWindow = weekly.subList(max(0, index - 2), index + 1)
            val rollingAverage = if (rollingWindow.isNotEmpty()) rollingWindow.map { it.second.first }.average() else null
            WeeklyVolumePoint(
                weekKey = week,
                totalVolume = data.first,
                workoutsCount = data.second,
                rollingAverage = rollingAverage
            )
        }
        val trimmed = points.takeLast(12)
        val comparisonPercent = if (trimmed.size < 2) {
            null
        } else {
            val last = trimmed.last()
            val prev = trimmed[trimmed.size - 2]
            if (prev.totalVolume == 0.0) null else (last.totalVolume - prev.totalVolume) / prev.totalVolume * 100
        }

        return WeeklyVolumeOverview(points = trimmed, comparisonPercent = comparisonPercent)
    }

    private fun buildExerciseProgress(sessions: List<SessionComputation>): ExerciseProgressUi {
        if (sessions.isEmpty()) return ExerciseProgressUi(emptyList(), null, ExerciseProgressMetric.EstimatedOneRm)

        val grouped = mutableMapOf<Int, MutableMap<LocalDate, DailyBest>>()
        sessions.forEach { session ->
            session.exerciseDailyBest.forEach { (exerciseId, best) ->
                val map = grouped.getOrPut(exerciseId) { mutableMapOf() }
                val entry = map.getOrPut(session.date) { DailyBest(best.bestLoad, best.bestEst, best.label) }
                if (best.bestLoad > entry.bestLoad) {
                    entry.bestLoad = best.bestLoad
                }
                if (best.bestEst > entry.bestEst) {
                    entry.bestEst = best.bestEst
                }
                if (entry.label == null) {
                    entry.label = best.label
                }
            }
        }

        val exercises = grouped.entries
            .map { (exerciseId, daily) ->
                val sorted = daily.entries.sortedBy { it.key }
                ExerciseProgressSeries(
                    exerciseId = exerciseId,
                    exerciseName = sorted.firstOrNull()?.value?.label ?: "Exercise #$exerciseId",
                    samples = sorted.map { it.key to it.value.bestEst },
                    secondarySamples = sorted.map { it.key to it.value.bestLoad }
                )
            }
            .sortedByDescending { series -> sessions.count { it.exerciseSummaries.containsKey(series.exerciseId) } }

        return ExerciseProgressUi(exercises = exercises.take(5), selectedExerciseId = null, metric = ExerciseProgressMetric.EstimatedOneRm)
    }

    private fun buildSession(
        session: WorkoutWithSetGroups,
        routineNames: Map<Int, String>,
        exerciseNameCache: ExerciseNameCache,
        bodyWeight: Double,
    ): SessionComputation {
        val routineName = routineNames[session.workout.routineId] ?: ""
        val startInstant = session.workout.startTime.toInstant()
        val endInstant = session.workout.endTime.toInstant()
        val durationMinutes = runCatching { Duration.between(startInstant, endInstant).toMinutes().toDouble() }.getOrNull()
        val date = startInstant.atZone(zoneId).toLocalDate()

        val sortedGroups = session.setGroups.sortedBy { it.group.position }
        val plannedWorkingSets = sortedGroups.sumOf { group ->
            group.sets.count { !it.isWarmup }
        }
        val exerciseSummaries = mutableMapOf<Int, ExerciseSummaryBuilder>()
        val sets = mutableListOf<WorkoutSetSample>()
        val exerciseNames = exerciseNameCache.names
        val normalizedNames = exerciseNameCache.normalizedNames

        sortedGroups.forEach { group ->
            val exerciseId = group.group.exerciseId
            val exerciseName = exerciseNames[exerciseId] ?: "Exercise #$exerciseId"
            val normalized = normalizedNames[exerciseId] ?: exerciseName.trim().lowercase(defaultLocale)
            group.sets.forEach setLoop@ { set ->
                if (!set.isPerformanceSet()) return@setLoop
                val normalizedWeight = normalizeWeight(set.weight, bodyWeight)
                sets += WorkoutSetSample(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    normalizedExerciseName = normalized,
                    reps = set.reps,
                    weight = normalizedWeight,
                    rawSet = set,
                    startInstant = startInstant
                )
                exerciseSummaries.getOrPut(exerciseId) { ExerciseSummaryBuilder(exerciseId, exerciseName) }
                    .addSet(set, normalizedWeight)
            }
        }

        val summaries = exerciseSummaries.mapValues { it.value.build() }
        val totalVolume = summaries.values.sumOf { it.totalVolume }
        val totalReps = summaries.values.sumOf { it.totalReps }
        val avgLoadPerRep = if (totalReps == 0) 0.0 else totalVolume / totalReps

        val dailyBest = summaries.mapValues { (_, summary) ->
            val bestLoad = summary.bestLoad ?: 0.0
            val bestEst = summary.bestEst ?: bestLoad
            DailyBest(bestLoad = bestLoad, bestEst = bestEst, label = summary.exerciseName)
        }

        return SessionComputation(
            workout = session.workout,
            routineName = routineName,
            date = date,
            durationMinutes = durationMinutes,
            totalVolume = totalVolume,
            totalReps = totalReps,
            avgLoadPerRep = avgLoadPerRep,
            exerciseSummaries = summaries,
            sets = sets,
            startInstant = startInstant,
            exerciseDailyBest = dailyBest,
            plannedWorkingSets = plannedWorkingSets,
        )
    }

    private fun weekKey(date: LocalDate): String {
        val week = date.get(weekFields.weekOfWeekBasedYear())
        val year = date.get(weekFields.weekBasedYear())
        return "%04d-W%02d".format(year, week)
    }

}

private data class ExerciseNameCache(
    val names: Map<Int, String>,
    val normalizedNames: Map<Int, String>,
)

private data class InsightsSource(
    val workouts: List<WorkoutWithSetGroups>,
    val routineNames: Map<Int, String>,
    val exerciseNameCache: ExerciseNameCache,
)

private data class PreferenceSnapshot(
    val progressMetric: ExerciseProgressMetric,
    val selectedExerciseId: Int?,
    val bodyWeight: Float,
    val currentWorkoutId: Int,
    val insightsDataStartEpochMillis: Long,
)

internal const val WEIGHT_EPSILON = 1e-3
private const val WEIGHT_ROUNDING_DECIMALS = 2

internal data class PrComputationResult(
    val events: List<PrEventUi>,
    val eventsByWorkout: Map<Int, List<PrEventUi>>,
    val currentByType: Map<PrType, List<PrEventUi>>,
)

internal data class WorkoutSetSample(
    val exerciseId: Int,
    val exerciseName: String,
    val normalizedExerciseName: String,
    val reps: Int?,
    val weight: Double?,
    val rawSet: WorkoutSet,
    val startInstant: Instant,
)

internal class ExerciseSummaryBuilder(
    private val exerciseId: Int,
    private val name: String,
) {
    private var totalVolume = 0.0
    private var totalReps = 0
    private var loadSum = 0.0
    private var loadCount = 0
    private var bestLoad = 0.0
    private var bestEst = 0.0
    private var topSet: TopSetUi? = null

    fun addSet(set: WorkoutSet, normalizedWeight: Double?) {
        val reps = set.reps ?: 0
        if (reps > 0) {
            totalReps += reps
        }

        if (normalizedWeight != null) {
            if (reps > 0) {
                totalVolume += reps * normalizedWeight
                loadSum += normalizedWeight
                loadCount += 1
                if (normalizedWeight > bestLoad) {
                    bestLoad = normalizedWeight
                }
                estimatedOneRm(normalizedWeight, reps)?.let { est ->
                    if (est > bestEst) {
                        bestEst = est
                    }
                }
            } else {
                if (normalizedWeight > bestLoad) {
                    bestLoad = normalizedWeight
                }
                if (normalizedWeight > bestEst) {
                    bestEst = normalizedWeight
                }
            }
        }

        val candidate = TopSetUi(
            exerciseId = exerciseId,
            exerciseName = name,
            load = normalizedWeight,
            reps = set.reps,
            estimatedOneRm = normalizedWeight?.let { weight ->
                set.reps?.let { repsForSet -> estimatedOneRm(weight, repsForSet) }
            },
        )
        val current = topSet
        if (current == null || isTopSetBetter(candidate, current)) {
            topSet = candidate
        }
    }

    fun build(): ExerciseSessionSummary {
        return ExerciseSessionSummary(
            exerciseId = exerciseId,
            exerciseName = name,
            totalVolume = totalVolume,
            totalReps = totalReps,
            averageLoad = if (loadCount == 0) 0.0 else loadSum / loadCount,
            bestLoad = if (bestLoad == 0.0) null else bestLoad,
            bestEst = if (bestEst == 0.0) null else bestEst,
            topSet = topSet,
        )
    }
}

internal data class ExerciseSessionSummary(
    val exerciseId: Int,
    val exerciseName: String,
    val totalVolume: Double,
    val totalReps: Int,
    val averageLoad: Double,
    val bestLoad: Double?,
    val bestEst: Double?,
    val topSet: TopSetUi? = null,
)

internal class DailyBest(
    var bestLoad: Double,
    var bestEst: Double,
    var label: String? = null,
)

internal data class SessionComputation(
    val workout: Workout,
    val routineName: String,
    val date: LocalDate,
    val durationMinutes: Double?,
    val totalVolume: Double,
    val totalReps: Int,
    val avgLoadPerRep: Double,
    val exerciseSummaries: Map<Int, ExerciseSessionSummary>,
    val sets: List<WorkoutSetSample>,
    val startInstant: Instant,
    val exerciseDailyBest: Map<Int, DailyBest>,
    val plannedWorkingSets: Int = 0,
)

internal fun computePersonalRecords(
    sessions: List<SessionComputation>,
): PrComputationResult {
    val bestLoad = mutableMapOf<Int, Double>()
    val bestEst = mutableMapOf<Int, Double>()
    val bestRepsAtLoad = mutableMapOf<Int, MutableMap<Double, Int>>()
    val bestLoadEvents = mutableMapOf<Int, PrEventUi>()
    val bestEstEvents = mutableMapOf<Int, PrEventUi>()
    val bestRepsEvents = mutableMapOf<Int, MutableMap<Double, PrEventUi>>()
    val events = mutableListOf<PrEventUi>()
    val perSession = mutableMapOf<Int, MutableList<PrEventUi>>()

    val orderedSessions = sessions.sortedWith(
        compareBy<SessionComputation> { it.startInstant }
            .thenBy { it.workout.workoutId }
    )

    orderedSessions.forEach { session ->
        val candidates = mutableMapOf<Int, MutableMap<PrType, PrCandidate>>()
        val repsCandidates = mutableMapOf<Int, MutableMap<Double, PrCandidate>>()

        session.sets.forEachIndexed { setIndex, sample ->
            if (!sample.rawSet.isPerformanceSet()) return@forEachIndexed
            val load = sample.weight ?: return@forEachIndexed
            val exerciseId = sample.exerciseId
            val reps = sample.reps
            val exerciseCandidates = candidates.getOrPut(exerciseId) { mutableMapOf() }
            val loadCandidate = PrCandidate(
                exerciseId = exerciseId,
                exerciseName = sample.exerciseName,
                setIndex = setIndex,
                value = load,
                reps = reps,
                load = load,
            )
            val previousLoadCandidate = exerciseCandidates[PrType.Load]
            if (previousLoadCandidate == null || isGreater(loadCandidate.value, previousLoadCandidate.value)) {
                exerciseCandidates[PrType.Load] = loadCandidate
            }

            if (reps != null && reps > 0) {
                val loadKey = load
                val byLoad = repsCandidates.getOrPut(exerciseId) { mutableMapOf() }
                val previous = byLoad[loadKey]
                if (previous == null || reps > previous.value) {
                    byLoad[loadKey] = loadCandidate.copy(value = reps.toDouble())
                }

                estimatedOneRm(load, reps)?.let { estimate ->
                    val estimateCandidate = loadCandidate.copy(value = estimate)
                    val previousEstimate = exerciseCandidates[PrType.EstimatedOneRm]
                    if (previousEstimate == null || isGreater(estimate, previousEstimate.value)) {
                        exerciseCandidates[PrType.EstimatedOneRm] = estimateCandidate
                    }
                }
            }
        }

        candidates.forEach { (exerciseId, exerciseCandidates) ->
            val sessionEvents = perSession.getOrPut(session.workout.workoutId) { mutableListOf() }
            val loadCandidate = exerciseCandidates[PrType.Load]
            if (loadCandidate != null) {
                val currentRecord = bestLoad[exerciseId]
                val recordEvent = loadCandidate.toUi(session, PrType.Load, "load")
                if (currentRecord != null && isGreater(loadCandidate.value, currentRecord)) {
                    events += recordEvent
                    sessionEvents += recordEvent
                }
                if (currentRecord == null || isGreater(loadCandidate.value, currentRecord)) {
                    bestLoad[exerciseId] = loadCandidate.value
                    bestLoadEvents[exerciseId] = recordEvent
                }
            }

            val repsByLoad = repsCandidates[exerciseId].orEmpty()
            repsByLoad.forEach { (loadKey, candidate) ->
                val currentRecord = bestRepsAtLoad.getOrPut(exerciseId) { mutableMapOf() }[loadKey]
                val recordEvent = candidate.toUi(session, PrType.RepsAtLoad, "reps")
                if (currentRecord != null && candidate.value > currentRecord) {
                    events += recordEvent
                    sessionEvents += recordEvent
                }
                if (currentRecord == null || candidate.value > currentRecord) {
                    bestRepsAtLoad.getOrPut(exerciseId) { mutableMapOf() }[loadKey] = candidate.value.toInt()
                    bestRepsEvents.getOrPut(exerciseId) { mutableMapOf() }[loadKey] = recordEvent
                }
            }

            val estimateCandidate = exerciseCandidates[PrType.EstimatedOneRm]
            if (estimateCandidate != null) {
                val currentRecord = bestEst[exerciseId]
                val recordEvent = estimateCandidate.toUi(session, PrType.EstimatedOneRm, "est")
                if (currentRecord != null && isGreater(estimateCandidate.value, currentRecord)) {
                    events += recordEvent
                    sessionEvents += recordEvent
                }
                if (currentRecord == null || isGreater(estimateCandidate.value, currentRecord)) {
                    bestEst[exerciseId] = roundToDecimals(estimateCandidate.value, WEIGHT_ROUNDING_DECIMALS)
                    bestEstEvents[exerciseId] = recordEvent.copy(value = bestEst[exerciseId]!!)
                }
            }
        }
    }

    val sortedEvents = events.sortedByDescending { it.occurredAt }
    val sortedPerSession = perSession.mapValues { entry ->
        entry.value.sortedByDescending { it.occurredAt }
    }
    val nameComparator = compareBy<PrEventUi> { it.exerciseName.lowercase(Locale.getDefault()) }
    val loadEvents = bestLoadEvents.values.sortedWith(nameComparator)
    val estEvents = bestEstEvents.values.sortedWith(nameComparator)
    val repsEvents = bestRepsEvents.mapNotNull { (_, entries) ->
        entries.values.maxWithOrNull(compareBy<PrEventUi>({ it.load ?: 0.0 }, { it.value }))
    }.sortedWith(nameComparator)
    val currentByType = mapOf(
        PrType.Load to loadEvents,
        PrType.RepsAtLoad to repsEvents,
        PrType.EstimatedOneRm to estEvents
    ).filterValues { it.isNotEmpty() }
    return PrComputationResult(sortedEvents, sortedPerSession, currentByType)
}

private data class PrCandidate(
    val exerciseId: Int,
    val exerciseName: String,
    val setIndex: Int,
    val value: Double,
    val reps: Int?,
    val load: Double?,
)

private fun PrCandidate.toUi(
    session: SessionComputation,
    type: PrType,
    suffix: String,
): PrEventUi {
    return PrEventUi(
        id = "${session.workout.workoutId}-${exerciseId}-$suffix-$setIndex",
        workoutId = session.workout.workoutId,
        setIndex = setIndex,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        type = type,
        value = value,
        reps = reps,
        load = load,
        occurredAt = session.startInstant,
    )
}

internal fun normalizeWeight(raw: Double?, bodyWeight: Double): Double? {
    val weight = raw ?: return null
    if (!weight.isFinite()) return null
    val adjusted = if (weight < 0) bodyWeight + weight else weight
    val rounded = roundToDecimals(adjusted, WEIGHT_ROUNDING_DECIMALS)
    return if (rounded <= 0.0) null else rounded
}

internal fun roundToDecimals(value: Double, decimals: Int): Double {
    if (decimals <= 0) return round(value)
    val factor = 10.0.pow(decimals)
    return round(value * factor) / factor
}

internal fun epley(load: Double, reps: Int): Double {
    val capped = min(reps, 12)
    return load * (1 + capped / 30.0)
}

internal fun estimatedOneRm(load: Double, reps: Int): Double? {
    if (!load.isFinite() || load <= 0.0 || reps !in 1..12) return null
    return roundToDecimals(epley(load, reps), WEIGHT_ROUNDING_DECIMALS)
}

private fun isTopSetBetter(candidate: TopSetUi, current: TopSetUi): Boolean {
    val candidateScore = candidate.estimatedOneRm ?: candidate.load ?: 0.0
    val currentScore = current.estimatedOneRm ?: current.load ?: 0.0
    if (isGreater(candidateScore, currentScore)) return true
    if (isGreater(currentScore, candidateScore)) return false
    return (candidate.reps ?: 0) > (current.reps ?: 0)
}

private fun isGreater(candidate: Double, current: Double): Boolean {
    return candidate - current > WEIGHT_EPSILON
}
