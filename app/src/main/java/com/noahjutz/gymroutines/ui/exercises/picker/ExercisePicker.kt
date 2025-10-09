/*
 * Splitfit
 * Copyright (C) 2020  Noah Jutz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.noahjutz.gymroutines.ui.exercises.picker

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.ui.components.Chip
import com.noahjutz.gymroutines.ui.components.SearchBar
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.ui.exercises.list.ExerciseListItem
import com.noahjutz.gymroutines.ui.exercises.detail.ExerciseDetailDialog
import com.noahjutz.gymroutines.ui.exercises.detail.toDetailData
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun ExercisePickerSheet(
    viewModel: ExercisePickerViewModel = getViewModel(),
    onExercisesSelected: (List<Int>) -> Unit,
    navToExerciseEditor: () -> Unit,
) {
    val allExercises by viewModel.allExercises.collectAsState()
    val selectedExerciseIds by viewModel.selectedExerciseIdsFlow.collectAsState(initial = emptyList())
    var detailItem by remember { mutableStateOf<ExerciseListItem?>(null) }

    detailItem?.toDetailData()?.let { data ->
        ExerciseDetailDialog(
            data = data,
            onDismiss = { detailItem = null },
            onEdit = null,
            onSave = if (detailItem?.exerciseId == null && detailItem?.entry != null) {
                {
                    detailItem?.let { viewModel.onSelectionChanged(it, true) }
                    detailItem = null
                }
            } else null
        )
    }
    Column {
        TopBar(
            title = stringResource(R.string.screen_pick_exercise),
            navigationIcon = {
                IconButton(
                    onClick = { onExercisesSelected(emptyList()) }
                ) { Icon(Icons.Default.Close, stringResource(R.string.btn_cancel)) }
            },
            actions = {
                TextButton(
                    onClick = { onExercisesSelected(selectedExerciseIds) },
                    enabled = selectedExerciseIds.isNotEmpty()
                ) {
                    Text(stringResource(R.string.btn_select_option))
                }
            }
        )
        val searchQuery by viewModel.nameFilter.collectAsState(initial = "")
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            value = searchQuery,
            onValueChange = viewModel::search
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(allExercises, key = { it.key }) { item ->
                val checked by viewModel.isSelected(item).collectAsState(initial = false)
                ExercisePickerListItem(
                    item = item,
                    checked = checked,
                    onCheckedChange = { selected ->
                        viewModel.onSelectionChanged(item, selected)
                    },
                    onPreview = { detailItem = item }
                )
                Divider()
            }

            item {
                ListItem(
                    modifier = Modifier.clickable(onClick = navToExerciseEditor),
                    icon = { Icon(Icons.Default.Add, null, tint = colors.primary) },
                    text = {
                        Text(
                            stringResource(R.string.btn_new_exercise),
                            color = colors.primary
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ExercisePickerListItem(
    item: ExerciseListItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onPreview: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange),
        icon = { Checkbox(checked = checked, onCheckedChange = null) },
        trailing = {
            IconButton(onClick = onPreview) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = stringResource(R.string.btn_view_details)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.subtitle1
                )
                item.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (item.chips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TagRow(tags = item.chips)
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagRow(tags: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            Chip(
                text = tag,
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            )
        }
    }
}
