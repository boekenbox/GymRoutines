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

package com.noahjutz.gymroutines.ui.exercises.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.domain.Exercise
import com.noahjutz.gymroutines.data.library.ExerciseLibraryRepository
import com.noahjutz.gymroutines.data.library.LibraryExercise
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseListItem(
    val exercise: Exercise,
    val libraryExercise: LibraryExercise?,
)

class ExerciseListViewModel(
    private val repository: ExerciseRepository,
    private val libraryRepository: ExerciseLibraryRepository,
) : ViewModel() {
    private val _nameFilter = MutableStateFlow("")
    val nameFilter = _nameFilter.asStateFlow()

    fun setNameFilter(filter: String) {
        _nameFilter.value = filter
    }

    val exercises = repository.exercises
        .combine(libraryRepository.exercises) { exercises, library ->
            exercises.filterNot(Exercise::hidden) to library.associateBy { it.id }
        }
        .combine(_nameFilter) { (exercises, libraryIndex), nameFilter ->
            val locale = Locale.getDefault()
            val normalizedFilter = nameFilter.trim().lowercase(locale)

            val items = exercises.map { exercise ->
                ExerciseListItem(
                    exercise = exercise,
                    libraryExercise = exercise.libraryExerciseId?.let(libraryIndex::get)
                )
            }

            if (normalizedFilter.isEmpty()) {
                items
            } else {
                items.filter { item ->
                    item.exercise.name.lowercase(locale).contains(normalizedFilter) ||
                        item.libraryExercise?.let { libraryExercise ->
                            libraryExercise.bodyParts.any { it.contains(normalizedFilter, locale) } ||
                                libraryExercise.targetMuscles.any { it.contains(normalizedFilter, locale) } ||
                                libraryExercise.equipments.any { it.contains(normalizedFilter, locale) }
                        } == true
                }
            }
        }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(exercise: Exercise) {
        viewModelScope.launch {
            repository.update(exercise.copy(hidden = true))
        }
    }

    private fun String.contains(other: String, locale: Locale): Boolean {
        return lowercase(locale).contains(other)
    }
}
