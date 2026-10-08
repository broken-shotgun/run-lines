package com.brokenshotgun.runlines.domain.model

object ScriptNameNormalizer {
    private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cc}]")
    private val whitespace = Regex("\\s+")

    fun normalize(name: String): String =
        name
            .replace(invalidFileNameCharacters, " ")
            .replace(whitespace, " ")
            .trim()
            .trimEnd(' ', '.')

    fun fromFileName(fileName: String): String {
        val baseName = fileName.substringAfterLast('/').substringAfterLast('\\')
        val nameWithoutExtension = baseName.substringBeforeLast('.', missingDelimiterValue = baseName)
        return normalize(nameWithoutExtension).ifBlank { "Untitled script" }
    }
}
