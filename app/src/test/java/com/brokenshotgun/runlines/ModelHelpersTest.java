/*
 * Copyright 2016 Jason Petterson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brokenshotgun.runlines;

import com.brokenshotgun.runlines.model.Line;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ModelHelpersTest {
    @Test
    public void testLineHtmlBold() {
        Line testLine = new Line(null, "my **bold** text is **great**");
        String expected = "my <b>bold</b> text is <b>great</b>";
        assertThat(testLine.getLineHtml(), is(expected));
    }

    @Test
    public void testLineHtmlItalics() {
        Line testLine = new Line(null, "my *italics* text is *great*");
        String expected = "my <i>italics</i> text is <i>great</i>";
        assertThat(testLine.getLineHtml(), is(expected));
    }

    @Test
    public void testLineHtmlUnderline() {
        Line testLine = new Line(null, "my _underlined_ text is _great_");
        String expected = "my <u>underlined</u> text is <u>great</u>";
        assertThat(testLine.getLineHtml(), is(expected));
    }

    @Test
    public void testLineHtmlCombinedEmphasis() {
        Line testLine = new Line(null, "For example, ***bold italics,*** or _an *italicized* word within an underlined phrase_");
        String expected = "For example, <i><b>bold italics,</b></i> or <u>an <i>italicized</i> word within an underlined phrase</u>";
        assertThat(testLine.getLineHtml(), is(expected));
    }
}
