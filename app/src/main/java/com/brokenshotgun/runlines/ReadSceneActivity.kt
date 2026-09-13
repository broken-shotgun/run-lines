/*
 * Copyright 2016 Jason Petterson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.brokenshotgun.runlines

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.brokenshotgun.runlines.adapters.LineArrayAdapter
import com.brokenshotgun.runlines.model.Actor
import com.brokenshotgun.runlines.model.Line
import com.brokenshotgun.runlines.model.Script
import com.brokenshotgun.runlines.utils.Intents
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.Locale

class ReadSceneActivity : AppCompatActivity() {
    private var textToSpeech: TextToSpeech? = null
    private var utteranceProgressListener: UtteranceProgressListener? = null
    private var script: Script? = null
    private var sceneIndex = -1
    private var lastUtteranceId = ""
    private var lastReadLineIndex = -1
    private var lineArrayAdapter: LineArrayAdapter? = null
    private val isEnabled: MutableList<Boolean> = ArrayList()
    private var actionsEnabled = false
    private var autoPauseEnabled = false
    private var autoPauseActorName: String? = null
    private val supportedVoiceMap: MutableMap<String, Voice> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_scene)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val result = Intent()
                    result.putExtra("script", script)
                    setResult(RESULT_OK, result)
                    finish()
                }
            }
        )
        checkTtsData()
        val extras = intent.extras!!
        script = extras["script"] as Script?
        sceneIndex = extras["sceneIndex"] as Int
        assert(script != null)
        title =
            getString(R.string.title_activity_read_script_prefix) + " \"" + (if (script!!.getScene(
                    sceneIndex
                ).name == ""
            ) getString(R.string.label_no_scene_name) else script!!.getScene(sceneIndex).name) + "\""
        val sharedPreferences = getPreferences(MODE_PRIVATE)
        actionsEnabled = sharedPreferences.getBoolean("actionsEnabled", true)
        val scriptPreferences = getSharedPreferences(script!!.name, MODE_PRIVATE)
        autoPauseEnabled = scriptPreferences.getBoolean("autoPauseEnabled", false)
        autoPauseActorName = scriptPreferences.getString("autoPauseActorName", null)
        val disabledActorSet = scriptPreferences.getStringSet("disabledActors", HashSet())
        refreshEnabledActors(disabledActorSet!!)

        val linesListView = findViewById<ListView>(R.id.lines_list)
        val playButton = findViewById<FloatingActionButton>(R.id.play)
        val editGroupButton = findViewById<FloatingActionButton>(R.id.edit_group)

        lineArrayAdapter = LineArrayAdapter(this, ArrayList())
        linesListView.adapter = lineArrayAdapter
        linesListView.isNestedScrollingEnabled = true
        linesListView.emptyView = findViewById(android.R.id.empty)
        linesListView.onItemClickListener = OnItemClickListener { _, _, i, _ ->
            playButton!!.setImageResource(R.drawable.ic_pause_white_48dp)
            skipToLine(i)
        }
        toggleActions(actionsEnabled)
        refreshLines()
        editGroupButton.setOnClickListener { showToggleLinesDialog() }
        playButton.setOnClickListener(View.OnClickListener { view ->
            if (sceneIndex < 0 || sceneIndex >= script!!.scenes.size) {
                Snackbar.make(view, R.string.alert_invalid_scene_index, Snackbar.LENGTH_LONG)
                    .show()
                return@OnClickListener
            }
            if (script!!.getScene(sceneIndex).lines.isEmpty()) {
                Snackbar.make(view, R.string.alert_no_lines, Snackbar.LENGTH_LONG)
                    .show()
                return@OnClickListener
            }
            if (textToSpeech == null) {
                Snackbar.make(view, R.string.alert_no_tts, Snackbar.LENGTH_LONG)
                    .show()
                return@OnClickListener
            }
            if (textToSpeech!!.isSpeaking) {
                playButton.setImageResource(R.drawable.ic_play_arrow_white_48dp)
                textToSpeech!!.stop()
            } else {
                playButton.setImageResource(R.drawable.ic_pause_white_48dp)
                playLine(lastReadLineIndex)
            }
        })
        val prevButton = findViewById<FloatingActionButton>(R.id.prev)
        val nextButton = findViewById<FloatingActionButton>(R.id.next)
        val stopButton = findViewById<FloatingActionButton>(R.id.stop)
        prevButton.setOnClickListener { skipToLine(lastReadLineIndex - 1) }
        nextButton.setOnClickListener { skipToLine(lastReadLineIndex + 1) }
        stopButton.setOnClickListener {
            playButton.setImageResource(R.drawable.ic_play_arrow_white_48dp)
            if (textToSpeech != null) textToSpeech!!.stop()
            resetReadProgress()
        }
        utteranceProgressListener = object : UtteranceProgressListener() {
            var autoPauseTriggered = false
            override fun onStart(utteranceId: String) {
                Log.d(ReadSceneActivity::class.java.name, "onStart() utterance id = $utteranceId")
                val indexStr = utteranceId.split("-").toTypedArray()[0]
                val index = indexStr.toInt()
                val isPause = utteranceId.endsWith("-$")
                lastReadLineIndex = index
                lastUtteranceId = utteranceId
                if (isPause) return
                runOnUiThread {
                    val currentLine = lineArrayAdapter!!.getItem(index)
                    autoPauseTriggered = if (autoPauseEnabled && currentLine != null &&
                        currentLine.actor.name.equals(autoPauseActorName, ignoreCase = true) &&
                        !autoPauseTriggered && textToSpeech != null &&
                        textToSpeech!!.isSpeaking
                    ) {
                        playButton.setImageResource(R.drawable.ic_play_arrow_white_48dp)
                        textToSpeech!!.stop()
                        true
                    } else {
                        playButton.setImageResource(R.drawable.ic_pause_white_48dp)
                        false
                    }
                    lineArrayAdapter!!.setSelectedItem(index)
                    lineArrayAdapter!!.notifyDataSetChanged()
                    linesListView.setSelection(index)
                }
            }

            override fun onDone(utteranceId: String) {
                if (utteranceId == lastUtteranceId) {
                    resetReadProgress()
                    autoPauseTriggered = false
                    runOnUiThread { playButton.setImageResource(R.drawable.ic_play_arrow_white_48dp) }
                }
            }

            override fun onError(utteranceId: String) {}
        }
    }

    private fun refreshEnabledActors(disabledActorSet: Set<String>) {
        if (!script!!.actors.isEmpty()) {
            isEnabled.clear()
            for (i in script!!.actors.indices) {
                isEnabled.add(!disabledActorSet.contains(script!!.actors[i].name))
            }
        }
    }

    private fun checkTtsData() {
        try {
            val checkIntent = Intent()
            checkIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
            Intents.maybeStartActivityForResult(this, checkIntent, TTS_DATA_CHECK_CODE)
        } catch (e: ActivityNotFoundException) {
            Log.e(
                ReadSceneActivity::class.java.name,
                "Oops! The function is not available in your device." + e.fillInStackTrace()
            )
            showTtsNotAvailableDialog()
        }
    }

    private fun showTtsNotAvailableDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.title_dialog_tts_not_available)
        builder.setMessage(R.string.message_dialog_tts_not_available)
        builder.create().show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_read_script, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val toggleActions = menu.findItem(R.id.toggle_actions)
        toggleActions.isChecked = actionsEnabled
        val toggleAutoPause = menu.findItem(R.id.toggle_auto_pause)
        toggleAutoPause.isChecked = autoPauseEnabled
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.tts_settings -> {
                showTtsSettingsDialog()
                true
            }
            R.id.toggle_settings -> {
                showToggleLinesDialog()
                true
            }
            R.id.edit_settings -> {
                onEditScriptButtonClicked(null)
                true
            }
            R.id.toggle_actions -> {
                actionsEnabled = !item.isChecked
                item.isChecked = actionsEnabled
                toggleActions(actionsEnabled)
                val sharedPreferences = getPreferences(MODE_PRIVATE)
                sharedPreferences.edit().putBoolean("actionsEnabled", actionsEnabled).apply()
                true
            }
            R.id.toggle_auto_pause -> {
                autoPauseEnabled = !item.isChecked
                item.isChecked = autoPauseEnabled
                val scriptPreferences = getSharedPreferences(script!!.name, MODE_PRIVATE)
                scriptPreferences.edit().putBoolean("autoPauseEnabled", autoPauseEnabled).apply()
                if (autoPauseEnabled) showAutoPauseActorSelectDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    public override fun onPause() {
        if (textToSpeech != null) {
            textToSpeech!!.stop()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (textToSpeech != null) {
            textToSpeech!!.shutdown()
            textToSpeech = null
        }
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EDIT_SCRIPT_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (script == null) {
                    finish()
                    return
                }
                val editScript: Script? = data!!.getParcelableExtra("script")
                if (editScript != null) {
                    script!!.copy(editScript)
                }
                val scriptPreferences = getSharedPreferences(script!!.name, MODE_PRIVATE)
                val disabledActorSet = scriptPreferences!!.getStringSet("disabledActors", HashSet())
                refreshEnabledActors(disabledActorSet!!)
                lineArrayAdapter!!.clear()
                lineArrayAdapter!!.addAll(script!!.getScene(sceneIndex).lines)
                lineArrayAdapter!!.notifyDataSetInvalidated()
            }
        }
        if (requestCode == TTS_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                textToSpeech = TextToSpeech(this) { status ->
                    if (status != TextToSpeech.ERROR && textToSpeech!!.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_AVAILABLE) {
                        textToSpeech!!.language = Locale.getDefault()
                    }
                    if (status == TextToSpeech.SUCCESS) {
                        Log.d(TAG, "[Voice] Voices Available: ")
                        val voices = textToSpeech!!.voices
                        if (voices != null) {
                            script!!.defaultVoice = null
                            for (v in voices) {
                                if (v.locale == Locale.getDefault()) {
                                    Log.d(TAG, "[Voice]\tVoice " + v.name)
                                    if (script!!.defaultVoice == null) {
                                        script!!.defaultVoice = v.name
                                    }
                                    supportedVoiceMap[v.name] = v
                                    script!!.addVoice(v.name)
                                }
                            }
                        }
                    }
                }
                val sharedPreferences = getPreferences(MODE_PRIVATE)
                val rate = sharedPreferences!!.getInt(
                    "speechRate",
                    TTS_MAX_SPEECH_RATE_PROGRESS / 2
                ) / TTS_MAX_SPEECH_RATE_PROGRESS.toFloat() * TTS_MAX_SPEECH_RATE
                textToSpeech!!.setSpeechRate(rate)
                textToSpeech!!.setOnUtteranceProgressListener(utteranceProgressListener)
            } else {
                try {
                    val installIntent = Intent()
                    installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                    Intents.maybeStartActivity(this, installIntent)
                } catch (ex: ActivityNotFoundException) {
                    Log.e(ReadSceneActivity::class.java.name, ex.message, ex)
//                    Snackbar.make(
//                        linesListView!!,
//                        R.string.alert_tts_not_available,
//                        Snackbar.LENGTH_INDEFINITE
//                    ).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun onEditScriptButtonClicked(view: View?) {
        val editIntent = Intent(this, EditSceneActivity::class.java)
        editIntent.putExtra("script", script)
        editIntent.putExtra("sceneIndex", sceneIndex)
        startActivityForResult(editIntent, EDIT_SCRIPT_REQUEST)
    }

    private fun skipToLine(lineIndex: Int) {
        lastReadLineIndex = lineIndex - 1
        lastUtteranceId = ""
        if (textToSpeech != null) {
            textToSpeech!!.stop()
        }
        playLine(lineIndex)
    }

    private fun playLine(startLineIndex: Int) {
        var i: Int
        i =
            if (startLineIndex < 0) 0 else if (startLineIndex >= lineArrayAdapter!!.count) lineArrayAdapter!!.count - 1 else startLineIndex
        if (i < 0 || lineArrayAdapter!!.isEmpty) {
            Log.d(
                TAG,
                "attempting to play invalid index = " + i + ", line array count = " + lineArrayAdapter!!.count
            )
            return
        }
        while (i < lineArrayAdapter!!.count) {
            val line = lineArrayAdapter!!.getItem(i)
            if (line != null) {
                if (line.enabled) {
                    val voiceKey = script!!.getVoice(line.actor.name)
                    if (supportedVoiceMap.containsKey(voiceKey)) {
                        textToSpeech!!.voice = supportedVoiceMap[voiceKey]
                    }
                    addToSpeechQueue(i, line.line)
                } else {
                    addToSpeechQueue(i, line.line, true)
                }
            }
            ++i
        }
    }

    private fun refreshLines() {
        resetReadProgress()
        val disabledActorSet: MutableSet<String> = HashSet()
        for (i in 0 until lineArrayAdapter!!.count) {
            val line = lineArrayAdapter!!.getItem(i)
            if (line != null) {
                val actorIndex = script!!.actors.indexOf(line.actor)
                if (actorIndex >= 0 && actorIndex < isEnabled.size) {
                    line.enabled = isEnabled[actorIndex]
                }
                if (!line.enabled) {
                    disabledActorSet.add(line.actor.name)
                }
            }
        }
        val scriptPreferences = getSharedPreferences(script!!.name, MODE_PRIVATE)
        scriptPreferences.edit().putStringSet("disabledActors", disabledActorSet).apply()
        lineArrayAdapter!!.notifyDataSetChanged()
    }

    private fun resetReadProgress() {
        lastReadLineIndex = -1
        lastUtteranceId = ""
    }

    private fun addToSpeechQueue(index: Int, line: String, isSilent: Boolean = false) {
        val utteranceId = index.toString()
        if (isSilent) {
            val silenceLengthInMilliseconds = getSilenceLength(line)
            silence(silenceLengthInMilliseconds, "$utteranceId-s")
            return
        }
        val maxLength = TextToSpeech.getMaxSpeechInputLength()
        var i = 0
        while (i < line.length) {
            val end = Math.min(i + maxLength, line.length)
            val utterance = line.substring(i, end)
            speak(utterance, if (i > 0) "$utteranceId-$i" else utteranceId)
            i += maxLength
        }
        silence(500L, "$utteranceId-$")
    }

    private fun speak(utterance: String, utteranceId: String) {
        if (textToSpeech == null) {
//            Snackbar.make(linesListView!!, R.string.alert_tts_not_ready, Snackbar.LENGTH_LONG)
//                .show()
            return
        }
        textToSpeech!!.speak(utterance, TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    private fun silence(duration: Long, utteranceId: String) {
        if (textToSpeech == null) {
//            Snackbar.make(linesListView!!, R.string.alert_tts_not_ready, Snackbar.LENGTH_LONG)
//                .show()
            return
        }
        textToSpeech!!.playSilentUtterance(duration, TextToSpeech.QUEUE_ADD, utteranceId)
    }

    private fun getSilenceLength(utterance: String): Long {
        if (utterance.trim { it <= ' ' } == "") return 0L
        val words = utterance.split(" ").toTypedArray().size
        return words * TTS_RATE
    }

    private fun toggleActions(actionsEnabled: Boolean) {
        if (actionsEnabled) {
            lastReadLineIndex = -1
            lineArrayAdapter!!.clear()
            lineArrayAdapter!!.addAll(script!!.getScene(sceneIndex).lines)
        } else {
            lastReadLineIndex = -1
            lineArrayAdapter!!.clear()
            val lines: MutableList<Line> = ArrayList(
                script!!.getScene(sceneIndex).lines
            )
            val actions: MutableList<Line> = ArrayList()
            for (line in lines) {
                if (line.actor == Actor.ACTION) {
                    actions.add(line)
                }
            }
            lines.removeAll(actions)
            lineArrayAdapter!!.addAll(lines)
        }
    }

    private fun showToggleLinesDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.title_dialog_edit_group)
        val actorNames = arrayOfNulls<String>(script!!.actors.size)
        actorNames[0] = getString(R.string.action_actor_name)
        for (i in 1 until script!!.actors.size) {
            actorNames[i] = script!!.actors[i].name
        }
        val isEnabledArray = BooleanArray(isEnabled.size)
        for (i in isEnabled.indices) {
            isEnabledArray[i] = isEnabled[i]
        }
        builder.setMultiChoiceItems(actorNames, isEnabledArray) { dialog, which, isChecked ->
            if (which >= 0 && which < isEnabled.size) {
                isEnabled[which] = isChecked
            }
            refreshLines()
        }
        builder.create().show()
    }

    private fun showTtsSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.title_dialog_edit_script)
        val inputLayout = LinearLayout(this)
        inputLayout.orientation = LinearLayout.VERTICAL
        val labelParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        labelParams.setMargins(50, 50, 50, 25)
        val speechRateValueLabel = TextView(this)
        val sharedPreferences = getPreferences(MODE_PRIVATE)

        speechRateValueLabel.text = String.format(
            Locale.US,
            getString(R.string.speech_rate),
            sharedPreferences.getInt(
                "speechRate",
                TTS_MAX_SPEECH_RATE_PROGRESS / 2
            ) / TTS_MAX_SPEECH_RATE_PROGRESS.toFloat() * TTS_MAX_SPEECH_RATE
        )
        speechRateValueLabel.gravity = Gravity.CENTER_HORIZONTAL
        inputLayout.addView(speechRateValueLabel, labelParams)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 0, 50, 25)
        val speechRateSeekBar = SeekBar(this)
        speechRateSeekBar.max = TTS_MAX_SPEECH_RATE_PROGRESS
        speechRateSeekBar.progress =
            sharedPreferences.getInt("speechRate", TTS_MAX_SPEECH_RATE_PROGRESS / 2)
        speechRateSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val rate = progress / TTS_MAX_SPEECH_RATE_PROGRESS.toFloat() * TTS_MAX_SPEECH_RATE
                if (textToSpeech != null) {
                    textToSpeech!!.setSpeechRate(rate)
                }
                speechRateValueLabel.text =
                    String.format(Locale.US, getString(R.string.speech_rate), rate)
                sharedPreferences!!.edit().putInt("speechRate", progress).apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                Log.d(
                    ReadSceneActivity::class.java.name,
                    "Speech rate change to: " + seekBar.progress / TTS_MAX_SPEECH_RATE_PROGRESS.toFloat() * 2f
                )
            }
        })
        inputLayout.addView(speechRateSeekBar, params)
        builder.setView(inputLayout)
        builder.create().show()
    }

    private fun showAutoPauseActorSelectDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.title_dialog_auto_pause_actor_select)
        builder.setCancelable(false)
        val actorNames = arrayOfNulls<String>(script!!.actors.size)
        actorNames[0] = getString(R.string.action_actor_name)
        for (i in 1 until script!!.actors.size) {
            actorNames[i] = script!!.actors[i].name
        }
        autoPauseActorName = actorNames[0]
        builder.setSingleChoiceItems(actorNames, 0) { _, i ->
            autoPauseActorName = actorNames[i]
            val scriptPreferences = getSharedPreferences(script!!.name, MODE_PRIVATE)
            scriptPreferences.edit().putString("autoPauseActorName", autoPauseActorName).apply()
        }
        builder.setPositiveButton(getString(R.string.save)) { dialogInterface, i -> dialogInterface.dismiss() }
        builder.create().show()
    }

    companion object {
        private const val TAG = "ReadSceneActivity"
        private const val TTS_MAX_SPEECH_RATE_PROGRESS = 10
        private const val TTS_MAX_SPEECH_RATE = 2f
        private const val EDIT_SCRIPT_REQUEST = 1
        private const val TTS_DATA_CHECK_CODE = 2
        private const val TTS_RATE = 800L //666L;
    }
}