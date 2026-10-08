package com.brokenshotgun.runlines.ui.reader.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.brokenshotgun.runlines.MainActivity
import com.brokenshotgun.runlines.R

object TtsPlaybackController {
    private const val CHANNEL_ID = "tts_playback_channel"
    const val ACTION_TOGGLE = "com.brokenshotgun.runlines.action.TOGGLE_TTS"
    const val ACTION_STOP = "com.brokenshotgun.runlines.action.STOP_TTS"

    private var toggleAction: (() -> Unit)? = null
    private var stopAction: (() -> Unit)? = null
    private var scriptTitle: String = "Script"
    private var sceneTitle: String = "Scene"
    private var currentScriptId: Long = -1L
    private var currentSceneIndex: Int = 0
    private var isPlaying: Boolean = false
    private var notificationVisible: Boolean = false
    private var progressMax: Int = 0
    private var progressValue: Int = 0
    private var progressText: String = ""

    fun bindPlayback(
        title: String,
        sceneName: String,
        scriptId: Long = -1L,
        sceneIndex: Int = 0,
        playing: Boolean,
        onToggle: () -> Unit,
        onStop: () -> Unit
    ) {
        scriptTitle = title
        sceneTitle = sceneName
        currentScriptId = scriptId
        currentSceneIndex = sceneIndex
        isPlaying = playing
        toggleAction = onToggle
        stopAction = onStop
        if (playing) {
            // Notification is refreshed by the caller when state changes.
        }
    }

    fun setPlayingState(
        context: Context,
        title: String,
        sceneName: String,
        scriptId: Long = -1L,
        sceneIndex: Int = 0,
        playing: Boolean,
        keepNotificationVisible: Boolean = false,
        progressMaxWords: Int = 0,
        progressWords: Int = 0,
        remainingTimeLabel: String = ""
    ) {
        scriptTitle = title
        sceneTitle = sceneName
        currentScriptId = scriptId
        currentSceneIndex = sceneIndex
        isPlaying = playing
        this.progressMax = progressMaxWords.coerceAtLeast(0)
        this.progressValue = progressWords.coerceIn(0, this.progressMax)
        progressText = remainingTimeLabel
        if (playing || keepNotificationVisible) {
            notificationVisible = true
            showNotification(context)
        } else {
            notificationVisible = false
            hideNotification(context)
        }
    }

    fun handleToggle() {
        toggleAction?.invoke()
    }

    fun handleStop() {
        stopAction?.invoke()
    }

    fun clearScript(context: Context, scriptId: Long) {
        if (currentScriptId != scriptId) return
        clearActivePlayback(context)
    }

    fun clearActivePlayback(context: Context) {
        toggleAction = null
        stopAction = null
        scriptTitle = "Script"
        sceneTitle = "Scene"
        currentScriptId = -1L
        currentSceneIndex = 0
        isPlaying = false
        progressMax = 0
        progressValue = 0
        progressText = ""
        hideNotification(context)
    }

    fun hideNotification(context: Context) {
        notificationVisible = false
        context.getSystemService(NotificationManager::class.java)?.cancel(1001)
    }

    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Script Reader Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Playback controls for spoken scripts"
            setSound(null, null)
        }
        notificationManager?.createNotificationChannel(channel)

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (currentScriptId >= 0L) {
                putExtra("script_id", currentScriptId)
                putExtra("scene_index", currentSceneIndex)
            }
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleActionTitle = if (isPlaying) "Pause" else "Play"
        val toggleIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val toggleIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, TtsPlaybackReceiver::class.java).apply {
                action = ACTION_TOGGLE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, TtsPlaybackReceiver::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = if (progressText.isNotBlank()) {
            "$sceneTitle • $progressText"
        } else {
            sceneTitle
        }

        val maxProgress = progressMax.coerceAtLeast(1)
        val currentProgress = progressValue.coerceIn(0, maxProgress)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentTitle(scriptTitle)
            .setContentText(notificationText)
            .setSubText(if (isPlaying) "Playing" else "Paused")
            .setProgress(maxProgress, currentProgress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(appPendingIntent)
            .addAction(
                NotificationCompat.Action(
                    toggleIcon,
                    toggleActionTitle,
                    toggleIntent
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_ff,
                    "Stop",
                    stopIntent
                )
            )
            .build()

        notificationManager?.notify(1001, notification)
    }
}

class TtsPlaybackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TtsPlaybackController.ACTION_TOGGLE -> TtsPlaybackController.handleToggle()
            TtsPlaybackController.ACTION_STOP -> TtsPlaybackController.handleStop()
        }
    }
}
