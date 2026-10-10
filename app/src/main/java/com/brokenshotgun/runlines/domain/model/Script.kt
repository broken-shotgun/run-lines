package com.brokenshotgun.runlines.domain.model

import com.google.gson.annotations.SerializedName
import kotlin.jvm.JvmOverloads

data class Script @JvmOverloads constructor(
    @field:SerializedName("name") var name: String,
    @field:SerializedName("credit") var credit: String? = null,
    @field:SerializedName("author") var author: String? = null,
    @field:SerializedName("source") var source: String? = null,
    @field:SerializedName("draftDate") var draftDate: String? = null,
    @field:SerializedName("contact") var contact: String? = null,
    @field:SerializedName("actors") val actors: MutableList<Actor> = mutableListOf(),
    @field:SerializedName("scenes") val scenes: MutableList<Scene> = mutableListOf(),
    @field:SerializedName("allVoices") val allVoices: MutableList<String> = mutableListOf(),
    @field:SerializedName("actorVoices") val actorVoices: MutableMap<String, String> = mutableMapOf(),
    @field:SerializedName("id") var id: Long = -1L,
    @field:SerializedName("mutedCharacterNames")
    private var persistedMutedCharacterNames: MutableSet<String>? = mutableSetOf()
) {
    @field:SerializedName("defaultVoice")
    var defaultVoice: String? = null
    @Transient
    private var pendingSceneActorReplacements: MutableMap<String, String>? = null

    val sceneActorReplacements: MutableMap<String, String>
        get() = pendingSceneActorReplacements
            ?: mutableMapOf<String, String>().also { pendingSceneActorReplacements = it }

    var mutedCharacterNames: MutableSet<String>
        get() = persistedMutedCharacterNames
            ?: mutableSetOf<String>().also { persistedMutedCharacterNames = it }
        set(value) {
            persistedMutedCharacterNames = value
        }

    companion object {
        fun create(name: String): Script {
            return Script(name).apply {
                actors.add(Actor.ACTION)
            }
        }
    }

    fun addActor(actor: Actor) {
        actors.add(actor)
    }

    fun replaceActor(actor: Actor, replacement: Actor) {
        if (actor == Actor.ACTION) return
        actors.remove(actor)
        sceneActorReplacements.entries.forEach { entry ->
            if (entry.value.equals(actor.name, ignoreCase = true)) {
                entry.setValue(replacement.name)
            }
        }
        sceneActorReplacements[actor.name.uppercase()] = replacement.name
        for (scene in scenes) {
            scene.replaceActor(actor, replacement)
        }
    }

    fun hasActor(currentActor: Actor): Boolean = actors.contains(currentActor)

    fun addScene(newScene: Scene) {
        newScene.number = scenes.size
        scenes.add(newScene)
    }

    fun assignVoice(actor: String, voice: String) {
        actorVoices[actor] = voice
    }

    fun getVoice(actor: String): String? {
        return actorVoices[actor] ?: defaultVoice
    }

    fun addVoice(voice: String) {
        allVoices.add(voice)
    }

    fun getScene(index: Int): Scene {
        return scenes[index]
    }

    override fun toString(): String = "Script{name='$name', credit='$credit', author='$author', source='$source', draftDate='$draftDate', contact='$contact', actors=$actors, scenes=$scenes, allVoices=$allVoices, actorVoices=$actorVoices, defaultVoice='$defaultVoice', id=$id}"

}
