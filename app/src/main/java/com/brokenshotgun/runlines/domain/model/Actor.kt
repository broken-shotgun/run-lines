package com.brokenshotgun.runlines.domain.model

import com.google.gson.annotations.SerializedName

data class Actor(
    @field:SerializedName("name") val name: String
) {
    override fun toString(): String = name

    companion object {
        const val ACTION_NAME = "ACTION"
        val ACTION = Actor(ACTION_NAME)
    }
}
