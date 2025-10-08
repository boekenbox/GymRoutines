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

package com.noahjutz.gymroutines.data.dao

import androidx.room.*
import com.noahjutz.gymroutines.data.domain.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("SELECT * FROM exercise_table ORDER BY name COLLATE NOCASE ASC")
    fun getExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise_table WHERE exerciseId == :id")
    suspend fun getExercise(id: Int): Exercise?

    @Query("SELECT * FROM exercise_table WHERE exerciseId == :id")
    fun getExerciseFlow(id: Int): Flow<Exercise?>
}
