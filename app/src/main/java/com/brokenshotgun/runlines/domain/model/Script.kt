package com.brokenshotgun.runlines.domain.model

import com.google.gson.annotations.SerializedName
import kotlin.jvm.JvmOverloads

data class Script @JvmOverloads constructor(
    var name: String,
    var credit: String? = null,
    var author: String? = null,
    var source: String? = null,
    var draftDate: String? = null,
    var contact: String? = null,
    val actors: MutableList<Actor> = mutableListOf(),
    val scenes: MutableList<Scene> = mutableListOf(),
    val allVoices: MutableList<String> = mutableListOf(),
    val actorVoices: MutableMap<String, String> = mutableMapOf(),
    var id: Long = -1L,
    @SerializedName("mutedCharacterNames")
    private var persistedMutedCharacterNames: MutableSet<String>? = mutableSetOf()
) {
    var defaultVoice: String? = null

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
