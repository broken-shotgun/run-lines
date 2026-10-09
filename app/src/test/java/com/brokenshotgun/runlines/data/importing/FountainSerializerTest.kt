package com.brokenshotgun.runlines.data.importing

import com.brokenshotgun.runlines.domain.model.Actor
import com.brokenshotgun.runlines.domain.model.Line
import com.brokenshotgun.runlines.domain.model.Scene
import com.brokenshotgun.runlines.domain.model.Script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FountainSerializerTest {
    @Test
    fun deserializeReadsTitleSceneCharacterDialogueAndAction() {
        val fountain = """
            Title: A Small Test
            Author: Test Writer

            INT. KITCHEN - DAY

            MAYA
            We made it.

            The door swings open.
        """.trimIndent()

        val script = FountainSerializer.deserialize(fountain)

        assertEquals("A Small Test", script.name)
        assertEquals("Test Writer", script.author)
        assertEquals(1, script.scenes.size)
        assertEquals("INT. KITCHEN - DAY", script.scenes.single().name)
        assertEquals("MAYA", script.scenes.single().lines.first().actor.name)
        assertEquals("We made it.", script.scenes.single().lines.first().line)
        assertTrue(script.scenes.single().lines.any { it.actor == Actor.ACTION })
    }

    @Test
    fun serializeAndDeserializeRetainScriptContent() {
        val script = Script.create("Round Trip").apply {
            author = "A Writer"
            scenes.add(
                Scene("EXT. GARDEN - NIGHT").apply {
                    addLine(Line(Actor("ALEX"), "Hello there."))
                    addAction("The lights go out.")
                }
            )
        }

        val result = FountainSerializer.deserialize(FountainSerializer.serialize(script))

        assertEquals(script.name, result.name)
        assertEquals(script.author, result.author)
        assertEquals(1, result.scenes.size)
        assertEquals("EXT. GARDEN - NIGHT", result.scenes.single().name)
        assertEquals("Hello there.", result.scenes.single().lines.first().line)
        assertFalse(result.scenes.single().lines.none { it.actor == Actor.ACTION })
    }
}
