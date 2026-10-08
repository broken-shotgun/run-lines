package com.brokenshotgun.runlines.data.local

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptJsonReaderTest {
    private val scriptJsonReader = ScriptJsonReader(Gson())

    @Test
    fun readSummaryLoadsFirstSceneAndKeepsRemainingSceneMetadata() {
        val script = scriptJsonReader.readSummary(scriptJson, 42L)

        assertEquals(42L, script.id)
        assertEquals("Test script", script.name)
        assertEquals(2, script.scenes.size)
        assertTrue(script.scenes[0].isLoaded)
        assertEquals("First", script.scenes[0].name)
        assertEquals("MAYA", script.scenes[0].lines.single().actor.name)
        assertFalse(script.scenes[1].isLoaded)
        assertEquals("Second", script.scenes[1].name)
        assertTrue(script.scenes[1].lines.isEmpty())
    }

    @Test
    fun readSceneLoadsOnlyRequestedScene() {
        val scene = scriptJsonReader.readScene(scriptJson, 1)

        assertTrue(scene!!.isLoaded)
        assertEquals("Second", scene.name)
        assertEquals("JUNE", scene.lines.single().actor.name)
    }

    @Test
    fun loadedStateIsNotPersistedInScriptJson() {
        val script = scriptJsonReader.readSummary(scriptJson, 42L)

        assertFalse(Gson().toJson(script).contains("isLoaded"))
    }

    private companion object {
        const val scriptJson = """
            {
              "name": "Test script",
              "scenes": [
                {
                  "name": "First",
                  "number": 0,
                  "lines": [{"actor": {"name": "MAYA"}, "line": "Hello"}]
                },
                {
                  "name": "Second",
                  "number": 1,
                  "lines": [{"actor": {"name": "JUNE"}, "line": "Goodbye"}]
                }
              ]
            }
        """
    }
}
