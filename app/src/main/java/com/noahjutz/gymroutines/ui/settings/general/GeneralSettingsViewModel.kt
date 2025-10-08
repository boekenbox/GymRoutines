package com.noahjutz.gymroutines.ui.settings.general

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.AppPrefs
import com.noahjutz.gymroutines.data.resetAppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GeneralSettingsViewModel(
    private val preferences: DataStore<Preferences>
) : ViewModel() {
    val restTimerSound: StateFlow<Boolean> = preferences.data
        .map { prefs -> prefs[AppPrefs.RestTimerSound.key] ?: AppPrefs.RestTimerSound.defaultValue }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppPrefs.RestTimerSound.defaultValue
        )

    val restTimerVibration: StateFlow<Boolean> = preferences.data
        .map { prefs -> prefs[AppPrefs.RestTimerVibration.key] ?: AppPrefs.RestTimerVibration.defaultValue }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppPrefs.RestTimerVibration.defaultValue
        )

    fun resetSettings() {
        viewModelScope.launch {
            preferences.resetAppSettings()
        }
    }

    fun setRestTimerSound(enabled: Boolean) {
        viewModelScope.launch {
            preferences.edit { it[AppPrefs.RestTimerSound.key] = enabled }
        }
    }

    fun setRestTimerVibration(enabled: Boolean) {
        viewModelScope.launch {
            preferences.edit { it[AppPrefs.RestTimerVibration.key] = enabled }
        }
    }
}
