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

    @Test
    fun importsForcedElementsAndRetainsSceneNumbers() {
        val fountain = """
            INT. ROOM - DAY #1A#

            @McCLANE
            Yippee.

            !SCANNING THE AISLES...
            The lights dim.

            ~A little song.

            >Burn to White.
        """.trimIndent()

        val script = FountainSerializer.deserialize(fountain)
        val scene = script.scenes.single()

        assertEquals("INT. ROOM - DAY", scene.name)
        assertEquals("1A", scene.fountainSceneNumber)
        assertEquals("McCLANE", scene.lines[0].actor.name)
        assertEquals("Yippee.", scene.lines[0].line)
        assertTrue(scene.lines.any { it.actor == Actor.ACTION && it.line.contains("SCANNING") })
        assertTrue(scene.lines.any { it.actor == Actor.ACTION && it.line.contains("little song") })
        assertTrue(scene.lines.any { it.actor == Actor.ACTION && it.line == "Burn to White." })

        val roundTrip = FountainSerializer.deserialize(FountainSerializer.serialize(script))
        assertEquals("1A", roundTrip.scenes.single().fountainSceneNumber)
        assertEquals("INT. ROOM - DAY", roundTrip.scenes.single().name)
    }

    @Test
    fun importsForcedSceneHeading() {
        val script = FountainSerializer.deserialize(
            "INT. ROOM - DAY\n\n.SNIPER SCOPE POV\n\nThe scope comes into focus."
        )

        assertEquals(2, script.scenes.size)
        assertEquals("SNIPER SCOPE POV", script.scenes[1].name)
    }

    @Test
    fun recognizesFountainSceneHeadingPrefixesAndKeepsIndentedAction() {
        val fountain = """
            INT. ROOM - DAY

                The lights come on.

            EST. FIELD - NIGHT

            INT./EXT. CAR - DAY

            INT/EXT. HOUSE - DAY

            I/E. DOORWAY - NIGHT
        """.trimIndent()

        val script = FountainSerializer.deserialize(fountain)

        assertEquals(
            listOf(
                "INT. ROOM - DAY",
                "EST. FIELD - NIGHT",
                "INT./EXT. CAR - DAY",
                "INT/EXT. HOUSE - DAY",
                "I/E. DOORWAY - NIGHT"
            ),
            script.scenes.map { it.name }
        )
        assertEquals("    The lights come on.", script.scenes.first().lines.single().line)
    }
}
