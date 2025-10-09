package com.noahjutz.gymroutines.data.exerciselibrary

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val INDEX_PATH = "exercise_index/exercise_library.json"
private const val METADATA_PATH = "exercise_index/metadata.json"

class ExerciseLibraryRepository(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val loadMutex = Mutex()
    private val state = MutableStateFlow<ExerciseLibrary?>(null)
    private var engine: ExerciseSearchEngine? = null

    suspend fun ensureLoaded(): ExerciseLibrary {
        state.value?.let { return it }
        return loadMutex.withLock {
            state.value?.let { return it }
            val (entries, metadata) = withContext(dispatcher) {
                val entries = context.assets.open(INDEX_PATH).use { input ->
                    val text = input.bufferedReader().use { it.readText() }
                    json.decodeFromString(ListSerializer(ExerciseLibraryEntry.serializer()), text)
                }
                val metadata = context.assets.open(METADATA_PATH).use { input ->
                    val text = input.bufferedReader().use { it.readText() }
                    json.decodeFromString(ExerciseLibraryMetadata.serializer(), text)
                }
                entries to metadata
            }
            ExerciseLibrary(entries = entries, metadata = metadata).also {
                engine = ExerciseSearchEngine(it.entries)
                state.value = it
            }
        }
    }

    fun observe(): Flow<ExerciseLibrary?> = state.asStateFlow()

    suspend fun search(
        query: String,
        filters: ExerciseSearchFilters,
        sort: ExerciseSearchSortOption
    ): ExerciseSearchResult {
        val library = ensureLoaded()
        val searchEngine = engine ?: ExerciseSearchEngine(library.entries).also { engine = it }
        return searchEngine.search(query, filters, sort)
    }

    suspend fun getExercise(id: String): ExerciseLibraryEntry? {
        val library = ensureLoaded()
        return library.entries.firstOrNull { it.id == id }
    }

    suspend fun relatedExercises(
        primaryMuscle: String?,
        equipment: String?,
        excludeId: String,
        limit: Int = 6
    ): List<ExerciseLibraryEntry> {
        val library = ensureLoaded()
        val entries = library.entries.filter { it.id != excludeId }
        val primaryMatches = primaryMuscle?.let { muscle ->
            entries.filter { muscle in it.targetMuscles }
        }.orEmpty()
        val equipmentMatches = equipment?.let { gear ->
            entries.filter { gear in it.equipments }
        }.orEmpty()

        val combined = (primaryMatches + equipmentMatches)
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

        return combined.take(limit)
    }
}

data class ExerciseLibrary(
    val entries: List<ExerciseLibraryEntry>,
    val metadata: ExerciseLibraryMetadata,
)
