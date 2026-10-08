package com.brokenshotgun.runlines.data.importing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScriptContentParserTest {
    private val parser = FountainScriptContentParser()

    @Test
    fun parsesTextAndFountainExtensionsCaseInsensitively() {
        val source = "Title: Parsed\n\nINT. ROOM - DAY\n\n"

        assertEquals("Parsed", parser.parse("script.TXT", source).name)
        assertEquals("Parsed", parser.parse("script.Fountain", source).name)
    }

    @Test
    fun rejectsUnsupportedFileExtensions() {
        assertThrows(IllegalArgumentException::class.java) {
            parser.parse("script.docx", "content")
        }
    }

    @Test
    fun usesSanitizedFileNameWhenImportedScriptHasNoTitle() {
        val script = parser.parse("C:/scripts/My: Play?.fountain", "INT. ROOM - DAY\n")

        assertEquals("My Play", script.name)
    }

    @Test
    fun sanitizesImportedTitleAndUsesFileNameForDefaultTitle() {
        val titledScript = parser.parse("filename.fountain", "Title: My: Play?\n\nINT. ROOM - DAY\n")
        val untitledScript = parser.parse("fallback.txt", "INT. ROOM - DAY\n")

        assertEquals("My Play", titledScript.name)
        assertEquals("fallback", untitledScript.name)
    }
}
