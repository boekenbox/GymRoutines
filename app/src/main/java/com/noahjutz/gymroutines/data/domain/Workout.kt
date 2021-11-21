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

package com.noahjutz.gymroutines.data.domain

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.noahjutz.gymroutines.util.minus
import java.util.*

@Entity(tableName = "workout_table")
data class Workout(
    val name: String = "",
    val startTime: Date = Calendar.getInstance().time,
    val endTime: Date = startTime,

    @PrimaryKey(autoGenerate = true)
    val workoutId: Int = 0,
)

data class WorkoutWithSets(
    @Embedded val workout: Workout,
    @Relation(
        parentColumn = "workoutId",
        entityColumn = "workoutId"
    ) val sets: List<WorkoutSet>
)

val Workout.duration get() = endTime - startTime
