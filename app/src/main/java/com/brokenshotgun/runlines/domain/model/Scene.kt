package com.brokenshotgun.runlines.domain.model

import com.google.gson.annotations.SerializedName
import kotlin.jvm.JvmOverloads

data class Scene @JvmOverloads constructor(
    @field:SerializedName("name") var name: String? = null,
    @field:SerializedName("number") var number: Int = 0,
    @field:SerializedName("lines") val lines: MutableList<Line> = mutableListOf(),
    @Transient var isLoaded: Boolean = true,
    @field:SerializedName("fountainSceneNumber") var fountainSceneNumber: String? = null
) {

    fun addLine(line: Line) {
        lines.add(line)
    }

    fun addAction(action: String) {
        lines.add(Line(Actor.ACTION, action))
    }

    fun replaceActor(actor: Actor, replacement: Actor) {
        for (line in lines) {
            if (line.actor == actor) {
                line.actor = replacement
            }
        }
    }

    override fun toString(): String = "Scene{name='$name', number=$number, lines=$lines}"

}
