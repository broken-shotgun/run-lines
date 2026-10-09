package com.brokenshotgun.runlines.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScriptNameNormalizerTest {
    @Test
    fun removesInvalidFileNameCharactersAndNormalizesWhitespace() {
        assertEquals(
            "A script title",
            ScriptNameNormalizer.normalize("  A  script:/title?  ")
        )
    }

    @Test
    fun trimsTheExtensionFromAnImportedFileName() {
        assertEquals(
            "my.script",
            ScriptNameNormalizer.fromFileName("C:\\scripts\\my.script.fountain")
        )
    }
}
