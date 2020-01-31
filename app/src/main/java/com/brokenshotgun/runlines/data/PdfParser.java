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

package com.brokenshotgun.runlines.data;

import com.brokenshotgun.runlines.model.Script;

public class PdfParser {
    private static final String NO_BREAK_SPACE = "\u00A0";
    private static final String SPACE = " ";

    public static Script parse(String script) {
        StringBuilder trimmed = new StringBuilder();
        String[] lines = script.replaceAll(NO_BREAK_SPACE, SPACE).split("\\n");
        for (String line : lines) {
            trimmed.append(line.trim());
            trimmed.append("\n");
        }
        return FountainSerializer.deserialize(trimmed.toString());
    }
}
