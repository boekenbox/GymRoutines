package com.noahjutz.gymroutines.ui.workout.insights

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.ui.components.TopBar
import org.koin.androidx.compose.getViewModel

@Composable
fun PrHistoryScreen(
    viewModel: WorkoutInsightsViewModel = getViewModel(),
    popBackStack: () -> Unit,
    navToWorkout: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val prs = state.prs

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.insights_prs_history_title),
                navigationIcon = {
                    IconButton(onClick = popBackStack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.btn_pop_back))
                    }
                }
            )
        }
    ) { padding ->
        if (prs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.insights_prs_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(prs, key = { it.id }) { pr ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        elevation = 2.dp
                    ) {
                        PrRow(
                            pr = pr,
                            onClick = { navToWorkout(pr.workoutId) },
                            modifier = Modifier
                                .clickable { navToWorkout(pr.workoutId) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
