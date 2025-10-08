package com.noahjutz.gymroutines.ui.exercises.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.library.ExerciseLibraryRepository
import com.noahjutz.gymroutines.data.library.LibraryExercise
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseLibraryUiState(
    val query: String = "",
    val items: List<LibraryExerciseListItem> = emptyList(),
    val bodyParts: List<String> = emptyList(),
    val equipments: List<String> = emptyList(),
    val muscles: List<String> = emptyList(),
    val selectedBodyPart: String? = null,
    val selectedEquipment: String? = null,
    val selectedMuscle: String? = null,
    val isLoading: Boolean = true,
)

data class LibraryExerciseListItem(
    val exercise: LibraryExercise,
    val isImported: Boolean,
)

private data class LibraryData(
    val exercises: List<LibraryExercise>,
    val bodyParts: List<String>,
    val equipments: List<String>,
    val muscles: List<String>,
)

private data class FilterState(
    val query: String,
    val bodyPart: String?,
    val equipment: String?,
    val muscle: String?,
)

class ExerciseLibraryViewModel(
    private val libraryRepository: ExerciseLibraryRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val locale = Locale.getDefault()

    init {
        libraryRepository.ensureLoaded()
    }

    private val _query = MutableStateFlow("")
    private val _selectedBodyPart = MutableStateFlow<String?>(null)
    private val _selectedEquipment = MutableStateFlow<String?>(null)
    private val _selectedMuscle = MutableStateFlow<String?>(null)

    private val importedIds: StateFlow<Set<String>> = exerciseRepository.exercises
        .map { exercises ->
            exercises
                .filterNot { it.hidden }
                .mapNotNull { it.libraryExerciseId }
                .toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val libraryData = combine(
        libraryRepository.exercises,
        libraryRepository.bodyParts,
        libraryRepository.equipments,
        libraryRepository.targetMuscles,
    ) { exercises, bodyParts, equipments, muscles ->
        LibraryData(
            exercises = exercises,
            bodyParts = bodyParts,
            equipments = equipments,
            muscles = muscles,
        )
    }

    private val filterState = combine(
        _query,
        _selectedBodyPart,
        _selectedEquipment,
        _selectedMuscle,
    ) { query, bodyPart, equipment, muscle ->
        FilterState(
            query = query,
            bodyPart = bodyPart,
            equipment = equipment,
            muscle = muscle,
        )
    }

    val uiState: StateFlow<ExerciseLibraryUiState> = combine(
        libraryData,
        importedIds,
        filterState,
    ) { data, imported, filters ->
        val normalizedQuery = filters.query.trim().lowercase(locale)

        val filteredExercises = data.exercises.filter { exercise ->
            matchesQuery(exercise, normalizedQuery) &&
                matchesBodyPart(exercise, filters.bodyPart) &&
                matchesEquipment(exercise, filters.equipment) &&
                matchesMuscle(exercise, filters.muscle)
        }

        ExerciseLibraryUiState(
            query = filters.query,
            items = filteredExercises
                .sortedBy { it.name.lowercase(locale) }
                .map { LibraryExerciseListItem(it, imported.contains(it.id)) },
            bodyParts = data.bodyParts,
            equipments = data.equipments,
            muscles = data.muscles,
            selectedBodyPart = filters.bodyPart,
            selectedEquipment = filters.equipment,
            selectedMuscle = filters.muscle,
            isLoading = data.exercises.isEmpty()
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLibraryUiState())

    fun setQuery(query: String) {
        _query.value = query
    }

    fun setBodyPart(bodyPart: String?) {
        _selectedBodyPart.value = normalizeSelection(bodyPart)
    }

    fun setEquipment(equipment: String?) {
        _selectedEquipment.value = normalizeSelection(equipment)
    }

    fun setMuscle(muscle: String?) {
        _selectedMuscle.value = normalizeSelection(muscle)
    }

    fun clearFilters() {
        _selectedBodyPart.value = null
        _selectedEquipment.value = null
        _selectedMuscle.value = null
    }

    fun importExercise(exercise: LibraryExercise, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val inserted = exerciseRepository.importLibraryExercise(exercise)
            onComplete(inserted.exerciseId)
        }
    }

    fun getExercise(id: String): LibraryExercise? {
        return libraryRepository.getExercise(id)
    }

    fun isExerciseImported(id: String): Boolean {
        return importedIds.value.contains(id)
    }

    private fun matchesQuery(exercise: LibraryExercise, query: String): Boolean {
        if (query.isBlank()) return true
        val name = exercise.name.lowercase(locale)
        if (name.contains(query)) return true
        return exercise.bodyParts.any { it.lowercase(locale).contains(query) } ||
            exercise.targetMuscles.any { it.lowercase(locale).contains(query) } ||
            exercise.secondaryMuscles.any { it.lowercase(locale).contains(query) } ||
            exercise.equipments.any { it.lowercase(locale).contains(query) }
    }

    private fun matchesBodyPart(exercise: LibraryExercise, bodyPart: String?): Boolean {
        bodyPart ?: return true
        return exercise.bodyParts.any { it.equals(bodyPart, ignoreCase = true) }
    }

    private fun matchesEquipment(exercise: LibraryExercise, equipment: String?): Boolean {
        equipment ?: return true
        return exercise.equipments.any { it.equals(equipment, ignoreCase = true) }
    }

    private fun matchesMuscle(exercise: LibraryExercise, muscle: String?): Boolean {
        muscle ?: return true
        return exercise.targetMuscles.any { it.equals(muscle, ignoreCase = true) } ||
            exercise.secondaryMuscles.any { it.equals(muscle, ignoreCase = true) }
    }

    private fun normalizeSelection(value: String?): String? {
        return value?.lowercase(locale)
    }
}
