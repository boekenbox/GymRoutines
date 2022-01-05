package com.noahjutz.gymroutines.ui.workout.completed

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.data.ColorTheme
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.ui.theme.SplitfitTheme
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@ExperimentalMaterialApi
@Composable
fun WorkoutCompleted(
    routineId: Int,
    workoutId: Int,
    popBackStack: () -> Unit,
    viewModel: WorkoutCompletedViewModel = getViewModel { parametersOf(routineId, workoutId) }
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopBar(
                navigationIcon = {
                    IconButton(onClick = popBackStack) { Icon(Icons.Default.ArrowBack, "back") }
                },
                title = ""
            )
        }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp, horizontal = 24.dp),
            ) {
                Text("Workout complete!", style = typography.h4)
                if (state is WorkoutCompletedViewModel.State.Found) {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.updateRoutine()
                            popBackStack()
                        }
                    ) {
                        Text("Update Routine")
                    }
                }
            }
            TextButton(
                onClick = popBackStack,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            ) {
                Text("Close")
            }
        }
    }
}

@ExperimentalMaterialApi
@Composable
@Preview
fun WorkoutCompletedPreview() {
    SplitfitTheme(colors = ColorTheme.Black) {
        WorkoutCompleted(
            popBackStack = {},
            routineId = -1,
            workoutId = -1
        )
    }
}
