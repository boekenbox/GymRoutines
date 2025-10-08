package com.noahjutz.gymroutines.data.library

import android.content.Context
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.jvm.Volatile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

class ExerciseLibraryRepository(
    private val context: Context,
    private val json: Json,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val loadMutex = Mutex()
    private val hasLoaded = AtomicBoolean(false)
    @Volatile
    private var loadingJob: Job? = null

    private val _exercises = MutableStateFlow<List<LibraryExercise>>(emptyList())
    val exercises: StateFlow<List<LibraryExercise>> = _exercises.asStateFlow()

    private val _bodyParts = MutableStateFlow<List<String>>(emptyList())
    val bodyParts: StateFlow<List<String>> = _bodyParts.asStateFlow()

    private val _equipments = MutableStateFlow<List<String>>(emptyList())
    val equipments: StateFlow<List<String>> = _equipments.asStateFlow()

    private val _targetMuscles = MutableStateFlow<List<String>>(emptyList())
    val targetMuscles: StateFlow<List<String>> = _targetMuscles.asStateFlow()

    fun ensureLoaded() {
        if (hasLoaded.get()) return
        val currentJob = loadingJob
        if (currentJob?.isActive == true) return
        val job = scope.launch {
            loadLibrary()
        }
        job.invokeOnCompletion {
            if (loadingJob === job) {
                loadingJob = null
            }
        }
        loadingJob = job
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun loadLibrary() {
        if (hasLoaded.get()) return
        withContext(dispatcher) {
            if (hasLoaded.get()) return@withContext
            loadMutex.withLock {
                if (hasLoaded.get()) return@withLock
                try {
                    val assets = context.assets
                    val exercises = assets.open(EXERCISES_PATH).use { stream ->
                        json.decodeFromStream(
                            ListSerializer(LibraryExercise.serializer()),
                            stream
                        )
                    }
                    val sortedExercises =
                        exercises.sortedBy { it.name.lowercase(Locale.getDefault()) }
                    _exercises.value = sortedExercises

                    _bodyParts.value =
                        loadNamedValues(BODYPARTS_PATH, sortedExercises.flatMap { it.bodyParts })
                    _equipments.value =
                        loadNamedValues(EQUIPMENTS_PATH, sortedExercises.flatMap { it.equipments })
                    _targetMuscles.value = loadNamedValues(
                        MUSCLES_PATH,
                        sortedExercises.flatMap { it.targetMuscles + it.secondaryMuscles }
                    )
                    hasLoaded.set(true)
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (throwable: Throwable) {
                    Log.e(TAG, "Failed to load exercise library assets", throwable)
                    _exercises.value = emptyList()
                    _bodyParts.value = emptyList()
                    _equipments.value = emptyList()
                    _targetMuscles.value = emptyList()
                    hasLoaded.set(false)
                }
            }
        }
    }

    fun getExercise(id: String): LibraryExercise? {
        return _exercises.value.firstOrNull { it.id == id }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadNamedValues(path: String, fallback: List<String>): List<String> {
        return try {
            context.assets.open(path).use { stream ->
                json.decodeFromStream(
                    ListSerializer(LibraryNamedValue.serializer()),
                    stream
                )
            }.map { it.name }
        } catch (_: Exception) {
            fallback
        }.mapNotNull { it.takeUnless(String::isBlank)?.trim() }
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it }))
    }

    companion object {
        private const val TAG = "ExerciseLibraryRepo"
        private const val LIBRARY_DIR = "exercise_library"
        private const val EXERCISES_PATH = "$LIBRARY_DIR/exercises.json"
        private const val BODYPARTS_PATH = "$LIBRARY_DIR/bodyparts.json"
        private const val EQUIPMENTS_PATH = "$LIBRARY_DIR/equipments.json"
        private const val MUSCLES_PATH = "$LIBRARY_DIR/muscles.json"
    }
}
