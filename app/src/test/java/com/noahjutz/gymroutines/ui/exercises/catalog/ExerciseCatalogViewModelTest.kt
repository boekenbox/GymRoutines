package com.noahjutz.gymroutines.ui.exercises.catalog

import com.noahjutz.gymroutines.MainDispatcherRule
import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibrary
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryMetadata
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryRepository
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseSearchFilters
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseSearchResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseCatalogViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun queryRemainsVisibleWhileCatalogRefreshes() = runTest {
        val libraryRepository = mockk<ExerciseLibraryRepository>()
        val exerciseRepository = mockk<ExerciseRepository>()
        val library = ExerciseLibrary(
            entries = emptyList(),
            metadata = ExerciseLibraryMetadata(
                count = 0,
                bodyParts = emptyList(),
                equipments = emptyList(),
                targetMuscles = emptyList(),
                secondaryMuscles = emptyList()
            )
        )
        val pendingResult = CompletableDeferred<ExerciseSearchResult>()

        coEvery { libraryRepository.ensureLoaded() } returns library
        coEvery {
            libraryRepository.search(any(), any<ExerciseSearchFilters>(), any())
        } coAnswers {
            pendingResult.await()
        }
        every { exerciseRepository.exercises } returns MutableStateFlow(emptyList())

        val viewModel = ExerciseCatalogViewModel(libraryRepository, exerciseRepository)

        val query = "bench press"
        viewModel.onQueryChanged(query)

        runCurrent()

        val loadingState = viewModel.state.value
        assertTrue(loadingState.isLoading)
        assertEquals(query, loadingState.query)

        pendingResult.complete(ExerciseSearchResult(emptyList(), emptyList()))
        advanceUntilIdle()

        val finalState = viewModel.state.value
        assertFalse(finalState.isLoading)
        assertEquals(query, finalState.query)
    }
}
