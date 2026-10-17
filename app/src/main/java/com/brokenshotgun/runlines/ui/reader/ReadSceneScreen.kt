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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
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
import com.brokenshotgun.runlines.R
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
    val untitledScriptLabel = stringResource(R.string.playback_untitled_script)
    val untitledSceneLabel = stringResource(R.string.scene_untitled)
    val couldNotLoadSceneLabel = stringResource(R.string.error_could_not_load_scene)
    val sceneNotFoundLabel = stringResource(R.string.error_scene_not_found)
    val sceneThisLabel = stringResource(R.string.scene_this)
    val untitledSceneHeadingLabel = stringResource(R.string.scene_untitled_heading)
    val defaultVoiceLabel = stringResource(R.string.common_default)
    val randomEnglishVoiceLabel = stringResource(R.string.voice_random_english)
    val randomAllVoiceLabel = stringResource(R.string.voice_random_all)
    val otherLanguageLabel = stringResource(R.string.common_other)
    val noEnglishVoicesLabel = stringResource(R.string.voice_no_english_available)
    val noVoicesAvailableLabel = stringResource(R.string.voice_none_available)
    val chooseVoiceReplacementLabel = stringResource(R.string.voice_choose_replacement)
    val sceneHeadingPrefixes = listOf(
        stringResource(R.string.scene_prefix_int),
        stringResource(R.string.scene_prefix_ext),
        stringResource(R.string.scene_prefix_est),
        stringResource(R.string.scene_prefix_int_ext_dotted),
        stringResource(R.string.scene_prefix_int_ext),
        stringResource(R.string.scene_prefix_ie)
    )
    val sceneSearchWord = stringResource(R.string.scene_search_word)
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
    var sceneHeadingPrefix by remember(sceneHeadingPrefixes.first()) {
        mutableStateOf(sceneHeadingPrefixes.first())
    }
    var sceneHeadingLocation by remember { mutableStateOf("") }
    var insertSceneBeforeCurrent by remember { mutableStateOf(false) }
    var sceneHeadingPrefixExpanded by remember { mutableStateOf(false) }
    var sceneSearchQuery by remember { mutableStateOf("") }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showCharacterLinesDialog by remember { mutableStateOf(false) }
    var voiceDialogCharacters by remember { mutableStateOf<List<VoiceAssignment>>(emptyList()) }
    var voiceDialogOptions by remember { mutableStateOf<List<VoiceOption>>(emptyList()) }
    var voiceDialogError by remember { mutableStateOf<String?>(null) }
    var voiceDialogRemovedCharacterReplacements by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var pendingVoiceRemoval by remember { mutableStateOf<VoiceAssignment?>(null) }
    var voiceRemovalReplacementName by remember { mutableStateOf(Actor.ACTION_NAME) }
    var voiceRemovalReplacementExpanded by remember { mutableStateOf(false) }
    var unsavedChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var editingLineIndex by remember { mutableStateOf<Int?>(null) }
    var editingActorName by remember { mutableStateOf("") }
    var editingLineText by remember { mutableStateOf("") }
    var originalEditingActorName by remember { mutableStateOf("") }
    var originalEditingLineText by remember { mutableStateOf("") }
    var draggingLineIndex by remember { mutableStateOf<Int?>(null) }
    var lineDropIndex by remember { mutableStateOf<Int?>(null) }
    var dragPointerY by remember { mutableFloatStateOf(0f) }
    val linePositions = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
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
                    ?: error(sceneNotFoundLabel)
                val scenes = scriptState.scenes.toMutableList()
                scenes[index] = loadedScene
                scriptState = scriptState.copy(scenes = scenes)
                onLoaded?.invoke()
            } catch (error: Exception) {
                sceneLoadError = error.message ?: couldNotLoadSceneLabel
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
            title = scriptState.name.ifBlank { untitledScriptLabel },
            sceneName = currentScene.name ?: untitledSceneLabel,
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

    fun estimatedRemainingTime(): Pair<Int, Int>? {
        val scene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return null
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
        return minutes.toInt() to secs.toInt()
    }
    val remainingTime = estimatedRemainingTime()
    val remainingMinutes = remainingTime?.first ?: 0
    val remainingSeconds = remainingTime?.second ?: 0
    val remainingSecondsLabel = pluralStringResource(
        R.plurals.playback_remaining_seconds,
        remainingSeconds.coerceAtLeast(0),
        remainingSeconds.coerceAtLeast(0)
    )
    val remainingTimeLabel = if (remainingTime != null) {
        if (remainingMinutes > 0) {
            val remainingMinutesLabel = pluralStringResource(
                R.plurals.playback_remaining_minutes,
                remainingMinutes,
                remainingMinutes
            )
            stringResource(
                R.string.playback_remaining_minutes_seconds,
                remainingMinutesLabel,
                remainingSecondsLabel
            )
        } else {
            stringResource(R.string.playback_remaining_seconds_left, remainingSecondsLabel)
        }
    } else {
        ""
    }

    fun pausePlayback() {
        mutedLineDelayJob?.cancel()
        mutedLineDelayJob = null
        activeUtteranceId = null
        textToSpeech?.stop()
        isPlaying = false
        TtsPlaybackController.setPlayingState(
            context = context,
            title = scriptState.name.ifBlank { untitledScriptLabel },
            sceneName = currentScene.name ?: untitledSceneLabel,
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

    fun updateLineDropIndex(pointerY: Float) {
        val visiblePositions = linePositions.toSortedMap()
        lineDropIndex = visiblePositions.entries
            .firstOrNull { pointerY < (it.value.first + it.value.second) / 2f }
            ?.key
            ?: visiblePositions.entries.lastOrNull()?.key?.plus(1)
    }

    fun finishLineReorder() {
        val sourceIndex = draggingLineIndex
        val insertionBoundary = lineDropIndex
        if (sourceIndex != null && insertionBoundary != null) {
            val destinationIndex = if (insertionBoundary > sourceIndex) {
                insertionBoundary - 1
            } else {
                insertionBoundary
            }
            val scene = scriptState.scenes.getOrNull(selectedSceneIndex)
            if (scene != null && sourceIndex in scene.lines.indices &&
                destinationIndex in scene.lines.indices && destinationIndex != sourceIndex
            ) {
                val updatedScript = cloneScriptState()
                val updatedScene = updatedScript.scenes[selectedSceneIndex]
                val beforeSnapshot = snapshotSceneLines(updatedScene)
                val movedLine = updatedScene.lines.removeAt(sourceIndex)
                updatedScene.lines.add(destinationIndex, movedLine)
                pushUndoState(beforeSnapshot)
                sceneRedoStack = emptyList()
                scriptState = updatedScript
                if (editingLineIndex == sourceIndex) editingLineIndex = destinationIndex
                currentLineIndex = when {
                    currentLineIndex == sourceIndex -> destinationIndex
                    sourceIndex < destinationIndex && currentLineIndex in (sourceIndex + 1)..destinationIndex ->
                        currentLineIndex - 1
                    destinationIndex < sourceIndex && currentLineIndex in destinationIndex until sourceIndex ->
                        currentLineIndex + 1
                    else -> currentLineIndex
                }
                onSaveScript(updatedScript)
            }
        }
        draggingLineIndex = null
        lineDropIndex = null
        linePositions.clear()
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
            }.thenBy { it.locale.country.uppercase(Locale.ROOT) }
                .thenComparator { first, second ->
                    compareVoiceNamesNaturally(first.name, second.name)
                })
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
        val resolvedDefault = scriptState.defaultVoice?.takeIf {
            it.isNotBlank() && it != DEFAULT_VOICE_NAME
        }
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
            configuredVoice.isNullOrBlank() ||
                configuredVoice.equals(DEFAULT_VOICE_NAME, ignoreCase = true) -> getPreferredDefaultTtsVoice()
            else -> configuredVoice
        }
    }

    fun openCharacterVoiceDialog() {
        val playableVoices = getPlayableTtsVoices()
        val fallbackVoices = scriptState.allVoices
            .filter { it.isNotBlank() && it != DEFAULT_VOICE_NAME }
            .filter { playableVoices.contains(it) || it == scriptState.defaultVoice }
        val allVoices = getAllTtsVoices()
        val availableVoices = (playableVoices + fallbackVoices + listOfNotNull(scriptState.defaultVoice))
            .distinct()
            .filter { it.isNotBlank() && it != DEFAULT_VOICE_NAME }
            .sortedWith { first, second ->
                val firstVoice = allVoices.firstOrNull { it.name == first }
                val secondVoice = allVoices.firstOrNull { it.name == second }
                val languageComparison = compareValues(
                    if (firstVoice?.locale?.language == "en") 0 else 1,
                    if (secondVoice?.locale?.language == "en") 0 else 1
                )
                if (languageComparison != 0) {
                    languageComparison
                } else {
                    val countryComparison = compareValues(
                        firstVoice?.locale?.country.orEmpty().uppercase(Locale.ROOT),
                        secondVoice?.locale?.country.orEmpty().uppercase(Locale.ROOT)
                    )
                    if (countryComparison != 0) {
                        countryComparison
                    } else {
                        compareVoiceNamesNaturally(first, second)
                    }
                }
            }
        voiceDialogOptions = createVoiceOptions(
            listOf(DEFAULT_VOICE_NAME) + availableVoices,
            allVoices,
            defaultVoiceLabel,
            randomEnglishVoiceLabel,
            randomAllVoiceLabel,
            otherLanguageLabel
        )
        val sceneCharacterNames = currentScene.lines
            .map { it.actor.name.uppercase() }
            .toSet()
        val characters = (currentScene.lines.map { it.actor } + scriptState.actors)
            .filterNot { it.name.equals(Actor.ACTION_NAME, ignoreCase = true) }
            .distinctBy { it.name.uppercase() }
            .sortedWith(compareByDescending<Actor> { it.name.uppercase() in sceneCharacterNames })
        voiceDialogCharacters = listOf(
            VoiceAssignment(
                name = Actor.ACTION_NAME,
                selectedVoice = scriptState.actorVoices[Actor.ACTION_NAME]
                    ?: scriptState.getVoice(Actor.ACTION_NAME)
                    ?: DEFAULT_VOICE_NAME,
                enabled = true,
                isInCurrentScene = Actor.ACTION_NAME in sceneCharacterNames
            )
        ) + characters.map { actor ->
                val configuredVoice = scriptState.actorVoices[actor.name.uppercase()]
                    ?: scriptState.actorVoices[actor.name]
                    ?: scriptState.getVoice(actor.name)
                VoiceAssignment(
                    name = actor.name.uppercase(),
                    selectedVoice = when {
                        configuredVoice.isNullOrBlank() -> DEFAULT_VOICE_NAME
                        configuredVoice.equals(DEFAULT_VOICE_NAME, ignoreCase = true) -> DEFAULT_VOICE_NAME
                        else -> configuredVoice
                    },
                    enabled = true,
                    isInCurrentScene = actor.name.uppercase() in sceneCharacterNames
                )
            }
        voiceDialogError = null
        voiceDialogRemovedCharacterReplacements = emptyMap()
        pendingVoiceRemoval = null
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
                    noEnglishVoicesLabel
                } else {
                    noVoicesAvailableLabel
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

        resolvedAssignments.forEach { assignment ->
            val normalizedName = assignment.name.uppercase()
            if (updatedScript.actors.none { it.name.uppercase() == normalizedName }) {
                updatedScript.actors.add(Actor(normalizedName))
            }
            val resolvedSelection = assignment.selectedVoice.trim()
            if (resolvedSelection.isBlank() || resolvedSelection.equals(DEFAULT_VOICE_NAME, ignoreCase = true)) {
                updatedScript.actorVoices.remove(normalizedName)
            } else {
                updatedScript.actorVoices[normalizedName] = resolvedSelection
            }
        }

        val removedNames = currentNames - incomingNames
        removedNames.forEach { removedName ->
            val actorToRemove = updatedScript.actors.firstOrNull { it.name.uppercase() == removedName } ?: return@forEach
            val replacementName = voiceDialogRemovedCharacterReplacements[removedName]
                ?: Actor.ACTION_NAME
            val replacementActor = if (replacementName.equals(Actor.ACTION_NAME, ignoreCase = true)) {
                Actor.ACTION
            } else {
                updatedScript.actors.firstOrNull { it.name.equals(replacementName, ignoreCase = true) }
                    ?: run {
                        voiceDialogError = chooseVoiceReplacementLabel
                        return
                    }
            }
            updatedScript.replaceActor(actorToRemove, replacementActor)
            updatedScript.actorVoices.remove(removedName)
            updatedScript.mutedCharacterNames.remove(removedName)
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
        voiceDialogRemovedCharacterReplacements = emptyMap()
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
                title = scriptState.name.ifBlank { untitledScriptLabel },
                sceneName = currentScene.name ?: untitledSceneLabel,
                scriptId = scriptState.id,
                sceneIndex = selectedSceneIndex,
                playing = true,
                progressMaxWords = currentScene.lines.sumOf { wordCount(it.line) },
                progressWords = currentScene.lines.take(currentLineIndex.coerceAtLeast(0) + 1).sumOf { wordCount(it.line) },
                remainingTimeLabel = remainingTimeLabel
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
            title = scriptState.name.ifBlank { untitledScriptLabel },
            sceneName = currentScene.name ?: untitledSceneLabel,
            scriptId = scriptState.id,
            sceneIndex = selectedSceneIndex,
            playing = isPlaying,
            keepNotificationVisible = !isPlaying && currentLineIndex >= 0,
            progressMaxWords = totalWords,
            progressWords = spokenWords,
            remainingTimeLabel = if (isPlaying) remainingTimeLabel else ""
        )
    }

    LaunchedEffect(selectedSceneIndex, scriptState.id, isPlaying) {
        TtsPlaybackController.bindPlayback(
            title = scriptState.name.ifBlank { untitledScriptLabel },
            sceneName = currentScene.name ?: untitledSceneLabel,
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
                    }.thenBy { it.locale.country.uppercase(Locale.ROOT) }
                        .thenComparator { first, second ->
                            compareVoiceNamesNaturally(first.name, second.name)
                        })
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
            .filter { it.isNotBlank() && it != DEFAULT_VOICE_NAME }
            .filter { playableVoices.contains(it) || it == scriptState.defaultVoice }
        val availableVoiceNames = (playableVoices + filteredLegacyVoices + listOfNotNull(scriptState.defaultVoice))
            .distinct()
            .filter { it.isNotBlank() && it != DEFAULT_VOICE_NAME }
            .sortedWith(compareBy<String> { voiceName ->
                voiceLanguagePriority[voiceName] ?: 1
            }.thenBy { voiceName ->
                allVoices.firstOrNull { it.name == voiceName }?.locale?.country.orEmpty()
                    .uppercase(Locale.ROOT)
            }.thenComparator { first, second ->
                compareVoiceNamesNaturally(first, second)
            })
        voiceDialogOptions = createVoiceOptions(
            listOf(DEFAULT_VOICE_NAME) + availableVoiceNames,
            allVoices,
            defaultVoiceLabel,
            randomEnglishVoiceLabel,
            randomAllVoiceLabel,
            otherLanguageLabel
        )
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
                title = {
                    Text(
                        text = scriptState.name.ifBlank {
                            untitledScriptLabel
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { requestBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
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
                            contentDescription = stringResource(R.string.scene_undo_edit),
                            tint = if (canUndo) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(
                        onClick = { redoSceneEdit() },
                        enabled = canRedo
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = stringResource(R.string.scene_redo_edit),
                            tint = if (canRedo) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.scene_options)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.scene_stop_at_end)) },
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
                            text = { Text(stringResource(R.string.scene_character_lines)) },
                            onClick = {
                                menuExpanded = false
                                showCharacterLinesDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.scene_character_voices)) },
                            onClick = {
                                menuExpanded = false
                                openCharacterVoiceDialog()
                            }
                        )
                        if (scriptState.scenes.isNotEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.scene_delete_title),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
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
                            contentDescription = stringResource(R.string.scene_add_line)
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            if (currentScene.isLoaded) togglePlayback()
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.scene_play_pause)
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
                            text = stringResource(R.string.scene_empty_title),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.scene_empty_message),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { showAddSceneDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.scene_add))
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
                            text = scriptState.scenes[selectedSceneIndex].name?.let { sceneName ->
                                stringResource(
                                    R.string.scene_number_title_label,
                                    selectedSceneIndex + 1,
                                    sceneName
                                )
                            } ?: stringResource(
                                R.string.scene_number_label,
                                selectedSceneIndex + 1
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.scene_choose)
                        )
                    }
                }

                item {
                    Text(
                        text = buildString {
                            append(
                                currentScene.name?.uppercase()
                                    ?: untitledSceneHeadingLabel
                            )
                            currentScene.fountainSceneNumber?.let { append(" #$it") }
                        },
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
                                Text(sceneLoadError ?: stringResource(R.string.scene_not_loaded))
                                TextButton(onClick = { loadScene(selectedSceneIndex) }) {
                                    Text(stringResource(R.string.common_retry))
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(currentScene.lines) { index, line ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (lineDropIndex == index) {
                                LineDropIndicator()
                            }
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
                                },
                                lineIndex = index,
                                onPositionChanged = { positionIndex, top, bottom ->
                                    val position = top to bottom
                                    if (linePositions[positionIndex] != position) {
                                        linePositions[positionIndex] = position
                                    }
                                },
                                onDragStart = { draggedIndex, pointerY ->
                                    draggingLineIndex = draggedIndex
                                    dragPointerY = pointerY
                                    updateLineDropIndex(pointerY)
                                },
                                onDrag = { draggedIndex, deltaY ->
                                    if (draggingLineIndex == draggedIndex) {
                                        dragPointerY += deltaY
                                        updateLineDropIndex(dragPointerY)
                                    }
                                },
                                onDragEnd = { finishLineReorder() },
                                onDragCancel = {
                                    draggingLineIndex = null
                                    lineDropIndex = null
                                    linePositions.clear()
                                }
                            )
                        }
                    }
                    if (lineDropIndex == currentScene.lines.size) {
                        item(key = "line-drop-end") {
                            LineDropIndicator()
                        }
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
                        Text(stringResource(R.string.scene_add))
                    }
                }
            }
        }
    }

    if (showDeleteSceneDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSceneDialog = false },
            title = { Text(stringResource(R.string.scene_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.scene_delete_confirmation,
                        currentScene.name ?: sceneThisLabel
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteCurrentScene() }) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSceneDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
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
            title = { Text(stringResource(R.string.scene_add)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (scriptState.scenes.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.scene_insert),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !insertSceneBeforeCurrent,
                                onClick = { insertSceneBeforeCurrent = false }
                            )
                            Text(
                                text = stringResource(R.string.scene_insert_after),
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
                                text = stringResource(R.string.scene_insert_before),
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
                            label = { Text(stringResource(R.string.scene_heading_prefix)) },
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
                            listOf(
                                R.string.scene_prefix_int,
                                R.string.scene_prefix_ext,
                                R.string.scene_prefix_est,
                                R.string.scene_prefix_int_ext_dotted,
                                R.string.scene_prefix_int_ext,
                                R.string.scene_prefix_ie
                            ).map(context::getString).forEach { prefix ->
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
                        label = { Text(stringResource(R.string.scene_name)) },
                        placeholder = { Text(stringResource(R.string.scene_location_time_placeholder)) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { addNewScene() },
                    enabled = sceneHeadingLocation.isNotBlank()
                ) {
                    Text(stringResource(R.string.common_add))
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
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showSceneSelector) {
        val normalizedQuery = sceneSearchQuery.trim()
        val matchingScenes = scriptState.scenes.indices.filter { index ->
            val scene = scriptState.scenes[index]
            val searchableText = "$sceneSearchWord ${index + 1} ${scene.number} ${scene.name.orEmpty()}"
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
            title = { Text(stringResource(R.string.scene_select_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = sceneSearchQuery,
                        onValueChange = { sceneSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.scene_search)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        }
                    )
                    if (matchingScenes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.scene_search_empty),
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
                                        text = scriptState.scenes[index].name?.let { sceneName ->
                                            stringResource(
                                                R.string.scene_number_title_label,
                                                index + 1,
                                                sceneName
                                            )
                                        } ?: stringResource(R.string.scene_number_label, index + 1),
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
                                            text = stringResource(R.string.common_selected),
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
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.scene_unsaved_title)) },
            text = { Text(stringResource(R.string.scene_unsaved_message)) },
            confirmButton = {
                TextButton(onClick = {
                    if (editingLineIndex != null) {
                        saveEditingLineChanges(editingLineIndex!!, editingActorName, editingLineText)
                    }
                    showExitDialog = false
                    onBack()
                }) {
                    Text(stringResource(R.string.scene_save_and_leave))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showExitDialog = false
                        exitEditMode()
                        onBack()
                    }) {
                        Text(stringResource(R.string.scene_discard))
                    }
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(stringResource(R.string.scene_keep_editing))
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
                        text = stringResource(R.string.scene_character_voices),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val voiceOptions = voiceDialogOptions.ifEmpty {
                        listOf(
                            VoiceOption(
                                DEFAULT_VOICE_NAME,
                                defaultVoiceLabel,
                                defaultVoiceLabel,
                                VoiceOptionKind.DEFAULT
                            ),
                            VoiceOption(
                                RANDOM_ENGLISH_VOICE,
                                randomEnglishVoiceLabel,
                                defaultVoiceLabel,
                                VoiceOptionKind.RANDOM_ENGLISH
                            ),
                            VoiceOption(
                                RANDOM_ALL_VOICE,
                                randomAllVoiceLabel,
                                defaultVoiceLabel,
                                VoiceOptionKind.RANDOM_ALL
                            )
                        )
                    }
                    val actionAssignment = voiceDialogCharacters.firstOrNull {
                        it.name.equals(Actor.ACTION_NAME, ignoreCase = true)
                    }
                    val sceneCharacters = voiceDialogCharacters.filter {
                        it.isInCurrentScene && !it.name.equals(Actor.ACTION_NAME, ignoreCase = true)
                    }
                    val scriptCharacters = voiceDialogCharacters.filter {
                        !it.isInCurrentScene && !it.name.equals(Actor.ACTION_NAME, ignoreCase = true)
                    }
                    actionAssignment?.let { assignment ->
                        CharacterVoiceEditorRow(
                            assignment = assignment,
                            voiceOptions = voiceOptions,
                            nameEditable = false,
                            canRemove = false,
                            onNameChange = {},
                            onVoiceChange = { newVoice ->
                                voiceDialogError = null
                                voiceDialogCharacters = voiceDialogCharacters.map {
                                    if (it === assignment) it.copy(selectedVoice = newVoice) else it
                                }
                            },
                            onRemove = {}
                        )
                    }
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
                                    text = stringResource(R.string.voice_scene_characters),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            itemsIndexed(sceneCharacters) { _, assignment ->
                                CharacterVoiceEditorRow(
                                    assignment = assignment,
                                    voiceOptions = voiceOptions,
                                    nameEditable = true,
                                    canRemove = true,
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
                                        pendingVoiceRemoval = assignment
                                        voiceRemovalReplacementName = Actor.ACTION_NAME
                                    }
                                )
                            }
                        }
                        if (scriptCharacters.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.voice_script_characters),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            itemsIndexed(scriptCharacters) { _, assignment ->
                                CharacterVoiceEditorRow(
                                    assignment = assignment,
                                    voiceOptions = voiceOptions,
                                    nameEditable = true,
                                    canRemove = true,
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
                                        pendingVoiceRemoval = assignment
                                        voiceRemovalReplacementName = Actor.ACTION_NAME
                                    }
                                )
                            }
                        }
                        item {
                            Button(
                                onClick = {
                                    voiceDialogCharacters = voiceDialogCharacters + VoiceAssignment(
                                        name = "",
                                        selectedVoice = DEFAULT_VOICE_NAME,
                                        enabled = true,
                                        isInCurrentScene = false
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.voice_add_character))
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
                            Text(stringResource(R.string.common_cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { saveCharacterVoiceDialog() }) {
                            Text(stringResource(R.string.common_save))
                        }
                    }
                }
            }
        }
    }

    pendingVoiceRemoval?.let { assignment ->
        val replacementOptions = (
            listOf(Actor.ACTION_NAME) +
                voiceDialogCharacters
                    .filterNot { it === assignment }
                    .map { it.name.trim().uppercase() }
                    .filter { it.isNotBlank() }
            ).distinct()
        var replacementMenuExpanded by remember(assignment) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { pendingVoiceRemoval = null },
            title = { Text(stringResource(R.string.voice_remove_character_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.voice_transfer_lines, assignment.name))
                    ExposedDropdownMenuBox(
                        expanded = replacementMenuExpanded,
                        onExpandedChange = {
                            replacementMenuExpanded = !replacementMenuExpanded
                        }
                    ) {
                        OutlinedTextField(
                            value = voiceRemovalReplacementName,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text(stringResource(R.string.voice_transfer_lines_to)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = replacementMenuExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = replacementMenuExpanded,
                            onDismissRequest = { replacementMenuExpanded = false }
                        ) {
                            replacementOptions.forEach { characterName ->
                                DropdownMenuItem(
                                    text = { Text(characterName) },
                                    onClick = {
                                        voiceRemovalReplacementName = characterName
                                        replacementMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        voiceDialogRemovedCharacterReplacements =
                            voiceDialogRemovedCharacterReplacements +
                                (assignment.name.uppercase() to voiceRemovalReplacementName)
                        voiceDialogCharacters =
                            voiceDialogCharacters.filterNot { it === assignment }
                        pendingVoiceRemoval = null
                    }
                ) {
                    Text(stringResource(R.string.voice_remove_character))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingVoiceRemoval = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
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
            title = { Text(stringResource(R.string.scene_character_lines)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.character_lines_description),
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
                            Text(stringResource(R.string.character_lines_all_on))
                        }
                        TextButton(
                            onClick = {
                                saveMutedCharacters(characters.map { it.name.uppercase() }.toSet())
                            },
                            enabled = characters.any { it.name.uppercase() !in currentlyMuted }
                        ) {
                            Text(stringResource(R.string.character_lines_all_off))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Actor.ACTION_NAME,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = true,
                            onCheckedChange = null,
                            enabled = false,
                            modifier = Modifier.size(48.dp)
                        )
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
                                    },
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCharacterLinesDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }
}

private const val DEFAULT_VOICE_NAME = "Default"
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

private data class TtsVoiceDisplayDetails(
    val name: String,
    val countryCode: String,
    val languageCode: String,
    val language: String
)

private fun createVoiceOptions(
    voiceNames: List<String>,
    voices: List<android.speech.tts.Voice>,
    defaultLabel: String,
    randomEnglishLabel: String,
    randomAllLabel: String,
    otherLanguageLabel: String
): List<VoiceOption> {
    val voicesByName = voices.associateBy { it.name }
    val voiceDetails = voiceNames
        .distinct()
        .filter {
            it.isNotBlank() &&
                it != DEFAULT_VOICE_NAME &&
                it != RANDOM_ENGLISH_VOICE &&
                it != RANDOM_ALL_VOICE
        }
        .map { name ->
            val voice = voicesByName[name]
            val locale = voice?.locale
            val language = voice?.locale?.getDisplayLanguage(Locale.getDefault())
                ?.takeIf { it.isNotBlank() }
                ?: otherLanguageLabel
            TtsVoiceDisplayDetails(
                name = name,
                countryCode = locale?.country
                    ?.takeIf { it.isNotBlank() }
                    ?.uppercase(Locale.ROOT)
                    ?: locale?.language
                        ?.takeIf { it.isNotBlank() }
                        ?.uppercase(Locale.ROOT)
                    ?: "ZZ",
                languageCode = locale?.language.orEmpty(),
                language = language
            )
        }
    val specialOptions = if (voiceNames.contains(DEFAULT_VOICE_NAME)) {
        listOf(
            VoiceOption(DEFAULT_VOICE_NAME, defaultLabel, defaultLabel, VoiceOptionKind.DEFAULT),
            VoiceOption(RANDOM_ENGLISH_VOICE, randomEnglishLabel, defaultLabel, VoiceOptionKind.RANDOM_ENGLISH),
            VoiceOption(RANDOM_ALL_VOICE, randomAllLabel, defaultLabel, VoiceOptionKind.RANDOM_ALL)
        )
    } else {
        emptyList()
    }

    return specialOptions + voiceDetails
        .groupBy { it.countryCode }
        .entries
        .flatMap { (countryCode, countryVoices) ->
            val sortedVoices = countryVoices.sortedWith(
                compareBy<TtsVoiceDisplayDetails> { it.languageCode }
                    .thenComparator { first, second ->
                        compareVoiceNamesNaturally(first.name, second.name)
                    }
            )
            val numberWidth = maxOf(2, sortedVoices.size.toString().length)
            sortedVoices.mapIndexed { index, voice ->
                val voiceNumber = (index + 1).toString().padStart(numberWidth, '0')
                VoiceOption(
                    voice.name,
                    "$countryCode-$voiceNumber (${voice.name})",
                    voice.language,
                    VoiceOptionKind.VOICE
                )
            }
        }.sortedWith(compareBy<VoiceOption> {
            when {
                it.kind == VoiceOptionKind.DEFAULT -> 0
                it.language == Locale.ENGLISH.getDisplayLanguage(Locale.getDefault()) -> 1
                else -> 2
            }
        }.thenBy { it.language.lowercase(Locale.getDefault()) }
            .thenBy { it.kind.ordinal }
            .thenBy { it.label.lowercase(Locale.getDefault()) })
}

private fun compareVoiceNamesNaturally(first: String, second: String): Int {
    var firstIndex = 0
    var secondIndex = 0

    while (firstIndex < first.length && secondIndex < second.length) {
        val firstChar = first[firstIndex]
        val secondChar = second[secondIndex]
        if (firstChar in '0'..'9' && secondChar in '0'..'9') {
            var firstEnd = firstIndex
            while (firstEnd < first.length && first[firstEnd] in '0'..'9') firstEnd++
            var secondEnd = secondIndex
            while (secondEnd < second.length && second[secondEnd] in '0'..'9') secondEnd++

            val firstDigits = first.substring(firstIndex, firstEnd)
            val secondDigits = second.substring(secondIndex, secondEnd)
            val firstSignificantDigits = firstDigits.trimStart('0').ifEmpty { "0" }
            val secondSignificantDigits = secondDigits.trimStart('0').ifEmpty { "0" }
            val numericLengthComparison = compareValues(
                firstSignificantDigits.length,
                secondSignificantDigits.length
            )
            if (numericLengthComparison != 0) return numericLengthComparison

            val numericComparison = firstSignificantDigits.compareTo(secondSignificantDigits)
            if (numericComparison != 0) return numericComparison

            val leadingZeroComparison = compareValues(firstDigits.length, secondDigits.length)
            if (leadingZeroComparison != 0) return leadingZeroComparison

            firstIndex = firstEnd
            secondIndex = secondEnd
        } else {
            val characterComparison = firstChar.lowercaseChar().compareTo(secondChar.lowercaseChar())
            if (characterComparison != 0) return characterComparison
            firstIndex++
            secondIndex++
        }
    }

    return compareValues(first.length - firstIndex, second.length - secondIndex)
}

private data class VoiceAssignment(
    var name: String,
    var selectedVoice: String,
    var enabled: Boolean = true,
    val isInCurrentScene: Boolean = false
)

@Composable
private fun LineDropIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(4.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small
            )
    )
}

private val UTTERANCE_ID_PATTERN = Regex("^scene_(\\d+)_line_(\\d+)_playback_\\d+$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterVoiceEditorRow(
    assignment: VoiceAssignment,
    voiceOptions: List<VoiceOption>,
    nameEditable: Boolean,
    canRemove: Boolean,
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
            label = { Text(stringResource(R.string.common_character)) },
            singleLine = true,
            readOnly = !nameEditable,
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
                label = { Text(stringResource(R.string.common_voice)) },
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
                        text = { Text(stringResource(R.string.common_default)) },
                        onClick = {
                            onVoiceChange(DEFAULT_VOICE_NAME)
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
                                            {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = stringResource(R.string.common_selected)
                                                )
                                            }
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
            enabled = canRemove,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.voice_remove_character),
                tint = if (canRemove) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
