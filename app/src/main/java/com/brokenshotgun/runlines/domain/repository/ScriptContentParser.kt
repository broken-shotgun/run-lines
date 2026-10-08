package com.brokenshotgun.runlines.domain.repository

import com.brokenshotgun.runlines.domain.model.Script

fun interface ScriptContentParser {
    fun parse(fileName: String, content: String): Script
}
