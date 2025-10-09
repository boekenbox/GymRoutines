package com.noahjutz.gymroutines.ui.exercises.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.domain.Exercise
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryEntry
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryRepository
import com.noahjutz.gymroutines.data.exerciselibrary.displayName
import com.noahjutz.gymroutines.data.exerciselibrary.libraryTag
import com.noahjutz.gymroutines.data.exerciselibrary.toExercise
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExerciseListViewModel(
    private val repository: ExerciseRepository,
    private val libraryRepository: ExerciseLibraryRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    val searchQuery = query.asStateFlow()
    private val selectedFilters = MutableStateFlow(setOf<String>())
    private val locale: Locale = Locale.getDefault()

    init {
        viewModelScope.launch { libraryRepository.ensureLoaded() }
    }

    fun setNameFilter(filter: String) {
        query.value = filter
    }

    fun toggleFilter(filter: String) {
        selectedFilters.update { current ->
            if (filter in current) current - filter else current + filter
        }
    }

    fun clearFilters() {
        selectedFilters.value = emptySet()
    }

    val uiState: StateFlow<ExerciseListUiState> = combine(
        repository.exercises,
        libraryRepository.observe(),
        query,
        selectedFilters
    ) { exercises, library, queryValue, filters ->
        val normalizedQuery = queryValue.trim().lowercase(locale)
        val libraryEntries = library?.entries.orEmpty()
        val libraryById = libraryEntries.associateBy { it.id }

        val visibleExercises = exercises.filterNot(Exercise::hidden)
        val hiddenLibraryIds = exercises.filter { it.hidden && it.tags.startsWith(LIBRARY_TAG_PREFIX) }
            .mapNotNull { it.tags.removePrefix(LIBRARY_TAG_PREFIX).takeIf(String::isNotBlank) }
            .toSet()

        val items = buildList<ExerciseListItem> {
            visibleExercises.forEach { exercise ->
                if (exercise.tags.startsWith(LIBRARY_TAG_PREFIX)) {
                    val id = exercise.tags.removePrefix(LIBRARY_TAG_PREFIX)
                    val entry = libraryById[id]
                    if (entry != null) {
                        add(exercise.toLibraryListItem(entry, locale))
                    }
                } else {
                    add(exercise.toCustomListItem(locale))
                }
            }

            libraryEntries.forEach { entry ->
                if (entry.id !in hiddenLibraryIds && this.none { it.isSameLibraryEntry(entry.id) }) {
                    add(entry.toLibraryListItem(locale))
                }
            }
        }

        val filteredItems = items
            .filter { item -> item.matchesQuery(normalizedQuery, filters) }
            .sortedBy { it.sortKey }

        val availableFilters = buildList {
            add(CUSTOM_FILTER_TAG)
            library?.metadata?.bodyParts.orEmpty()
                .map { it.formatTag(locale) }
                .forEach { tag -> if (tag !in this) add(tag) }
            library?.metadata?.equipments.orEmpty()
                .map { it.formatTag(locale) }
                .forEach { tag -> if (tag !in this) add(tag) }
        }

        ExerciseListUiState(
            isLoading = library == null,
            query = queryValue,
            selectedFilters = filters,
            availableFilters = availableFilters,
            items = filteredItems
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExerciseListUiState(isLoading = true)
        )

    fun delete(exercise: Exercise) {
        viewModelScope.launch {
            repository.update(exercise.copy(hidden = true))
        }
    }

    fun ensureExercise(entry: ExerciseLibraryEntry, onResult: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val existing = repository.getExerciseByTag(entry.libraryTag)
            val desiredName = entry.displayName(locale)
            val exerciseId = if (existing != null) {
                if (existing.name.equals(entry.name.trim(), ignoreCase = true) && existing.name != desiredName) {
                    repository.update(existing.copy(name = desiredName))
                }
                existing.exerciseId
            } else {
                repository.insert(entry.toExercise()).toInt()
            }
            onResult(exerciseId)
        }
    }

}

data class ExerciseListUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val selectedFilters: Set<String> = emptySet(),
    val availableFilters: List<String> = emptyList(),
    val items: List<ExerciseListItem> = emptyList()
)

data class ExerciseListItem(
    val key: String,
    val title: String,
    val subtitle: String?,
    val chips: List<String>,
    val searchTexts: List<String>,
    val filterTags: Set<String>,
    val sortKey: String,
    val exerciseId: Int?,
    val exercise: Exercise?,
    val entry: ExerciseLibraryEntry?
)
