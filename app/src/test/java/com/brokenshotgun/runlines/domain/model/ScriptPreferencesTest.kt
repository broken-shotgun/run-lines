package com.brokenshotgun.runlines.domain.model

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptPreferencesTest {
    private val gson = Gson()

    @Test
    fun mutedCharactersSurviveScriptSerialization() {
        val script = Script("Test script").apply {
            mutedCharacterNames.add("MAYA")
        }

        val restoredScript = gson.fromJson(gson.toJson(script), Script::class.java)

        assertEquals(setOf("MAYA"), restoredScript.mutedCharacterNames)
    }

    @Test
    fun renamingScriptWithCopyPreservesMutedCharacters() {
        val script = Script("Test script").apply {
            mutedCharacterNames.add("MAYA")
        }

        assertEquals(setOf("MAYA"), script.copy(name = "Renamed script").mutedCharacterNames)
    }

    @Test
    fun olderScriptsDefaultToNoMutedCharacters() {
        val restoredScript = gson.fromJson("""{"name":"Older script"}""", Script::class.java)

        assertTrue(restoredScript.mutedCharacterNames.isEmpty())
    }

    @Test
    fun loadedSceneStateIsNotPersisted() {
        val script = Script("Test script").apply {
            scenes.add(Scene(name = "Deferred", isLoaded = false))
        }

        val restoredScript = gson.fromJson(gson.toJson(script), Script::class.java)

        assertTrue(restoredScript.scenes.single().isLoaded)
    }

    @Test
    fun serializedModelFieldsKeepTheirPersistedJsonNames() {
        val script = Script("Test script").apply {
            actors.add(Actor("MAYA"))
            scenes.add(Scene(name = "Opening").apply {
                lines.add(Line(Actor("MAYA"), "Hello"))
            })
        }

        val json = JsonParser.parseString(gson.toJson(script)).asJsonObject

        assertTrue(json.has("name"))
        assertTrue(json.has("actors"))
        assertTrue(json.has("scenes"))
        assertTrue(json.getAsJsonArray("actors")[0].asJsonObject.has("name"))
        val scene = json.getAsJsonArray("scenes")[0].asJsonObject
        assertTrue(scene.has("name"))
        val line = scene.getAsJsonArray("lines")[0].asJsonObject
        assertTrue(line.has("actor"))
        assertTrue(line.has("line"))
    }
}
