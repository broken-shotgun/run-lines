package com.brokenshotgun.runlines.data.importing

import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.model.ScriptNameNormalizer
import com.brokenshotgun.runlines.domain.repository.ScriptContentParser

class FountainScriptContentParser : ScriptContentParser {
    override fun parse(fileName: String, content: String): Script {
        val script = when (fileName.substringAfterLast('.', "").lowercase()) {
            "txt", "fountain" -> FountainSerializer.deserialize(content)
            "pdf" -> PdfParser.parse(content)
            else -> throw IllegalArgumentException("Unsupported script file type: $fileName")
        }
        val normalizedName = ScriptNameNormalizer.normalize(script.name)
        script.name = if (
            normalizedName.isBlank() || normalizedName.equals("Untitled script", ignoreCase = true)
        ) {
            ScriptNameNormalizer.fromFileName(fileName)
        } else {
            normalizedName
        }
        return script
    }
}
