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

package com.noahjutz.gymroutines.ui.workout.in_progress

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.AppPrefs
import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.RoutineRepository
import com.noahjutz.gymroutines.data.WorkoutRepository
import com.noahjutz.gymroutines.data.domain.Exercise
import com.noahjutz.gymroutines.data.domain.WorkoutSet
import com.noahjutz.gymroutines.data.domain.WorkoutSetGroup
import com.noahjutz.gymroutines.data.domain.WorkoutSetGroupWithSets
import com.noahjutz.gymroutines.data.domain.WorkoutWithSetGroups
import com.noahjutz.gymroutines.util.formatRestDuration
import com.noahjutz.gymroutines.REST_TIMER_CHANNEL_ID
import com.noahjutz.gymroutines.ui.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

private const val REST_TIMER_NOTIFICATION_ID = 1

data class RestTimerState(
    val setId: Int,
    val groupId: Int,
    val isWarmup: Boolean,
    val remainingSeconds: Int,
)

class WorkoutInProgressViewModel(
    private val preferences: DataStore<Preferences>,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    private val application: Application,
    private val workoutId: Int,
) : ViewModel() {
    val workout = workoutRepository.getWorkoutFlow(workoutId)
    private var _workout: WorkoutWithSetGroups? = null
    private val _restTimerState = MutableStateFlow<RestTimerState?>(null)
    val restTimerState: StateFlow<RestTimerState?> = _restTimerState.asStateFlow()
    private var restTimerJob: Job? = null
    private var restTimerSoundEnabled = AppPrefs.RestTimerSound.defaultValue
    private var restTimerVibrationEnabled = AppPrefs.RestTimerVibration.defaultValue

    val routineName = workout.map {
        it?.workout?.routineId?.let { routineId ->
            routineRepository.getRoutine(routineId)?.name
        } ?: application.resources.getString(R.string.unnamed_routine)
    }

    init {
        viewModelScope.launch {
            launch {
                workout.collect { workout ->
                    _workout = workout
                }
            }
            launch {
                preferences.data.collect { prefs ->
                    restTimerSoundEnabled =
                        prefs[AppPrefs.RestTimerSound.key] ?: AppPrefs.RestTimerSound.defaultValue
                    restTimerVibrationEnabled =
                        prefs[AppPrefs.RestTimerVibration.key]
                            ?: AppPrefs.RestTimerVibration.defaultValue
                }
            }
            launch {
                while (true) {
                    setEndTime(Calendar.getInstance().time)
                    delay(1000)
                }
            }
        }
    }

    fun getExercise(exerciseId: Int): Flow<Exercise?> {
        return exerciseRepository.getExerciseFlow(exerciseId)
    }

    fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            workoutRepository.delete(set)
        }
    }

    fun addSet(setGroup: WorkoutSetGroupWithSets) {
        viewModelScope.launch {
            val lastSet = setGroup.sets.lastOrNull()
            workoutRepository.insert(
                WorkoutSet(
                    groupId = setGroup.group.id,
                    reps = lastSet?.reps,
                    weight = lastSet?.weight,
                    time = lastSet?.time,
                    distance = lastSet?.distance,
                    isWarmup = lastSet?.isWarmup ?: false,
                )
            )
        }
    }

    fun addExercises(exerciseIds: List<Int>) {
        _workout?.let { workout ->
            viewModelScope.launch {
                for (exerciseId in exerciseIds) {
                    val setGroup = WorkoutSetGroup(
                        exerciseId = exerciseId,
                        workoutId = workout.workout.workoutId,
                        position = workout.setGroups.size,
                    )
                    val groupId = workoutRepository.insert(setGroup)
                    val set = WorkoutSet(
                        groupId = groupId.toInt(),
                    )
                    workoutRepository.insert(set)
                }
            }
        }
    }

    fun swapSetGroups(id1: Int, id2: Int) {
        viewModelScope.launch {
            val g1 = workoutRepository.getSetGroup(id1)
            val g2 = workoutRepository.getSetGroup(id2)
            if (g1 != null && g2 != null) {
                val newG1 = g1.copy(position = g2.position)
                val newG2 = g2.copy(position = g1.position)
                workoutRepository.update(newG1)
                workoutRepository.update(newG2)
            }
        }
    }

    fun updateReps(set: WorkoutSet, reps: Int?) {
        viewModelScope.launch {
            workoutRepository.update(set.copy(reps = reps))
        }
    }

    fun updateWeight(set: WorkoutSet, weight: Double?) {
        viewModelScope.launch {
            workoutRepository.update(set.copy(weight = weight))
        }
    }

    fun updateTime(set: WorkoutSet, time: Int?) {
        viewModelScope.launch {
            workoutRepository.update(set.copy(time = time))
        }
    }

    fun updateDistance(set: WorkoutSet, distance: Double?) {
        viewModelScope.launch {
            workoutRepository.update(set.copy(distance = distance))
        }
    }

    fun updateWarmup(set: WorkoutSet, isWarmup: Boolean) {
        viewModelScope.launch {
            if (isWarmup && !canBeWarmup(set)) return@launch

            workoutRepository.update(set.copy(isWarmup = isWarmup))
        }
    }

    private fun canBeWarmup(set: WorkoutSet): Boolean {
        val setsInGroup = _workout?.setGroups
            ?.firstOrNull { it.group.id == set.groupId }
            ?.sets
            ?.sortedBy { it.workoutSetId }
            ?: return true

        val targetIndex = setsInGroup.indexOfFirst { it.workoutSetId == set.workoutSetId }
        if (targetIndex < 0) return true

        return setsInGroup.take(targetIndex).all { it.isWarmup }
    }

    fun updateExerciseNotes(exerciseId: Int, notes: String) {
        viewModelScope.launch {
            exerciseRepository.getExercise(exerciseId)?.let { exercise ->
                exerciseRepository.update(exercise.copy(notes = notes))
            }
        }
    }

    fun updateChecked(set: WorkoutSet, checked: Boolean) {
        viewModelScope.launch {
            workoutRepository.update(set.copy(complete = checked))
            if (checked && !set.complete) {
                startRestTimer(set)
            } else if (!checked && set.complete) {
                cancelRestTimerIfMatches(set)
            }
        }
    }

    fun adjustRestTimer(amountSeconds: Int) {
        if (amountSeconds == 0) return
        val current = _restTimerState.value ?: return
        val newRemaining = (current.remainingSeconds + amountSeconds).coerceAtLeast(0)
        if (newRemaining == 0) {
            clearRestTimer()
            return
        }
        val updated = current.copy(remainingSeconds = newRemaining)
        _restTimerState.value = updated
        updateRestTimerNotification(updated)
    }

    private fun startRestTimer(set: WorkoutSet) {
        val workout = _workout ?: return
        val group = workout.setGroups.firstOrNull { it.group.id == set.groupId } ?: return
        val duration = if (set.isWarmup) {
            group.group.restTimerWarmupSeconds
        } else {
            group.group.restTimerWorkingSeconds
        }
        if (duration <= 0) {
            clearRestTimer()
            return
        }

        clearRestTimer()

        val initialState = RestTimerState(
            setId = set.workoutSetId,
            groupId = set.groupId,
            isWarmup = set.isWarmup,
            remainingSeconds = duration,
        )
        _restTimerState.value = initialState
        updateRestTimerNotification(initialState)

        restTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentState = _restTimerState.value ?: break
                if (currentState.remainingSeconds <= 1) {
                    completeRestTimer()
                    break
                }
                val updatedState = currentState.copy(remainingSeconds = currentState.remainingSeconds - 1)
                _restTimerState.value = updatedState
                updateRestTimerNotification(updatedState)
            }
        }
    }

    private fun cancelRestTimerIfMatches(set: WorkoutSet) {
        val current = _restTimerState.value ?: return
        if (current.setId == set.workoutSetId) {
            clearRestTimer()
        }
    }

    private fun clearRestTimer() {
        restTimerJob?.cancel()
        restTimerJob = null
        _restTimerState.value = null
        NotificationManagerCompat.from(application).cancel(REST_TIMER_NOTIFICATION_ID)
    }

    private fun completeRestTimer() {
        val state = _restTimerState.value ?: return
        restTimerJob?.cancel()
        restTimerJob = null

        val label = if (state.isWarmup) {
            application.getString(R.string.rest_timer_indicator_warmup)
        } else {
            application.getString(R.string.rest_timer_indicator_working)
        }

        val builder = NotificationCompat.Builder(application, REST_TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gymroutines)
            .setContentTitle(application.getString(R.string.rest_timer_notification_complete_title))
            .setContentText(
                application.getString(R.string.rest_timer_notification_complete_body, label)
            )
            .setAutoCancel(true)
            .setContentIntent(createWorkoutPendingIntent())

        if (!restTimerSoundEnabled && !restTimerVibrationEnabled) {
            builder.setSilent(true)
        } else {
            var defaults = 0
            if (restTimerSoundEnabled) defaults = defaults or NotificationCompat.DEFAULT_SOUND
            if (restTimerVibrationEnabled) defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
            builder.setDefaults(defaults)
        }

        NotificationManagerCompat.from(application).notify(REST_TIMER_NOTIFICATION_ID, builder.build())
        _restTimerState.value = null
    }

    private fun updateRestTimerNotification(state: RestTimerState?) {
        val notificationManager = NotificationManagerCompat.from(application)
        if (state == null) {
            notificationManager.cancel(REST_TIMER_NOTIFICATION_ID)
            return
        }
        val label = if (state.isWarmup) {
            application.getString(R.string.rest_timer_indicator_warmup)
        } else {
            application.getString(R.string.rest_timer_indicator_working)
        }
        val builder = NotificationCompat.Builder(application, REST_TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gymroutines)
            .setContentTitle(application.getString(R.string.rest_timer_notification_running_title))
            .setContentText(
                application.getString(
                    R.string.rest_timer_notification_running_body,
                    label,
                    formatRestDuration(state.remainingSeconds)
                )
            )
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(createWorkoutPendingIntent())

        notificationManager.notify(REST_TIMER_NOTIFICATION_ID, builder.build())
    }

    private fun createWorkoutPendingIntent(): PendingIntent? {
        val workoutIntent = Intent(
            Intent.ACTION_VIEW,
            "https://gymroutines.com/workoutInProgress/$workoutId".toUri(),
            application,
            MainActivity::class.java
        )
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return TaskStackBuilder.create(application).run {
            addNextIntentWithParentStack(workoutIntent)
            getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT or flag)
        }
    }

    private fun setEndTime(endTime: Date) {
        _workout?.workout?.let { workout ->
            viewModelScope.launch {
                workoutRepository.update(workout.copy(endTime = endTime))
            }
        }
    }

    fun cancelWorkout(onCompletion: () -> Unit) {
        _workout?.let { workout ->
            clearRestTimer()
            viewModelScope.launch {
                workoutRepository.delete(workout.workout)
                preferences.edit { it[AppPrefs.CurrentWorkout.key] = -1 }
                onCompletion()
            }
        }
    }

    fun finishWorkout(onCompletion: () -> Unit) {
        clearRestTimer()
        viewModelScope.launch {
            preferences.edit { it[AppPrefs.CurrentWorkout.key] = -1 }
            onCompletion()
        }
    }

    override fun onCleared() {
        clearRestTimer()
        super.onCleared()
    }
}
