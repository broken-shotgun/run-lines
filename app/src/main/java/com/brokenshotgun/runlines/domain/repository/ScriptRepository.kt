package com.brokenshotgun.runlines.domain.repository

import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.model.Scene

interface ScriptRepository {
    suspend fun getScripts(): List<Script>

    suspend fun getScriptSummary(id: Long): Script?

    suspend fun getScene(scriptId: Long, sceneIndex: Int): Scene?

    suspend fun insertScript(script: Script): Script

    suspend fun updateScript(script: Script)

    suspend fun deleteScript(script: Script)
}

fun interface ScriptExporter {
    fun export(script: Script): String
}
