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

@file:Suppress("IllegalIdentifier")

package com.noahjutz.gymroutines.data

import androidx.room.TypeConverter
import com.noahjutz.gymroutines.data.domain.ExerciseSet
import com.noahjutz.gymroutines.data.domain.SetGroup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class Converters {
    @TypeConverter
    fun fromSets(sets: List<ExerciseSet>): String = Json.encodeToString(sets)

    @TypeConverter
    fun toSets(json: String): List<ExerciseSet> = Json.decodeFromString(json)

    @TypeConverter
    fun fromDate(date: Date): Long = date.time

    @TypeConverter
    fun toDate(time: Long): Date = Date(time)
}
