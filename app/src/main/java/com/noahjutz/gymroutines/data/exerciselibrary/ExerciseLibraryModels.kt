package com.noahjutz.gymroutines.data.exerciselibrary

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseLibraryEntry(
    val id: String,
    val name: String,
    val heroAsset: String? = null,
    val mediaAssets: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val bodyParts: List<String> = emptyList(),
    val targetMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val equipments: List<String> = emptyList(),
    val force: String? = null,
    val mechanic: String? = null,
    val difficulty: String? = null,
    val category: String? = null,
    val alias: List<String> = emptyList(),
    val normalizedName: String,
    val searchTerms: List<String> = emptyList(),
    val checksum: String,
)

@Serializable
data class ExerciseLibraryMetadata(
    val count: Int,
    val bodyParts: List<String> = emptyList(),
    val equipments: List<String> = emptyList(),
    val targetMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
)

fun ExerciseLibraryEntry.matchesAsset(path: String): Boolean {
    return mediaAssets.any { it.endsWith(path) } || heroAsset?.endsWith(path) == true
}
