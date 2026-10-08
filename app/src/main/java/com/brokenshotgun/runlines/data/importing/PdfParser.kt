package com.brokenshotgun.runlines.data.importing

import com.brokenshotgun.runlines.domain.model.Script

object PdfParser {
    private const val NO_BREAK_SPACE = "\u00A0"
    private const val SPACE = " "

    fun parse(script: String): Script {
        val trimmed = StringBuilder()
        val lines = script.replace(NO_BREAK_SPACE, SPACE).split("\n")
        for (line in lines) {
            trimmed.append(line.trim())
            trimmed.append("\n")
        }
        return FountainSerializer.deserialize(trimmed.toString())
    }
}
