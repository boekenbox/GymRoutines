package com.noahjutz.gymroutines.ui.workout.insights

import com.noahjutz.gymroutines.data.domain.Workout
import com.noahjutz.gymroutines.data.domain.WorkoutSet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class WorkoutInsightsViewModelTest {

    @Test
    fun increasingLoadProducesLoadAndOneRmPrs() {
        val exerciseId = 101
        val baseInstant = Instant.parse("2023-01-01T10:00:00Z")
        val sessions = listOf(
            createSession(
                workoutId = 1,
                instant = baseInstant,
                exerciseId = exerciseId,
                exerciseName = "Shoulder Press",
                weight = 15.0,
                reps = 8,
            ),
            createSession(
                workoutId = 2,
                instant = baseInstant.plus(1, ChronoUnit.DAYS),
                exerciseId = exerciseId,
                exerciseName = "Shoulder Press",
                weight = 20.0,
                reps = 8,
            ),
        )

        val result = computePersonalRecords(sessions)

        val loadEvents = result.events.filter { it.type == PrType.Load }
        assertEquals(2, loadEvents.size)
        assertEquals(20.0, loadEvents.first().value, 1e-6)

        val oneRmEvents = result.events.filter { it.type == PrType.EstimatedOneRm }
        assertEquals(2, oneRmEvents.size)
        assertTrue(oneRmEvents.first().value > oneRmEvents.last().value)

        val secondWorkoutEvents = result.eventsByWorkout[2]
        assertNotNull(secondWorkoutEvents)
        assertTrue(secondWorkoutEvents.any { it.type == PrType.Load && it.value == 20.0 })
    }

    @Test
    fun zeroWeightsAreIgnoredAndNegativeWeightsIncludeBodyWeight() {
        val baseInstant = Instant.parse("2023-02-01T08:00:00Z")
        val sessions = listOf(
            createSession(
                workoutId = 10,
                instant = baseInstant,
                exerciseId = 33,
                exerciseName = "Bench Press",
                weight = 0.0,
                reps = 10,
                bodyWeight = 82.5,
            ),
            createSession(
                workoutId = 11,
                instant = baseInstant.plus(1, ChronoUnit.DAYS),
                exerciseId = 33,
                exerciseName = "Bench Press",
                weight = -5.0,
                reps = 8,
                bodyWeight = 82.5,
            ),
        )

        val result = computePersonalRecords(sessions)

        assertTrue(result.eventsByWorkout[10].isNullOrEmpty())
        val workout11Events = result.eventsByWorkout[11]
        assertNotNull(workout11Events)
        val loadPr = workout11Events.firstOrNull { it.type == PrType.Load }
        assertNotNull(loadPr)
        assertEquals(77.5, loadPr.value, 1e-6)
        assertTrue(result.events.any { it.type == PrType.Load && it.value == 77.5 })
    }

    @Test
    fun repeatedWeightsWithNoiseDoNotCreateDuplicateRecords() {
        val baseInstant = Instant.parse("2023-03-10T07:30:00Z")
        val exerciseId = 12
        val sessions = listOf(
            createSession(
                workoutId = 21,
                instant = baseInstant,
                exerciseId = exerciseId,
                exerciseName = "Deadlift",
                weight = 20.0,
                reps = 5,
            ),
            createSession(
                workoutId = 22,
                instant = baseInstant.plus(1, ChronoUnit.DAYS),
                exerciseId = exerciseId,
                exerciseName = "Deadlift",
                weight = 20.0004,
                reps = 5,
            ),
            createSession(
                workoutId = 23,
                instant = baseInstant.plus(2, ChronoUnit.DAYS),
                exerciseId = exerciseId,
                exerciseName = "Deadlift",
                weight = 20.1,
                reps = 5,
            ),
        )

        val result = computePersonalRecords(sessions)

        val loadEvents = result.events.filter { it.type == PrType.Load }
        assertEquals(2, loadEvents.size)
        assertTrue(result.eventsByWorkout[22].isNullOrEmpty())
        assertTrue(result.eventsByWorkout[23]?.any { it.type == PrType.Load } == true)
    }

    @Test
    fun normalizeWeightDropsInvalidValuesAndRounds() {
        assertNull(normalizeWeight(null, 80.0))
        assertNull(normalizeWeight(Double.NaN, 80.0))
        assertNull(normalizeWeight(-120.0, 80.0))
        assertEquals(75.0, normalizeWeight(-5.0, 80.0) ?: error("warmup should normalize"), 1e-6)
        assertEquals(20.12, normalizeWeight(20.123, 80.0) ?: error("weight should normalize"), 1e-6)
    }

    private fun createSession(
        workoutId: Int,
        instant: Instant,
        exerciseId: Int,
        exerciseName: String,
        weight: Double?,
        reps: Int?,
        bodyWeight: Double = 80.0,
    ): SessionComputation {
        val normalizedWeight = normalizeWeight(weight, bodyWeight)
        val workout = Workout(
            routineId = 1,
            startTime = Date.from(instant),
            endTime = Date.from(instant.plus(1, ChronoUnit.HOURS)),
            workoutId = workoutId,
        )
        val totalReps = reps ?: 0
        val totalVolume = if (normalizedWeight != null && totalReps > 0) normalizedWeight * totalReps else 0.0
        val averageLoad = if (normalizedWeight != null && totalReps > 0) normalizedWeight else 0.0
        val bestLoad = normalizedWeight
        val bestEst = if (normalizedWeight != null && totalReps > 0) {
            roundToDecimals(epley(normalizedWeight, totalReps), 2)
        } else {
            normalizedWeight
        }
        val summary = ExerciseSessionSummary(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            totalVolume = totalVolume,
            totalReps = totalReps,
            averageLoad = averageLoad,
            bestLoad = bestLoad,
            bestEst = bestEst,
        )
        val dailyBest = DailyBest(
            bestLoad = bestLoad ?: 0.0,
            bestEst = bestEst ?: (bestLoad ?: 0.0),
            label = exerciseName,
        )
        val set = WorkoutSet(
            groupId = 1,
            reps = reps,
            weight = weight,
        )
        val sample = WorkoutSetSample(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            normalizedExerciseName = exerciseName.lowercase(),
            reps = reps,
            weight = normalizedWeight,
            rawSet = set,
            startInstant = instant,
        )

        return SessionComputation(
            workout = workout,
            routineName = "Routine",
            date = instant.atZone(ZoneId.systemDefault()).toLocalDate(),
            durationMinutes = 60.0,
            totalVolume = totalVolume,
            totalReps = totalReps,
            avgLoadPerRep = if (totalReps == 0) 0.0 else totalVolume / totalReps,
            exerciseSummaries = mapOf(exerciseId to summary),
            sets = listOf(sample),
            startInstant = instant,
            exerciseDailyBest = mapOf(exerciseId to dailyBest),
        )
    }
}
