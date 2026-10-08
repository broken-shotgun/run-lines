package com.brokenshotgun.runlines.domain

import com.brokenshotgun.runlines.domain.model.Actor
import com.brokenshotgun.runlines.domain.model.Line
import org.junit.Assert.assertEquals
import org.junit.Test

class LineTest {
    @Test
    fun rendersSupportedInlineEmphasisAsHtml() {
        val line = Line(
            Actor("MAYA"),
            "A **bold** and *italic* _underlined_ line"
        )

        assertEquals(
            "A <b>bold</b> and <i>italic</i> <u>underlined</u> line",
            line.getLineHtml()
        )
    }

    @Test
    fun addingDialogueAppendsWithLineBreak() {
        val line = Line(Actor("MAYA"), "First thought")

        line.addDialogue("Second thought")

        assertEquals("First thought\nSecond thought", line.line)
    }
}
