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

import android.app.AlarmManager
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
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.ceil

private const val MAX_REST_SECONDS = 99 * 60 + 59

data class RestTimerState(
    val setId: Int,
    val groupId: Int,
    val isWarmup: Boolean,
    val remainingSeconds: Int,
    val endTimeEpochMillis: Long,
)

class WorkoutInProgressViewModel(
    private val preferences: DataStore<Preferences>,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    private val application: Application,
    private val workoutId: Int,
) : ViewModel() {
    private val databaseWorkout = workoutRepository.getWorkoutFlow(workoutId)
    private val _currentEndTime = MutableStateFlow<Date?>(null)
    val workout: Flow<WorkoutWithSetGroups?> =
        combine(databaseWorkout, _currentEndTime) { workout, endTimeOverride ->
            workout?.let { current ->
                val effectiveEndTime = endTimeOverride ?: current.workout.endTime
                if (effectiveEndTime == current.workout.endTime) {
                    current
                } else {
                    current.copy(workout = current.workout.copy(endTime = effectiveEndTime))
                }
            }
        }
    private var _workout: WorkoutWithSetGroups? = null
    private val _restTimerState = MutableStateFlow<RestTimerState?>(null)
    val restTimerState: StateFlow<RestTimerState?> = _restTimerState.asStateFlow()
    private var restTimerJob: Job? = null
    private var restTimerSoundEnabled = AppPrefs.RestTimerSound.defaultValue
    private var restTimerVibrationEnabled = AppPrefs.RestTimerVibration.defaultValue
    private var restTimerRestored = false

    private val alarmManager: AlarmManager by lazy {
        application.getSystemService(AlarmManager::class.java)
    }

    val routineName = workout.map {
        it?.workout?.routineId?.let { routineId ->
            routineRepository.getRoutine(routineId)?.name
        } ?: application.resources.getString(R.string.unnamed_routine)
    }

    init {
        viewModelScope.launch {
            launch {
                databaseWorkout.collect { workout ->
                    _workout = workout
                    if (workout == null) {
                        _currentEndTime.value = null
                    } else {
                        val storedEndTime = workout.workout.endTime
                        val currentEnd = _currentEndTime.value
                        if (currentEnd == null || storedEndTime.after(currentEnd)) {
                            _currentEndTime.value = storedEndTime
                        }
                    }
                }
            }
            launch {
                preferences.data.collect { prefs ->
                    restTimerSoundEnabled =
                        prefs[AppPrefs.RestTimerSound.key] ?: AppPrefs.RestTimerSound.defaultValue
                    restTimerVibrationEnabled =
                        prefs[AppPrefs.RestTimerVibration.key]
                            ?: AppPrefs.RestTimerVibration.defaultValue
                    if (!restTimerRestored) {
                        restTimerRestored = true
                        restoreRestTimer(prefs)
                    }
                }
            }
            launch(Dispatchers.Default) {
                while (isActive) {
                    if (_workout != null) {
                        _currentEndTime.value = Calendar.getInstance().time
                    }
                    delay(1000)
                }
            }
        }
    }

    fun getExercise(exerciseId: Int): Flow<Exercise?> {
        return exerciseRepository.getExerciseFlow(exerciseId)
    }

    private suspend fun restoreRestTimer(prefs: Preferences) {
        val endTime = prefs[AppPrefs.RestTimerEndEpochMillis.key]
            ?: AppPrefs.RestTimerEndEpochMillis.defaultValue
        val storedWorkoutId = prefs[AppPrefs.RestTimerWorkoutId.key]
            ?: AppPrefs.RestTimerWorkoutId.defaultValue
        if (storedWorkoutId != workoutId) return
        if (endTime <= System.currentTimeMillis()) {
            clearPersistedRestTimer()
            return
        }

        val state = RestTimerState(
            setId = prefs[AppPrefs.RestTimerSetId.key] ?: AppPrefs.RestTimerSetId.defaultValue,
            groupId = prefs[AppPrefs.RestTimerGroupId.key] ?: AppPrefs.RestTimerGroupId.defaultValue,
            isWarmup = prefs[AppPrefs.RestTimerIsWarmup.key] ?: AppPrefs.RestTimerIsWarmup.defaultValue,
            remainingSeconds = secondsRemaining(endTime),
            endTimeEpochMillis = endTime,
        )
        _restTimerState.value = state
        scheduleRestTimerAlarm(state)
        startRestTimerTicker()
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
                exerciseIds.forEachIndexed { index, exerciseId ->
                    val setGroup = WorkoutSetGroup(
                        exerciseId = exerciseId,
                        workoutId = workout.workout.workoutId,
                        position = workout.setGroups.size + index,
                        saveToRoutine = false,
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

    fun updateRestTimers(groupId: Int, warmupSeconds: Int, workingSeconds: Int) {
        viewModelScope.launch {
            workoutRepository.getSetGroup(groupId)?.let { group ->
                val clampedWarmup = warmupSeconds.coerceIn(0, MAX_REST_SECONDS)
                val clampedWorking = workingSeconds.coerceIn(0, MAX_REST_SECONDS)

                workoutRepository.update(
                    group.copy(
                        restTimerWarmupSeconds = clampedWarmup,
                        restTimerWorkingSeconds = clampedWorking,
                    )
                )

                val current = _restTimerState.value
                if (current != null && current.groupId == groupId) {
                    val newDuration = if (current.isWarmup) clampedWarmup else clampedWorking
                    if (newDuration <= 0) {
                        clearRestTimer()
                    } else {
                        val newEnd = minOf(
                            current.endTimeEpochMillis,
                            System.currentTimeMillis() + newDuration * 1_000L,
                        )
                        val updatedState = current.copy(
                            remainingSeconds = secondsRemaining(newEnd),
                            endTimeEpochMillis = newEnd,
                        )
                        _restTimerState.value = updatedState
                        persistRestTimer(updatedState)
                        scheduleRestTimerAlarm(updatedState)
                        updateRestTimerNotification(updatedState)
                    }
                }
            }
        }
    }

    fun setSaveToRoutine(groupId: Int, save: Boolean) {
        viewModelScope.launch {
            workoutRepository.getSetGroup(groupId)?.let { group ->
                workoutRepository.update(group.copy(saveToRoutine = save))
            }
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
        val newEnd = current.endTimeEpochMillis + amountSeconds * 1_000L
        if (newEnd <= System.currentTimeMillis()) {
            clearRestTimer()
            return
        }
        val updated = current.copy(
            remainingSeconds = secondsRemaining(newEnd),
            endTimeEpochMillis = newEnd,
        )
        _restTimerState.value = updated
        viewModelScope.launch {
            persistRestTimer(updated)
        }
        scheduleRestTimerAlarm(updated)
        updateRestTimerNotification(updated)
    }

    private suspend fun startRestTimer(set: WorkoutSet) {
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

        clearRestTimerState()

        val endTime = System.currentTimeMillis() + duration * 1_000L
        val initialState = RestTimerState(
            setId = set.workoutSetId,
            groupId = set.groupId,
            isWarmup = set.isWarmup,
            remainingSeconds = duration,
            endTimeEpochMillis = endTime,
        )
        _restTimerState.value = initialState
        persistRestTimer(initialState)
        scheduleRestTimerAlarm(initialState)
        updateRestTimerNotification(initialState)
        startRestTimerTicker()
    }

    private fun startRestTimerTicker() {
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (isActive) {
                val currentState = _restTimerState.value ?: break
                val remaining = secondsRemaining(currentState.endTimeEpochMillis)
                if (remaining <= 0) {
                    completeRestTimer()
                    break
                }
                if (remaining != currentState.remainingSeconds) {
                    val updatedState = currentState.copy(remainingSeconds = remaining)
                    _restTimerState.value = updatedState
                    updateRestTimerNotification(updatedState)
                }
                delay(1_000)
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
        clearRestTimerState()
        viewModelScope.launch {
            clearPersistedRestTimer()
        }
    }

    private fun clearRestTimerState() {
        _restTimerState.value?.let(::cancelRestTimerAlarm)
        restTimerJob?.cancel()
        restTimerJob = null
        _restTimerState.value = null
        runCatching {
            NotificationManagerCompat.from(application).cancel(REST_TIMER_NOTIFICATION_ID)
        }
    }

    private suspend fun completeRestTimer() {
        val state = _restTimerState.value ?: return
        cancelRestTimerAlarm(state)
        restTimerJob = null
        clearPersistedRestTimer()
        showRestTimerCompleteNotification(
            context = application,
            workoutId = workoutId,
            isWarmup = state.isWarmup,
            soundEnabled = restTimerSoundEnabled,
            vibrationEnabled = restTimerVibrationEnabled,
        )
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
            .setWhen(state.endTimeEpochMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setContentIntent(createWorkoutPendingIntent(application, workoutId))

        runCatching { notificationManager.notify(REST_TIMER_NOTIFICATION_ID, builder.build()) }
    }

    private fun scheduleRestTimerAlarm(state: RestTimerState) {
        val intent = Intent(application, RestTimerReceiver::class.java).apply {
            putExtra(RestTimerReceiver.EXTRA_END_EPOCH_MILLIS, state.endTimeEpochMillis)
            putExtra(RestTimerReceiver.EXTRA_WORKOUT_ID, workoutId)
            putExtra(RestTimerReceiver.EXTRA_SET_ID, state.setId)
            putExtra(RestTimerReceiver.EXTRA_GROUP_ID, state.groupId)
            putExtra(RestTimerReceiver.EXTRA_IS_WARMUP, state.isWarmup)
        }
        val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            state.setId.coerceAtLeast(0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutable,
        )
        val triggerAt = state.endTimeEpochMillis
        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent,
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent,
                    )
                }
                else -> alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }.onFailure {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent,
                    )
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            }
        }
    }

    private fun cancelRestTimerAlarm(state: RestTimerState) {
        val intent = Intent(application, RestTimerReceiver::class.java)
        val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            state.setId.coerceAtLeast(0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutable,
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private suspend fun persistRestTimer(state: RestTimerState) {
        preferences.edit { values ->
            values[AppPrefs.RestTimerEndEpochMillis.key] = state.endTimeEpochMillis
            values[AppPrefs.RestTimerWorkoutId.key] = workoutId
            values[AppPrefs.RestTimerSetId.key] = state.setId
            values[AppPrefs.RestTimerGroupId.key] = state.groupId
            values[AppPrefs.RestTimerIsWarmup.key] = state.isWarmup
        }
    }

    private suspend fun clearPersistedRestTimer() {
        preferences.edit { values ->
            values.remove(AppPrefs.RestTimerEndEpochMillis.key)
            values.remove(AppPrefs.RestTimerWorkoutId.key)
            values.remove(AppPrefs.RestTimerSetId.key)
            values.remove(AppPrefs.RestTimerGroupId.key)
            values.remove(AppPrefs.RestTimerIsWarmup.key)
        }
    }

    private fun secondsRemaining(endTimeEpochMillis: Long): Int {
        val remainingMillis = (endTimeEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        return ceil(remainingMillis / 1_000.0).toInt()
    }

    private suspend fun persistCurrentEndTimeIfNeeded(forceTime: Date? = null) {
        val workout = _workout?.workout ?: return
        val endTime = forceTime ?: _currentEndTime.value ?: Calendar.getInstance().time
        workoutRepository.update(workout.copy(endTime = endTime))
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
            val now = Calendar.getInstance().time
            _currentEndTime.value = now
            persistCurrentEndTimeIfNeeded(now)
            preferences.edit { it[AppPrefs.CurrentWorkout.key] = -1 }
            onCompletion()
        }
    }

    override fun onCleared() {
        runBlocking { persistCurrentEndTimeIfNeeded(Calendar.getInstance().time) }
        // The persisted deadline and alarm intentionally outlive this screen ViewModel.
        restTimerJob?.cancel()
        restTimerJob = null
        super.onCleared()
    }
}
