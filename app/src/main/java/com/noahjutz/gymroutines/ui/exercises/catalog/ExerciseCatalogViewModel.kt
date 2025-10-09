package com.noahjutz.gymroutines.ui.exercises.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryEntry
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryRepository
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseSearchFilters
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseSearchSortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExerciseCatalogViewModel(
    private val repository: ExerciseLibraryRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(ExerciseCatalogFilters())
    private val sort = MutableStateFlow(ExerciseSearchSortOption.RELEVANCE)

    private val _state = MutableStateFlow(
        ExerciseCatalogUiState(
            isLoading = true,
            exercises = emptyList(),
            suggestions = emptyList(),
            total = 0,
            filters = ExerciseCatalogFilters(),
            facets = ExerciseCatalogFacets()
        )
    )
    val state: StateFlow<ExerciseCatalogUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repository.ensureLoaded() }
        viewModelScope.launch {
            combine(query, filters, sort) { query, filters, sort ->
                Triple(query, filters, sort)
            }.collectLatest { (queryValue, filtersValue, sortValue) ->
                refresh(queryValue, filtersValue, sortValue)
            }
        }
    }

    private suspend fun refresh(
        query: String,
        activeFilters: ExerciseCatalogFilters,
        sort: ExerciseSearchSortOption
    ) {
        val library = repository.ensureLoaded()
        val searchFilters = ExerciseSearchFilters(
            bodyParts = activeFilters.bodyParts,
            equipments = activeFilters.equipments,
            primaryMuscles = activeFilters.primaryMuscles,
            secondaryMuscles = activeFilters.secondaryMuscles,
            difficulty = activeFilters.difficulty,
            mechanics = activeFilters.mechanics,
        )
        val result = repository.search(query, searchFilters, sort)
        _state.value = ExerciseCatalogUiState(
            isLoading = false,
            exercises = result.exercises,
            suggestions = result.suggestions,
            total = library.metadata.count,
            filters = activeFilters,
            facets = ExerciseCatalogFacets(
                bodyParts = library.metadata.bodyParts,
                equipments = library.metadata.equipments,
                primaryMuscles = library.metadata.targetMuscles,
                secondaryMuscles = library.metadata.secondaryMuscles,
                difficulties = library.entries.mapNotNull { it.difficulty }.toSet().sorted(),
                mechanics = library.entries.mapNotNull { it.mechanic }.toSet().sorted(),
            ),
            query = query,
            sort = sort
        )
    }

    fun onQueryChanged(value: String) {
        query.value = value
    }

    fun onSortChanged(option: ExerciseSearchSortOption) {
        sort.value = option
    }

    fun toggleBodyPart(value: String) = toggle(filters.value.bodyParts) { updated ->
        filters.update { it.copy(bodyParts = updated) }
    }(value)

    fun toggleEquipment(value: String) = toggle(filters.value.equipments) { updated ->
        filters.update { it.copy(equipments = updated) }
    }(value)

    fun togglePrimaryMuscle(value: String) = toggle(filters.value.primaryMuscles) { updated ->
        filters.update { it.copy(primaryMuscles = updated) }
    }(value)

    fun toggleSecondaryMuscle(value: String) = toggle(filters.value.secondaryMuscles) { updated ->
        filters.update { it.copy(secondaryMuscles = updated) }
    }(value)

    fun toggleDifficulty(value: String) = toggle(filters.value.difficulty) { updated ->
        filters.update { it.copy(difficulty = updated) }
    }(value)

    fun toggleMechanic(value: String) = toggle(filters.value.mechanics) { updated ->
        filters.update { it.copy(mechanics = updated) }
    }(value)

    fun clearFilters() {
        filters.value = ExerciseCatalogFilters()
    }

    fun currentSort(): ExerciseSearchSortOption = sort.value

    fun currentQuery(): String = query.value

    private fun toggle(
        source: Set<String>,
        update: (Set<String>) -> Unit
    ): (String) -> Unit {
        return { value ->
            val normalized = value.trim()
            val updated = if (normalized in source) {
                source - normalized
            } else {
                source + normalized
            }
            update(updated)
        }
    }
}

data class ExerciseCatalogUiState(
    val isLoading: Boolean,
    val exercises: List<ExerciseLibraryEntry>,
    val suggestions: List<String>,
    val total: Int,
    val filters: ExerciseCatalogFilters,
    val facets: ExerciseCatalogFacets,
    val query: String = "",
    val sort: ExerciseSearchSortOption = ExerciseSearchSortOption.RELEVANCE,
)

data class ExerciseCatalogFilters(
    val bodyParts: Set<String> = emptySet(),
    val equipments: Set<String> = emptySet(),
    val primaryMuscles: Set<String> = emptySet(),
    val secondaryMuscles: Set<String> = emptySet(),
    val difficulty: Set<String> = emptySet(),
    val mechanics: Set<String> = emptySet(),
)

data class ExerciseCatalogFacets(
    val bodyParts: List<String> = emptyList(),
    val equipments: List<String> = emptyList(),
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val difficulties: List<String> = emptyList(),
    val mechanics: List<String> = emptyList(),
)
