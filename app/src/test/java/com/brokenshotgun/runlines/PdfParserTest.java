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
import com.brokenshotgun.runlines.data.PdfParser;
import com.brokenshotgun.runlines.model.Script;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class PdfParserTest {
    @Test
    public void testParseScript() {
        Script result = PdfParser.parse(TEST_CONTENT);
        assertNotNull(result);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(result));
    }

    String TEST_CONTENT = "\n" +
            "\n" +
            "Title: The Flick A1S7  \n" +
            " \n" +
            "==== \n" +
            " \n" +
            "A day later.  Sam and Avery are standing, holding their brooms, in the middle of the aisle.  They \n" +
            "are gazing up at the tile ceiling, which now has an ominous gap in it.  \n" +
            " \n" +
            "SAM \n" +
            "...It happened on Sunday.  \n" +
            " \n" +
            "They stare at it for a while. \n" +
            " \n" +
            "SAM \n" +
            "Brian and Rebecca are working, it's the matinee, ho hum, ho hum, there's just a few people in \n" +
            "the audience, and out of nowhere this huge chunk of tile...  \n" +
            "(he points to the gap in the ceiling)  \n" +
            "...comes crashing down and LANDS ON THE SEAT NEXT TO SOME OLD LADY.  Like two \n" +
            "more inches to the right and she'd be dead.  Apparently there was plaster all over her old lady \n" +
            "sweater.   \n" +
            " \n" +
            "AVERY \n" +
            "Did she\u00AD she could sue, right?  Could you sue over that kind of thing? \n" +
            " \n" +
            "SAM \n" +
            "Probably.  Probably.  But Brian is like this huge charmer, apparently Brian just like turned on the \n" +
            "charm and calmed her down and gave her a voucher for like six free popcorns and six free \n" +
            "sodas which by the way he just drew himself on a receipt or something so if an old lady comes \n" +
            "in with a weird cartoon that says she gets a free popcorn or soda give it to her no questions \n" +
            "asked.  \n" +
            " \n" +
            "AVERY \n" +
            "He didn't even give her free tickets?  \n" +
            " \n" +
            "SAM \n" +
            "He didn't even do that. \n" +
            " \n" +
            "They gaze up at the ceiling for a long time.  \n" +
            " \n" +
            "SAM \n" +
            "It's a liability.  It's a huge liability.  It's\u00AD someone's gonna get killed and then what.  \n" +
            " \n" +
            "AVERY \n" +
            "Can't he just put in a new ceiling?  \n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            " \n" +
            "SAM \n" +
            "Well.  These are the questions a normal person would ask.  But we're talking about Steve. \n" +
            "Steve will never spend a dime on anything.  Steve would rather this place burn down than he \n" +
            "like spend a little money to make it safe or have a nacho machine at concessions.  Wouldn't that \n" +
            "be nice?  If we could make those nachos with the little cheese squirter thingy?  I keep telling him \n" +
            "to get one of those.  \n" +
            " \n" +
            "They go back to sweeping. \n" +
            " \n" +
            "Avery nods grimly. \n" +
            " \n" +
            "SAM \n" +
            "We haven't sold out a single show since Slumdog Millionaire.  It's pathetic.  \n" +
            " \n" +
            "They sweep for a while.  Silence.  \n" +
            " \n" +
            "AVERY \n" +
            "Oh.  So uh.  I went up to the booth the other day and I, uh... I didn't realize there were so many \n" +
            "old reels up there.  \n" +
            " \n" +
            "SAM \n" +
            "Oh yeah.  Steve's such a sketchball.  He's supposed to send them back to the distributor.  And \n" +
            "they're all like sitting up there collecting dust.  Some of them are really old.  \n" +
            " \n" +
            "AVERY \n" +
            "There's a lot of good stuff up there.  \n" +
            " \n" +
            "SAM \n" +
            "I guess.  \n" +
            " \n" +
            "AVERY \n" +
            "I uh... This is probably a stupid idea.  But.  Uh.  I was thinking that on uh... I was thinking on \n" +
            "Friday it might be fun to like... we could like just stay here after the last show and watch one or \n" +
            "two of me.   \n" +
            "(short pause)  \n" +
            "Like I saw Goodfellas and Boogie Nights and a couple other\u00AD  \n" +
            "(nervously)  \n" +
            "It would just be awesome to see them on like the big screen.  I've only watched Goodfellas on \n" +
            "my computer which is pretty like blasphemous when you think about it.   \n" +
            "(pause)   \n" +
            "It's fine if like you're busy or not interested or whatever.   \n" +
            " \n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "Sam is still sweeping. \n" +
            " \n" +
            "SAM \n" +
            "No. No.  That sounds... that sounds cool.  I just uh... I'm not gonna be here this weekend.  \n" +
            " \n" +
            "AVERY \n" +
            "Oh.  \n" +
            " \n" +
            "SAM \n" +
            "Yeah.  Rose will be here if you need help with anything... Steve didn't tell you?  \n" +
            " \n" +
            "AVERY \n" +
            "No.  \n" +
            " \n" +
            "SAM \n" +
            "Uch.  He's an idiot.   \n" +
            "(a short pause)   \n" +
            "Yeah. It's not that hard with two people.  Rose will come early and help you with set\u00ADup and box \n" +
            "office and stuff and then you'll just do clean\u00ADup on your own.  \n" +
            " \n" +
            "AVERY \n" +
            "Oh.  Yeah.  / Okay\u00AD  \n" +
            " \n" +
            "SAM \n" +
            "You've been here three weeks so I assumed you felt comfortable with everything / and\u00AD  \n" +
            " \n" +
            "AVERY \n" +
            "Oh, yeah.  Yeah. Sure. \n" +
            " \n" +
            "SAM \n" +
            "Steve should pay you double but of course he won't.  So you'll just get my dinner money.  \n" +
            " \n" +
            "AVERY \n" +
            "You odn't need / to\u00AD  \n" +
            " \n" +
            "SAM \n" +
            "No, that's the way it works.  \n" +
            " \n" +
            "Pause. \n" +
            " \n" +
            "AVERY \n" +
            "Where are you going?  \n" +
            " \n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "A short and unnecessarily weird pause. \n" +
            " \n" +
            "SAM \n" +
            "...My brother is getting married.  \n" +
            " \n" +
            "AVERY \n" +
            "Oh!  Wow.  \n" +
            " \n" +
            "SAM \n" +
            "Yeah.  In Connecticut.  Right outside Bridgeport.  \n" +
            " \n" +
            "AVERY \n" +
            "Congratulations.  \n" +
            " \n" +
            "SAM \n" +
            "Yeah.  \n" +
            " \n" +
            "AVERY \n" +
            "And your whole family is going?  \n" +
            " \n" +
            "SAM \n" +
            "Yeah. Yeah.  \n" +
            " \n" +
            "AVERY \n" +
            "Cool.   \n" +
            "(pause)   \n" +
            "Older or younger?  \n" +
            " \n" +
            "SAM \n" +
            "What?  \n" +
            " \n" +
            "AVERY \n" +
            "Older or younger?  \n" +
            " \n" +
            "SAM \n" +
            "Oh.  Um.  Older.  Yeah.  He's 39.  \n" +
            " \n" +
            "Pause. \n" +
            " \n" +
            "AVERY \n" +
            "Do you like the woman?  \n" +
            " \n" +
            "SAM \n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "What?  \n" +
            " \n" +
            "AVERY \n" +
            "Do you like the woman he's marrying?  \n" +
            " \n" +
            "SAM \n" +
            "Uh.  Yeah.  I mean, I don't know, I've never met her.  \n" +
            " \n" +
            "AVERY \n" +
            "Oh. \n" +
            " \n" +
            "A long silence during which they go back to cleaning.  \n" +
            " \n" +
            "AVERY \n" +
            "What's your brother's name?  \n" +
            " \n" +
            "SAM \n" +
            "Jesse.  \n" +
            " \n" +
            "A long pause. \n" +
            " \n" +
            "SAM \n" +
            "(casually)  \n" +
            "He's retarded.  \n" +
            " \n" +
            "A short, confused pause.  \n" +
            " \n" +
            "AVERY \n" +
            "Do you mean / like he's\u00AD  \n" +
            " \n" +
            "SAM \n" +
            "Like in the actual definition of the word.  \n" +
            " \n" +
            "AVERY \n" +
            "Oh!  Okay.  \n" +
            " \n" +
            "SAM \n" +
            "She's retarded too.  The woman he's marrying.  \n" +
            " \n" +
            "AVERY \n" +
            "...Okay.  \n" +
            " \n" +
            "SAM \n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "They met at this uh residential uh facility in Connecticut.  \n" +
            " \n" +
            "AVERY \n" +
            "Cool. Cool.  \n" +
            " \n" +
            "SAM \n" +
            "Yeah.  \n" +
            " \n" +
            "AVERY \n" +
            "Um.  They must like each other a lot.  \n" +
            " \n" +
            "SAM \n" +
            "I guess.  \n" +
            " \n" +
            "Pause.  \n" +
            " \n" +
            "AVERY \n" +
            "Is it... does he have Down / Syndrome?  \n" +
            " \n" +
            "SAM \n" +
            "No.  He's just uh\u00AD He's basically like the\u00AD he's basically like a third grader.  \n" +
            " \n" +
            "AVERY \n" +
            "Oh.  \n" +
            " \n" +
            "Pause.  More sweeping. \n" +
            " \n" +
            "SAM \n" +
            "(a confession)  \n" +
            "I don't know him all that well.  \n" +
            " \n" +
            "Rose enters with a yo\u00ADyo.  \n" +
            " \n" +
            "AVERY \n" +
            "(trying to politely end the conversation)  \n" +
            "Well!  Next weekend maybe.  \n" +
            " \n" +
            "ROSE \n" +
            "Next weekend maybe what?  \n" +
            " \n" +
            "AVERY \n" +
            "Uh.  We were thinking of staying past closing on Friday and watching Goodfellas.  \n" +
            " \n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "ROSE \n" +
            "Oh yeah.  We have that upstairs.  \n" +
            " \n" +
            "AVERY \n" +
            "But Sam's gonna be away so I was saying maybe / next \u00AD  \n" +
            " \n" +
            "ROSE \n" +
            "I'll stay and watch it with you.  \n" +
            " \n" +
            "Sam, who has been standing in the second row, house right (stage left), sits down heavily in \n" +
            "one of the seats and closes his eyes. \n" +
            " \n" +
            "AVERY \n" +
            "(glancing over at Sam) Oh... uh\u00AD  \n" +
            " \n" +
            "AVERY \n" +
            "I don't know... maybe we should wait for Sam and then we can / all \u00AD  \n" +
            " \n" +
            "ROSE \n" +
            "Well we don't have to watch Goodfellas.  We could watch something else.  There's Mulholland \n" +
            "Drive.  That movie is hot.  And some other ones too from before it was The Flick.  I'm totally free \n" +
            "on Friday.  Let's do it.  \n" +
            " \n" +
            "Sam's face is no longer visible to Rose and Avery; he faces the movie screen and stares up at \n" +
            "it, beseechingly. \n" +
            " \n" +
            "AVERY \n" +
            "Uh. Yeah.  Sure.  Okay.  \n" +
            " \n" +
            "ROSE \n" +
            "Awesome.  Yes.  We should bring music and have like a rockin' dance party.  \n" +
            " \n" +
            "Sam closes his eyes. \n" +
            " \n" +
            "ROSE \n" +
            "Where are you gonna be this weekend, Sam?  \n" +
            " \n" +
            "SAM \n" +
            "(flatly, still facing forward)  \n" +
            "...My brother's wedding.  \n" +
            " \n" +
            "ROSE \n" +
            "Where?  \n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            " \n" +
            "SAM \n" +
            "Bridgeport.  \n" +
            " \n" +
            "ROSE \n" +
            "Cool.  \n" +
            " \n" +
            "Rose glances at the back of Sam's head, clocking that he's being a little weird, and then exits. \n" +
            "Avery starts to gather up his broom and dustpan and heads towards the door. \n" +
            " \n" +
            "AVERY \n" +
            "Hey.  Uh.  I keep meaning to ask.  Did you ever find out what was going on with your skin?  \n" +
            " \n" +
            "Sam is still sitting and facing forward during the next speech. \n" +
            " \n" +
            "SAM \n" +
            "Yeah.  I went to the doctor.   \n" +
            "(a short pause)  \n" +
            "It's called \"Pityriasis Rosea.\"  \n" +
            "(a short pause)  \n" +
            "It's not contagious.  \n" +
            "(a short pause)  \n" +
            "It looks like a fungus but it's not.   \n" +
            "(a short pause)  \n" +
            "They don't know what causes it but you get it all over your torso and it itches like fuck and it \n" +
            "lasts 6\u00AD8 weeks.  \n" +
            " \n" +
            "AVERY \n" +
            "Ah man. \n" +
            " \n" +
            "SAM \n" +
            "The good news is you only get it once in your life.  \n" +
            " \n" +
            "He stands up and lifts the back of shirt so Avery can see it. \n" +
            " \n" +
            "AVERY \n" +
            "Whoa!  \n" +
            " \n" +
            "SAM \n" +
            "The spots make like a\u00AD if the spots are in a distinctive Christmas Tree Formation you know it's \n" +
            "Pityriasis Rosea.  \n" +
            " \n" +
            "AVERY \n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "Wow.  Wow.  That looks pretty bad. \n" +
            " \n" +
            "Sam lowers his shirt. \n" +
            " \n" +
            "SAM \n" +
            "(deeply depressed)  \n" +
            "Yeah.  It's not though.  \n" +
            " \n" +
            "AVERY \n" +
            "You want box office or refreshments?  \n" +
            " \n" +
            "A long pause while Sam contemplates this. \n" +
            " \n" +
            "SAM \n" +
            "Box office.  \n" +
            " \n" +
            "After a few seconds, Sam slowly gets up and they start to exit together.  Rose enteres the \n" +
            "projection booth and we see her moving around in the window. \n" +
            " \n" +
            "Sam stands in the aisle and looks up at her. \n" +
            " \n" +
            "Avery waits by the door. \n" +
            " \n" +
            "Eventually Sam tears his gaze away from the projection booth, and they leave together.  The \n" +
            "doors slame shit. \n" +
            " \n" +
            "Blackout. \n" +
            "\n" +
            "\n";
}
