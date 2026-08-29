package com.example.focuslockout.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "blocker_preferences")

class BlockerPreferences(private val context: Context) {

    companion object {
        val KEY_BLOCK_YOUTUBE_SHORTS = booleanPreferencesKey("block_youtube_shorts")
        val KEY_BLOCK_INSTAGRAM_REELS = booleanPreferencesKey("block_instagram_reels")
    }

    val blockYoutubeShorts: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BLOCK_YOUTUBE_SHORTS] ?: true
    }

    val blockInstagramReels: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BLOCK_INSTAGRAM_REELS] ?: true
    }

    suspend fun setBlockYoutubeShorts(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BLOCK_YOUTUBE_SHORTS] = enabled
        }
    }

    suspend fun setBlockInstagramReels(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BLOCK_INSTAGRAM_REELS] = enabled
        }
    }
}
