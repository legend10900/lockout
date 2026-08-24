package com.example.focuslockout.data

import com.example.focuslockout.data.model.MediaPlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FocusStateManager {
    private val _isFocusActive = MutableStateFlow(false)
    val isFocusActive: StateFlow<Boolean> = _isFocusActive.asStateFlow()

    private val _remainingTimeMillis = MutableStateFlow(0L)
    val remainingTimeMillis: StateFlow<Long> = _remainingTimeMillis.asStateFlow()

    private val _totalDurationMillis = MutableStateFlow(0L)
    val totalDurationMillis: StateFlow<Long> = _totalDurationMillis.asStateFlow()

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    private val _mediaState = MutableStateFlow(MediaPlaybackState())
    val mediaState: StateFlow<MediaPlaybackState> = _mediaState.asStateFlow()
    
    // Command flow from UI to MediaControllerService
    private val _mediaCommand = MutableStateFlow<MediaCommand?>(null)
    val mediaCommand: StateFlow<MediaCommand?> = _mediaCommand.asStateFlow()

    fun setFocusActive(active: Boolean) {
        _isFocusActive.value = active
    }

    fun setRemainingTimeMillis(millis: Long) {
        _remainingTimeMillis.value = millis
    }

    fun setTotalDurationMillis(millis: Long) {
        _totalDurationMillis.value = millis
    }

    fun setCallActive(active: Boolean) {
        _isCallActive.value = active
    }

    fun updateMediaState(state: MediaPlaybackState) {
        _mediaState.value = state
    }
    
    fun sendMediaCommand(command: MediaCommand) {
        _mediaCommand.value = command
    }
    
    fun clearMediaCommand() {
        _mediaCommand.value = null
    }

    enum class MediaCommand {
        PLAY, PAUSE, NEXT, PREVIOUS
    }
}
