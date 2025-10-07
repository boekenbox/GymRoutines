package com.noahjutz.gymroutines.util

import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import kotlin.time.Duration

fun Date.formatSimple(): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(this)
}

fun Duration.pretty(): String = toComponents { h, m, _, _ -> "${h}h ${m}min" }

val List<Date>.currentDailyStreak: Int
    get() = currentDailyStreakInternal()

internal fun List<Date>.currentDailyStreakInternal(now: Date = Date()): Int {
    if (isEmpty()) return 0
    val zone = ZoneId.systemDefault()
    val today = now.toInstant().atZone(zone).toLocalDate()
    val days = map { it.toInstant().atZone(zone).toLocalDate() }
        .filter { !it.isAfter(today) }
        .toSet()

    if (!days.contains(today)) {
        return 0
    }

    var streak = 0
    var current = today
    while (days.contains(current)) {
        streak += 1
        current = current.minusDays(1)
    }

    return streak
}
