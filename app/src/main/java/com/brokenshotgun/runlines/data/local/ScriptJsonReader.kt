package com.brokenshotgun.runlines.data.local

import com.brokenshotgun.runlines.domain.model.Scene
import com.brokenshotgun.runlines.domain.model.Script
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

internal class ScriptJsonReader(private val gson: Gson) {
    fun readSummary(json: String, scriptId: Long): Script {
        val reader = JsonReader(StringReader(json))
        val metadata = JsonObject()
        var firstScene: Scene? = null
        var scenesFound = false

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (name == "scenes") {
                scenesFound = true
                val scenes = JsonArray()
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                } else {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val scene = if (firstScene == null) {
                            requireNotNull(gson.fromJson<Scene>(reader, Scene::class.java)).apply {
                                isLoaded = true
                                firstScene = this
                            }
                        } else {
                            readSceneSummary(reader)
                        }
                        scenes.add(gson.toJsonTree(scene))
                    }
                    reader.endArray()
                }
                metadata.add("scenes", scenes)
            } else {
                metadata.add(name, JsonParser.parseReader(reader))
            }
        }
        reader.endObject()
        if (!scenesFound) {
            metadata.add("scenes", JsonArray())
        }

        val script = requireNotNull(gson.fromJson(metadata, Script::class.java)) {
            "Stored script data was empty"
        }
        script.id = scriptId
        script.scenes.forEachIndexed { index, scene ->
            scene.isLoaded = index == 0
        }
        firstScene?.let { script.scenes[0] = it }
        return script
    }

    fun readScene(json: String, sceneIndex: Int): Scene? {
        if (sceneIndex < 0) return null

        val reader = JsonReader(StringReader(json))
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() != "scenes") {
                reader.skipValue()
                continue
            }
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return null
            }
            reader.beginArray()
            var index = 0
            while (reader.hasNext()) {
                if (index++ == sceneIndex) {
                    return gson.fromJson<Scene>(reader, Scene::class.java)?.apply {
                        isLoaded = true
                    }
                }
                reader.skipValue()
            }
            reader.endArray()
            return null
        }
        reader.endObject()
        return null
    }

    private fun readSceneSummary(reader: JsonReader): Scene {
        var name: String? = null
        var number = 0

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "name" -> name = if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    null
                } else {
                    reader.nextString()
                }
                "number" -> number = if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    0
                } else {
                    reader.nextInt()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return Scene(name = name, number = number, isLoaded = false)
    }
}
