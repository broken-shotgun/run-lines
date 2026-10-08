package com.brokenshotgun.runlines.domain.repository

import com.brokenshotgun.runlines.domain.model.Script

interface ScriptRepository {
    suspend fun getScripts(): List<Script>

    suspend fun getScript(id: Long): Script?

    suspend fun insertScript(script: Script): Script

    suspend fun updateScript(script: Script)

    suspend fun deleteScript(script: Script)
}

fun interface ScriptExporter {
    fun export(script: Script): String
}
