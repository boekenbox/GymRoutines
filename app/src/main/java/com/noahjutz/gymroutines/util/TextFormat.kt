package com.noahjutz.gymroutines.util

import java.util.Locale

fun String.toDisplayCase(locale: Locale = Locale.getDefault()): String {
    if (isBlank()) return this
    val whitespaceRegex = "\\s+".toRegex()
    return trim()
        .split(whitespaceRegex)
        .joinToString(" ") { word ->
            word.lowercase(locale)
                .replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(locale) else char.toString()
                }
        }
}
