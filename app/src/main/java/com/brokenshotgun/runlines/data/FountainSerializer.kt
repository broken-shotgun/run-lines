package com.brokenshotgun.runlines.data

import com.brokenshotgun.runlines.model.Script

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

    const val BLOCK_COMMENT_PATTERN      = "\\n\\/\\*([^<>]+?)\\*\\/\\n"
    const val BRACKET_COMMENT_PATTERN    = "\\n\\[{2}([^<>]+?)\\]{2}\\n"
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


//------------------------------------------------------------------------------
// The following regexes aren't used by the code here, but may be useful for you

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
    fun serialize(script: Script): String {
        return ""
    }

    @JvmStatic
    fun deserialize(fountain: String): Script {
        return Script("")
    }
}