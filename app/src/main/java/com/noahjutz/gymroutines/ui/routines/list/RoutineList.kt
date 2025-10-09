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

package com.noahjutz.gymroutines.ui.routines.list

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.Routine
import com.noahjutz.gymroutines.ui.components.SearchBar
import com.noahjutz.gymroutines.ui.components.SwipeToDeleteBackground
import com.noahjutz.gymroutines.ui.components.TopBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun RoutineList(
    navToRoutineEditor: (Long) -> Unit,
    navToSettings: () -> Unit,
    viewModel: RoutineListViewModel = getViewModel(),
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_routine_list),
                actions = {
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.btn_more))
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(onClick = navToSettings) {
                                Text(stringResource(R.string.screen_settings))
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.defaultMinSize(minHeight = 52.dp),
                onClick = {
                    viewModel.addRoutine(
                        onComplete = { id ->
                            navToRoutineEditor(id)
                        }
                    )
                },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.btn_new_routine)) },
                shape = MaterialTheme.shapes.large,
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 10.dp
                )
            )
        },
    ) { paddingValues ->
        val routines by viewModel.routines.collectAsState()

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val routineList = routines
            if (routineList == null) {
                RoutineListPlaceholder()
            } else {
                RoutineListContent(
                    routines = routineList,
                    navToRoutineEditor = navToRoutineEditor,
                    viewModel = viewModel
                )
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun RoutineListContent(
    routines: List<Routine>,
    navToRoutineEditor: (Long) -> Unit,
    viewModel: RoutineListViewModel
) {
    val scope = rememberCoroutineScope()
    val nameFilter by viewModel.nameFilter.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                value = nameFilter,
                onValueChange = viewModel::setNameFilter
            )
        }

        items(
            items = routines,
            key = { it.routineId },
            contentType = { "routine" }
        ) { routine ->
            val dismissState = rememberDismissState()

            SwipeToDismiss(
                modifier = Modifier
                    .animateItemPlacement()
                    .zIndex(if (dismissState.offset.value == 0f) 0f else 1f),
                state = dismissState,
                background = { SwipeToDeleteBackground(dismissState) }
            ) {
                val elevation by animateDpAsState(
                    if (dismissState.dismissDirection != null) 8.dp else 3.dp
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { navToRoutineEditor(routine.routineId.toLong()) },
                    shape = MaterialTheme.shapes.large,
                    elevation = elevation
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val initial = routine.name.trim().takeIf { it.isNotEmpty() }?.first()
                                    ?.uppercase()
                                    ?: stringResource(R.string.unnamed_routine).first().uppercase()
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colors.primary
                                )
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = routine.name.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.unnamed_routine),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.routine_card_hint),
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        expanded = false
                                        scope.launch {
                                            dismissState.dismiss(DismissDirection.StartToEnd)
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.btn_delete))
                                }
                            }
                        }
                    }
                }
            }

            if (dismissState.targetValue != DismissValue.Default) {
                AlertDialog(
                    title = {
                        Text(
                            stringResource(
                                R.string.dialog_title_delete,
                                routine.name.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.unnamed_routine)
                            )
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.deleteRoutine(routine.routineId) },
                            content = { Text(stringResource(R.string.btn_delete)) }
                        )
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { scope.launch { dismissState.reset() } },
                            content = { Text(stringResource(R.string.btn_cancel)) }
                        )
                    },
                    onDismissRequest = { scope.launch { dismissState.reset() } }
                )
            }
        }
        item {
            // Fix FAB overlap
            Spacer(Modifier.height(56.dp))
        }
    }
}
