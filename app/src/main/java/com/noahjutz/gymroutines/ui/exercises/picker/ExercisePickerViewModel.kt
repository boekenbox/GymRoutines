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

package com.noahjutz.gymroutines.ui.exercises.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.domain.Exercise
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class ExercisePickerViewModel(
    exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val _nameFilter = MutableStateFlow("")
    private val exercises = exerciseRepository.exercises
    private val _selectedExerciseIds = MutableStateFlow(emptyList<Int>())

    fun search(name: String) {
        _nameFilter.value = name
    }

    fun addExercise(exercise: Exercise) {
        val id = exercise.exerciseId
        _selectedExerciseIds.update { current ->
            if (current.contains(id)) current else current + id
        }
    }

    fun removeExercise(exercise: Exercise) {
        val id = exercise.exerciseId
        _selectedExerciseIds.update { current ->
            current.filterNot { it == id }
        }
    }

    val nameFilter = _nameFilter.asStateFlow()

    val allExercises = exercises
        .combine(_nameFilter) { exercises, nameFilter ->
            val locale = Locale.getDefault()
            val normalizedFilter = nameFilter.trim().lowercase(locale)
            val visibleExercises = exercises.filterNot(Exercise::hidden)

            if (normalizedFilter.isEmpty()) {
                visibleExercises
            } else {
                visibleExercises.filter { exercise ->
                    exercise.name.lowercase(locale).contains(normalizedFilter)
                }
            }
        }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selectedExercises = _selectedExerciseIds.asStateFlow()

    val selectedExerciseIds = selectedExercises

    fun exercisesContains(exercise: Exercise) =
        selectedExercises.map { it.contains(exercise.exerciseId) }
}
