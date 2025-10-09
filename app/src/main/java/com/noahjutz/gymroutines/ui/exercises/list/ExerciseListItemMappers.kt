package com.noahjutz.gymroutines.ui.exercises.list

import com.noahjutz.gymroutines.data.domain.Exercise
import com.noahjutz.gymroutines.data.exerciselibrary.ExerciseLibraryEntry
import com.noahjutz.gymroutines.data.exerciselibrary.displayName
import java.util.Locale

internal const val CUSTOM_FILTER_TAG = "Custom"
internal const val LIBRARY_TAG_PREFIX = "library:"

internal fun Exercise.toCustomListItem(locale: Locale): ExerciseListItem {
    val normalizedName = name.lowercase(locale)
    val searchable = buildList {
        add(normalizedName)
        if (notes.isNotBlank()) {
            add(notes.lowercase(locale))
        }
    }
    return ExerciseListItem(
        key = exerciseId.toString(),
        title = name,
        subtitle = null,
        chips = listOf(CUSTOM_FILTER_TAG),
        searchTexts = searchable,
        filterTags = setOf(CUSTOM_FILTER_TAG),
        sortKey = normalizedName,
        exerciseId = exerciseId,
        exercise = this,
        entry = null
    )
}

internal fun Exercise.toLibraryListItem(entry: ExerciseLibraryEntry, locale: Locale): ExerciseListItem {
    return entry.toLibraryListItem(locale, importedExercise = this)
}

internal fun ExerciseLibraryEntry.toLibraryListItem(
    locale: Locale,
    importedExercise: Exercise? = null
): ExerciseListItem {
    val displayName = displayName(locale)
    val subtitle = buildList {
        targetMuscles.takeIf { it.isNotEmpty() }
            ?.joinToString { muscle -> muscle.formatTag(locale) }
            ?.takeIf { it.isNotBlank() }
            ?.let(::add)
        equipments.takeIf { it.isNotEmpty() }
            ?.joinToString { equipment -> equipment.formatTag(locale) }
            ?.takeIf { it.isNotBlank() }
            ?.let(::add)
    }.joinToString(" • ").ifBlank { null }

    val chips = buildList {
        bodyParts.mapTo(this) { it.formatTag(locale) }
        equipments.mapTo(this) { it.formatTag(locale) }
    }

    val searchTexts = buildSet {
        add(displayName.lowercase(locale))
        add(name.lowercase(locale))
        alias.mapTo(this) { it.lowercase(locale) }
        searchTerms.mapTo(this) { it.lowercase(locale) }
    }

    val filterTags = bodyParts.mapTo(mutableSetOf()) { it.formatTag(locale) }
    equipments.mapTo(filterTags) { it.formatTag(locale) }

    return ExerciseListItem(
        key = importedExercise?.exerciseId?.toString() ?: "${LIBRARY_TAG_PREFIX}$id",
        title = displayName,
        subtitle = subtitle,
        chips = chips,
        searchTexts = searchTexts.toList(),
        filterTags = filterTags,
        sortKey = displayName.lowercase(locale),
        exerciseId = importedExercise?.exerciseId,
        exercise = importedExercise,
        entry = this
    )
}

internal fun ExerciseListItem.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return searchTexts.any { it.contains(query) }
}

internal fun ExerciseListItem.matchesFilters(selected: Set<String>): Boolean {
    if (selected.isEmpty()) return true
    return selected.any { it in filterTags }
}

internal fun ExerciseListItem.matchesQuery(query: String, filters: Set<String>): Boolean {
    return matchesQuery(query) && matchesFilters(filters)
}

internal fun ExerciseListItem.isSameLibraryEntry(libraryId: String): Boolean {
    return entry?.id == libraryId
}

internal fun String.formatTag(locale: Locale): String {
    if (isBlank()) return this
    return split(Regex("\\s+")).joinToString(" ") { token ->
        token.split('-')
            .joinToString("-") { part ->
                if (part.any(Char::isUpperCase)) {
                    part
                } else {
                    part.lowercase(locale).replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(locale) else char.toString()
                    }
                }
            }
    }
}
