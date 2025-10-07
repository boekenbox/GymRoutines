package com.noahjutz.gymroutines.ui.workout.insights

import java.time.Instant
import java.time.LocalDate

internal data class WorkoutDurationChartData(
    val aggregated: List<Pair<Float, Float>>,
    val raw: List<Pair<Float, Float>>
)

data class SessionSummaryUi(
    val workoutId: Int,
    val routineName: String,
    val date: LocalDate,
    val totalVolume: Double,
    val totalReps: Int,
    val avgLoadPerRep: Double,
    val durationMinutes: Double?,
    val prCount: Int
)

data class WeeklyVolumePoint(
    val weekKey: String,
    val totalVolume: Double,
    val workoutsCount: Int,
    val rollingAverage: Double?
)

data class WeeklyVolumeOverview(
    val points: List<WeeklyVolumePoint>,
    val comparisonPercent: Double?
)

data class SessionComparisonRow(
    val exerciseName: String,
    val loadDelta: Double,
    val repsDelta: Int,
    val volumeDelta: Double
)

data class SessionComparisonUi(
    val latestWorkoutId: Int,
    val routineName: String,
    val comparisonDate: LocalDate?,
    val rows: List<SessionComparisonRow>,
    val sessionVolumeDelta: Double,
    val isFirstTime: Boolean
)

enum class PrType { Load, RepsAtLoad, EstimatedOneRm }

data class PrEventUi(
    val id: String,
    val workoutId: Int,
    val setIndex: Int,
    val exerciseName: String,
    val type: PrType,
    val value: Double,
    val reps: Int?,
    val load: Double?,
    val occurredAt: Instant
)

data class ConsistencyUi(
    val workoutsPerWeekAverage: Double,
    val daysSinceLastWorkout: Long?,
    val currentStreak: Int
)

data class RoutineUsageUi(
    val routineName: String,
    val usageCount: Int,
    val averageDaysBetween: Double?
)

data class RoutineUtilizationUi(
    val routines: List<RoutineUsageUi>,
    val exerciseUsage: List<Pair<String, Int>>
)

enum class ExerciseProgressMetric { Load, EstimatedOneRm }

data class ExerciseProgressSeries(
    val exerciseId: Int,
    val exerciseName: String,
    val samples: List<Pair<LocalDate, Double>>,
    val secondarySamples: List<Pair<LocalDate, Double>>
)

data class ExerciseProgressUi(
    val exercises: List<ExerciseProgressSeries>,
    val selectedExerciseId: Int?,
    val metric: ExerciseProgressMetric
)

data class WorkoutInsightsUiState(
    val isLoading: Boolean = true,
    val durationChart: WorkoutDurationChartData? = null,
    val lastSessionSummary: SessionSummaryUi? = null,
    val weeklyVolume: WeeklyVolumeOverview? = null,
    val sessionComparison: SessionComparisonUi? = null,
    val prs: List<PrEventUi> = emptyList(),
    val exerciseProgress: ExerciseProgressUi = ExerciseProgressUi(emptyList(), null, ExerciseProgressMetric.EstimatedOneRm),
    val consistency: ConsistencyUi? = null,
    val routineUtilization: RoutineUtilizationUi? = null
)
