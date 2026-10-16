package com.brokenshotgun.runlines.data.importing

import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.model.ScriptNameNormalizer
import com.brokenshotgun.runlines.domain.repository.ScriptContentParser

class FountainScriptContentParser(
    private val untitledScriptName: String = "Untitled script",
    private val untitledSceneName: String = "Untitled scene"
) : ScriptContentParser {
    override fun parse(fileName: String, content: String): Script {
        val script = when (fileName.substringAfterLast('.', "").lowercase()) {
            "txt", "fountain" -> FountainSerializer.deserialize(
                content,
                untitledScriptName,
                untitledSceneName
            )
            "pdf" -> PdfParser.parse(content, untitledScriptName, untitledSceneName)
            else -> throw IllegalArgumentException("Unsupported script file type: $fileName")
        }
        val normalizedName = ScriptNameNormalizer.normalize(script.name)
        script.name = if (
            normalizedName.isBlank() || normalizedName.equals(untitledScriptName, ignoreCase = true)
        ) {
            ScriptNameNormalizer.fromFileName(fileName, untitledScriptName)
        } else {
            normalizedName
        }
        return script
    }
}
