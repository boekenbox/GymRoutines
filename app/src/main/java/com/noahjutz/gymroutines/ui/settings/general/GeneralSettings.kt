package com.noahjutz.gymroutines.ui.settings.general

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.ui.components.TopBar
import org.koin.androidx.compose.getViewModel

@ExperimentalMaterialApi
@Composable
fun GeneralSettings(
    popBackStack: () -> Unit,
    viewModel: GeneralSettingsViewModel = getViewModel()
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_general_settings),
                navigationIcon = {
                    IconButton(onClick = popBackStack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.btn_pop_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        val restTimerSound by viewModel.restTimerSound.collectAsState()
        val restTimerVibration by viewModel.restTimerVibration.collectAsState()
        val (isVisible, setIsVisible) = remember { mutableStateOf(false) }

        Column(Modifier.padding(paddingValues)) {
            ListItem(
                modifier = Modifier.clickable { viewModel.setRestTimerSound(!restTimerSound) },
                text = { Text(stringResource(R.string.pref_rest_timer_sound)) },
                secondaryText = { Text(stringResource(R.string.pref_detail_rest_timer_sound)) },
                icon = { Icon(Icons.Default.VolumeUp, null) },
                trailing = {
                    Switch(
                        checked = restTimerSound,
                        onCheckedChange = { viewModel.setRestTimerSound(it) }
                    )
                }
            )
            Divider()
            ListItem(
                modifier = Modifier.clickable { viewModel.setRestTimerVibration(!restTimerVibration) },
                text = { Text(stringResource(R.string.pref_rest_timer_vibration)) },
                secondaryText = { Text(stringResource(R.string.pref_detail_rest_timer_vibration)) },
                icon = { Icon(Icons.Default.Vibration, null) },
                trailing = {
                    Switch(
                        checked = restTimerVibration,
                        onCheckedChange = { viewModel.setRestTimerVibration(it) }
                    )
                }
            )
            Divider()
            ListItem(
                modifier = Modifier.clickable { setIsVisible(true) },
                text = { Text(stringResource(R.string.pref_reset_settings)) },
                icon = { Icon(Icons.Default.RestartAlt, null) }
            )
        }
        ResetDialog(
            isVisible = isVisible,
            onDismiss = { setIsVisible(false) },
            onReset = { viewModel.resetSettings() }
        )
    }
}

@Composable
private fun ResetDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(stringResource(R.string.dialog_title_reset_settings))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReset()
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.dialog_confirm_reset_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
