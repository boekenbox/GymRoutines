package com.noahjutz.gymroutines.ui.exercises.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryEntry
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryRepository
import com.noahjutz.gymroutines.data.exerciselibrary.displayName
import com.noahjutz.gymroutines.data.exerciselibrary.libraryTag
import com.noahjutz.gymroutines.data.exerciselibrary.toExercise
import com.noahjutz.gymroutines.ui.exercises.list.ExerciseListItem
import com.noahjutz.gymroutines.ui.exercises.list.isSameLibraryEntry
import com.noahjutz.gymroutines.ui.exercises.list.matchesQuery
import com.noahjutz.gymroutines.ui.exercises.list.toCustomListItem
import com.noahjutz.gymroutines.ui.exercises.list.toLibraryListItem
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LIBRARY_TAG_PREFIX = "library:"

class ExercisePickerViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val libraryRepository: ExerciseLibraryRepository,
) : ViewModel() {
    private val locale: Locale = Locale.getDefault()
    private val query = MutableStateFlow("")
    private val selectedExerciseIds = MutableStateFlow(emptyList<Int>())
    private val selectedLibraryIds = MutableStateFlow(emptySet<String>())

    init {
        viewModelScope.launch { libraryRepository.ensureLoaded() }
    }

    fun search(name: String) {
        query.value = name
    }

    val nameFilter: Flow<String> = query

    val allExercises = combine(
        exerciseRepository.exercises,
        libraryRepository.observe(),
        query
    ) { exercises, library, queryValue ->
        val normalizedQuery = queryValue.trim().lowercase(locale)
        val libraryEntries = library?.entries.orEmpty()
        val libraryById = libraryEntries.associateBy { it.id }

        val visibleExercises = exercises.filterNot { it.hidden }
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

        items
            .filter { it.matchesQuery(normalizedQuery) }
            .sortedBy { it.sortKey }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedExerciseIdsFlow: Flow<List<Int>> = selectedExerciseIds

    fun isSelected(item: ExerciseListItem): Flow<Boolean> {
        return combine(selectedExerciseIds, selectedLibraryIds) { exerciseIds, libraryIds ->
            when {
                item.exerciseId != null -> exerciseIds.contains(item.exerciseId)
                item.entry != null -> libraryIds.contains(item.entry.id)
                else -> false
            }
        }
    }

    fun onSelectionChanged(item: ExerciseListItem, selected: Boolean) {
        if (selected) {
            viewModelScope.launch {
                if (item.exerciseId != null) {
                    selectedExerciseIds.update { current ->
                        if (current.contains(item.exerciseId)) current else current + item.exerciseId
                    }
                } else if (item.entry != null) {
                    selectedLibraryIds.update { it + item.entry.id }
                    val exerciseId = ensureExercise(item.entry)
                    if (selectedLibraryIds.value.contains(item.entry.id)) {
                        selectedExerciseIds.update { current ->
                            if (current.contains(exerciseId)) current else current + exerciseId
                        }
                    }
                }
            }
        } else {
            if (item.exerciseId != null) {
                selectedExerciseIds.update { current -> current.filterNot { it == item.exerciseId } }
            }
            item.entry?.let { entry ->
                selectedLibraryIds.update { it - entry.id }
            }
        }
    }

    fun removeExercise(item: ExerciseListItem) {
        onSelectionChanged(item, false)
    }

    private suspend fun ensureExercise(entry: ExerciseLibraryEntry): Int {
        val existing = exerciseRepository.getExerciseByTag(entry.libraryTag)
        val desiredName = entry.displayName(locale)
        return if (existing != null) {
            if (existing.name.equals(entry.name.trim(), ignoreCase = true) && existing.name != desiredName) {
                exerciseRepository.update(existing.copy(name = desiredName))
            }
            existing.exerciseId
        } else {
            exerciseRepository.insert(entry.toExercise()).toInt()
        }
    }
}
