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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_table")
data class Exercise(
    var name: String = "",
    @ColumnInfo(defaultValue = "")
    var notes: String = "",
    var logReps: Boolean = true,
    var logWeight: Boolean = false,
    var logTime: Boolean = false,
    var logDistance: Boolean = false,
    var hidden: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    var isCustom: Boolean = true,
    @ColumnInfo(defaultValue = "NULL")
    var libraryExerciseId: String? = null,

    @PrimaryKey(autoGenerate = true)
    val exerciseId: Int = 0
)
