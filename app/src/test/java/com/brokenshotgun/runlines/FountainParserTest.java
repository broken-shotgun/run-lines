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

import com.brokenshotgun.runlines.data.FountainParser;
import com.brokenshotgun.runlines.model.Actor;
import com.brokenshotgun.runlines.model.Line;
import com.brokenshotgun.runlines.model.Scene;
import com.brokenshotgun.runlines.model.Script;
import com.brokenshotgun.runlines.model.upgrade.ScriptV073;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class FountainParserTest {

    @Before
    public void setup() {

    }

    @Test
    public void testParseScript() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        assert classLoader != null;
        URL resource = classLoader.getResource("bigfish.fountain.txt");
        File file = new File(resource.getPath());
        String testScriptStr = convertStreamToString(new FileInputStream(file));

        Script result = FountainParser.parse(testScriptStr);
        assertNotNull(result);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(result));
    }

    @Test
    public void testFormatScript() {
        Script script = new Script("Fountain Format Test Script");
        Actor actor = new Actor("JASON");
        script.addActor(actor);
        Scene scene = new Scene("INT. A CREEPY BASEMENT");
        script.addScene(scene);
        scene.addLine(new Line(actor, "hello world"));
        scene.addAction("Jason whips off his sunglasses");
        scene.addLine(new Line(actor, "my name is Jason\n(pause)\nAnd this is a test"));
        scene.addAction("Queue CSI: Miami YEAAAAAAAAAAAAAAAAAH (Won't Get Fooled Again by The Who)");

        String result = FountainParser.format(script);
        System.out.println(result);
    }

    @Test
    public void testScriptModelUpgrade() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        assert classLoader != null;
        URL resource = classLoader.getResource("test_release_v0.7.3.json");
        File file = new File(resource.getPath());
        String testJson = convertStreamToString(new FileInputStream(file));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ScriptV073 test = gson.fromJson(testJson, ScriptV073.class);
        assertNotNull(test);

        Script upgrade = test.convert();
        String result = FountainParser.format(upgrade);
        System.out.println(result);
    }

    private String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }
}
