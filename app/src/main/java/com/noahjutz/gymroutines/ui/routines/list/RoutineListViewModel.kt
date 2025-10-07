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

package com.noahjutz.gymroutines.ui.routines.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.RoutineRepository
import com.noahjutz.gymroutines.data.domain.Routine
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

class RoutineListViewModel(
    private val repository: RoutineRepository,
) : ViewModel() {
    private val _nameFilter = MutableStateFlow("")
    val nameFilter = _nameFilter.asStateFlow()

    val routines =
        repository.routines
            .combine(nameFilter) { routines, filter ->
                val locale = Locale.getDefault()
                val normalizedFilter = filter.trim().lowercase(locale)
                val visibleRoutines = routines.filterNot(Routine::hidden)

                if (normalizedFilter.isEmpty()) {
                    visibleRoutines
                } else {
                    visibleRoutines.filter { routine ->
                        routine.name.lowercase(locale).contains(normalizedFilter)
                    }
                }
            }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setNameFilter(name: String) {
        _nameFilter.value = name
    }

    fun deleteRoutine(routineId: Int) {
        viewModelScope.launch {
            repository.getRoutine(routineId)?.let { routine ->
                repository.update(routine.copy(hidden = true))
            }
        }
    }

    fun addRoutine(onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insert(Routine())
            onComplete(id)
        }
    }
}
