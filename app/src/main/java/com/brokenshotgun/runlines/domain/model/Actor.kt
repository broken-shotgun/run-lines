package com.brokenshotgun.runlines.domain.model

data class Actor(val name: String) {
    override fun toString(): String = name

    companion object {
        const val ACTION_NAME = "ACTION"
        val ACTION = Actor(ACTION_NAME)
    }
}
