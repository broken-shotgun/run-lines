package com.brokenshotgun.runlines.domain.usecase

import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.repository.ScriptContentParser
import com.brokenshotgun.runlines.domain.repository.ScriptRepository

class GetScriptsUseCase(private val repository: ScriptRepository) {
    suspend operator fun invoke(): List<Script> = repository.getScripts()
}

class GetScriptUseCase(private val repository: ScriptRepository) {
    suspend operator fun invoke(id: Long): Script? = repository.getScript(id)
}

class CreateScriptUseCase(private val repository: ScriptRepository) {
    suspend operator fun invoke(name: String): Script =
        repository.insertScript(Script.create(name))
}

class ImportScriptUseCase(
    private val repository: ScriptRepository,
    private val parser: ScriptContentParser
) {
    suspend operator fun invoke(fileName: String, content: String): Script =
        repository.insertScript(parser.parse(fileName, content))
}

class UpdateScriptUseCase(private val repository: ScriptRepository) {
    suspend operator fun invoke(script: Script) = repository.updateScript(script)
}

class DeleteScriptUseCase(private val repository: ScriptRepository) {
    suspend operator fun invoke(script: Script) = repository.deleteScript(script)
}
