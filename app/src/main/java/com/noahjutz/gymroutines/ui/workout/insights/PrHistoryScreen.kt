package com.noahjutz.gymroutines.ui.workout.insights

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    val currentPrs = state.currentPrs
    val hasCurrent = currentPrs.byType.values.any { it.isNotEmpty() }
    val hasHistory = prs.isNotEmpty()
    val hasContent = hasCurrent || hasHistory

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
        if (!hasContent) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (hasCurrent) {
                    item {
                        CurrentPrSection(
                            currentPrs = currentPrs,
                            navToWorkout = navToWorkout
                        )
                    }
                }

                if (hasHistory) {
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.insights_prs_history_section_title),
                                style = MaterialTheme.typography.h6
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    items(prs, key = { it.id }) { pr ->
                        PrEventCard(pr = pr, navToWorkout = navToWorkout)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentPrSection(
    currentPrs: CurrentPrsUi,
    navToWorkout: (Int) -> Unit
) {
    val availableTypes = PrType.values().filter { currentPrs.byType[it].orEmpty().isNotEmpty() }
    var selectedType by rememberSaveable { mutableStateOf(PrType.EstimatedOneRm) }

    LaunchedEffect(currentPrs.byType) {
        if (availableTypes.isNotEmpty() && selectedType !in availableTypes) {
            selectedType = availableTypes.first()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.insights_prs_current_title),
            style = MaterialTheme.typography.h6
        )

        if (availableTypes.isEmpty()) {
            Text(
                text = stringResource(R.string.insights_prs_current_empty),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        } else {
            val selectedIndex = availableTypes.indexOf(selectedType).takeIf { it >= 0 } ?: 0
            TabRow(
                selectedTabIndex = selectedIndex,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary
            ) {
                availableTypes.forEachIndexed { index, type ->
                    Tab(
                        selected = index == selectedIndex,
                        onClick = { selectedType = type },
                        text = { Text(text = type.displayName()) }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                currentPrs.byType[selectedType].orEmpty().forEach { pr ->
                    PrEventCard(pr = pr, navToWorkout = navToWorkout)
                }
            }
        }
    }
}

@Composable
private fun PrEventCard(
    pr: PrEventUi,
    navToWorkout: (Int) -> Unit
) {
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
