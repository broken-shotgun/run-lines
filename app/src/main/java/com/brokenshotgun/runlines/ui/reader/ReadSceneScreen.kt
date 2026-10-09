package com.brokenshotgun.runlines.ui.reader

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import java.util.Locale
import com.brokenshotgun.runlines.ui.reader.playback.TtsPlaybackController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadSceneScreen(
    script: Script,
    sceneIndex: Int,
    onBack: () -> Unit,
    onSaveScript: (Script) -> Unit = {},
    onInsertScene: (Script, Int) -> Unit = { updatedScript, _ -> onSaveScript(updatedScript) },
    onDeleteScene: (Script, Int) -> Unit = { _, _ -> },
    onLoadScene: suspend (Int) -> Scene?
) {
    var scriptState by remember(script.id, script.name, script.scenes.size) {
        mutableStateOf(script)
    }
    val context = LocalContext.current
    val playbackScope = rememberCoroutineScope()
    val safeSceneIndex = sceneIndex.coerceIn(0, scriptState.scenes.lastIndex.coerceAtLeast(0))
    var selectedSceneIndex by remember(script.id) {
        mutableIntStateOf(safeSceneIndex)
    }
    val currentScene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: scriptState.scenes.firstOrNull() ?: Scene()

    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var activeUtteranceId by remember { mutableStateOf<String?>(null) }
    var playbackUtteranceSequence by remember { mutableLongStateOf(0L) }
    val activeScriptId by rememberUpdatedState(scriptState.id)
    val readerActive = remember(script.id) { mutableStateOf(true) }
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var mutedLineDelayJob by remember { mutableStateOf<Job?>(null) }
    var stopAtSceneEnd by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteSceneDialog by remember { mutableStateOf(false) }
    var showSceneSelector by remember { mutableStateOf(false) }
    var showAddSceneDialog by remember { mutableStateOf(false) }
    var sceneHeadingPrefix by remember { mutableStateOf("INT.") }
    var sceneHeadingLocation by remember { mutableStateOf("") }
    var insertSceneBeforeCurrent by remember { mutableStateOf(false) }
    var sceneHeadingPrefixExpanded by remember { mutableStateOf(false) }
    var sceneSearchQuery by remember { mutableStateOf("") }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showCharacterLinesDialog by remember { mutableStateOf(false) }
    var voiceDialogCharacters by remember { mutableStateOf<List<VoiceAssignment>>(emptyList()) }
    var voiceDialogOptions by remember { mutableStateOf<List<VoiceOption>>(emptyList()) }
    var voiceDialogError by remember { mutableStateOf<String?>(null) }
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
    var loadingSceneIndices by remember(script.id) { mutableStateOf<Set<Int>>(emptySet()) }
    var sceneLoadError by remember(script.id) { mutableStateOf<String?>(null) }

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
                    }.toMutableList(),
                    isLoaded = scene.isLoaded
                )
            }.toMutableList(),
            allVoices = scriptState.allVoices.toMutableList(),
            actorVoices = scriptState.actorVoices.toMutableMap(),
            id = scriptState.id
        ).apply {
            defaultVoice = scriptState.defaultVoice
            mutedCharacterNames = scriptState.mutedCharacterNames.toMutableSet()
            sceneActorReplacements.putAll(scriptState.sceneActorReplacements)
        }
    }

    fun refreshScriptState() {
        scriptState = cloneScriptState()
    }

    fun loadScene(index: Int, onLoaded: (() -> Unit)? = null) {
        val scene = scriptState.scenes.getOrNull(index) ?: return
        if (scene.isLoaded) {
            onLoaded?.invoke()
            return
        }
        if (index in loadingSceneIndices) return

        playbackScope.launch {
            loadingSceneIndices = loadingSceneIndices + index
            sceneLoadError = null
            try {
                val loadedScene = onLoadScene(index)
                    ?: error("Scene ${index + 1} could not be found")
                val scenes = scriptState.scenes.toMutableList()
                scenes[index] = loadedScene
                scriptState = scriptState.copy(scenes = scenes)
                onLoaded?.invoke()
            } catch (error: Exception) {
                sceneLoadError = error.message ?: "Could not load scene"
            } finally {
                loadingSceneIndices = loadingSceneIndices - index
            }
        }
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
        mutedLineDelayJob?.cancel()
        mutedLineDelayJob = null
        activeUtteranceId = null
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

    LaunchedEffect(script.id, selectedSceneIndex) {
        if (scriptState.scenes.isNotEmpty()) {
            loadScene(selectedSceneIndex)
        }
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
        mutedLineDelayJob?.cancel()
        mutedLineDelayJob = null
        activeUtteranceId = null
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

    fun addNewScene() {
        val location = sceneHeadingLocation.trim()
        if (location.isBlank()) return

        val updatedScript = cloneScriptState()
        val insertionIndex = if (updatedScript.scenes.isEmpty()) {
            0
        } else {
            (selectedSceneIndex + if (insertSceneBeforeCurrent) 0 else 1)
                .coerceIn(0, updatedScript.scenes.size)
        }
        val newScene = Scene(name = "$sceneHeadingPrefix $location".uppercase(Locale.ROOT))
        updatedScript.scenes.add(insertionIndex, newScene)
        updatedScript.scenes.forEachIndexed { index, scene -> scene.number = index }
        scriptState = updatedScript
        selectedSceneIndex = insertionIndex
        currentLineIndex = -1
        resetSceneEditHistory()
        sceneHeadingLocation = ""
        insertSceneBeforeCurrent = false
        showAddSceneDialog = false
        onInsertScene(updatedScript, insertionIndex)
    }

    fun deleteCurrentScene() {
        val deletedSceneIndex = selectedSceneIndex
        if (deletedSceneIndex !in scriptState.scenes.indices) return

        if (isPlaying) {
            stopPlayback()
        }
        val updatedScript = cloneScriptState()
        updatedScript.scenes.removeAt(deletedSceneIndex)
        updatedScript.scenes.forEachIndexed { index, scene -> scene.number = index }
        scriptState = updatedScript
        selectedSceneIndex = deletedSceneIndex.coerceAtMost(updatedScript.scenes.lastIndex.coerceAtLeast(0))
        currentLineIndex = -1
        exitEditMode()
        resetSceneEditHistory()
        showDeleteSceneDialog = false
        onDeleteScene(updatedScript, deletedSceneIndex)
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
            .distinctBy { it.name }
            .sortedWith(compareBy<android.speech.tts.Voice> {
                if (it.locale.language == "en") 0 else 1
            }.thenBy { it.name.lowercase() })
            .mapNotNull { it.name }
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
        voiceDialogOptions = createVoiceOptions(listOf("Default") + availableVoices, allVoices)
        val sceneCharacterNames = currentScene.lines
            .map { it.actor.name.uppercase() }
            .toSet()
        voiceDialogCharacters = (currentScene.lines.map { it.actor } + scriptState.actors)
            .filter { it != Actor.ACTION }
            .distinctBy { it.name.uppercase() }
            .sortedWith(compareByDescending<Actor> { it.name.uppercase() in sceneCharacterNames })
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
                    enabled = true,
                    isInCurrentScene = actor.name.uppercase() in sceneCharacterNames
                )
            }
        voiceDialogError = null
        showVoiceDialog = true
    }

    fun saveCharacterVoiceDialog() {
        voiceDialogError = null
        val selectableVoices = voiceDialogOptions
            .filter { it.kind == VoiceOptionKind.VOICE }
            .map { it.name }
        val playableVoiceNames = getPlayableTtsVoices().toSet()
        val availableVoices = selectableVoices.filter { it in playableVoiceNames }
        val availableTtsVoices = getAllTtsVoices()
        val englishVoices = availableVoices.filter { voiceName ->
            availableTtsVoices.firstOrNull { it.name == voiceName }?.locale?.language == Locale.ENGLISH.language
        }
        val resolvedAssignments = mutableListOf<VoiceAssignment>()
        for (assignment in voiceDialogCharacters
            .filter { it.enabled && it.name.isNotBlank() }
        ) {
            val voicePool = when (assignment.selectedVoice) {
                RANDOM_ENGLISH_VOICE -> englishVoices
                RANDOM_ALL_VOICE -> availableVoices
                else -> null
            }
            val selectedVoice = if (voicePool != null) {
                voicePool.randomOrNull()
            } else {
                assignment.selectedVoice
            }
            if (selectedVoice == null) {
                voiceDialogError = if (assignment.selectedVoice == RANDOM_ENGLISH_VOICE) {
                    "No English voices are available."
                } else {
                    "No voices are available."
                }
                return
            }
            resolvedAssignments.add(assignment.copy(selectedVoice = selectedVoice))
        }

        val updatedScript = cloneScriptState()
        val currentNames = updatedScript.actors.filter { it != Actor.ACTION }.map { it.name.uppercase() }.toSet()

        val incomingNames = resolvedAssignments
            .map { it.name.uppercase() }
            .toSet()

        val removedNames = currentNames - incomingNames
        removedNames.forEach { removedName ->
            val actorToRemove = updatedScript.actors.firstOrNull { it.name.uppercase() == removedName } ?: return@forEach
            updatedScript.replaceActor(actorToRemove, Actor.ACTION)
            updatedScript.actorVoices.remove(removedName)
            updatedScript.mutedCharacterNames.remove(removedName)
        }

        resolvedAssignments.forEach { assignment ->
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
        mutedLineDelayJob?.cancel()
        mutedLineDelayJob = null
        val validSceneIndex = targetSceneIndex.coerceIn(0, scriptState.scenes.lastIndex.coerceAtLeast(0))
        val targetScene = scriptState.scenes.getOrNull(validSceneIndex) ?: return
        if (!targetScene.isLoaded) {
            loadScene(validSceneIndex) {
                speakLineAt(validSceneIndex, targetLineIndex)
            }
            return
        }

        if (targetLineIndex < targetScene.lines.size) {
            selectedSceneIndex = validSceneIndex
            currentLineIndex = targetLineIndex
            val line = targetScene.lines[targetLineIndex]
            if (
                line.actor != Actor.ACTION &&
                scriptState.mutedCharacterNames.any { it.equals(line.actor.name, ignoreCase = true) }
            ) {
                activeUtteranceId = null
                textToSpeech?.stop()
                if (isPlaying) {
                    val estimatedDurationMillis = (wordCount(line.line) / 2.5 * 1000).toLong()
                    mutedLineDelayJob = playbackScope.launch {
                        delay(estimatedDurationMillis)
                        if (
                            readerActive.value &&
                            activeScriptId == script.id &&
                            isPlaying &&
                            selectedSceneIndex == validSceneIndex &&
                            currentLineIndex == targetLineIndex
                        ) {
                            val completedScene = scriptState.scenes.getOrNull(validSceneIndex) ?: return@launch
                            if (targetLineIndex + 1 < completedScene.lines.size) {
                                speakLineAt(validSceneIndex, targetLineIndex + 1)
                            } else if (!stopAtSceneEnd && validSceneIndex + 1 < scriptState.scenes.size) {
                                speakLineAt(validSceneIndex + 1, 0)
                            } else {
                                stopPlayback()
                            }
                        }
                    }
                }
                return
            }
            val selectedVoiceName = resolveVoiceChoiceForLine(line)
            if (!selectedVoiceName.isNullOrBlank()) {
                val matchingVoice = textToSpeech?.voices?.firstOrNull { it.name == selectedVoiceName }
                if (matchingVoice != null) {
                    textToSpeech?.voice = matchingVoice
                }
            }
            playbackUtteranceSequence += 1
            val utteranceId = "scene_${validSceneIndex}_line_${targetLineIndex}_playback_$playbackUtteranceSequence"
            activeUtteranceId = utteranceId
            textToSpeech?.speak(line.line, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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

    fun advancePlaybackFrom(completedSceneIndex: Int, completedLineIndex: Int) {
        val completedScene = scriptState.scenes.getOrNull(completedSceneIndex) ?: return
        if (completedLineIndex + 1 < completedScene.lines.size) {
            speakLineAt(completedSceneIndex, completedLineIndex + 1)
        } else if (!stopAtSceneEnd && completedSceneIndex + 1 < scriptState.scenes.size) {
            speakLineAt(completedSceneIndex + 1, 0)
        } else {
            stopPlayback()
        }
    }

    val advancePlaybackFromLatest by rememberUpdatedState(
        newValue = { completedSceneIndex: Int, completedLineIndex: Int ->
            advancePlaybackFrom(completedSceneIndex, completedLineIndex)
        }
    )

    fun saveMutedCharacters(mutedNames: Set<String>) {
        val updatedScript = cloneScriptState()
        val characterNames = updatedScript.actors
            .filter { it != Actor.ACTION }
            .map { it.name.uppercase() }
            .toSet()
        val currentLine = updatedScript.scenes
            .getOrNull(selectedSceneIndex)
            ?.lines
            ?.getOrNull(currentLineIndex)
        val wasCurrentLineMuted = currentLine != null &&
            currentLine.actor != Actor.ACTION &&
            updatedScript.mutedCharacterNames.any { it.equals(currentLine.actor.name, ignoreCase = true) }

        updatedScript.mutedCharacterNames.clear()
        updatedScript.mutedCharacterNames.addAll(
            mutedNames.map { it.uppercase() }.filter { it in characterNames }
        )
        scriptState = updatedScript
        onSaveScript(updatedScript)

        val isCurrentLineMuted = currentLine != null &&
            currentLine.actor != Actor.ACTION &&
            updatedScript.mutedCharacterNames.any { it.equals(currentLine.actor.name, ignoreCase = true) }
        if (isPlaying && currentLine != null && wasCurrentLineMuted != isCurrentLineMuted) {
            speakLineAt(selectedSceneIndex, currentLineIndex)
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
        if (scriptState.scenes.isEmpty()) return@LaunchedEffect
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

    LaunchedEffect(script.id) {
        delay(250)
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS && readerActive.value) {
                Log.d("ReadSceneScreen", "TTS Initialized for script ${script.id}")
            }
        }
        textToSpeech = engine
        try {
            awaitCancellation()
        } finally {
            engine.stop()
            engine.shutdown()
            if (textToSpeech === engine) {
                textToSpeech = null
            }
        }
    }

    LaunchedEffect(textToSpeech, script.id) {
        val engine = textToSpeech ?: return@LaunchedEffect
        val (allVoices, playableVoices, voiceLanguagePriority) = withContext(Dispatchers.IO) {
            try {
                val voices = engine.voices?.toList() ?: emptyList()
                val playable = voices
                    .asSequence()
                    .filter { voice ->
                        !voice.name.isNullOrBlank() &&
                            !voice.isNetworkConnectionRequired
                    }
                    .distinctBy { it.name }
                    .sortedWith(compareBy<android.speech.tts.Voice> {
                        if (it.locale.language == "en") 0 else 1
                    }.thenBy { it.name.lowercase() })
                    .mapNotNull { it.name }
                    .toList()
                val languagePriority = voices.associate { voice ->
                    voice.name to if (voice.locale.language == "en") 0 else 1
                }
                Triple(voices, playable, languagePriority)
            } catch (_: IllegalStateException) {
                Triple(emptyList(), emptyList(), emptyMap())
            }
        }
        val filteredLegacyVoices = scriptState.allVoices
            .filter { it.isNotBlank() && it != "Default" }
            .filter { playableVoices.contains(it) || it == scriptState.defaultVoice }
        val availableVoiceNames = (playableVoices + filteredLegacyVoices + listOfNotNull(scriptState.defaultVoice))
            .distinct()
            .filter { it.isNotBlank() && it != "Default" }
            .sortedWith(compareBy<String> { voiceName ->
                voiceLanguagePriority[voiceName] ?: 1
            }.thenBy { it.lowercase() })
        voiceDialogOptions = createVoiceOptions(listOf("Default") + availableVoiceNames, allVoices)
        engine.setOnUtteranceProgressListener(ReadSceneTTSListener { utteranceId ->
            val utteranceMatch = UTTERANCE_ID_PATTERN.matchEntire(utteranceId.orEmpty())
                ?: return@ReadSceneTTSListener
            val completedSceneIndex = utteranceMatch.groupValues[1].toIntOrNull()
                ?: return@ReadSceneTTSListener
            val completedLineIndex = utteranceMatch.groupValues[2].toIntOrNull()
                ?: return@ReadSceneTTSListener
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (
                    !readerActive.value ||
                    activeScriptId != script.id ||
                    !isPlaying ||
                    activeUtteranceId != utteranceId
                ) {
                    return@post
                }
                activeUtteranceId = null
                advancePlaybackFromLatest(completedSceneIndex, completedLineIndex)
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
                            text = { Text("Character lines") },
                            onClick = {
                                menuExpanded = false
                                showCharacterLinesDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Character voices") },
                            onClick = {
                                menuExpanded = false
                                openCharacterVoiceDialog()
                            }
                        )
                        if (scriptState.scenes.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Delete scene", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteSceneDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (scriptState.scenes.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (currentScene.isLoaded) addNewLineToBottom()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add line"
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            if (currentScene.isLoaded) togglePlayback()
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause"
                        )
                    }
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
            if (scriptState.scenes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "No scenes yet",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Add a scene to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { showAddSceneDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add scene")
                        }
                    }
                }
            } else {
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

                if (!currentScene.isLoaded) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (selectedSceneIndex in loadingSceneIndices) {
                                CircularProgressIndicator()
                            } else {
                                Text(sceneLoadError ?: "Scene not loaded")
                                TextButton(onClick = { loadScene(selectedSceneIndex) }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(currentScene.lines) { index, line ->
                        LineEditRow(
                            line = line,
                            isSelected = index == currentLineIndex,
                            isEditing = editingLineIndex == index,
                            isLineMuted = line.actor != Actor.ACTION &&
                                scriptState.mutedCharacterNames.any { it.equals(line.actor.name, ignoreCase = true) },
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

                item {
                    OutlinedButton(
                        onClick = { showAddSceneDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add scene")
                    }
                }
            }
        }
    }

    if (showDeleteSceneDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSceneDialog = false },
            title = { Text("Delete scene?") },
            text = {
                Text(
                    "Delete ${currentScene.name ?: "this scene"} and all of its lines? " +
                        "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteCurrentScene() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSceneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddSceneDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddSceneDialog = false
                sceneHeadingLocation = ""
                insertSceneBeforeCurrent = false
                sceneHeadingPrefixExpanded = false
            },
            title = { Text("Add scene") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (scriptState.scenes.isNotEmpty()) {
                        Text(
                            text = "Insert",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !insertSceneBeforeCurrent,
                                onClick = { insertSceneBeforeCurrent = false }
                            )
                            Text(
                                text = "After current scene",
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { insertSceneBeforeCurrent = false }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = insertSceneBeforeCurrent,
                                onClick = { insertSceneBeforeCurrent = true }
                            )
                            Text(
                                text = "Before current scene",
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { insertSceneBeforeCurrent = true }
                            )
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = sceneHeadingPrefixExpanded,
                        onExpandedChange = {
                            sceneHeadingPrefixExpanded = !sceneHeadingPrefixExpanded
                        }
                    ) {
                        OutlinedTextField(
                            value = sceneHeadingPrefix,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Heading prefix") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = sceneHeadingPrefixExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = sceneHeadingPrefixExpanded,
                            onDismissRequest = { sceneHeadingPrefixExpanded = false }
                        ) {
                            listOf("INT.", "EXT.", "EST.", "INT./EXT.", "INT/EXT.", "I/E.").forEach { prefix ->
                                DropdownMenuItem(
                                    text = { Text(prefix) },
                                    onClick = {
                                        sceneHeadingPrefix = prefix
                                        sceneHeadingPrefixExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = sceneHeadingLocation,
                        onValueChange = { sceneHeadingLocation = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Scene name") },
                        placeholder = { Text("Location - Time") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { addNewScene() },
                    enabled = sceneHeadingLocation.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddSceneDialog = false
                        sceneHeadingLocation = ""
                        insertSceneBeforeCurrent = false
                        sceneHeadingPrefixExpanded = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
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

                    val voiceOptions = voiceDialogOptions.ifEmpty {
                        listOf(
                            VoiceOption("Default", "Default", "Default", VoiceOptionKind.DEFAULT),
                            VoiceOption(RANDOM_ENGLISH_VOICE, "Random English", "Default", VoiceOptionKind.RANDOM_ENGLISH),
                            VoiceOption(RANDOM_ALL_VOICE, "Random All", "Default", VoiceOptionKind.RANDOM_ALL)
                        )
                    }
                    val sceneCharacters = voiceDialogCharacters.filter { it.isInCurrentScene }
                    val scriptCharacters = voiceDialogCharacters.filterNot { it.isInCurrentScene }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        if (sceneCharacters.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Characters in this scene",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            itemsIndexed(sceneCharacters) { _, assignment ->
                                CharacterVoiceEditorRow(
                                    assignment = assignment,
                                    voiceOptions = voiceOptions,
                                    onNameChange = { newName ->
                                        voiceDialogCharacters = voiceDialogCharacters.map {
                                            if (it === assignment) it.copy(name = newName.uppercase()) else it
                                        }
                                    },
                                    onVoiceChange = { newVoice ->
                                        voiceDialogError = null
                                        voiceDialogCharacters = voiceDialogCharacters.map {
                                            if (it === assignment) it.copy(selectedVoice = newVoice) else it
                                        }
                                    },
                                    onRemove = {
                                        voiceDialogCharacters = voiceDialogCharacters.filterNot { it === assignment }
                                    }
                                )
                            }
                        }
                        if (scriptCharacters.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Characters in the script",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            itemsIndexed(scriptCharacters) { _, assignment ->
                                CharacterVoiceEditorRow(
                                    assignment = assignment,
                                    voiceOptions = voiceOptions,
                                    onNameChange = { newName ->
                                        voiceDialogCharacters = voiceDialogCharacters.map {
                                            if (it === assignment) it.copy(name = newName.uppercase()) else it
                                        }
                                    },
                                    onVoiceChange = { newVoice ->
                                        voiceDialogError = null
                                        voiceDialogCharacters = voiceDialogCharacters.map {
                                            if (it === assignment) it.copy(selectedVoice = newVoice) else it
                                        }
                                    },
                                    onRemove = {
                                        voiceDialogCharacters = voiceDialogCharacters.filterNot { it === assignment }
                                    }
                                )
                            }
                        }
                        item {
                            Button(
                                onClick = {
                                    voiceDialogCharacters = voiceDialogCharacters + VoiceAssignment(
                                        name = "",
                                        selectedVoice = "Default",
                                        enabled = true,
                                        isInCurrentScene = false
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add character")
                            }
                        }
                    }

                    voiceDialogError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
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

    if (showCharacterLinesDialog) {
        val characters = scriptState.actors
            .filter { it != Actor.ACTION }
            .distinctBy { it.name.uppercase() }
            .sortedBy { it.name.uppercase() }
        val currentlyMuted = scriptState.mutedCharacterNames
            .map { it.uppercase() }
            .toSet()

        AlertDialog(
            onDismissRequest = { showCharacterLinesDialog = false },
            title = { Text("Character lines") },
            text = {
                Column {
                    Text(
                        text = "Turn lines on or off for the whole script.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { saveMutedCharacters(emptySet()) },
                            enabled = currentlyMuted.isNotEmpty()
                        ) {
                            Text("All on")
                        }
                        TextButton(
                            onClick = {
                                saveMutedCharacters(characters.map { it.name.uppercase() }.toSet())
                            },
                            enabled = characters.any { it.name.uppercase() !in currentlyMuted }
                        ) {
                            Text("All off")
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        itemsIndexed(characters, key = { _, actor -> actor.name.uppercase() }) { _, actor ->
                            val isEnabled = actor.name.uppercase() !in currentlyMuted
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = actor.name.uppercase(),
                                    modifier = Modifier.weight(1f)
                                )
                                Checkbox(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        val updatedMutedNames = currentlyMuted.toMutableSet()
                                        if (checked) {
                                            updatedMutedNames.remove(actor.name.uppercase())
                                        } else {
                                            updatedMutedNames.add(actor.name.uppercase())
                                        }
                                        saveMutedCharacters(updatedMutedNames)
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCharacterLinesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private const val RANDOM_ENGLISH_VOICE = "__random_english__"
private const val RANDOM_ALL_VOICE = "__random_all__"

private enum class VoiceOptionKind {
    DEFAULT,
    RANDOM_ENGLISH,
    RANDOM_ALL,
    VOICE
}

private data class VoiceOption(
    val name: String,
    val label: String,
    val language: String,
    val kind: VoiceOptionKind
)

private fun createVoiceOptions(
    voiceNames: List<String>,
    voices: List<android.speech.tts.Voice>
): List<VoiceOption> {
    val voicesByName = voices.associateBy { it.name }
    val voiceDetails = voiceNames
        .distinct()
        .filter {
            it.isNotBlank() &&
                it != "Default" &&
                it != RANDOM_ENGLISH_VOICE &&
                it != RANDOM_ALL_VOICE
        }
        .map { name ->
            val voice = voicesByName[name]
            val localeLabel = voice?.locale?.getDisplayName(Locale.getDefault())
                ?.takeIf { it.isNotBlank() }
                ?: "Voice"
            val language = voice?.locale?.getDisplayLanguage(Locale.getDefault())
                ?.takeIf { it.isNotBlank() }
                ?: "Other"
            val isEnglish = voice?.locale?.language == Locale.ENGLISH.language
            Triple(name, localeLabel, if (isEnglish) "English" else language)
        }
    val specialOptions = if (voiceNames.contains("Default")) {
        listOf(
            VoiceOption("Default", "Default", "Default", VoiceOptionKind.DEFAULT),
            VoiceOption(RANDOM_ENGLISH_VOICE, "Random English", "Default", VoiceOptionKind.RANDOM_ENGLISH),
            VoiceOption(RANDOM_ALL_VOICE, "Random All", "Default", VoiceOptionKind.RANDOM_ALL)
        )
    } else {
        emptyList()
    }

    return specialOptions + voiceDetails
        .groupBy { (_, localeLabel, language) -> localeLabel to language }
        .values
        .flatMap { options ->
            options.mapIndexed { index, (name, localeLabel, language) ->
                val voiceNumber = Regex("(?:_|-)(\\d+)(?:-|$)")
                    .find(name.lowercase(Locale.ROOT))
                    ?.groupValues
                    ?.get(1)
                    ?: (index + 1).takeIf { options.size > 1 }?.toString()
                val variantLabel = "Voice" + (voiceNumber?.let { " $it" } ?: "")
                VoiceOption(name, "$localeLabel · $variantLabel", language, VoiceOptionKind.VOICE)
            }
        }.sortedWith(compareBy<VoiceOption> {
            when {
                it.language == "Default" -> 0
                it.language == "English" -> 1
                else -> 2
            }
        }.thenBy { it.language.lowercase(Locale.getDefault()) }
            .thenBy { it.kind.ordinal }
            .thenBy { it.label.lowercase(Locale.getDefault()) })
}

private data class VoiceAssignment(
    var name: String,
    var selectedVoice: String,
    var enabled: Boolean = true,
    val isInCurrentScene: Boolean = false
)

private val UTTERANCE_ID_PATTERN = Regex("^scene_(\\d+)_line_(\\d+)_playback_\\d+$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterVoiceEditorRow(
    assignment: VoiceAssignment,
    voiceOptions: List<VoiceOption>,
    onNameChange: (String) -> Unit,
    onVoiceChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedVoiceBringIntoViewRequester = remember { BringIntoViewRequester() }
    val selectedVoiceOption = voiceOptions.firstOrNull { it.name == assignment.selectedVoice }
    LaunchedEffect(expanded, selectedVoiceOption?.name) {
        if (expanded && selectedVoiceOption != null) {
            withFrameNanos { }
            selectedVoiceBringIntoViewRequester.bringIntoView()
        }
    }
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
                value = selectedVoiceOption?.label ?: assignment.selectedVoice,
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
                    voiceOptions
                        .groupBy { it.language }
                        .forEach { (language, groupedOptions) ->
                            if (groupedOptions.isNotEmpty()) {
                                HorizontalDivider()
                                Text(
                                    text = language,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                groupedOptions.forEach { option ->
                                    val isSelected = option.name == assignment.selectedVoice
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        trailingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                        } else {
                                            null
                                        },
                                        modifier = if (isSelected) {
                                            Modifier
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .bringIntoViewRequester(selectedVoiceBringIntoViewRequester)
                                        } else {
                                            Modifier
                                        },
                                        onClick = {
                                            onVoiceChange(option.name)
                                            expanded = false
                                        }
                                    )
                                }
                            }
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
