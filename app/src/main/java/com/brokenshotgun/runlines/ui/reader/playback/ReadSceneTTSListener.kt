package com.brokenshotgun.runlines.ui.reader.playback

import android.speech.tts.UtteranceProgressListener
import android.util.Log

class ReadSceneTTSListener(
    private val onLineDone: () -> Unit
) : UtteranceProgressListener() {
    override fun onStart(utteranceId: String?) {
        Log.d("ReadSceneTTSListener", "Started speaking: $utteranceId")
    }

    override fun onDone(utteranceId: String?) {
        Log.d("ReadSceneTTSListener", "Done speaking: $utteranceId")
        onLineDone()
    }

    override fun onError(utteranceId: String?) {
        Log.e("ReadSceneTTSListener", "TTS Error: $utteranceId")
    }
}
