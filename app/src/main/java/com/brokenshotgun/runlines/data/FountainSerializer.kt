package com.brokenshotgun.runlines.data

import com.brokenshotgun.runlines.model.Script
import java.lang.Exception

object FountainSerializer {
    const val UNIVERSAL_LINE_BREAKS_PATTERN  = "\\r\\n|\\r|\\n"
    const val UNIVERSAL_LINE_BREAKS_TEMPLATE = "\n"

    /** Patterns */

    const val SCENE_HEADER_PATTERN       = "(?<=\\n)(([iI][nN][tT]|[eE][xX][tT]|[^\\w][eE][sS][tT]|\\.|[iI]\\.?\\/[eE]\\.?)([^\\n]+))\\n"
    const val ACTION_PATTERN             = "([^<>]*?)(\\n{2}|\\n<)"
    const val MULTI_LINE_ACTION_PATTERN  = "\n{2}(([^a-z\\n:]+?[\\.\\?,\\s!\\*_]*?)\n{2}){1,2}"
    const val CHARACTER_CUE_PATTERN      = "(?<=\\n)([ \\t]*[^<>a-z\\s\\/\\n][^<>a-z:!\\?\\n]*[^<>a-z\\(!\\?:,\\n\\.][ \\t]?)\\n{1}(?!\\n)"
    const val DIALOGUE_PATTERN           = "(<(Character|Parenthetical)>[^<>\\n]+<\\/(Character|Parenthetical)>)([^<>]*?)(?=\\n{2}|\\n{1}<Parenthetical>)"
    const val PARENTHETICAL_PATTERN      = "(\\([^<>]*?\\)[\\s]?)\n"
    const val TRANSITION_PATTERN         = "\\n([\\*_]*([^<>\\na-z]*TO:|FADE TO BLACK\\.|FADE OUT\\.|CUT TO BLACK\\.)[\\*_]*)\\n"
    const val FORCED_TRANSITION_PATTERN  = "\\n((&gt;|>)\\s*[^<>\\n]+)\\n"     // need to look for &gt; pattern because we run this regex against marked up content
    const val FALSE_TRANSITION_PATTERN   = "\\n((&gt;|>)\\s*[^<>\\n]+(&lt;\\s*))\\n"     // need to look for &gt; pattern because we run this regex against marked up content
    const val PAGE_BREAK_PATTERN         = "(?<=\\n)(\\s*[\\=\\-\\_]{3,8}\\s*)\\n{1}"
    const val CLEANUP_PATTERN            = "<Action>\\s*<\\/Action>"
    const val FIRST_LINE_ACTION_PATTERN  = "^\\n\\n([^<>\\n#]*?)\\n"
    const val SCENE_NUMBER_PATTERN       = "(\\#([0-9A-Za-z\\.\\)-]+)\\#)"
    const val SECTION_HEADER_PATTERN     = "((#+)(\\s*[^\\n]*))\\n?"

    /** Templates */

    const val SCENE_HEADER_TEMPLATE      = "\n<Scene Heading>$1</Scene Heading>"
    const val ACTION_TEMPLATE            = "<Action>$1</Action>$2"
    const val MULTI_LINE_ACTION_TEMPLATE = "\n<Action>$2</Action>"
    const val CHARACTER_CUE_TEMPLATE     = "<Character>$1</Character>"
    const val DIALOGUE_TEMPLATE          = "$1<Dialogue>$4</Dialogue>"
    const val PARENTHETICAL_TEMPLATE     = "<Parenthetical>$1</Parenthetical>"
    const val TRANSITION_TEMPLATE        = "\n<Transition>$1</Transition>"
    const val FORCED_TRANSITION_TEMPLATE = "\n<Transition>$1</Transition>"
    const val FALSE_TRANSITION_TEMPLATE  = "\n<Action>$1</Action>"
    const val PAGE_BREAK_TEMPLATE        = "\n<Page Break></Page Break>\n"
    const val CLEANUP_TEMPLATE           = ""
    const val FIRST_LINE_ACTION_TEMPLATE = "<Action>$1</Action>\n"
    const val SECTION_HEADER_TEMPLATE    = "<Section Heading>$1</Section Heading>"

    /** Block Comments */

    const val BLOCK_COMMENT_PATTERN      = "\\n/\\*([^<>]+?)\\*/\\n"
    const val BRACKET_COMMENT_PATTERN    = "\\n\\[{2}([^<>]+?)]{2}\\n"
    const val SYNOPSIS_PATTERN           = "\\n={1}([^<>=][^<>]+?)\\n"     // we need to make sure we don't catch ==== as a synopsis

    const val BLOCK_COMMENT_TEMPLATE     = "\n<Boneyard>$1</Boneyard>\n"
    const val BRACKET_COMMENT_TEMPLATE   = "\n<Comment>$1</Comment>\n"
    const val SYNOPSIS_TEMPLATE          = "\n<Synopsis>$1</Synopsis>\n"

    const val NEWLINE_REPLACEMENT        = "@@@@@"
    const val NEWLINE_RESTORE            = "\n"


    /** Title Page */

    const val TITLE_PAGE_PATTERN             = "^([^\\n]+:(([ \\t]*|\\n)[^\\n]+\\n)+)+\\n"
    const val INLINE_DIRECTIVE_PATTERN       = "^([\\w\\s&]+):\\s*([^\\s][\\w&,\\.\\?!:\\(\\)\\/\\s-©\\*\\_]+)$"
    const val MULTI_LINE_DIRECTIVE_PATTERN   = "^([\\w\\s&]+):\\s*$"
    const val MULTI_LINE_DATA_PATTERN        = "^([ ]{2,8}|\\t)([^<>]+)$"


    /** Misc */

    const val DUAL_DIALOGUE_PATTERN          = "\\^\\s*$"
    const val CENTERED_TEXT_PATTERN          = "^>[^<>\\n]+<"


    /** Styling for FDX */

    const val BOLD_ITALIC_UNDERLINE_PATTERN  = "(_\\*{3}|\\*{3}_)([^<>]+)(_\\*{3}|\\*{3}_)"
    const val BOLD_ITALIC_PATTERN            = "(\\*{3})([^<>]+)(\\*{3})"
    const val BOLD_UNDERLINE_PATTERN         = "(_\\*{2}|\\*{2}_)([^<>]+)(_\\*{2}|\\*{2}_)"
    const val ITALIC_UNDERLINE_PATTERN       = "(_\\*{1}|\\*{1}_)([^<>]+)(_\\*{1}|\\*{1}_)"
    const val BOLD_PATTERN                   = "(\\*{2})([^<>]+)(\\*{2})"
    const val ITALIC_PATTERN                 = "(?<!\\\\)(\\*{1})([^<>]+)(\\*{1})"
    const val UNDERLINE_PATTERN              = "(_)([^<>_]+)(_)"

    /** Styling templates */

    const val BOLD_ITALIC_UNDERLINE_TEMPLATE = "Bold+Italic+Underline"
    const val BOLD_ITALIC_TEMPLATE           = "Bold+Italic"
    const val BOLD_UNDERLINE_TEMPLATE        = "Bold+Underline"
    const val ITALIC_UNDERLINE_TEMPLATE      = "Italic+Underline"
    const val BOLD_TEMPLATE                  = "Bold"
    const val ITALIC_TEMPLATE                = "Italic"
    const val UNDERLINE_TEMPLATE             = "Underline"

    /** Data classes */
    data class FNElement(
            var elementType: String = "",
            var elementText: String = "",
            var isCentered: Boolean = false,
            var sceneNumber: String = "",
            var isDualDialogue: Boolean = false,
            var sectionDepth: Int = 0)

    @JvmStatic
    fun serialize(script: Script): String {
        // TODO
        return ""
    }

    @JvmStatic
    fun deserialize(script: String): Script {
        val titleTokens = parseTitlePageOfString(getScriptTitlePage(script))
        val bodyTokens = parseBodyOfString(getScriptBody(script))
        val allTokens = titleTokens + bodyTokens

        // for debugging only
        println("##### all tokens for Script =")
        for(token in allTokens) {
            println("<${token.elementType}>${token.elementText}</${token.elementType}>")
        }


        return Script(allTokens)
    }

    private fun parseBodyOfString(scriptBody: String) : Array<FNElement> {
        // Three-pass parsing method.
        // 1st we check for block comments, and manipulate them for regexes
        // 2nd we run regexes against the file to convert it into a marked up format
        // 3rd we split the marked up elements, and loop through them adding each to
        //   an our array of FNElements.
        //
        // The intermediate marked up format makes subsequent parsing very simple,
        // even if it means less efficiency overall.

        // 1st pass - Block comments
        // The regexes aren't smart enough (yet) to deal with newlines in the
        // comments, so we need to convert them before processing.
        var scriptContent = scriptBody

        BLOCK_COMMENT_PATTERN.toRegex().findAll(scriptContent).forEach { blockMatch ->
            val modifiedBlock = blockMatch.value.replace("\n", NEWLINE_REPLACEMENT)
            scriptContent = scriptContent.replace(blockMatch.value, modifiedBlock, true)
        }

        BRACKET_COMMENT_PATTERN.toRegex().findAll(scriptContent).forEach { bracketMatch ->
            val modifiedBlock = bracketMatch.value.replace("\n", NEWLINE_REPLACEMENT)
            scriptContent = scriptContent.replace(bracketMatch.value, modifiedBlock, true)
        }

        // Sanitize < and > chars for conversion to the markup
        scriptContent = scriptContent.replace("<", "&lt;", true)
        scriptContent = scriptContent.replace(">", "&gt;", true)
        scriptContent = scriptContent.replace("...", "::trip::", true)

        // 2nd pass - Regexes
        // Blast the script with regexes.
        // Make sure pattern and template regexes match up!
        val patterns  = arrayOf(UNIVERSAL_LINE_BREAKS_PATTERN, BLOCK_COMMENT_PATTERN,
                BRACKET_COMMENT_PATTERN, SYNOPSIS_PATTERN, PAGE_BREAK_PATTERN, FALSE_TRANSITION_PATTERN, FORCED_TRANSITION_PATTERN,
                SCENE_HEADER_PATTERN, FIRST_LINE_ACTION_PATTERN, TRANSITION_PATTERN,
                CHARACTER_CUE_PATTERN, PARENTHETICAL_PATTERN, DIALOGUE_PATTERN, SECTION_HEADER_PATTERN,
                ACTION_PATTERN, CLEANUP_PATTERN, NEWLINE_REPLACEMENT)

        val templates = arrayOf(UNIVERSAL_LINE_BREAKS_TEMPLATE, BLOCK_COMMENT_TEMPLATE,
                BRACKET_COMMENT_TEMPLATE, SYNOPSIS_TEMPLATE, PAGE_BREAK_TEMPLATE, FALSE_TRANSITION_TEMPLATE, FORCED_TRANSITION_TEMPLATE,
                SCENE_HEADER_TEMPLATE, FIRST_LINE_ACTION_TEMPLATE, TRANSITION_TEMPLATE,
                CHARACTER_CUE_TEMPLATE, PARENTHETICAL_TEMPLATE, DIALOGUE_TEMPLATE, SECTION_HEADER_TEMPLATE,
                ACTION_TEMPLATE, CLEANUP_TEMPLATE, NEWLINE_RESTORE)

        if (patterns.size != templates.size) {
            throw Exception("The pattern and template arrays don't have the same number of objects!")
        }

        for(i in patterns.indices) {
            scriptContent = patterns[i].toRegex().replace(scriptContent, templates[i])
        }

        // 3rd pass - Array construction
        val elementsArray = mutableListOf<FNElement>()
        val tagMatching = "<([a-zA-Z\\s]+)>([^<>]*)</[a-zA-Z\\s]+>"
        tagMatching.toRegex().findAll(scriptContent).forEach { tag ->
            val element = FNElement()
            val elementType = tag.groupValues[1]
            val elementText = tag.groupValues[2]

            // Convert < and > back to normal
            var cleanedText = elementText
            cleanedText = cleanedText.replace("&lt;", "<", true)
            cleanedText = cleanedText.replace("&gt;", ">", true)
            cleanedText = cleanedText.replace("::trip::", "...", true)

            // Deal with scene numbers if we are in a scene heading
            if (elementType == "Scene Heading") {
                val sceneNumberMatch = SCENE_HEADER_PATTERN.toRegex(RegexOption.IGNORE_CASE).find(cleanedText)
                if (sceneNumberMatch != null) {
                    val fullSceneNumberText = sceneNumberMatch.groupValues[1]
                    val sceneNumber = sceneNumberMatch.groupValues[2]
                    element.sceneNumber = sceneNumber
                    cleanedText = cleanedText.replace(fullSceneNumberText, "", true)
                }
            }

            element.elementType = elementType
            element.elementText = cleanedText.trim()

            elementsArray.add(element)
        }

        /*
        for (NSInteger i = 0; i < max; i++) {

            // More refined processing of elements based on text/type
            if ([element.elementText isMatchedByRegex:CENTERED_TEXT_PATTERN]) {
                element.isCentered = YES;
                element.elementText = [[element.elementText stringByMatching:@"(>?)\\s*([^<>\\n]*)\\s*(<?)" capture:2] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
            }

            if ([element.elementType isEqualToString:@"Scene Heading"]) {
                // Check for a forced scene heading. Remove preceeding dot.
                element.elementText = [element.elementText stringByMatching:@"^\\.?(.+)" capture:1];
            }

            if ([element.elementType isEqualToString:@"Section Heading"]) {
                // Clean the section text, and get the section depth
                NSString *depthChars = [element.elementText stringByMatching:SECTION_HEADER_PATTERN capture:2];
                NSUInteger depth = [depthChars length];
                element.sectionDepth = depth;
                element.elementText = [element.elementText stringByMatching:SECTION_HEADER_PATTERN capture:3];
            }

            if (i > 1 && [element.elementType isEqualToString:@"Character"] && [element.elementText isMatchedByRegex:DUAL_DIALOGUE_PATTERN]) {
                element.isDualDialogue = YES;

                // clean the ^ mark
                element.elementText = [element.elementText stringByReplacingOccurrencesOfRegex:@"\\s*\\^$" withString:@""];

                // find the previous character cue
                NSInteger j = i - 1;

                FNElement *previousElement;
                NSSet *dialogueBlockTypes   = [NSSet setWithObjects:@"Dialogue", @"Parenthetical", nil];
                do {
                    previousElement = elementsArray[j];
                    if ([previousElement.elementType isEqualToString:@"Character"]) {
                        previousElement.isDualDialogue = YES;
                        previousElement.elementText = [previousElement.elementText stringByReplacingOccurrencesOfString:@"^" withString:@""];
                    }
                    j--;
                } while (j >= 0 && [dialogueBlockTypes containsObject:previousElement.elementType]);
            }

            [elementsArray addObject:element];
        }
        return [NSArray arrayWithArray:elementsArray];
         */
        return elementsArray.toTypedArray()
    }

    private fun parseTitlePageOfString(scriptTitle: String) : Array<FNElement> {
        val elementsArray = mutableListOf<FNElement>()

        /*
        NSMutableString *rawTitlePage = [NSMutableString stringWithString:[self titlePageOfString:string]];
        NSMutableArray *contents = [NSMutableArray array];

        // Line by line parsing
        // split the title page using newlines, then walk through the array and determine what is what
        // this allows us to look for very specific things and better handle non-uniform title pages

        // split the string
        NSArray *splitTitlePage = [rawTitlePage componentsSeparatedByString:@"\n"];

        NSString *openDirective = nil;
        NSMutableArray *directiveData = [NSMutableArray array];

        for (NSString *line in splitTitlePage) {
            // is this an inline directive or a multi-line one?
            if ([line isMatchedByRegex:INLINE_DIRECTIVE_PATTERN]) {
                // if there's an openDirective with data, save it
                if (openDirective != nil && [directiveData count] > 0) {
                    [contents addObject:@{openDirective: directiveData}];
                    directiveData = [NSMutableArray array];
                }
                openDirective = nil;

                NSString *key = [[line stringByMatching:INLINE_DIRECTIVE_PATTERN capture:1] lowercaseString];
                NSString *val = [line stringByMatching:INLINE_DIRECTIVE_PATTERN capture:2];

                // validation
                if ([key isEqualToString:@"author"] || [key isEqualToString:@"author(s)"]) {
                    key = @"authors";
                }

                [contents addObject:@{key: @[val]}];
            }
            else if ([line isMatchedByRegex:MULTI_LINE_DIRECTIVE_PATTERN]) {
                // if there's an openDirective with data, save it
                if (openDirective != nil && [directiveData count] > 0) {
                    [contents addObject:@{openDirective: directiveData}];
                }

                openDirective = [[line stringByMatching:MULTI_LINE_DIRECTIVE_PATTERN capture:1] lowercaseString];
                directiveData = [NSMutableArray array];

                // validation
                if ([openDirective isEqualToString:@"author"] || [openDirective isEqualToString:@"author(s)"]) {
                    openDirective = @"authors";
                }
            }
            else {
                if ([line stringByMatching:MULTI_LINE_DATA_PATTERN capture:2]) {
                    [directiveData addObject:[line stringByMatching:MULTI_LINE_DATA_PATTERN capture:2]];
                }
            }
        }

        if (openDirective != nil && [directiveData count] > 0) {
            [contents addObject:@{openDirective: directiveData}];
        }
        return contents;
         */
        return elementsArray.toTypedArray()
    }

    private fun getScriptBody(script: String) : String {
        var body = script
        body = body.replace("^\\n+".toRegex(), "")

        // Find title page by looking for the first blank line, then checking the
        // text above it. If a title page is found we remove it, leaving only the
        // body content.
        val firstBlankLine = body.indexOf("\n\n")
        if(firstBlankLine != -1) {
            var documentTop = body.substring(0, firstBlankLine+1)
            documentTop += "\n"

            if (TITLE_PAGE_PATTERN.toRegex().matches(documentTop)) {
                body = body.substring(firstBlankLine+1)
            }
        }

        return "\n\n$body\n\n"
    }

    private fun getScriptTitlePage(script: String) : String {
        var body = script
        body = body.replace("^\\n+".toRegex(), "")

        val firstBlankLine = body.indexOf("\n\n")
        if(firstBlankLine != -1) {
            var documentTop = body.substring(0, firstBlankLine+1)
            documentTop += "\n"

            if (TITLE_PAGE_PATTERN.toRegex().matches(documentTop)) {
                documentTop = documentTop.replace("^\n+".toRegex(), "")
                documentTop = documentTop.replace("\n+$".toRegex(), "")
                return documentTop
            }
        }

        return ""
    }
}