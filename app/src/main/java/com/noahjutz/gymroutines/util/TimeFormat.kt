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

package com.noahjutz.gymroutines.util

import java.util.Locale

fun formatRestDuration(seconds: Int): String {
    if (seconds <= 0) return "0:00"
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds)
}

fun secondsToDurationDigits(seconds: Int): String {
    if (seconds <= 0) return ""
    val clampedSeconds = seconds.coerceIn(0, 5999)
    val minutes = clampedSeconds / 60
    val remainingSeconds = clampedSeconds % 60
    return if (minutes == 0) {
        remainingSeconds.toString()
    } else {
        minutes.coerceAtMost(99).toString() + remainingSeconds.toString().padStart(2, '0')
    }
}

fun durationDigitsToSeconds(value: String): Int {
    if (value.isBlank()) return 0
    val padded = value.padStart(4, '0').takeLast(4)
    val minutes = padded.substring(0, 2).toIntOrNull() ?: return 0
    val seconds = padded.substring(2, 4).toIntOrNull() ?: return 0
    return (minutes * 60) + seconds
}
