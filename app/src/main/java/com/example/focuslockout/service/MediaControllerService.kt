package com.example.focuslockout.service

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import com.example.focuslockout.data.FocusStateManager
import com.example.focuslockout.data.model.MediaPlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MediaControllerService : NotificationListenerService(), MediaSessionManager.OnActiveSessionsChangedListener {

    private lateinit var mediaSessionManager: MediaSessionManager
    private var activeController: MediaController? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaState()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, MediaControllerService::class.java)
        
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(this, componentName)
            val sessions = mediaSessionManager.getActiveSessions(componentName)
            onActiveSessionsChanged(sessions)
        } catch (e: SecurityException) {
            Log.e("MediaControllerService", "Missing notification listener permission", e)
        }
        
        // Listen for UI commands
        serviceScope.launch {
            FocusStateManager.mediaCommand.collect { command ->
                command?.let {
                    handleCommand(it)
                    FocusStateManager.clearMediaCommand()
                }
            }
        }
    }

    override fun onDestroy() {
        mediaSessionManager.removeOnActiveSessionsChangedListener(this)
        activeController?.unregisterCallback(mediaControllerCallback)
        super.onDestroy()
    }

    override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
        activeController?.unregisterCallback(mediaControllerCallback)
        
        // Prioritize YouTube Music or pick first active
        activeController = controllers?.find { it.packageName == "com.google.android.apps.youtube.music" }
            ?: controllers?.firstOrNull()

        activeController?.registerCallback(mediaControllerCallback)
        updateMediaState()
    }

    private fun updateMediaState() {
        val controller = activeController
        if (controller == null) {
            FocusStateManager.updateMediaState(MediaPlaybackState())
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        
        val actions = playbackState?.actions ?: 0L
        val canSkipNext = (actions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L
        val canSkipPrev = (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L

        FocusStateManager.updateMediaState(
            MediaPlaybackState(
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                canSkipNext = canSkipNext,
                canSkipPrev = canSkipPrev
            )
        )
    }

    private fun handleCommand(command: FocusStateManager.MediaCommand) {
        val transportControls = activeController?.transportControls ?: return
        when (command) {
            FocusStateManager.MediaCommand.PLAY -> transportControls.play()
            FocusStateManager.MediaCommand.PAUSE -> transportControls.pause()
            FocusStateManager.MediaCommand.NEXT -> transportControls.skipToNext()
            FocusStateManager.MediaCommand.PREVIOUS -> transportControls.skipToPrevious()
        }
    }
}
