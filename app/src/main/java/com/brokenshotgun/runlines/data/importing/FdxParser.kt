package com.brokenshotgun.runlines.data.importing

import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.domain.model.Actor
import com.brokenshotgun.runlines.domain.model.Line
import com.brokenshotgun.runlines.domain.model.Scene
import org.w3c.dom.Element
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import java.io.StringReader

object FdxParser {
    fun parse(
        content: String,
        untitledScriptName: String = "Untitled script",
        untitledSceneName: String = "Untitled scene"
    ): Script {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        factory.isXIncludeAware = false
        factory.isExpandEntityReferences = false
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(content)))
        val paragraphs = mutableListOf<Pair<String, String>>()
        var title: String? = null
        val paragraphNodes = document.getElementsByTagName("Paragraph")
        for (index in 0 until paragraphNodes.length) {
            val paragraph = paragraphNodes.item(index) as? Element ?: continue
            val type = paragraph.getAttribute("Type")
            val textNodes = paragraph.getElementsByTagName("Text")
            val text = buildString {
                for (textIndex in 0 until textNodes.length) {
                    append(textNodes.item(textIndex).textContent)
                }
            }.trim()
            if (type.isNotBlank() && text.isNotEmpty()) {
                if (type.equals("Title", ignoreCase = true)) {
                    title = title ?: text
                } else {
                    paragraphs += type to if (type.equals("Character", ignoreCase = true)) {
                        "@$text"
                    } else {
                        text
                    }
                }
            }
        }

        val script = Script.create(title ?: untitledScriptName)
        var currentScene = Scene()
        var currentSceneAdded = false
        var currentLine: Line? = null
        val actors = mutableMapOf<String, Actor>()

        fun ensureCurrentScene() {
            if (!currentSceneAdded) {
                currentScene.number = script.scenes.size
                script.scenes.add(currentScene)
                currentSceneAdded = true
            }
        }

        paragraphs.forEach { (type, text) ->
            when (type.lowercase()) {
                "scene heading" -> {
                    currentScene = Scene(name = text)
                    currentScene.number = script.scenes.size
                    script.scenes.add(currentScene)
                    currentSceneAdded = true
                    currentLine = null
                }
                "character" -> {
                    ensureCurrentScene()
                    val actor = actors.getOrPut(text) {
                        Actor(text).also(script::addActor)
                    }
                    currentLine = Line(actor, "")
                    currentScene.addLine(currentLine!!)
                }
                "dialogue", "parenthetical" -> {
                    ensureCurrentScene()
                    val line = currentLine
                    if (line == null) {
                        currentScene.addAction(text)
                    } else {
                        line.addDialogue(text)
                    }
                }
                else -> {
                    ensureCurrentScene()
                    currentScene.addAction(text)
                    currentLine = null
                }
            }
        }
        if (script.scenes.isEmpty()) {
            script.scenes.add(Scene(name = untitledSceneName, number = 0))
        }
        return script
    }
}
