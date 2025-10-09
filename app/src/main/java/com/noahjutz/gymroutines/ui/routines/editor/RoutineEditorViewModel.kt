/*
 * Splitfit
 * Copyright (C) 2020  Noah Jutz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.noahjutz.gymroutines.ui.routines.editor

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.AppPrefs
import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.RoutineRepository
import com.noahjutz.gymroutines.data.WorkoutRepository
import com.noahjutz.gymroutines.data.domain.Exercise
import com.noahjutz.gymroutines.data.domain.Routine
import com.noahjutz.gymroutines.data.domain.RoutineSet
import com.noahjutz.gymroutines.data.domain.RoutineSetGroup
import com.noahjutz.gymroutines.data.domain.RoutineSetGroupWithSets
import com.noahjutz.gymroutines.data.domain.RoutineWithSetGroups
import com.noahjutz.gymroutines.data.domain.Workout
import com.noahjutz.gymroutines.data.domain.WorkoutSet
import com.noahjutz.gymroutines.data.domain.WorkoutSetGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutineEditorViewModel(
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val preferences: DataStore<Preferences>,
    routineId: Int,
) : ViewModel() {
    val isWorkoutInProgress: Flow<Boolean> =
        preferences.data.map { it[AppPrefs.CurrentWorkout.key]?.let { it >= 0 } ?: false }
    private val routineWithSetGroups = routineRepository.getRoutineWithSetGroups(routineId)
    val routine = routineWithSetGroups
    private val _routineFlow = routineRepository.getRoutineFlow(routineId)
    private var _routine: Routine? = null
    private val _setsFlow = routineRepository.getSetsFlow(routineId)
    private var _sets = emptyList<RoutineSet>()
    private val _setGroupsFlow = routineRepository.getSetGroupsFlow(routineId)
    private var _setGroups = emptyList<RoutineSetGroup>()

    val uiState: StateFlow<RoutineEditorUiState> = combine(
        routineWithSetGroups,
        exerciseRepository.exercises
    ) { routineWithGroups, exercises ->
        RoutineEditorUiState(
            routineWithSetGroups = routineWithGroups,
            exercisesById = exercises.associateBy { it.exerciseId }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoutineEditorUiState()
    )

    init {
        viewModelScope.launch {
            launch {
                _routineFlow.collect { _routine = it }
            }
            launch {
                _setsFlow.collect { _sets = it }
            }
            launch {
                _setGroupsFlow.collect { _setGroups = it }
            }
        }
    }

    fun getExercise(exerciseId: Int): Flow<Exercise?> {
        return exerciseRepository.getExerciseFlow(exerciseId)
    }

    fun updateExerciseNotes(exerciseId: Int, notes: String) {
        viewModelScope.launch {
            exerciseRepository.getExercise(exerciseId)?.let { exercise ->
                exerciseRepository.update(exercise.copy(notes = notes))
            }
        }
    }

    fun updateName(name: String) {
        _routine?.let {
            viewModelScope.launch {
                routineRepository.update(it.copy(name = name))
            }
        }
    }

    fun deleteSet(set: RoutineSet) {
        viewModelScope.launch {
            routineRepository.delete(set)
        }
    }

    fun addSet(setGroup: RoutineSetGroupWithSets) {
        viewModelScope.launch {
            val lastSet = setGroup.sets.lastOrNull()
            routineRepository.insert(
                RoutineSet(
                    groupId = setGroup.group.id,
                    reps = lastSet?.reps,
                    weight = lastSet?.weight,
                    time = lastSet?.time,
                    distance = lastSet?.distance,
                    isWarmup = lastSet?.isWarmup ?: false,
                )
            )
        }
    }

    fun addExercises(exerciseIds: List<Int>) {
        _routine?.let { routine ->
            viewModelScope.launch {
                for (exerciseId in exerciseIds) {
                    exerciseRepository.getExercise(exerciseId)?.let { exercise ->
                        if (!exercise.logReps || !exercise.logWeight) {
                            exerciseRepository.update(
                                exercise.copy(logReps = true, logWeight = true)
                            )
                        }
                    }
                    val setGroup = RoutineSetGroup(
                        exerciseId = exerciseId,
                        routineId = routine.routineId,
                        position = _setGroups.size,
                    )
                    val groupId = routineRepository.insert(setGroup)
                    val set = RoutineSet(
                        groupId = groupId.toInt(),
                    )
                    routineRepository.insert(set)
                }
            }
        }
    }

    fun swapSetGroups(id1: Int, id2: Int) {
        viewModelScope.launch {
            val g1 = routineRepository.getSetGroup(id1)
            val g2 = routineRepository.getSetGroup(id2)
            if (g1 != null && g2 != null) {
                val newG1 = g1.copy(position = g2.position)
                val newG2 = g2.copy(position = g1.position)
                routineRepository.update(newG1)
                routineRepository.update(newG2)
            }
        }
    }

    fun updateReps(set: RoutineSet, reps: Int?) {
        viewModelScope.launch {
            routineRepository.update(set.copy(reps = reps))
        }
    }

    fun updateWeight(set: RoutineSet, weight: Double?) {
        viewModelScope.launch {
            routineRepository.update(set.copy(weight = weight))
        }
    }

    fun updateTime(set: RoutineSet, time: Int?) {
        viewModelScope.launch {
            routineRepository.update(set.copy(time = time))
        }
    }

    fun updateDistance(set: RoutineSet, distance: Double?) {
        viewModelScope.launch {
            routineRepository.update(set.copy(distance = distance))
        }
    }

    fun updateWarmup(set: RoutineSet, isWarmup: Boolean) {
        viewModelScope.launch {
            if (isWarmup && !canBeWarmup(set)) return@launch

            routineRepository.update(set.copy(isWarmup = isWarmup))
        }
    }

    fun updateRestTimers(groupId: Int, warmupSeconds: Int, workingSeconds: Int) {
        viewModelScope.launch {
            routineRepository.getSetGroup(groupId)?.let { group ->
                routineRepository.update(
                    group.copy(
                        restTimerWarmupSeconds = warmupSeconds,
                        restTimerWorkingSeconds = workingSeconds,
                    )
                )
            }
        }
    }

    private fun canBeWarmup(set: RoutineSet): Boolean {
        val setsInGroup = _sets
            .filter { it.groupId == set.groupId }
            .sortedBy { it.routineSetId }

        val targetIndex = setsInGroup.indexOfFirst { it.routineSetId == set.routineSetId }
        if (targetIndex < 0) return true

        return setsInGroup.take(targetIndex).all { it.isWarmup }
    }

    fun startWorkout(onWorkoutStarted: (Long) -> Unit) {
        viewModelScope.launch {
            _routine?.let { _routine ->
                val workout = Workout(
                    routineId = _routine.routineId
                )
                val workoutId = workoutRepository.insert(workout)

                for (routineSetGroup in _setGroups) {
                    val workoutSetGroup = WorkoutSetGroup(
                        workoutId = workoutId.toInt(),
                        exerciseId = routineSetGroup.exerciseId,
                        position = routineSetGroup.position,
                        restTimerWarmupSeconds = routineSetGroup.restTimerWarmupSeconds,
                        restTimerWorkingSeconds = routineSetGroup.restTimerWorkingSeconds,
                    )
                    val setGroupId = workoutRepository.insert(workoutSetGroup)

                    for (routineSet in _sets.filter { it.groupId == routineSetGroup.id }) {
                        val workoutSet = WorkoutSet(
                            groupId = setGroupId.toInt(),
                            reps = routineSet.reps,
                            weight = routineSet.weight,
                            time = routineSet.time,
                            distance = routineSet.distance,
                            complete = false,
                            isWarmup = routineSet.isWarmup,
                        )
                        workoutRepository.insert(workoutSet)
                    }
                }

                preferences.edit {
                    it[AppPrefs.CurrentWorkout.key] = workoutId.toInt()
                }

                onWorkoutStarted(workoutId)
            }
        }
    }
}

data class RoutineEditorUiState(
    val routineWithSetGroups: RoutineWithSetGroups? = null,
    val exercisesById: Map<Int, Exercise> = emptyMap(),
)
