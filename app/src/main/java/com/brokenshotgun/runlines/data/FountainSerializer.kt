package com.brokenshotgun.runlines.data

import com.brokenshotgun.runlines.model.Actor
import com.brokenshotgun.runlines.model.Script
import java.util.*

/** Data classes */
data class FNElement(
        var elementType: String = "",
        var elementText: String = "",
        var isCentered: Boolean = false,
        var sceneNumber: String = "",
        var isDualDialogue: Boolean = false,
        var sectionDepth: Int = 0)

object FountainSerializer {
    private const val UNIVERSAL_LINE_BREAKS_PATTERN  = "\\r\\n|\\r|\\n"
    private const val UNIVERSAL_LINE_BREAKS_TEMPLATE = "\n"

    /** Patterns */
    private const val SCENE_HEADER_PATTERN       = "(?<=\\n)(([iI][nN][tT]|[eE][xX][tT]|[^\\w][eE][sS][tT]|\\.|[iI]\\.?/[eE]\\.?)([^\\n]+))\\n"
    private const val ACTION_PATTERN             = "([^<>]*?)(\\n{2}|\\n<)"
    private const val MULTI_LINE_ACTION_PATTERN  = "\n{2}(([^a-z\\n:]+?[\\.\\?,\\s!\\*_]*?)\n{2}){1,2}"
    private const val CHARACTER_CUE_PATTERN      = "(?<=\\n)([ \\t]*[^<>a-z\\s\\/\\n][^<>a-z:!\\?\\n]*[^<>a-z\\(!\\?:,\\n\\.][ \\t]?)\\n{1}(?!\\n)"
    private const val DIALOGUE_PATTERN           = "(<(Character|Parenthetical)>[^<>\\n]+<\\/(Character|Parenthetical)>)([^<>]*?)(?=\\n{2}|\\n{1}<Parenthetical>)"
    private const val PARENTHETICAL_PATTERN      = "(\\([^<>]*?\\)[\\s]?)\n"
    private const val TRANSITION_PATTERN         = "\\n([\\*_]*([^<>\\na-z]*TO:|FADE TO BLACK\\.|FADE OUT\\.|CUT TO BLACK\\.)[\\*_]*)\\n"
    private const val FORCED_TRANSITION_PATTERN  = "\\n((&gt;|>)\\s*[^<>\\n]+)\\n"     // need to look for &gt; pattern because we run this regex against marked up content
    private const val FALSE_TRANSITION_PATTERN   = "\\n((&gt;|>)\\s*[^<>\\n]+(&lt;\\s*))\\n"     // need to look for &gt; pattern because we run this regex against marked up content
    private const val PAGE_BREAK_PATTERN         = "(?<=\\n)(\\s*[\\=\\-\\_]{3,8}\\s*)\\n{1}"
    private const val CLEANUP_PATTERN            = "<Action>\\s*<\\/Action>"
    private const val FIRST_LINE_ACTION_PATTERN  = "^\\n\\n([^<>\\n#]*?)\\n"
    private const val SCENE_NUMBER_PATTERN       = "(\\#([0-9A-Za-z\\.\\)-]+)\\#)"
    private const val SECTION_HEADER_PATTERN     = "^((#+)(\\s*[^\\n]*))\n?$"

    const val CHARACTER_EXTENSION_PATTERN = "(\\([^<>]*?\\)[\\s]?)"

    /** Templates */
    private const val SCENE_HEADER_TEMPLATE      = "\n<Scene Heading>$1</Scene Heading>"
    private const val ACTION_TEMPLATE            = "<Action>$1</Action>$2"
    private const val MULTI_LINE_ACTION_TEMPLATE = "\n<Action>$2</Action>"
    private const val CHARACTER_CUE_TEMPLATE     = "<Character>$1</Character>"
    private const val DIALOGUE_TEMPLATE          = "$1<Dialogue>$4</Dialogue>"
    private const val PARENTHETICAL_TEMPLATE     = "<Parenthetical>$1</Parenthetical>"
    private const val TRANSITION_TEMPLATE        = "\n<Transition>$1</Transition>"
    private const val FORCED_TRANSITION_TEMPLATE = "\n<Transition>$1</Transition>"
    private const val FALSE_TRANSITION_TEMPLATE  = "\n<Action>$1</Action>"
    private const val PAGE_BREAK_TEMPLATE        = "\n<Page Break></Page Break>\n"
    private const val CLEANUP_TEMPLATE           = ""
    private const val FIRST_LINE_ACTION_TEMPLATE = "<Action>$1</Action>\n"
    private const val SECTION_HEADER_TEMPLATE    = "<Section Heading>$1</Section Heading>"

    /** Block Comments */
    private const val BLOCK_COMMENT_PATTERN      = "\\n/\\*([^<>]+?)\\*/\\n"
    private const val BRACKET_COMMENT_PATTERN    = "\\n\\[{2}([^<>]+?)]{2}\\n"
    private const val SYNOPSIS_PATTERN           = "\\n={1}([^<>=][^<>]+?)\\n"     // we need to make sure we don't catch ==== as a synopsis

    private const val BLOCK_COMMENT_TEMPLATE     = "\n<Boneyard>$1</Boneyard>\n"
    private const val BRACKET_COMMENT_TEMPLATE   = "\n<Comment>$1</Comment>\n"
    private const val SYNOPSIS_TEMPLATE          = "\n<Synopsis>$1</Synopsis>\n"

    private const val NEWLINE_REPLACEMENT        = "@@@@@"
    private const val NEWLINE_RESTORE            = "\n"

    /** Title Page */
    private const val TITLE_PAGE_PATTERN             = "^([^\\n]+:(([ \\t]*|\\n)[^\\n]+\\n)+)+\\n"
    private const val INLINE_DIRECTIVE_PATTERN       = "^([\\w\\s&]+):\\s*([^\\s][\\w&,.?!:()/\\s-©*_]+)$"
    private const val MULTI_LINE_DIRECTIVE_PATTERN   = "^([\\w\\s&]+):\\s*$"
    private const val MULTI_LINE_DATA_PATTERN        = "^([ ]{2,8}|\\t)([^<>]+)$"

    /** Misc */
    private const val DUAL_DIALOGUE_PATTERN          = "\\^\\s*$"
    private const val CENTERED_TEXT_PATTERN          = "^>[^<>\\n]+<"

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

    @JvmStatic
    fun getCharacterExtensions(characterName: String) : List<String> {
        val charExtList = mutableListOf<String>()
        CHARACTER_EXTENSION_PATTERN.toRegex().findAll(characterName).forEach { charExtList.add(it.value) }
        return charExtList.toList()
    }

    @JvmStatic
    fun serialize(script: Script): String {
        val builder = StringBuilder()

        builder.append("Title: ").append(script.name).append("\n")

        if (script.credit != null) {
            builder.append("Credit: ").append(script.credit).append("\n")
        }
        if (script.author != null) {
            builder.append("Author: ").append(script.author).append("\n")
        }
        if (script.source != null) {
            builder.append("Source: ").append(script.source).append("\n")
        }
        if (script.draftDate != null) {
            builder.append("Draft date: ").append(script.draftDate).append("\n")
        }
        if (script.contact != null) {
            builder.append("Contact: ").append(script.contact).append("\n")
        }
        builder.append("\n\n")

        for (scene in script.scenes) {
            var sceneName = scene.name.uppercase(Locale.getDefault())
            if (!sceneName.startsWith("INT.") && !sceneName.startsWith("EXT.")) {
                sceneName = ".$sceneName"
            }
            builder.append(sceneName).append("\n\n")
            for (line in scene.lines) {
                val actorName = line.actor.name.uppercase(Locale.getDefault())
                if (actorName != "" &&
                    actorName != Actor.ACTION_NAME) {
                    builder.append(actorName).append("\n")
                }
                builder.append(line.line).append("\n\n")
            }
        }

        return builder.toString()
    }

    @JvmStatic
    fun deserialize(script: String): Script {
        val titleTokens = parseTitlePageOfString(getScriptTitlePage(script))
        val bodyTokens = parseBodyOfString(getScriptBody(script))

        /*
        // for debugging only
        println("##### all tokens for Script #####")
        println(titleTokens)
        for(token in bodyTokens) {
            println("[isCentered=${token.isCentered}, " +
                    "sceneNumber=${token.sceneNumber}, " +
                    "isDualDialogue=${token.isDualDialogue}, " +
                    "sectionDepth=${token.sectionDepth}]\t" +
                    "<${token.elementType}>${token.elementText}</${token.elementType}>")
        }
        */

        return Script(titleTokens, bodyTokens)
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

        if (patterns.count() != templates.count()) {
            throw Exception("The pattern and template arrays don't have the same number of objects!")
        }

        for(i in patterns.indices) {
            scriptContent = patterns[i].toRegex(RegexOption.MULTILINE).replace(scriptContent, templates[i])
        }

        // 3rd pass - Array construction
        var i = 0
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

            // More refined processing of elements based on text/type
            val centeredTextMatch = CENTERED_TEXT_PATTERN.toRegex().find(element.elementText)
            if (centeredTextMatch != null) {
                element.isCentered = true
                val match = "(>?)\\s*([^<>\\n]*)\\s*(<?)".toRegex().find(element.elementText)
                if (match != null) {
                    element.elementText = match.groupValues[2].trim()
                }
            }

            if (element.elementType == "Scene Heading") {
                // Check for a forced scene heading. Remove preceding dot.
                val forcedSceneHeadingMatch = "^\\.?(.+)".toRegex().find(element.elementText)
                if (forcedSceneHeadingMatch != null) {
                    element.elementText = forcedSceneHeadingMatch.groupValues[1]
                }
            }

            if (element.elementType == "Section Heading") {
                // Clean the section text, and get the section depth
                val sectionHeaderMatch = SECTION_HEADER_PATTERN.toRegex().find(element.elementText)
                if (sectionHeaderMatch != null) {
                    val depthChars = sectionHeaderMatch.groupValues[2]
                    val depth = depthChars.length
                    element.sectionDepth = depth
                    element.elementText = sectionHeaderMatch.groupValues[3].trim()
                }
            }

            if (i > 1 && element.elementType == "Character" && DUAL_DIALOGUE_PATTERN.toRegex().containsMatchIn(element.elementText)) {
                element.isDualDialogue = true

                // clean the ^ mark
                element.elementText = element.elementText.replace("\\s*\\^$".toRegex(), "")

                var j = i - 1

                var previousElement: FNElement?
                val dialogueBlockTypes = setOf("Dialogue", "Parenthetical")
                do {
                    previousElement = elementsArray[j]
                    if (previousElement.elementType == "Character") {
                        previousElement.isDualDialogue = true
                        previousElement.elementText = previousElement.elementText.replace("^", "")
                    }
                    j--
                } while (j >= 0 && dialogueBlockTypes.contains(previousElement!!.elementType))
            }

            elementsArray.add(element)
            i++
        }

        return elementsArray.toTypedArray()
    }

    private fun parseTitlePageOfString(scriptTitle: String) : Map<String, List<String>> {
        val contents = mutableMapOf<String, List<String>>()

        // Line by line parsing
        // split the title page using newlines, then walk through the array and determine what is what
        // this allows us to look for very specific things and better handle non-uniform title pages

        // split the string
        val splitTitlePage = scriptTitle.split("\n")
        var openDirective: String? = null
        val directiveData = mutableListOf<String>()
        for (line in splitTitlePage) {
            // is this an inline directive or a multi-line one?
            val inlineResult = INLINE_DIRECTIVE_PATTERN.toRegex().find(line)
            if (inlineResult != null) {
                if (openDirective != null && directiveData.count() > 0) {
                    contents[openDirective] = directiveData.toList()
                    directiveData.clear()
                }
                openDirective = null

                var key = inlineResult.groupValues[1].lowercase(Locale.getDefault())
                val value = inlineResult.groupValues[2]

                // validation
                if (key == "author" || key == "author(s)") {
                    key = "authors"
                }

                contents[key] = listOf(value)
                continue
            }

            val multiLineDirectiveResult = MULTI_LINE_DIRECTIVE_PATTERN.toRegex().find(line)
            if (multiLineDirectiveResult != null) {
                if (openDirective != null && directiveData.count() > 0) {
                    contents[openDirective] = directiveData.toList()
                }

                openDirective = multiLineDirectiveResult.groupValues[1].lowercase(Locale.getDefault())
                directiveData.clear()

                if (openDirective == "author" || openDirective == "author(s)") {
                    openDirective = "authors"
                }

                continue
            }

            val multiLineDataResult = MULTI_LINE_DATA_PATTERN.toRegex().find(line)
            if (multiLineDataResult != null) {
                directiveData += multiLineDataResult.groupValues[2]
            }
        }

        if (openDirective != null && directiveData.count() > 0) {
            contents[openDirective] = directiveData.toList()
        }

        return contents.toMap()
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