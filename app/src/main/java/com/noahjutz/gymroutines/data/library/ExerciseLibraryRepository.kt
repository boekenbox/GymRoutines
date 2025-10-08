package com.noahjutz.gymroutines.data.library

import android.content.Context
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class ExerciseLibraryRepository(
    private val context: Context,
    private val json: Json,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _exercises = MutableStateFlow<List<LibraryExercise>>(emptyList())
    val exercises: StateFlow<List<LibraryExercise>> = _exercises.asStateFlow()

    private val _bodyParts = MutableStateFlow<List<String>>(emptyList())
    val bodyParts: StateFlow<List<String>> = _bodyParts.asStateFlow()

    private val _equipments = MutableStateFlow<List<String>>(emptyList())
    val equipments: StateFlow<List<String>> = _equipments.asStateFlow()

    private val _targetMuscles = MutableStateFlow<List<String>>(emptyList())
    val targetMuscles: StateFlow<List<String>> = _targetMuscles.asStateFlow()

    init {
        scope.launch {
            loadLibrary()
        }
    }

    suspend fun loadLibrary() {
        withContext(dispatcher) {
            val assets = context.assets
            val exercisesJson = assets.open(EXERCISES_PATH).bufferedReader().use { it.readText() }
            val exercises = json.decodeFromString(ListSerializer(LibraryExercise.serializer()), exercisesJson)
            _exercises.value = exercises.sortedBy { it.name.lowercase(Locale.getDefault()) }

            _bodyParts.value = loadNamedValues(BODYPARTS_PATH, exercises.flatMap { it.bodyParts })
            _equipments.value = loadNamedValues(EQUIPMENTS_PATH, exercises.flatMap { it.equipments })
            _targetMuscles.value = loadNamedValues(MUSCLES_PATH, exercises.flatMap { it.targetMuscles + it.secondaryMuscles })
        }
    }

    fun getExercise(id: String): LibraryExercise? {
        return _exercises.value.firstOrNull { it.id == id }
    }

    private fun loadNamedValues(path: String, fallback: List<String>): List<String> {
        return try {
            val text = context.assets.open(path).bufferedReader().use { it.readText() }
            val values = json.decodeFromString(ListSerializer(LibraryNamedValue.serializer()), text)
            values.map { it.name }
        } catch (_: Exception) {
            fallback
        }.mapNotNull { it.takeUnless(String::isBlank)?.trim() }
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it }))
    }

    companion object {
        private const val LIBRARY_DIR = "exercise_library"
        private const val EXERCISES_PATH = "$LIBRARY_DIR/exercises.json"
        private const val BODYPARTS_PATH = "$LIBRARY_DIR/bodyparts.json"
        private const val EQUIPMENTS_PATH = "$LIBRARY_DIR/equipments.json"
        private const val MUSCLES_PATH = "$LIBRARY_DIR/muscles.json"
    }
}
