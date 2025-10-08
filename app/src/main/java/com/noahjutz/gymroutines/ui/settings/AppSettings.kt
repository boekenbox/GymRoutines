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

package com.noahjutz.gymroutines.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.util.formatSimple
import org.koin.androidx.compose.getViewModel

@ExperimentalMaterialApi
@Composable
fun AppSettings(
    popBackStack: () -> Unit,
    navToAbout: () -> Unit,
    navToAppearanceSettings: () -> Unit,
    navToDataSettings: () -> Unit,
    navToGeneralSettings: () -> Unit,
    viewModel: AppSettingsViewModel = getViewModel(),
) {
    val bodyWeight by viewModel.bodyWeight.collectAsState()
    val isEditingBodyWeight by viewModel.isEditingBodyWeight.collectAsState()
    val unit = stringResource(R.string.insights_unit_weight)

    if (isEditingBodyWeight) {
        BodyWeightDialog(
            initialValue = bodyWeight,
            unit = unit,
            onDismiss = { viewModel.setEditingBodyWeight(false) },
            onConfirm = { weight ->
                viewModel.updateBodyWeight(weight)
                viewModel.setEditingBodyWeight(false)
            }
        )
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_settings),
                navigationIcon = {
                    IconButton(onClick = popBackStack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.btn_pop_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier.scrollable(
                orientation = Orientation.Vertical,
                state = rememberScrollState()
            ).padding(paddingValues)
        ) {
            ListItem(
                modifier = Modifier.clickable { viewModel.setEditingBodyWeight(true) },
                text = { Text(stringResource(R.string.pref_body_weight)) },
                secondaryText = {
                    Text(
                        text = stringResource(
                            R.string.pref_body_weight_summary,
                            bodyWeight,
                            unit
                        )
                    )
                },
                icon = { Icon(Icons.Default.FitnessCenter, null) }
            )
            ListItem(
                modifier = Modifier.clickable(onClick = navToGeneralSettings),
                text = { Text(stringResource(R.string.screen_general_settings)) },
                icon = { Icon(Icons.Default.Construction, null) }
            )
            ListItem(
                modifier = Modifier.clickable(onClick = navToAppearanceSettings),
                text = { Text(stringResource(R.string.screen_appearance_settings)) },
                icon = { Icon(Icons.Default.DarkMode, null) }
            )
            ListItem(
                modifier = Modifier.clickable(onClick = navToDataSettings),
                text = { Text(stringResource(R.string.screen_data_settings)) },
                icon = { Icon(Icons.Default.Shield, null) },
            )
            Divider()
            ListItem(
                modifier = Modifier.clickable(onClick = navToAbout),
                text = { Text(stringResource(R.string.screen_about)) },
                icon = { Icon(Icons.Default.Info, null) }
            )
        }
    }
}

@Composable
private fun BodyWeightDialog(
    initialValue: Double,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var value by rememberSaveable(initialValue) { mutableStateOf(initialValue.formatSimple()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_title_body_weight)) },
        text = {
            Column {
                Text(text = stringResource(R.string.dialog_body_body_weight, unit))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        isError = false
                    },
                    isError = isError,
                    label = { Text(stringResource(R.string.pref_body_weight)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (isError) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.error_body_weight_invalid),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = value.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        onConfirm(parsed)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text(text = stringResource(R.string.dialog_confirm_body_weight))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
