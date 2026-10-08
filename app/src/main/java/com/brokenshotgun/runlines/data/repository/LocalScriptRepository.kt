package com.brokenshotgun.runlines.data.repository

import com.brokenshotgun.runlines.data.local.ScriptDatabase
import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.model.Scene
import com.brokenshotgun.runlines.domain.repository.ScriptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalScriptRepository(
    private val database: ScriptDatabase
) : ScriptRepository {
    override suspend fun getScripts(): List<Script> = withContext(Dispatchers.IO) {
        database.getScripts()
    }

    override suspend fun getScriptSummary(id: Long): Script? = withContext(Dispatchers.IO) {
        database.getScriptSummary(id)
    }

    override suspend fun getScene(scriptId: Long, sceneIndex: Int): Scene? = withContext(Dispatchers.IO) {
        database.getScene(scriptId, sceneIndex)
    }

    override suspend fun insertScript(script: Script): Script = withContext(Dispatchers.IO) {
        database.insertScript(script)
        script
    }

    override suspend fun updateScript(script: Script) {
        withContext(Dispatchers.IO) {
            database.updateScript(script)
        }
    }

    override suspend fun deleteScript(script: Script) {
        withContext(Dispatchers.IO) {
            database.deleteScript(script)
        }
    }
}
