package com.noahjutz.gymroutines.data.exerciselibrary

import com.noahjutz.gymroutines.data.domain.Exercise
import java.util.Locale

private val bodyweightKeywords = setOf(
    "body weight",
    "bodyweight",
    "no equipment"
)

private val distanceKeywords = setOf(
    "run",
    "row",
    "bike",
    "cycle",
    "walk",
    "ski",
    "sprint"
)

fun ExerciseLibraryEntry.toExercise(): Exercise {
    val equipments = equipments.map { it.trim() }.filter { it.isNotEmpty() }
    val primaryMuscles = targetMuscles.map { it.trim() }.filter { it.isNotEmpty() }
    val secondaryMuscles = secondaryMuscles.map { it.trim() }.filter { it.isNotEmpty() }
    val normalizedEquipments = equipments.map { it.lowercase(Locale.getDefault()) }
    val isBodyweight = normalizedEquipments.isEmpty() || normalizedEquipments.all { it in bodyweightKeywords }
    val likelyDistance = primaryMuscles.any { muscle ->
        val normalized = muscle.lowercase(Locale.getDefault())
        distanceKeywords.any { keyword -> normalized.contains(keyword) }
    }
    val notesSections = buildList {
        if (instructions.isNotEmpty()) {
            add(formatSection("Instructions", instructions))
        }
        if (tips.isNotEmpty()) {
            add(formatSection("Tips", tips))
        }
        val details = buildList {
            if (equipments.isNotEmpty()) {
                add("Equipment: ${equipments.joinToString()}")
            }
            if (primaryMuscles.isNotEmpty()) {
                add("Primary muscles: ${primaryMuscles.joinToString()}")
            }
            if (secondaryMuscles.isNotEmpty()) {
                add("Secondary muscles: ${secondaryMuscles.joinToString()}")
            }
            force?.takeIf { it.isNotBlank() }?.let {
                add("Force: ${it.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }}")
            }
            mechanic?.takeIf { it.isNotBlank() }?.let {
                add("Mechanic: ${it.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }}")
            }
            difficulty?.takeIf { it.isNotBlank() }?.let {
                add("Difficulty: ${it.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }}")
            }
        }
        if (details.isNotEmpty()) {
            add(details.joinToString(separator = "\n"))
        }
    }

    return Exercise(
        name = name.trim(),
        notes = notesSections.joinToString(separator = "\n\n").trim(),
        logReps = true,
        logWeight = !isBodyweight,
        logTime = false,
        logDistance = likelyDistance,
        hidden = false,
        tags = libraryTag,
        exerciseId = 0,
    )
}

val ExerciseLibraryEntry.libraryTag: String
    get() = "library:$id"

private fun formatSection(title: String, lines: List<String>): String {
    return buildString {
        appendLine(title)
        lines.filter { it.isNotBlank() }
            .forEach { line -> appendLine("\u2022 ${line.trim()}") }
    }.trimEnd()
}
