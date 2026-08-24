package com.example.focuslockout.data.model

data class MediaPlaybackState(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSkipPrev: Boolean = false
)
