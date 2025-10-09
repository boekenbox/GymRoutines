package com.noahjutz.gymroutines.ui.workout.completed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Undo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.ui.graphics.Brush

@ExperimentalMaterialApi
@Composable
fun WorkoutCompleted(
    workoutId: Int,
    routineId: Int,
    popBackStack: () -> Unit,
    navToWorkoutInProgress: () -> Unit,
    viewModel: WorkoutCompletedViewModel = getViewModel { parametersOf(workoutId, routineId) }
) {
    val palette = MaterialTheme.colors
    val background = remember(palette.primary, palette.background) {
        Brush.verticalGradient(
            listOf(
                palette.background,
                palette.background.copy(alpha = 0.92f)
            )
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = { viewModel.startWorkout(navToWorkoutInProgress) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = palette.primary,
                contentColor = palette.onPrimary
            )
        ) {
            Icon(Icons.Default.Undo, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_continue_workout))
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(palette.secondary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = palette.secondary,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.title_workout_completed), style = typography.h4)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.body_workout_completed), style = typography.h6, color = palette.onSurface.copy(alpha = 0.8f))
                }
                Text(
                    text = stringResource(R.string.body_workout_completed_saved),
                    style = typography.body1,
                    color = palette.onSurface.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        TextButton(
            onClick = popBackStack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(stringResource(R.string.btn_close))
        }
    }
}

@ExperimentalMaterialApi
@Composable
@Preview
fun WorkoutCompletedPreview() {
    MaterialTheme {
        Surface {
            WorkoutCompleted(
                workoutId = 0,
                routineId = 0,
                popBackStack = { },
                navToWorkoutInProgress = { }
            )
        }
    }
}
