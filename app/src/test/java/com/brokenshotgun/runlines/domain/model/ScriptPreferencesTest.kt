package com.brokenshotgun.runlines.domain.model

import com.google.gson.Gson
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
}
