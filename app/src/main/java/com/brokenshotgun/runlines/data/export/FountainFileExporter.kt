package com.brokenshotgun.runlines.data.export

import com.brokenshotgun.runlines.data.importing.FountainSerializer
import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.repository.ScriptExporter
import java.io.File

class FountainFileExporter(private val documentsDirectory: File) : ScriptExporter {
    override fun export(script: Script): String {
        val directory = File(documentsDirectory, "Run Lines")
        check(directory.isDirectory || directory.mkdirs()) {
            "Could not create export directory: ${directory.absolutePath}"
        }
        val safeName = script.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return File(directory, "$safeName.fountain").apply {
            writeText(FountainSerializer.serialize(script))
        }.absolutePath
    }
}
