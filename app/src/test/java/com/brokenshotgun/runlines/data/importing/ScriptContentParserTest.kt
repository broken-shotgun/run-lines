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

    @Test
    fun usesLocalizedFallbackNamesWhenParsingUntitledScripts() {
        val localizedParser = FountainScriptContentParser(
            untitledScriptName = "Untitled screenplay",
            untitledSceneName = "Untitled screenplay scene"
        )

        val script = localizedParser.parse(
            "fallback.fountain",
            "Title: Untitled screenplay\n\n"
        )

        assertEquals("fallback", script.name)
        assertEquals("Untitled screenplay scene", script.scenes.single().name)
    }

    @Test
    fun parsesFinalDraftFdxFiles() {
        val fdx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FinalDraft>
              <Content>
                <Paragraph Type="Title"><Text>Example Script</Text></Paragraph>
                <Paragraph Type="Scene Heading"><Text>INT. ROOM - DAY</Text></Paragraph>
                <Paragraph Type="Action"><Text>A light turns on.</Text></Paragraph>
                <Paragraph Type="Character"><Text>ALICE</Text></Paragraph>
                <Paragraph Type="Dialogue"><Text>Hello there.</Text></Paragraph>
              </Content>
            </FinalDraft>
        """.trimIndent()

        val script = parser.parse("example.fdx", fdx)

        assertEquals("Example Script", script.name)
        assertEquals("INT. ROOM - DAY", script.scenes.single().name)
        assertEquals("A light turns on.", script.scenes.single().lines[0].line)
        assertEquals("Hello there.", script.scenes.single().lines[1].line)
    }
}
