package com.noahjutz.gymroutines.data.library

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibraryExercise(
    @SerialName("exerciseId")
    val id: String,
    val name: String,
    val gifUrl: String,
    val targetMuscles: List<String>,
    val bodyParts: List<String>,
    val equipments: List<String>,
    val secondaryMuscles: List<String>,
    val instructions: List<String>
)

@Serializable
data class LibraryNamedValue(
    val name: String
)
