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

import androidx.annotation.NonNull;

import com.brokenshotgun.runlines.model.Actor;
import com.brokenshotgun.runlines.model.Line;
import com.brokenshotgun.runlines.model.Scene;
import com.brokenshotgun.runlines.model.Script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FountainParser {
    public static final String GLOBAL_TITLE_PAGE_REGEX = "(?sim)^(title|credit|author[s]?|source|notes|draft date|date|contact|copyright):(.+)";
    public static final String TITLE_PAGE_REGEX = "(?im)^(title|credit|author[s]?|source|notes|draft date|date|contact|copyright):(.+)";
    public static final String SCENE_HEADING_REGEX = "(?i)^((?:\\*{0,3}_?)?(?:(?:int|ext|est|i\\/e)[. ]).+)|^(?:\\.(?!\\.+))(.+)";
    public static final String SCENE_NUMBER_REGEX = "( *#(.+)# *)";
    public static final String TRANSITION_REGEX = "^((?:FADE (?:TO BLACK|OUT)|CUT TO BLACK)\\.|.+ TO\\:)|^(?:> *)(.+)";
    public static final String DIALOGUE_REGEX = "^([A-Z*_]+[0-9A-Z (._\\-')]*)(\\^?)?(?:\\n(?!\\n+))([\\s\\S]+)";
    public static final String PARENTHETICAL_REGEX = "^(\\(.+\\))$";
    public static final String ACTION_REGEX = "^(.+)";
    public static final String CENTERED_REGEX = "^(?:> *)(.+)(?: *<)(\\n.+)*";
    public static final String SECTION_REGEX = "^(#+)(?: *)(.*)";
    public static final String SYNOPSIS_REGEX = "^(?:\\=(?!\\=+) *)(.*)";
    public static final String NOTE_REGEX = "^(?:\\[{2}(?!\\[+))(.+)(?:\\]{2}(?!\\[+))$";
    public static final String NOTE_INLINE_REGEX = "(?:\\[{2}(?!\\[+))([\\s\\S]+?)(?:\\]{2}(?!\\[+))";
    public static final String BONEYARD_REGEX = "(^\\/\\*|^\\*\\/)$";
    public static final String PAGE_BREAK_REGEX = "^\\={3,}$";
    public static final String LINE_BREAK_REGEX = "^ {2}$";
    public static final String EMPHASIS_REGEX = "(_|\\*{1,3}|_\\*{1,3}|\\*{1,3}_)(.+)(_|\\*{1,3}|_\\*{1,3}|\\*{1,3}_)";
    public static final String BOLD_ITALIC_UNDERLINE_REGEX = "(_{1}\\*{3}(?=.+\\*{3}_{1})|\\*{3}_{1}(?=.+_{1}\\*{3}))(.+?)(\\*{3}_{1}|_{1}\\*{3})";
    public static final String BOLD_UNDERLINE_REGEX = "(_{1}\\*{2}(?=.+\\*{2}_{1})|\\*{2}_{1}(?=.+_{1}\\*{2}))(.+?)(\\*{2}_{1}|_{1}\\*{2})";
    public static final String ITALIC_UNDERLINE_REGEX = "(?:_{1}\\*{1}(?=.+\\*{1}_{1})|\\*{1}_{1}(?=.+_{1}\\*{1}))(.+?)(\\*{1}_{1}|_{1}\\*{1})";
    public static final String BOLD_ITALIC_REGEX = "(\\*{3}(?=.+\\*{3}))(.+?)(\\*{3})";
    public static final String BOLD_REGEX = "(\\*{2}(?=.+\\*{2}))(.+?)(\\*{2})";
    public static final String ITALIC_REGEX = "(\\*{1}(?=.+\\*{1}))(.+?)(\\*{1})";
    public static final String UNDERLINE_REGEX = "(_{1}(?=.+_{1}))(.+?)(_{1})";
    public static final String SPLITTER_REGEX = "\\n{2,}";
    public static final String CLEANER_REGEX = "^\\n+|\\n+$";
    public static final String STANDARDIZER_REGEX = "\\r\\n|\\r";
    public static final String WHITESPACER_REGEX = "(?m)^\\t+|^ {3,}";

    private static String lexer(String script) {
        return script.replace(BONEYARD_REGEX, "\n$1\n")
                .replace(STANDARDIZER_REGEX, "\n")
                .replace(CLEANER_REGEX, "")
                .replace(WHITESPACER_REGEX, "");
    }

    private static List<Token> tokenize(String script) {
        String[] src = lexer(script).split(SPLITTER_REGEX);
        int i = src.length;
        String line, text, meta;
        int x, xlen;
        List<Token> tokens = new ArrayList<>();
        boolean dual = false;

        while (i-- > 0) {
            line = src[i];

            if (line.matches(GLOBAL_TITLE_PAGE_REGEX)) {
                Matcher matcher = Pattern.compile(TITLE_PAGE_REGEX).matcher(line);

                // FIXME does not work for Notes
                while (matcher.find()) {
                    Token title = new Token();
                    title.type = matcher.group(1).trim().toLowerCase().replace(" ", "_");
                    title.text = matcher.group(2).trim();
                    tokens.add(title);
                }

                continue;
            }

            if (line.matches(SCENE_HEADING_REGEX)) {
                Matcher matcher = Pattern.compile(SCENE_HEADING_REGEX).matcher(line);

                if (matcher.matches()) {
                    text = matcher.group(1);
                    meta = "";

                    if (text == null) text = matcher.group(2);

                    if (text != null && text.indexOf("  ") != text.length() - 2) {
                        if (text.matches(SCENE_NUMBER_REGEX)) {
                            Matcher numMatcher = Pattern.compile(SCENE_NUMBER_REGEX).matcher(text);

                            if (numMatcher.matches()) {
                                meta = numMatcher.group(2);
                                text = text.replace(SCENE_NUMBER_REGEX, "");
                            }
                        }

                        Token heading = new Token();
                        heading.type = "scene_heading";
                        heading.text = text;
                        heading.scene_number = meta;
                        tokens.add(heading);
                    }
                }

                continue;
            }

            if (line.matches(CENTERED_REGEX)) {
//                match = line.split(CENTERED_REGEX);
//
//                Token centered = new Token();
//                centered.type = "centered";
//                centered.text = match[0].replace("/>|</g", "");
//                tokens.add(centered);
                continue;
            }

            if (line.matches(TRANSITION_REGEX)) {
//                match = line.split(TRANSITION_REGEX);
//
//                Token transition = new Token();
//                transition.type = "transition";
//                transition.text = match[1]; //|| match[2];
//                tokens.add(transition);
                continue;
            }

            if (line.matches(DIALOGUE_REGEX)) {
                Matcher matcher = Pattern.compile(DIALOGUE_REGEX).matcher(line);
                if (matcher.matches()) {
                    String actor = matcher.group(1);
                    if (actor != null && actor.indexOf("  ") != actor.length() - 2) {

                        String dual_actor = matcher.group(2);
                        // we're iterating from the bottom up, so we need to push these backwards
                        if (dual_actor != null && !dual_actor.equals("")) {
                            Token dual_end = new Token();
                            dual_end.type = "dual_dialogue_end";
                            tokens.add(dual_end);
                        }

                        Token end = new Token();
                        end.type = "dialogue_end";
                        tokens.add(end);

                        // FIXME unsure if parts pattern is working
                        String dialogue = matcher.group(3);
                        Matcher parts = Pattern.compile("(\\(.+\\))(?:\\n+)").matcher(dialogue);
                        if (parts.matches()) {
                            for (x = 0, xlen = parts.groupCount(); x < xlen; x++) {
                                text = parts.group(xlen - x);

                                if (text.length() > 0) {
                                    Token dialogueToken = new Token();
                                    dialogueToken.type = text.matches(PARENTHETICAL_REGEX) ? "parenthetical" : "dialogue";
                                    dialogueToken.text = text;
                                    tokens.add(dialogueToken);
                                }
                            }
                        } else {
                            Token dialog = new Token();
                            dialog.type = "dialogue";
                            dialog.text = dialogue;
                            tokens.add(dialog);
                        }

                        Token character = new Token();
                        character.type = "character";
                        character.text = actor.trim();
                        tokens.add(character);

                        Token begin = new Token();
                        begin.type = "dialogue_begin";
                        begin.dual = (dual_actor != null && !dual_actor.equals("")) ? "right" : dual ? "left" : null;
                        tokens.add(begin);

                        if (dual) {
                            Token dual_begin = new Token();
                            dual_begin.type = "dual_dialogue_begin";
                            tokens.add(dual_begin);
                        }

                        dual = (dual_actor != null && !dual_actor.equals(""));
                        continue;
                    }
                }
            }

            if (line.matches(SECTION_REGEX)) {
//                match = line.split(SECTION_REGEX);
//
//                Token section = new Token();
//                section.type = "section";
//                section.text = match[2];
//                section.depth = match[1].length();
//                tokens.add(section);
                continue;
            }

            if (line.matches(SYNOPSIS_REGEX)) {
//                match = line.split(SYNOPSIS_REGEX);
//
//                Token synopsis = new Token();
//                synopsis.type = "synopsis";
//                synopsis.text = match[1];
//                tokens.add(synopsis);
                continue;
            }

            if (line.matches(NOTE_REGEX)) {
//                match = line.split(NOTE_REGEX);
//
//                Token note = new Token();
//                note.type = "note";
//                note.text = match[1];
//                tokens.add(note);
                continue;
            }

            if (line.matches(BONEYARD_REGEX)) {
//                match = line.split(BONEYARD_REGEX);
//
//                Token boneyard = new Token();
//                boneyard.type = match[0].startsWith("/") ? "boneyard_begin" : "boneyard_end";
//                tokens.add(boneyard);
                continue;
            }

            if (line.matches(PAGE_BREAK_REGEX)) {
//                Token page_break = new Token();
//                page_break.type = "page_break";
//                tokens.add(page_break);
                continue;
            }

            if (line.matches(LINE_BREAK_REGEX)) {
//                Token line_break = new Token();
//                line_break.type = "line_break";
//                tokens.add(line_break);
                continue;
            }

            Token action = new Token();
            action.type = "action";
            action.text = line;
            tokens.add(action);
        }

        return tokens;
    }

    @NonNull
    public static String format(Script script) {
        StringBuilder builder = new StringBuilder();

        builder.append("Title: ").append(script.getName()).append("\n\n");

        for (Scene scene : script.getScenes()) {
            String sceneName = scene.getName().toUpperCase();
            if (!sceneName.startsWith("INT. ") && !sceneName.startsWith("EXT. ")) {
                sceneName = "INT. " + sceneName;
            }
            builder.append(sceneName).append("\n\n");

            for (Line line : scene.getLines()) {
                String actorName = line.getActor().getName().toUpperCase();
                if (!actorName.equals("") && !actorName.equals(Actor.ACTION_NAME)) builder.append(actorName).append("\n");
                builder.append(line.getLine()).append("\n\n");
            }
        }

        return builder.toString();
    }

    private static class Token {
        public String type;
        public String text;
        public String dual;
        public String scene_number;
        public int depth;
    }

    public static Script parse(String script) {
        Script result = new Script("");
        List<Token> tokens = tokenize(script);
        Collections.reverse(tokens);

        Scene currentScene = null;
        Actor currentActor = null;
        for (Token token : tokens) {
            switch (token.type) {
                case "title":
                    result.setName(token.text);
                    break;
                case "character":
                    currentActor = new Actor(token.text);

                    if (!result.hasActor(currentActor))
                        result.addActor(currentActor);

                    break;
                case "dialogue_begin":
                    if (currentScene == null) {
                        currentScene = new Scene("");
                        result.addScene(currentScene);
                    }
                    break;
                case "dialogue":
                    if (currentScene != null) {
                        currentScene.addLine(new Line(currentActor, token.text));
                    }
                    break;
                case "dialogue_end":
                    break;
                case "scene_heading":
                    currentScene = new Scene(token.text);
                    result.addScene(currentScene);
                    break;
                case "action":
                    if (currentScene == null) {
                        currentScene = new Scene("");
                        result.addScene(currentScene);
                    }
                    currentScene.addAction(token.text);
                    break;
            }
        }

        return result;
    }
}
