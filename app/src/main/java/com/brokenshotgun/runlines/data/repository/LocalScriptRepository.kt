package com.brokenshotgun.runlines.data.repository

import com.brokenshotgun.runlines.data.local.ScriptDatabase
import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.repository.ScriptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalScriptRepository(
    private val database: ScriptDatabase
) : ScriptRepository {
    override suspend fun getScripts(): List<Script> = withContext(Dispatchers.IO) {
        database.getScripts()
    }

    override suspend fun getScript(id: Long): Script? = withContext(Dispatchers.IO) {
        database.getScript(id)
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
