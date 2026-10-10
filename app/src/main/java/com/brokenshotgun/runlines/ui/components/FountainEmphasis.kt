package com.brokenshotgun.runlines.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

private data class EmphasisDelimiter(
    val start: Int,
    val marker: String
)

private data class EmphasisPair(
    val opening: EmphasisDelimiter,
    val closing: EmphasisDelimiter
)

private data class StyledRange(
    val style: SpanStyle,
    val start: Int,
    val end: Int
)

internal fun String.toFountainAnnotatedString(): AnnotatedString {
    val openDelimiters = mutableListOf<EmphasisDelimiter>()
    val pairs = mutableListOf<EmphasisPair>()
    var index = 0

    while (index < length) {
        if (this[index] == '\n') {
            openDelimiters.clear()
            index++
            continue
        }
        val marker = emphasisMarkerAt(index)
        if (marker == null || isEscaped(index)) {
            index++
            continue
        }

        val delimiter = EmphasisDelimiter(index, marker)
        val canClose = index > 0 && !this[index - 1].isWhitespace()
        val canOpen = index + marker.length < length && !this[index + marker.length].isWhitespace()
        if (canClose && openDelimiters.lastOrNull()?.marker == marker) {
            pairs += EmphasisPair(openDelimiters.removeAt(openDelimiters.lastIndex), delimiter)
        } else if (canOpen) {
            openDelimiters += delimiter
        }
        index += marker.length
    }

    val markerLengths = buildMap {
        pairs.forEach { pair ->
            put(pair.opening.start, pair.opening.marker.length)
            put(pair.closing.start, pair.closing.marker.length)
        }
    }
    val styledRanges = mutableListOf<StyledRange>()
    val renderedText = StringBuilder(length)
    index = 0

    while (index < length) {
        if (this[index] == '\\' && index + 1 < length) {
            val escapedMarker = emphasisMarkerAt(index + 1)
            if (escapedMarker != null && isEscaped(index + 1)) {
                renderedText.append(escapedMarker)
                index += escapedMarker.length + 1
                continue
            }
        }

        val markerLength = markerLengths[index]
        if (markerLength != null) {
            index += markerLength
            continue
        }

        val style = pairs.asSequence()
            .filter { index >= it.opening.start + it.opening.marker.length && index < it.closing.start }
            .fold(SpanStyle()) { current, pair ->
                when (pair.opening.marker) {
                    "*" -> current.copy(fontStyle = FontStyle.Italic)
                    "**" -> current.copy(fontWeight = FontWeight.Bold)
                    "***" -> current.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                    "****" -> current.copy(fontWeight = FontWeight.Bold)
                    "*****" -> current.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                    "_" -> current.copy(textDecoration = TextDecoration.Underline)
                    else -> current
                }
            }

        val outputStart = renderedText.length
        renderedText.append(this[index])
        if (style != SpanStyle()) {
            val previous = styledRanges.lastOrNull()
            if (previous != null && previous.style == style && previous.end == outputStart) {
                styledRanges[styledRanges.lastIndex] = previous.copy(end = outputStart + 1)
            } else {
                styledRanges += StyledRange(style, outputStart, outputStart + 1)
            }
        }
        index++
    }

    return AnnotatedString.Builder().apply {
        append(renderedText.toString())
        styledRanges.forEach { addStyle(it.style, it.start, it.end) }
    }.toAnnotatedString()
}

private fun String.emphasisMarkerAt(index: Int): String? {
    if (this[index] == '_') return "_"
    if (this[index] != '*') return null

    var end = index
    while (end < length && this[end] == '*') end++
    return when (end - index) {
        1 -> "*"
        2 -> "**"
        3 -> "***"
        4 -> "****"
        5 -> "*****"
        else -> null
    }
}

private fun String.isEscaped(index: Int): Boolean {
    var backslashCount = 0
    var cursor = index - 1
    while (cursor >= 0 && this[cursor] == '\\') {
        backslashCount++
        cursor--
    }
    return backslashCount % 2 == 1
}
