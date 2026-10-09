package com.brokenshotgun.runlines.domain.usecase

import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.repository.ScriptExporter

class ExportScriptUseCase(private val exporter: ScriptExporter) {
    operator fun invoke(script: Script): String = exporter.export(script)
}
