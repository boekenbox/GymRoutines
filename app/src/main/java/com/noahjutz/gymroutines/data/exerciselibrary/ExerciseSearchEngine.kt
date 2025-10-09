package com.noahjutz.gymroutines.data.exerciselibrary

import kotlin.math.max

class ExerciseSearchEngine(
    private val entries: List<ExerciseLibraryEntry>
) {
    private val index: Map<String, MutableSet<Int>> = buildIndex()
    private val nameLookup = entries.map { it.normalizedName }

    private fun buildIndex(): Map<String, MutableSet<Int>> {
        val map = mutableMapOf<String, MutableSet<Int>>()
        entries.forEachIndexed { index, entry ->
            entry.searchTerms.forEach { term ->
                if (term.isBlank()) return@forEach
                map.getOrPut(term) { mutableSetOf() }.add(index)
            }
            map.getOrPut(entry.normalizedName) { mutableSetOf() }.add(index)
            entry.alias.forEach { alias ->
                val normalized = alias.lowercase()
                map.getOrPut(normalized) { mutableSetOf() }.add(index)
            }
        }
        return map
    }

    fun search(
        query: String,
        filters: ExerciseSearchFilters,
        sort: ExerciseSearchSortOption
    ): ExerciseSearchResult {
        val normalizedQuery = query.trim().lowercase()
        val tokenMatches = if (normalizedQuery.isEmpty()) {
            entries.indices.toSet()
        } else {
            val tokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
            tokens.fold(entries.indices.toSet()) { acc, token ->
                val hits = index[token] ?: emptySet()
                acc.intersect(hits)
            }
        }

        val candidates = if (tokenMatches.isNotEmpty() || normalizedQuery.isBlank()) {
            tokenMatches
        } else {
            entries.indices.filter { idx ->
                val entry = entries[idx]
                entry.normalizedName.contains(normalizedQuery) ||
                    entry.alias.any { it.lowercase().contains(normalizedQuery) }
            }.toSet()
        }

        val filtered = candidates.filter { idx ->
            val entry = entries[idx]
            filters.bodyParts.isEmpty() || entry.bodyParts.any { it in filters.bodyParts }
        }.filter { idx ->
            val entry = entries[idx]
            filters.equipments.isEmpty() || entry.equipments.any { it in filters.equipments }
        }.filter { idx ->
            val entry = entries[idx]
            filters.primaryMuscles.isEmpty() || entry.targetMuscles.any { it in filters.primaryMuscles }
        }.filter { idx ->
            val entry = entries[idx]
            filters.secondaryMuscles.isEmpty() || entry.secondaryMuscles.any { it in filters.secondaryMuscles }
        }.filter { idx ->
            val entry = entries[idx]
            filters.difficulty.isEmpty() || entry.difficulty in filters.difficulty
        }.filter { idx ->
            val entry = entries[idx]
            filters.mechanics.isEmpty() || entry.mechanic in filters.mechanics
        }

        val ranked = filtered.sortedWith(sort.comparator(entries, normalizedQuery))
        val suggestions = if (ranked.isEmpty() && normalizedQuery.isNotBlank()) {
            suggestions(normalizedQuery).take(5)
        } else {
            emptyList()
        }
        return ExerciseSearchResult(
            exercises = ranked.map { entries[it] },
            suggestions = suggestions
        )
    }

    private fun suggestions(query: String): List<String> {
        return entries
            .map { entry -> entry.name to trigramSimilarity(query, entry.normalizedName) }
            .sortedByDescending { it.second }
            .filter { it.second > 0.2 }
            .map { it.first }
    }

    private fun trigramSimilarity(source: String, target: String): Double {
        val sourceTrigrams = trigrams(source)
        val targetTrigrams = trigrams(target)
        if (sourceTrigrams.isEmpty() || targetTrigrams.isEmpty()) return 0.0
        val intersection = sourceTrigrams.intersect(targetTrigrams).size
        return intersection.toDouble() / max(sourceTrigrams.size, targetTrigrams.size)
    }

    private fun trigrams(value: String): Set<String> {
        val normalized = value.replace(" ", "_")
        if (normalized.length < 3) return setOf(normalized)
        return (0..normalized.length - 3).mapTo(mutableSetOf()) { idx ->
            normalized.substring(idx, idx + 3)
        }
    }
}

data class ExerciseSearchFilters(
    val bodyParts: Set<String> = emptySet(),
    val equipments: Set<String> = emptySet(),
    val primaryMuscles: Set<String> = emptySet(),
    val secondaryMuscles: Set<String> = emptySet(),
    val difficulty: Set<String> = emptySet(),
    val mechanics: Set<String> = emptySet(),
)

data class ExerciseSearchResult(
    val exercises: List<ExerciseLibraryEntry>,
    val suggestions: List<String>,
)

enum class ExerciseSearchSortOption {
    RELEVANCE,
    NAME,
    EQUIPMENT,
    BODY_PART;

    fun comparator(
        entries: List<ExerciseLibraryEntry>,
        query: String
    ): Comparator<Int> = when (this) {
        RELEVANCE -> Comparator { left, right ->
            relevance(entries[left], query).compareTo(relevance(entries[right], query)) * -1
        }
        NAME -> Comparator { left, right ->
            entries[left].name.compareTo(entries[right].name, ignoreCase = true)
        }
        EQUIPMENT -> Comparator { left, right ->
            val leftEquipment = entries[left].equipments.firstOrNull() ?: ""
            val rightEquipment = entries[right].equipments.firstOrNull() ?: ""
            val result = leftEquipment.compareTo(rightEquipment, ignoreCase = true)
            if (result == 0) entries[left].name.compareTo(entries[right].name, ignoreCase = true) else result
        }
        BODY_PART -> Comparator { left, right ->
            val leftBodyPart = entries[left].bodyParts.firstOrNull() ?: ""
            val rightBodyPart = entries[right].bodyParts.firstOrNull() ?: ""
            val result = leftBodyPart.compareTo(rightBodyPart, ignoreCase = true)
            if (result == 0) entries[left].name.compareTo(entries[right].name, ignoreCase = true) else result
        }
    }

    private fun relevance(entry: ExerciseLibraryEntry, query: String): Int {
        if (query.isBlank()) return 0
        val normalized = entry.normalizedName
        var score = 0
        if (normalized == query) score += 100
        if (normalized.startsWith(query)) score += 40
        if (normalized.contains(query)) score += 20
        score += entry.searchTerms.count { it.contains(query) }
        return score
    }
}
