package com.brokenshotgun.runlines.domain.model

import com.google.gson.annotations.SerializedName
import java.util.regex.Pattern
import kotlin.jvm.JvmOverloads

data class Line @JvmOverloads constructor(
    @field:SerializedName("actor") var actor: Actor,
    @field:SerializedName("line") var line: String,
    @field:SerializedName("order") var order: Int = 0,
    @field:SerializedName("characterExtensions")
    val characterExtensions: MutableList<String> = mutableListOf()
) {
    @field:SerializedName("enabled")
    var enabled: Boolean = true
        private set

    @field:SerializedName("lineHtml")
    private var lineHtml: String? = null

    companion object {
        private val UNDERSCORE_PATTERN = Pattern.compile("_([^_]+)_")
        private val ITALICIZE_PATTERN = Pattern.compile("\\*([^*]+)\\*")
        private val BOLD_PATTERN = Pattern.compile("\\*\\*([^*{2}]+)\\*\\*")

    }

    fun addDialogue(newLine: String) {
        if (line.isEmpty()) {
            this.line = newLine
        } else {
            this.line += "\n" + newLine
        }
    }

    fun getLineHtml(): String {
        if (lineHtml == null) {
            var current = line

            val boldBuffer = StringBuffer()
            val boldMatcher = BOLD_PATTERN.matcher(current)
            while (boldMatcher.find()) {
                boldMatcher.appendReplacement(boldBuffer, "<b>${boldMatcher.group(1)}</b>")
            }
            boldMatcher.appendTail(boldBuffer)
            current = boldBuffer.toString()

            val italicsBuffer = StringBuffer()
            val italicsMatcher = ITALICIZE_PATTERN.matcher(current)
            while (italicsMatcher.find()) {
                italicsMatcher.appendReplacement(italicsBuffer, "<i>${italicsMatcher.group(1)}</i>")
            }
            italicsMatcher.appendTail(italicsBuffer)
            current = italicsBuffer.toString()

            val underlineBuffer = StringBuffer()
            val underlineMatcher = UNDERSCORE_PATTERN.matcher(current)
            while (underlineMatcher.find()) {
                underlineMatcher.appendReplacement(underlineBuffer, "<u>${underlineMatcher.group(1)}</u>")
            }
            underlineMatcher.appendTail(underlineBuffer)
            current = underlineBuffer.toString()

            lineHtml = current.replace("\n", "<br>")
        }
        return lineHtml!!
    }

    override fun toString(): String = "Line{line='$line', characterExtensions=$characterExtensions}"

}
