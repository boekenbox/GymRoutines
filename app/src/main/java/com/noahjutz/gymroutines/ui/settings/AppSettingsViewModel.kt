package com.noahjutz.gymroutines.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.AppPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppSettingsViewModel(
    private val preferences: DataStore<Preferences>,
) : ViewModel() {

    private val bodyWeightFlow = preferences.data.map { prefs ->
        prefs[AppPrefs.BodyWeight.key] ?: AppPrefs.BodyWeight.defaultValue
    }

    val bodyWeight: StateFlow<Double> = bodyWeightFlow
        .map { it.toDouble() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppPrefs.BodyWeight.defaultValue.toDouble()
        )

    private val _isEditingBodyWeight = MutableStateFlow(false)
    val isEditingBodyWeight: StateFlow<Boolean> = _isEditingBodyWeight

    fun setEditingBodyWeight(isEditing: Boolean) {
        _isEditingBodyWeight.value = isEditing
    }

    fun updateBodyWeight(weight: Double) {
        viewModelScope.launch {
            preferences.edit { prefs ->
                prefs[AppPrefs.BodyWeight.key] = weight.toFloat()
            }
        }
    }
}
