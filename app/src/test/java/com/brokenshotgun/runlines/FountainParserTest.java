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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class FountainParserTest {

    @Before
    public void setup() {

    }

    @Test
    public void testParseScript() {
        Script result = FountainParser.parse(TEST_SCRIPT_FOUNTAIN);
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

    private static final String TEST_SCRIPT_FOUNTAIN =
            "Title: Big Fish\n" +
            "Credit: written by\n" +
            "Author: John August\n" +
            "Source: based on the novel by Daniel Wallace\n" +
            "Notes:\t\n" +
            "\tFINAL PRODUCTION DRAFT\n" +
            "\tincludes post-production dialogue \n" +
            "\tand omitted scenes\n" +
            "Copyright: (c) 2003 Columbia Pictures\n" +
            "\n" +
            "This is a Southern story, full of lies and fabrications, but truer for their inclusion.\n" +
            "\n" +
            "====\n" +
            "\n" +
            "**FADE IN:**\n" +
            "\n" +
            "A RIVER.\n" +
            "\n" +
            "We're underwater, watching a fat catfish swim along.  \n" +
            "\n" +
            "This is The Beast.\n" +
            "\n" +
            "EDWARD (V.O.)\n" +
            "There are some fish that cannot be caught.  It's not that they're faster or stronger than other fish.  They're just touched by something extra.  Call it luck.  Call it grace.  One such fish was The Beast.  \n" +
            "\n" +
            "The Beast's journey takes it past a dangling fish hook, baited with worms.  Past a tempting lure, sparkling in the sun.  Past a swiping bear claw.  The Beast isn't worried.\n" +
            "\n" +
            "EDWARD (V.O.)(CONT'D)\n" +
            "By the time I was born, he was already a legend.  He'd taken more hundred-dollar lures than any fish in Alabama. Some said that fish was the ghost of Henry Walls, a thief who'd drowned in that river 60 years before.   Others claimed he was a lesser dinosaur, left over from the Cretaceous period.\n" +
            "\n" +
            "INT.  WILL'S BEDROOM - NIGHT (1973)\n" +
            "\n" +
            "WILL BLOOM, AGE 3, listens wide-eyed as his father EDWARD BLOOM, 40's and handsome, tells the story.  In every gesture, Edward is bigger than life, describing each detail with absolute conviction.\n" +
            "\n" +
            "EDWARD\n" +
            "I didn't put any stock into such speculation or superstition.  All I knew was I'd been trying to catch that fish since I was a boy no bigger than you.  \n" +
            "(closer)\n" +
            "And on the day you were born, that was the day I finally caught him.\n" +
            "\n" +
            "EXT.  CAMPFIRE - NIGHT (1977)\n" +
            "\n" +
            "A few years later, and Will sits with the other INDIAN GUIDES as Edward continues telling the story to the tribe.  \n" +
            "\n" +
            "EDWARD\n" +
            "Now, I'd tried everything on it:  worms, lures, peanut butter, peanut butter-and-cheese.  But on that day I had a revelation:  if that fish was the ghost of a thief, the usual bait wasn't going to work.  I would have to use something he truly desired. \n" +
            "\n" +
            "Edward points to his wedding band, glinting in the firelight.\n" +
            "\n" +
            "LITTLE BRAVE\n" +
            "(confused)\n" +
            "Your finger?\n" +
            "\n" +
            "Edward slips his ring off.\n" +
            "\n" +
            "EDWARD\n" +
            "Gold.\n" +
            "\n" +
            "While the other boys are rapt with attention, Will looks bored.  He's heard this story before.\n" +
            "\n" +
            "EDWARD\n" +
            "I tied my ring to the strongest line they made -- strong enough to hold up a bridge, they said, if just for a few minutes -- and I cast upriver.\n" +
            "\n" +
            "INT.  BLOOM FRONT HALL - NIGHT (1987)\n" +
            "\n" +
            "Edward is chatting up Will's pretty DATE to the homecoming dance.  She is enjoying the story, but also the force of Edward's charisma.  He's hypnotizing.\n" +
            "\n" +
            "EDWARD (CONT'D)\n" +
            "The Beast jumped up and grabbed it before the ring even hit the water.  And just as fast, he snapped clean through that line.\n" +
            "\n" +
            "WILL, now 17 with braces, is fuming and ready to leave.  His mother SANDRA -- from whom he gets his good looks and practicality -- stands with him at the door.\n" +
            "\n" +
            "EDWARD\n" +
            "You can see my predicament.  My wedding ring, the symbol of fidelity to my wife, soon to be the mother of my child, was now lost in the gut of an uncatchable fish.\n" +
            "\n" +
            "ON WILL AND SANDRA\n" +
            "\n" +
            "WILL\n" +
            "(low but insistent)\n" +
            "Make him stop.\n" +
            "\n" +
            "His mother pats him sympathetically, then adjusts his tie.\n" +
            "\n" +
            "WILL'S DATE\n" +
            "What did you do?\n" +
            "\n" +
            "EDWARD\n" +
            "I followed that fish up-river and down-river for three days and three nights, until I finally had him boxed in.\n" +
            "\n" +
            "Will regards his father with exasperated contempt.\n" +
            "\n" +
            "EDWARD\n" +
            "With these two hands, I reached in and snatched that fish out of the river.  I looked him straight in the eye.  And I made a remarkable discovery. \n" +
            "\n" +
            "INT.  TINY PARIS RESTAURANT (LA RUE 14°) - NIGHT (1998)\n" +
            "\n" +
            "WILL, now 28, sits with his gorgeous bride JOSEPHINE.  This is their wedding reception, crowded with their friends and family.  They should be joyful, but Will is furious.\n" +
            "\n" +
            "Edward has the floor, ostensibly for a toast.  The room is cozy and drunk.\n" +
            "\n" +
            "EDWARD\n" +
            "This fish, the Beast.  The whole time we were calling it a him, when in fact it was a her.  It was fat with eggs, and was going to lay them any day.\n" +
            "\n" +
            "Over near the doorway, we spot Sandra, just returned from the restrooms.  She looks gorgeous.  She couldn't be any happier if this were her own wedding.\n" +
            "\n" +
            "EDWARD\n" +
            "Now, I was in a situation.  I could gut that fish and get my ring back, but doing so I would be killing the smartest catfish in the Ashton River, soon to be mother of a hundred others.  \n" +
            "\n" +
            "Will can't take any more.  Josephine tries to hold him back, but he gets up and leaves.  Edward doesn't even notice.\n" +
            "\n" +
            "EDWARD (CONT'D)\n" +
            "Did I want to deprive my soon-to-be-born son the chance to catch a fish like this of his own?  This lady fish and I, well, we had the same destiny.\n" +
            "\n" +
            "As he leaves, Will mutters in perfect unison with his father--\n" +
            "\n" +
            "EDWARD AND WILL\n" +
            "We were part of the same equation.\n" +
            "\n" +
            "Will reaches the door, where his mother intercepts him.\n" +
            "\n" +
            "SANDRA\n" +
            "Honey, it's still your night.\n" +
            "\n" +
            "Will can't articulate his anger.  He just leaves.\n" +
            "\n" +
            "EDWARD\n" +
            "Now, you may well ask, since this lady fish wasn't the ghost of a thief, why did it strike so quick on gold when nothing else would attract it?\n" +
            "(closer; he holds up his ring)\n" +
            "That was the lesson I learned that day, the day my son was born.  \n" +
            "\n" +
            "He focuses his words on Sandra.  This story is -- and has always been -- about her more than anyone.\n" +
            "\n" +
            "EDWARD\n" +
            "Sometimes, the only way to catch an uncatchable woman is to offer her a wedding ring.\n" +
            "\n" +
            "A LAUGH from the crowd.  \n" +
            "\n" +
            "Edward motions for Sandra to get up here with him.  As she crosses, we can see that thirty years of marriage has not lessened their affection for each other.  \n" +
            "\n" +
            "As they kiss, Edward tweaks her chin a special little way.  The crowd APPLAUDS.\n" +
            "\n" +
            "Edward toasts the happy couple.  Josephine covers well for her absent husband, a smile as warm as summer.\n" +
            "\n" +
            "Edward downs his champagne in a gulp.\n" +
            "\n" +
            "EXT.  OUTSIDE LA RUE 14° - NIGHT\n" +
            "\n" +
            "We come into the middle of an argument on the sidewalk.  Occasional PASSERSBY take notice, especially as it gets more heated.  Both men are a little drunk.\n" +
            "\n" +
            "EDWARD\n" +
            "What, a father's not allowed to talk about his son?";
}
