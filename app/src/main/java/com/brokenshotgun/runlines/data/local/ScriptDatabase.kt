package com.brokenshotgun.runlines.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.brokenshotgun.runlines.domain.model.Actor
import com.brokenshotgun.runlines.domain.model.Scene
import com.brokenshotgun.runlines.domain.model.Script
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.brokenshotgun.runlines.data.local.ScriptReaderContract.ScriptEntry

class ScriptDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "ScriptReader.db"
        private const val TEXT_TYPE = " TEXT"
        private const val COMMA_SEP = ","

        private val SQL_CREATE_SCRIPT_TABLE =
            "CREATE TABLE ${ScriptEntry.TABLE_NAME} (" +
                    "${android.provider.BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${ScriptEntry.COLUMN_NAME_SCRIPT_JSON}$TEXT_TYPE$COMMA_SEP" +
                    "${ScriptEntry.COLUMN_NAME_CREATE_DATE}$TEXT_TYPE" +
                    " )"

    }

    private val gson: Gson = GsonBuilder().create()
    private val scriptJsonReader = ScriptJsonReader(gson)

    fun insertScript(script: Script) {
        val db = writableDatabase

        val sValues = ContentValues().apply {
            put(ScriptEntry.COLUMN_NAME_SCRIPT_JSON, serialize(script))
            put(ScriptEntry.COLUMN_NAME_CREATE_DATE, System.currentTimeMillis())
        }

        val newScriptId = db.insert(
            ScriptEntry.TABLE_NAME,
            null,
            sValues
        )

        check(newScriptId >= 0L) { "Failed to insert script '${script.name}'" }
        script.id = newScriptId
    }

    fun updateScript(script: Script) {
        val db = writableDatabase
        val existingScript = getFullScript(script.id)
            ?: error("Script ${script.id} was not found for update")
        val mergedScript = script.copy(
            scenes = existingScript.scenes.mapIndexed { index, existingScene ->
                script.scenes.getOrNull(index)?.takeIf { it.isLoaded } ?: run {
                    val replacements = script.sceneActorReplacements
                    if (replacements.isEmpty()) {
                        existingScene
                    } else {
                        val replacementLines = existingScene.lines.map { line ->
                            val replacement = replacements[line.actor.name.uppercase()]
                            if (replacement == null) line else line.copy(
                                actor = Actor(replacement)
                            )
                        }
                        if (replacementLines == existingScene.lines) {
                            existingScene
                        } else {
                            existingScene.copy(lines = replacementLines.toMutableList())
                        }
                    }
                }
            }.toMutableList()
        ).apply {
            defaultVoice = script.defaultVoice
            if (script.scenes.size > existingScript.scenes.size) {
                scenes.addAll(script.scenes.drop(existingScript.scenes.size))
            }
        }

        val values = ContentValues().apply {
            put(ScriptEntry.COLUMN_NAME_SCRIPT_JSON, serialize(mergedScript))
        }

        val selection = "${android.provider.BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(script.id.toString())

        val updatedRows = db.update(
            ScriptEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
        check(updatedRows > 0) { "Script ${script.id} was not found for update" }
    }

    fun insertScene(scriptId: Long, sceneIndex: Int, scene: Scene) {
        val db = writableDatabase
        val script = getFullScript(scriptId)
            ?: error("Script $scriptId was not found for scene insertion")
        require(sceneIndex in 0..script.scenes.size) {
            "Scene insertion index $sceneIndex is invalid for script $scriptId"
        }

        script.scenes.add(sceneIndex, scene)
        script.scenes.forEachIndexed { index, existingScene -> existingScene.number = index }
        val values = ContentValues().apply {
            put(ScriptEntry.COLUMN_NAME_SCRIPT_JSON, serialize(script))
        }
        val selection = "${android.provider.BaseColumns._ID} = ?"
        val updatedRows = db.update(
            ScriptEntry.TABLE_NAME,
            values,
            selection,
            arrayOf(scriptId.toString())
        )
        check(updatedRows > 0) { "Script $scriptId was not found for scene insertion" }
    }

    fun deleteScene(scriptId: Long, sceneIndex: Int) {
        val db = writableDatabase
        val script = getFullScript(scriptId)
            ?: error("Script $scriptId was not found for scene deletion")
        require(sceneIndex in script.scenes.indices) {
            "Scene $sceneIndex was not found in script $scriptId"
        }

        script.scenes.removeAt(sceneIndex)
        script.scenes.forEachIndexed { index, scene -> scene.number = index }
        val values = ContentValues().apply {
            put(ScriptEntry.COLUMN_NAME_SCRIPT_JSON, serialize(script))
        }
        val selection = "${android.provider.BaseColumns._ID} = ?"
        val updatedRows = db.update(
            ScriptEntry.TABLE_NAME,
            values,
            selection,
            arrayOf(scriptId.toString())
        )
        check(updatedRows > 0) { "Script $scriptId was not found for scene deletion" }
    }

    fun deleteScript(script: Script) {
        val db = writableDatabase

        val selection = "${android.provider.BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(script.id.toString())

        db.delete(
            ScriptEntry.TABLE_NAME,
            selection,
            selectionArgs
        )
    }

    fun getScripts(): List<Script> {
        val results = mutableListOf<Script>()

        val db = readableDatabase

        val projection = arrayOf(
            android.provider.BaseColumns._ID,
            ScriptEntry.COLUMN_NAME_SCRIPT_JSON,
        )

        val sortOrder = "${ScriptEntry.COLUMN_NAME_CREATE_DATE} DESC"

        db.query(
            ScriptEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            sortOrder
        ).use { c ->
            while (c.moveToNext()) {
                val scriptId = c.getLong(0)
                val scriptJson = c.getString(1)
                val script = deserialize(scriptJson)
                script.id = scriptId
                results.add(script)
            }
        }

        return results
    }

    fun getScriptSummary(id: Long): Script? {
        val projection = arrayOf(ScriptEntry.COLUMN_NAME_SCRIPT_JSON)
        val selection = "${android.provider.BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        return readableDatabase.query(
            ScriptEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                scriptJsonReader.readSummary(cursor.getString(0), id)
            }
        }
    }

    fun getScene(scriptId: Long, sceneIndex: Int): Scene? {
        val projection = arrayOf(ScriptEntry.COLUMN_NAME_SCRIPT_JSON)
        val selection = "${android.provider.BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(scriptId.toString())
        return readableDatabase.query(
            ScriptEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                scriptJsonReader.readScene(cursor.getString(0), sceneIndex)
            }
        }
    }

    private fun getFullScript(id: Long): Script? {
        val projection = arrayOf(ScriptEntry.COLUMN_NAME_SCRIPT_JSON)
        val selection = "${android.provider.BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        return readableDatabase.query(
            ScriptEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                deserialize(cursor.getString(0)).apply { this.id = id }
            }
        }
    }

    private fun serialize(script: Script): String {
        return gson.toJson(script)
    }

    private fun deserialize(json: String): Script {
        return requireNotNull(gson.fromJson(json, Script::class.java)) {
            "Stored script data was empty"
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_SCRIPT_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException("No database migration is defined from $oldVersion to $newVersion")
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException("Database downgrade from $oldVersion to $newVersion is not supported")
    }
}
