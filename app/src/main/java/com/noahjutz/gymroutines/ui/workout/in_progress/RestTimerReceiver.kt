package com.noahjutz.gymroutines.ui.workout.in_progress

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import com.noahjutz.gymroutines.data.AppPrefs
import com.noahjutz.gymroutines.data.datastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferences = appContext.datastore
                val stored = preferences.data.first()
                val expectedEnd = intent.getLongExtra(EXTRA_END_EPOCH_MILLIS, 0L)
                val storedEnd = stored[AppPrefs.RestTimerEndEpochMillis.key]
                    ?: AppPrefs.RestTimerEndEpochMillis.defaultValue
                val workoutId = intent.getIntExtra(EXTRA_WORKOUT_ID, -1)
                val storedWorkoutId = stored[AppPrefs.RestTimerWorkoutId.key]
                    ?: AppPrefs.RestTimerWorkoutId.defaultValue
                if (expectedEnd <= 0L || expectedEnd != storedEnd || workoutId != storedWorkoutId) {
                    return@launch
                }

                showRestTimerCompleteNotification(
                    context = appContext,
                    workoutId = workoutId,
                    isWarmup = intent.getBooleanExtra(EXTRA_IS_WARMUP, false),
                    soundEnabled = stored[AppPrefs.RestTimerSound.key]
                        ?: AppPrefs.RestTimerSound.defaultValue,
                    vibrationEnabled = stored[AppPrefs.RestTimerVibration.key]
                        ?: AppPrefs.RestTimerVibration.defaultValue,
                )
                preferences.edit { values ->
                    values.remove(AppPrefs.RestTimerEndEpochMillis.key)
                    values.remove(AppPrefs.RestTimerWorkoutId.key)
                    values.remove(AppPrefs.RestTimerSetId.key)
                    values.remove(AppPrefs.RestTimerGroupId.key)
                    values.remove(AppPrefs.RestTimerIsWarmup.key)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_END_EPOCH_MILLIS = "extra_end_epoch_millis"
        const val EXTRA_WORKOUT_ID = "extra_workout_id"
        const val EXTRA_SET_ID = "extra_set_id"
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_IS_WARMUP = "extra_is_warmup"
    }
}
