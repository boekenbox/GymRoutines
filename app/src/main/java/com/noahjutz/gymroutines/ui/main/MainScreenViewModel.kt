package com.noahjutz.gymroutines.ui.main

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import com.noahjutz.gymroutines.data.AppPrefs
import com.noahjutz.gymroutines.data.ColorTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MainScreenViewModel(
    preferences: DataStore<Preferences>
) : ViewModel() {
    val colorTheme = preferences.data.map { preferences ->
        ColorTheme.fromName(preferences[AppPrefs.AppTheme.key])
    }

    val currentWorkoutId: Flow<Int> = preferences.data.map { preferences ->
        preferences[AppPrefs.CurrentWorkout.key] ?: -1
    }

    val showBottomLabels = preferences.data.map { preferences ->
        preferences[AppPrefs.ShowBottomNavLabels.key] ?: true
    }
}
