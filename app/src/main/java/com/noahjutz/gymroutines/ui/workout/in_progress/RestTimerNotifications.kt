package com.noahjutz.gymroutines.ui.workout.in_progress

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.REST_TIMER_CHANNEL_ID
import com.noahjutz.gymroutines.ui.MainActivity

internal const val REST_TIMER_NOTIFICATION_ID = 1

internal fun createWorkoutPendingIntent(context: Context, workoutId: Int): PendingIntent? {
    val workoutIntent = Intent(
        Intent.ACTION_VIEW,
        "https://gymroutines.com/workoutInProgress/$workoutId".toUri(),
        context,
        MainActivity::class.java,
    )
    val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_IMMUTABLE
    } else {
        0
    }
    return TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(workoutIntent)
        getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT or immutable)
    }
}

internal fun showRestTimerCompleteNotification(
    context: Context,
    workoutId: Int,
    isWarmup: Boolean,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
) {
    val label = if (isWarmup) {
        context.getString(R.string.rest_timer_indicator_warmup)
    } else {
        context.getString(R.string.rest_timer_indicator_working)
    }
    val builder = NotificationCompat.Builder(context, REST_TIMER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_gymroutines)
        .setContentTitle(context.getString(R.string.rest_timer_notification_complete_title))
        .setContentText(
            context.getString(R.string.rest_timer_notification_complete_body, label)
        )
        .setAutoCancel(true)
        .setContentIntent(createWorkoutPendingIntent(context, workoutId))

    if (!soundEnabled && !vibrationEnabled) {
        builder.setSilent(true)
    } else {
        var defaults = 0
        if (soundEnabled) defaults = defaults or NotificationCompat.DEFAULT_SOUND
        if (vibrationEnabled) defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
        builder.setDefaults(defaults)
    }

    runCatching {
        NotificationManagerCompat.from(context).notify(REST_TIMER_NOTIFICATION_ID, builder.build())
    }
}
