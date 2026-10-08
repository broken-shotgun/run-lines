package com.brokenshotgun.runlines.ui.reader

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.brokenshotgun.runlines.domain.model.Actor
import com.brokenshotgun.runlines.domain.model.Line
import com.brokenshotgun.runlines.domain.model.Scene
import com.brokenshotgun.runlines.domain.model.Script
import com.brokenshotgun.runlines.ui.components.LineEditRow
import com.brokenshotgun.runlines.ui.reader.playback.ReadSceneTTSListener
import com.brokenshotgun.runlines.ui.reader.playback.TtsPlaybackController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadSceneScreen(
    script: Script,
    sceneIndex: Int,
    onBack: () -> Unit,
    onSaveScript: (Script) -> Unit = {}
) {
    var scriptState by remember(script.id, script.name, script.scenes.size) {
        mutableStateOf(script)
    }
    val context = LocalContext.current
    val safeSceneIndex = sceneIndex.coerceIn(0, scriptState.scenes.lastIndex.coerceAtLeast(0))
    var selectedSceneIndex by remember(scriptState.scenes.size, safeSceneIndex) {
        mutableIntStateOf(safeSceneIndex)
    }
    val currentScene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: scriptState.scenes.firstOrNull() ?: return

    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    val activeScriptId by rememberUpdatedState(scriptState.id)
    val readerActive = remember(script.id) { mutableStateOf(true) }
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var stopAtSceneEnd by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showSceneSelector by remember { mutableStateOf(false) }
    var sceneSearchQuery by remember { mutableStateOf("") }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var voiceDialogCharacters by remember { mutableStateOf<List<VoiceAssignment>>(emptyList()) }
    var voiceDialogOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var unsavedChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var editingLineIndex by remember { mutableStateOf<Int?>(null) }
    var editingActorName by remember { mutableStateOf("") }
    var editingLineText by remember { mutableStateOf("") }
    var originalEditingActorName by remember { mutableStateOf("") }
    var originalEditingLineText by remember { mutableStateOf("") }
    val maxHistoryActions = 500
    var sceneUndoStack by remember(selectedSceneIndex) { mutableStateOf<List<List<Line>>>(emptyList()) }
    var sceneRedoStack by remember(selectedSceneIndex) { mutableStateOf<List<List<Line>>>(emptyList()) }

    fun pushUndoState(snapshot: List<Line>) {
        sceneUndoStack = (sceneUndoStack + listOf(snapshot)).takeLast(maxHistoryActions)
        if (sceneUndoStack.size > maxHistoryActions) {
            sceneUndoStack = sceneUndoStack.takeLast(maxHistoryActions)
        }
    }

    fun snapshotSceneLines(scene: Scene): List<Line> {
        return scene.lines.map { it.copy(actor = it.actor, line = it.line, order = it.order, characterExtensions = it.characterExtensions.toMutableList()) }
    }

    fun applySceneLines(scene: Scene, lines: List<Line>) {
        scene.lines.clear()
        scene.lines.addAll(lines.map { it.copy(actor = it.actor, line = it.line, order = it.order, characterExtensions = it.characterExtensions.toMutableList()) })
    }

    fun resetSceneEditHistory() {
        sceneUndoStack = emptyList()
        sceneRedoStack = emptyList()
    }

    fun cloneScriptState(): Script {
        return Script(
            name = scriptState.name,
            credit = scriptState.credit,
            author = scriptState.author,
            source = scriptState.source,
            draftDate = scriptState.draftDate,
            contact = scriptState.contact,
            actors = scriptState.actors.map { it.copy() }.toMutableList(),
            scenes = scriptState.scenes.map { scene ->
                scene.copy(
                    name = scene.name,
                    number = scene.number,
                    lines = scene.lines.map { line ->
                        line.copy(
                            actor = line.actor.copy(),
                            line = line.line,
                            order = line.order,
                            characterExtensions = line.characterExtensions.toMutableList()
                        )
                    }.toMutableList()
                )
            }.toMutableList(),
            allVoices = scriptState.allVoices.toMutableList(),
            actorVoices = scriptState.actorVoices.toMutableMap(),
            id = scriptState.id
        ).apply {
            defaultVoice = scriptState.defaultVoice
        }
    }

    fun refreshScriptState() {
        scriptState = cloneScriptState()
    }

    fun isEditingDirty(): Boolean {
        return editingLineIndex != null && (editingActorName != originalEditingActorName || editingLineText != originalEditingLineText)
    }

    fun exitEditMode() {
        editingLineIndex = null
        editingActorName = ""
        editingLineText = ""
        originalEditingActorName = ""
        originalEditingLineText = ""
        unsavedChanges = false
    }

    fun saveAndLeave() {
        onSaveScript(scriptState)
        showExitDialog = false
        exitEditMode()
        onBack()
    }

    fun requestBack() {
        if (editingLineIndex != null && isEditingDirty()) {
            showExitDialog = true
        } else {
            if (editingLineIndex != null) {
                exitEditMode()
            }
            onBack()
        }
    }

    fun stopPlayback() {
        textToSpeech?.stop()
        isPlaying = false
        currentLineIndex = -1
        TtsPlaybackController.setPlayingState(
            context = context,
            title = scriptState.name.ifBlank { "Untitled script" },
            sceneName = currentScene.name ?: "Untitled scene",
            scriptId = scriptState.id,
            sceneIndex = selectedSceneIndex,
            playing = false,
            keepNotificationVisible = false
        )
    }

    fun selectScene(index: Int) {
        if (isPlaying) {
            stopPlayback()
        }
        selectedSceneIndex = index
        currentLineIndex = -1
        editingLineIndex = null
        resetSceneEditHistory()
    }

    fun sceneLabel(index: Int): String {
        val scene = scriptState.scenes[index]
        return "Scene ${index + 1}${scene.name?.let { " • $it" } ?: ""}"
    }

    fun wordCount(text: String): Int {
        if (text.isBlank()) return 0
        var count = 0
        val parts = text.split(Regex("\\s+"))
        for (part in parts) {
            if (part.isNotBlank()) {
                count += 1
            }
        }
        return count
    }

    fun estimatedSceneProgress(): Pair<Int, Int> {
        val scene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return 0 to 0
        var spokenWords = 0
        var remainingWords = 0
        val startIndex = currentLineIndex.coerceAtLeast(0)
        for (index in scene.lines.indices) {
            val lineWords = wordCount(scene.lines[index].line)
            if (index <= startIndex) {
                spokenWords += lineWords
            } else {
                remainingWords += lineWords
            }
        }
        val totalWords = (spokenWords + remainingWords).coerceAtLeast(1)
        return totalWords to spokenWords.coerceAtLeast(0)
    }

    fun estimatedRemainingTimeLabel(): String {
        val scene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return ""
        if (!isPlaying) return ""
        var remainingWords = 0
        val startIndex = currentLineIndex.coerceAtLeast(0)
        for (index in scene.lines.indices) {
            if (index > startIndex) {
                remainingWords += wordCount(scene.lines[index].line)
            }
        }
        val wordsPerSecond = 2.5
        val secondsLeft = (remainingWords / wordsPerSecond).toLong().coerceAtLeast(0)
        val minutes = secondsLeft / 60
        val secs = secondsLeft % 60
        return if (minutes > 0) "${minutes}m ${secs}s left" else "${secs}s left"
    }

    fun pausePlayback() {
        textToSpeech?.stop()
        isPlaying = false
        TtsPlaybackController.setPlayingState(
            context = context,
            title = scriptState.name.ifBlank { "Untitled script" },
            sceneName = currentScene.name ?: "Untitled scene",
            scriptId = scriptState.id,
            sceneIndex = selectedSceneIndex,
            playing = false,
            keepNotificationVisible = true
        )
    }

    fun renameActorAcrossScript(currentActor: Actor, newName: String) {
        if (newName.isBlank()) return
        val replacement = Actor(newName)
        if (currentActor == Actor.ACTION) return

        scriptState.replaceActor(currentActor, replacement)
        val actorIndex = scriptState.actors.indexOf(currentActor)
        if (actorIndex >= 0) {
            scriptState.actors[actorIndex] = replacement
        }
        refreshScriptState()
    }

    fun beginEditingLine(lineIndex: Int) {
        val scene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return
        if (lineIndex !in scene.lines.indices) return
        val line = scene.lines[lineIndex]
        editingLineIndex = lineIndex
        editingActorName = line.actor.name.uppercase()
        editingLineText = line.line
        originalEditingActorName = line.actor.name.uppercase()
        originalEditingLineText = line.line
        unsavedChanges = false
    }

    fun saveEditingLineChanges(lineIndex: Int, actorName: String, lineText: String) {
        val scene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return
        if (lineIndex !in scene.lines.indices) return

        val updatedScript = cloneScriptState()
        val updatedScene = updatedScript.scenes.getOrNull(selectedSceneIndex) ?: return
        val currentLine = updatedScene.lines.getOrNull(lineIndex) ?: return
        val beforeSnapshot = snapshotSceneLines(updatedScene)
        val normalizedActorName = actorName.trim().uppercase()

        if (normalizedActorName.isNotBlank()) {
            val existingActor = updatedScript.actors.firstOrNull { it.name.equals(normalizedActorName, ignoreCase = true) }
            val targetActor = existingActor ?: Actor(normalizedActorName).also { updatedScript.actors.add(it) }

            if (currentLine.actor == Actor.ACTION) {
                currentLine.actor = targetActor
            } else if (!currentLine.actor.name.equals(targetActor.name, ignoreCase = true)) {
                updatedScript.replaceActor(currentLine.actor, targetActor)
                currentLine.actor = targetActor
            } else {
                currentLine.actor = targetActor
            }
        }
        if (currentLine.line != lineText) {
            currentLine.line = lineText
        }

        val afterSnapshot = snapshotSceneLines(updatedScene)
        if (beforeSnapshot != afterSnapshot) {
            pushUndoState(beforeSnapshot)
            sceneRedoStack = emptyList()
        }

        scriptState = updatedScript
        onSaveScript(scriptState)
        exitEditMode()
    }

    fun undoSceneEdit() {
        val previous = sceneUndoStack.lastOrNull() ?: return
        val updatedScript = cloneScriptState()
        val updatedScene = updatedScript.scenes.getOrNull(selectedSceneIndex) ?: return

        sceneRedoStack = (sceneRedoStack + listOf(snapshotSceneLines(updatedScene))).takeLast(maxHistoryActions)
        sceneUndoStack = sceneUndoStack.dropLast(1)

        val newLines = previous.map { it.copy(actor = it.actor, line = it.line, order = it.order, characterExtensions = it.characterExtensions.toMutableList()) }
        updatedScene.lines.clear()
        updatedScene.lines.addAll(newLines)

        scriptState = updatedScript
        onSaveScript(scriptState)
    }

    fun redoSceneEdit() {
        val next = sceneRedoStack.lastOrNull() ?: return
        val updatedScript = cloneScriptState()
        val updatedScene = updatedScript.scenes.getOrNull(selectedSceneIndex) ?: return

        sceneUndoStack = (sceneUndoStack + listOf(snapshotSceneLines(updatedScene))).takeLast(maxHistoryActions)
        sceneRedoStack = sceneRedoStack.dropLast(1)

        val newLines = next.map { it.copy(actor = it.actor, line = it.line, order = it.order, characterExtensions = it.characterExtensions.toMutableList()) }
        updatedScene.lines.clear()
        updatedScene.lines.addAll(newLines)

        scriptState = updatedScript
        onSaveScript(scriptState)
    }

    fun deleteLineAndSave(lineIndex: Int) {
        val scene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return
        if (lineIndex !in scene.lines.indices) return

        val updatedScript = cloneScriptState()
        val updatedScene = updatedScript.scenes.getOrNull(selectedSceneIndex) ?: return
        val beforeSnapshot = snapshotSceneLines(updatedScene)
        pushUndoState(beforeSnapshot)
        sceneRedoStack = emptyList()
        updatedScene.lines.removeAt(lineIndex)

        scriptState = updatedScript
        onSaveScript(scriptState)
        exitEditMode()
    }

    fun addNewLineToBottom() {
        val updatedScript = cloneScriptState()
        val updatedScene = updatedScript.scenes.getOrNull(selectedSceneIndex) ?: return
        val newLine = Line(Actor.ACTION, "")
        updatedScene.lines.add(newLine)
        val newIndex = updatedScene.lines.lastIndex
        scriptState = updatedScript
        beginEditingLine(newIndex)
        onSaveScript(scriptState)
    }

    fun getAllTtsVoices(): List<android.speech.tts.Voice> {
        return try {
            textToSpeech?.voices?.toList() ?: emptyList()
        } catch (_: IllegalStateException) {
            emptyList()
        }
    }

    fun getPlayableTtsVoices(): List<String> {
        val allVoices = getAllTtsVoices()
        return allVoices
            .asSequence()
            .filter { voice ->
                val locale = voice.locale
                !voice.name.isNullOrBlank() &&
                    locale != null &&
                    !voice.isNetworkConnectionRequired
            }
            .mapNotNull { it.name }
            .distinct()
            .sortedWith(compareBy<String> { voiceName ->
                val voiceLocale = allVoices.firstOrNull { it.name == voiceName }?.locale
                val localeKey = voiceLocale?.language ?: ""
                if (localeKey == "en") 0 else 1
            }.thenBy { it.lowercase() })
            .toList()
    }

    fun getPreferredDefaultTtsVoice(): String? {
        val playableVoices = getPlayableTtsVoices()
        val allVoices = getAllTtsVoices()
        val firstEnUsVoice = playableVoices.firstOrNull { voiceName ->
            val locale = allVoices.firstOrNull { it.name == voiceName }?.locale
            locale != null && locale.language == "en" && locale.country == "US"
        }
        val resolvedDefault = scriptState.defaultVoice?.takeIf { it.isNotBlank() && it != "Default" }
            ?: firstEnUsVoice
            ?: playableVoices.firstOrNull()
        if (!resolvedDefault.isNullOrBlank()) {
            scriptState.defaultVoice = resolvedDefault
        }
        return resolvedDefault
    }

    fun resolveVoiceChoiceForLine(line: Line): String? {
        val actorName = line.actor.name
        val configuredVoice = scriptState.actorVoices[actorName.uppercase()]
            ?: scriptState.actorVoices[actorName]
            ?: scriptState.getVoice(actorName)
        return when {
            configuredVoice.isNullOrBlank() || configuredVoice.equals("Default", ignoreCase = true) -> getPreferredDefaultTtsVoice()
            else -> configuredVoice
        }
    }

    fun openCharacterVoiceDialog() {
        val playableVoices = getPlayableTtsVoices()
        val fallbackVoices = scriptState.allVoices
            .filter { it.isNotBlank() && it != "Default" }
            .filter { playableVoices.contains(it) || it == scriptState.defaultVoice }
        val allVoices = getAllTtsVoices()
        val availableVoices = (playableVoices + fallbackVoices + listOfNotNull(scriptState.defaultVoice))
            .distinct()
            .filter { it.isNotBlank() && it != "Default" }
            .sortedWith(compareBy<String> { voiceName ->
                val voiceLocale = allVoices.firstOrNull { it.name == voiceName }?.locale
                val localeKey = voiceLocale?.language ?: ""
                if (localeKey == "en") 0 else 1
            }.thenBy { it.lowercase() })
        voiceDialogOptions = listOf("Default") + availableVoices
        voiceDialogCharacters = scriptState.actors
            .filter { it != Actor.ACTION }
            .map { actor ->
                val configuredVoice = scriptState.actorVoices[actor.name.uppercase()]
                    ?: scriptState.actorVoices[actor.name]
                    ?: scriptState.getVoice(actor.name)
                VoiceAssignment(
                    name = actor.name.uppercase(),
                    selectedVoice = when {
                        configuredVoice.isNullOrBlank() -> "Default"
                        configuredVoice.equals("Default", ignoreCase = true) -> "Default"
                        else -> configuredVoice
                    },
                    enabled = true
                )
            }
        showVoiceDialog = true
    }

    fun saveCharacterVoiceDialog() {
        val updatedScript = cloneScriptState()
        val currentNames = updatedScript.actors.filter { it != Actor.ACTION }.map { it.name.uppercase() }.toSet()

        val incomingNames = voiceDialogCharacters
            .filter { it.enabled && it.name.isNotBlank() }
            .map { it.name.uppercase() }
            .toSet()

        val removedNames = currentNames - incomingNames
        removedNames.forEach { removedName ->
            val actorToRemove = updatedScript.actors.firstOrNull { it.name.uppercase() == removedName } ?: return@forEach
            updatedScript.actors.remove(actorToRemove)
            updatedScript.actorVoices.remove(removedName)
            updatedScript.scenes.forEach { scene ->
                scene.lines.forEach { line ->
                    if (line.actor.name.uppercase() == removedName) {
                        line.actor = Actor.ACTION
                    }
                }
            }
        }

        voiceDialogCharacters.filter { it.enabled && it.name.isNotBlank() }.forEach { assignment ->
            val normalizedName = assignment.name.uppercase()
            val actor = updatedScript.actors.firstOrNull { it.name.uppercase() == normalizedName }
                ?: Actor(normalizedName).also { updatedScript.actors.add(it) }
            val resolvedSelection = assignment.selectedVoice.trim()
            if (resolvedSelection.isBlank() || resolvedSelection.equals("Default", ignoreCase = true)) {
                updatedScript.actorVoices.remove(normalizedName)
            } else {
                updatedScript.actorVoices[normalizedName] = resolvedSelection
            }
        }

        val firstEnUsVoice = textToSpeech?.voices
            ?.firstOrNull { voice ->
                val locale = voice.locale
                locale != null && locale.language == "en" && locale.country == "US"
            }
            ?.name
        if (!firstEnUsVoice.isNullOrBlank()) {
            updatedScript.defaultVoice = firstEnUsVoice
        }

        scriptState = updatedScript
        onSaveScript(scriptState)
        showVoiceDialog = false
    }

    fun cancelCharacterVoiceDialog() {
        showVoiceDialog = false
    }

    fun cancelEditingLine() {
        if (isEditingDirty()) {
            showExitDialog = true
        } else {
            exitEditMode()
        }
    }

    DisposableEffect(script.id) {
        onDispose {
            readerActive.value = false
            textToSpeech?.stop()
            textToSpeech = null
            TtsPlaybackController.clearScript(context, script.id)
        }
    }

    BackHandler {
        requestBack()
    }

    fun speakLineAt(targetSceneIndex: Int, targetLineIndex: Int) {
        val validSceneIndex = targetSceneIndex.coerceIn(0, scriptState.scenes.lastIndex.coerceAtLeast(0))
        val targetScene = scriptState.scenes.getOrNull(validSceneIndex) ?: return

        if (targetLineIndex < targetScene.lines.size) {
            selectedSceneIndex = validSceneIndex
            currentLineIndex = targetLineIndex
            val line = targetScene.lines[targetLineIndex]
            val selectedVoiceName = resolveVoiceChoiceForLine(line)
            if (!selectedVoiceName.isNullOrBlank()) {
                val matchingVoice = textToSpeech?.voices?.firstOrNull { it.name == selectedVoiceName }
                if (matchingVoice != null) {
                    textToSpeech?.voice = matchingVoice
                }
            }
            textToSpeech?.speak(line.line, TextToSpeech.QUEUE_FLUSH, null, "scene_${validSceneIndex}_line_$targetLineIndex")
            return
        }

        if (stopAtSceneEnd) {
            stopPlayback()
            return
        }

        val nextSceneIndex = validSceneIndex + 1
        if (nextSceneIndex < scriptState.scenes.size) {
            speakLineAt(nextSceneIndex, 0)
        } else {
            stopPlayback()
        }
    }

    fun togglePlayback() {
        if (isPlaying) {
            pausePlayback()
        } else {
            isPlaying = true
            val startIndex = if (currentLineIndex == -1) 0 else currentLineIndex
            TtsPlaybackController.setPlayingState(
                context = context,
                title = scriptState.name.ifBlank { "Untitled script" },
                sceneName = currentScene.name ?: "Untitled scene",
                scriptId = scriptState.id,
                sceneIndex = selectedSceneIndex,
                playing = true,
                progressMaxWords = currentScene.lines.sumOf { wordCount(it.line) },
                progressWords = currentScene.lines.take(currentLineIndex.coerceAtLeast(0) + 1).sumOf { wordCount(it.line) },
                remainingTimeLabel = estimatedRemainingTimeLabel()
            )
            speakLineAt(selectedSceneIndex, startIndex)
        }
    }

    LaunchedEffect(script) {
        scriptState = script
    }

    LaunchedEffect(selectedSceneIndex, currentLineIndex, isPlaying, currentScene.name) {
        val (totalWords, spokenWords) = estimatedSceneProgress()
        TtsPlaybackController.setPlayingState(
            context = context,
            title = scriptState.name.ifBlank { "Untitled script" },
            sceneName = currentScene.name ?: "Untitled scene",
            scriptId = scriptState.id,
            sceneIndex = selectedSceneIndex,
            playing = isPlaying,
            keepNotificationVisible = !isPlaying && currentLineIndex >= 0,
            progressMaxWords = totalWords,
            progressWords = spokenWords,
            remainingTimeLabel = estimatedRemainingTimeLabel()
        )
    }

    LaunchedEffect(selectedSceneIndex, scriptState.id, isPlaying) {
        TtsPlaybackController.bindPlayback(
            title = scriptState.name.ifBlank { "Untitled script" },
            sceneName = currentScene.name ?: "Untitled scene",
            scriptId = scriptState.id,
            sceneIndex = selectedSceneIndex,
            playing = isPlaying,
            onToggle = { togglePlayback() },
            onStop = { stopPlayback() }
        )
    }

    DisposableEffect(script.id) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS && readerActive.value) {
                Log.d("ReadSceneScreen", "TTS Initialized for script ${script.id}")
            }
        }
        textToSpeech = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            if (textToSpeech === engine) {
                textToSpeech = null
            }
        }
    }

    LaunchedEffect(textToSpeech, script.id) {
        val playableVoices = getPlayableTtsVoices()
        val filteredLegacyVoices = scriptState.allVoices
            .filter { it.isNotBlank() && it != "Default" }
            .filter { playableVoices.contains(it) || it == scriptState.defaultVoice }
        val allVoices = getAllTtsVoices()
        voiceDialogOptions = listOf("Default") + (playableVoices + filteredLegacyVoices + listOfNotNull(scriptState.defaultVoice))
            .distinct()
            .filter { it.isNotBlank() && it != "Default" }
            .sortedWith(compareBy<String> { voiceName ->
                val voiceLocale = allVoices.firstOrNull { it.name == voiceName }?.locale
                val localeKey = voiceLocale?.language ?: ""
                if (localeKey == "en") 0 else 1
            }.thenBy { it.lowercase() })
        textToSpeech?.setOnUtteranceProgressListener(ReadSceneTTSListener {
            if (readerActive.value && activeScriptId == script.id && isPlaying) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (!readerActive.value || activeScriptId != script.id || !isPlaying) {
                        return@post
                    }
                    val activeScene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return@post
                    if (currentLineIndex + 1 < activeScene.lines.size) {
                        speakLineAt(selectedSceneIndex, currentLineIndex + 1)
                    } else if (!stopAtSceneEnd) {
                        val nextSceneIndex = selectedSceneIndex + 1
                        if (nextSceneIndex < scriptState.scenes.size) {
                            speakLineAt(nextSceneIndex, 0)
                        } else {
                            stopPlayback()
                        }
                    } else {
                        stopPlayback()
                    }
                }
            }
        })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = scriptState.name.ifBlank { "Untitled script" }) },
                navigationIcon = {
                    IconButton(onClick = { requestBack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val canUndo = sceneUndoStack.isNotEmpty()
                    val canRedo = sceneRedoStack.isNotEmpty()

                    IconButton(
                        onClick = { undoSceneEdit() },
                        enabled = canUndo
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo edit",
                            tint = if (canUndo) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(
                        onClick = { redoSceneEdit() },
                        enabled = canRedo
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo edit",
                            tint = if (canRedo) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Scene options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Stop at scene end") },
                            trailingIcon = {
                                Checkbox(
                                    checked = stopAtSceneEnd,
                                    onCheckedChange = null
                                )
                            },
                            onClick = {
                                stopAtSceneEnd = !stopAtSceneEnd
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Character voices") },
                            onClick = {
                                menuExpanded = false
                                openCharacterVoiceDialog()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = { addNewLineToBottom() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add line"
                    )
                }

                FloatingActionButton(
                    onClick = { togglePlayback() }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause"
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedButton(
                    onClick = {
                        sceneSearchQuery = ""
                        showSceneSelector = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = sceneLabel(selectedSceneIndex),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Choose scene"
                    )
                }
            }

            item {
                Text(
                    text = currentScene.name?.uppercase() ?: "UNTITLED SCENE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            itemsIndexed(currentScene.lines) { index, line ->
                LineEditRow(
                    line = line,
                    isSelected = index == currentLineIndex,
                    isEditing = editingLineIndex == index,
                    editingActorName = if (editingLineIndex == index) editingActorName else line.actor.name.uppercase(),
                    editingLineText = if (editingLineIndex == index) editingLineText else line.line,
                    characterSuggestions = scriptState.actors
                        .map { it.name }
                        .filter { it != Actor.ACTION_NAME }
                        .distinct()
                        .sorted(),
                    onClick = {
                        if (isPlaying) {
                            speakLineAt(selectedSceneIndex, index)
                        } else {
                            currentLineIndex = index
                        }
                    },
                    onLongClick = {
                        beginEditingLine(index)
                    },
                    onActorValueChange = {
                        editingActorName = it.uppercase()
                    },
                    onLineValueChange = {
                        editingLineText = it
                    },
                    onSave = {
                        saveEditingLineChanges(index, editingActorName, editingLineText)
                        currentLineIndex = index
                    },
                    onCancel = {
                        cancelEditingLine()
                    },
                    onDelete = {
                        deleteLineAndSave(index)
                    }
                )
            }
        }
    }

    if (showSceneSelector) {
        val normalizedQuery = sceneSearchQuery.trim()
        val matchingScenes = scriptState.scenes.indices.filter { index ->
            val scene = scriptState.scenes[index]
            val searchableText = "Scene ${index + 1} ${scene.number} ${scene.name.orEmpty()}"
            searchableText.contains(normalizedQuery, ignoreCase = true)
        }
        val sceneListState = rememberLazyListState()
        val selectedScenePosition = matchingScenes.indexOf(selectedSceneIndex)

        LaunchedEffect(matchingScenes, selectedSceneIndex) {
            if (selectedScenePosition >= 0) {
                sceneListState.scrollToItem(selectedScenePosition)
            }
        }

        AlertDialog(
            onDismissRequest = { showSceneSelector = false },
            title = { Text("Select scene") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = sceneSearchQuery,
                        onValueChange = { sceneSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search scenes") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        }
                    )
                    if (matchingScenes.isEmpty()) {
                        Text(
                            text = "No scenes found",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            state = sceneListState,
                            modifier = Modifier.heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(matchingScenes) { _, index ->
                                val selected = index == selectedSceneIndex
                                TextButton(
                                    onClick = {
                                        selectScene(index)
                                        showSceneSelector = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = sceneLabel(index),
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1
                                    )
                                    if (selected) {
                                        Text(
                                            text = "Selected",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSceneSelector = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Unsaved changes") },
            text = { Text("You have edits that haven't been saved. Save them before leaving or discard them.") },
            confirmButton = {
                TextButton(onClick = {
                    if (editingLineIndex != null) {
                        saveEditingLineChanges(editingLineIndex!!, editingActorName, editingLineText)
                    }
                    showExitDialog = false
                    onBack()
                }) {
                    Text("Save & leave")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showExitDialog = false
                        exitEditMode()
                        onBack()
                    }) {
                        Text("Discard")
                    }
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Keep editing")
                    }
                }
            }
        )
    }

    if (showVoiceDialog) {
        Dialog(
            onDismissRequest = { cancelCharacterVoiceDialog() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Character voices",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val voiceOptions = voiceDialogOptions.ifEmpty { listOf("Default") }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        itemsIndexed(voiceDialogCharacters) { _, assignment ->
                            CharacterVoiceEditorRow(
                                assignment = assignment,
                                voiceOptions = voiceOptions,
                                onNameChange = { newName ->
                                    val updated = voiceDialogCharacters.map {
                                        if (it === assignment) it.copy(name = newName.uppercase()) else it
                                    }
                                    voiceDialogCharacters = updated
                                },
                                onVoiceChange = { newVoice ->
                                    val updated = voiceDialogCharacters.map {
                                        if (it === assignment) it.copy(selectedVoice = newVoice) else it
                                    }
                                    voiceDialogCharacters = updated
                                },
                                onRemove = {
                                    voiceDialogCharacters = voiceDialogCharacters.filterNot { it === assignment }
                                }
                            )
                        }
                        item {
                            Button(
                                onClick = {
                                    voiceDialogCharacters = voiceDialogCharacters + VoiceAssignment(
                                        name = "",
                                        selectedVoice = voiceOptions.firstOrNull() ?: "",
                                        enabled = true
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add character")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { cancelCharacterVoiceDialog() }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { saveCharacterVoiceDialog() }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

private data class VoiceAssignment(
    var name: String,
    var selectedVoice: String,
    var enabled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterVoiceEditorRow(
    assignment: VoiceAssignment,
    voiceOptions: List<String>,
    onNameChange: (String) -> Unit,
    onVoiceChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = assignment.name,
            onValueChange = { onNameChange(it.uppercase()) },
            label = { Text("Character") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = assignment.selectedVoice,
                onValueChange = { onVoiceChange(it) },
                label = { Text("Voice") },
                singleLine = true,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (voiceOptions.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Default") },
                        onClick = {
                            onVoiceChange("Default")
                            expanded = false
                        }
                    )
                } else {
                    voiceOptions.forEach { voice ->
                        DropdownMenuItem(
                            text = { Text(voice) },
                            onClick = {
                                onVoiceChange(voice)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove character",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
