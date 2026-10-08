package com.brokenshotgun.runlines.domain.model

import kotlin.jvm.JvmOverloads

data class Scene @JvmOverloads constructor(
    var name: String? = null,
    var number: Int = 0,
    val lines: MutableList<Line> = mutableListOf()
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
