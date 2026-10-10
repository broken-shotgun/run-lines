package com.brokenshotgun.runlines.ui.components

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FountainEmphasisTest {
    @Test
    fun rendersEachFountainEmphasisStyle() {
        val text = "An _underlined_, *italic*, **bold**, and ***bold italic*** phrase."

        val annotated = text.toFountainAnnotatedString()

        assertEquals("An underlined, italic, bold, and bold italic phrase.", annotated.text)
        assertTrue(annotated.spanStyles.any { it.item.textDecoration == TextDecoration.Underline })
        assertTrue(annotated.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(annotated.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(
            annotated.spanStyles.any {
                it.item.fontWeight == FontWeight.Bold && it.item.fontStyle == FontStyle.Italic
            }
        )
    }

    @Test
    fun combinesNestedEmphasisAndLeavesUnmatchedMarkersVisible() {
        val text = "_an *_italicized_* word within an underlined phrase_ and *unfinished"

        val annotated = text.toFountainAnnotatedString()

        assertEquals("an italicized word within an underlined phrase and *unfinished", annotated.text)
        assertTrue(
            annotated.spanStyles.any {
                it.item.fontStyle == FontStyle.Italic &&
                    it.item.textDecoration == TextDecoration.Underline
            }
        )
        "an italicized word within an underlined phrase".indices.forEach { index ->
            assertTrue(
                annotated.spanStyles.any {
                    it.start <= index &&
                        it.end > index &&
                        it.item.textDecoration == TextDecoration.Underline
                }
            )
        }
    }
}
