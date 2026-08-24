package com.example.focuslockout.ui.focus

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuslockout.MainActivity
import com.example.focuslockout.auth.BiometricAuthManager
import com.example.focuslockout.data.FocusStateManager
import com.example.focuslockout.service.FocusForegroundService
import kotlinx.coroutines.launch

@Composable
fun FocusScreen(activity: MainActivity) {
    val remainingMillis by FocusStateManager.remainingTimeMillis.collectAsState()
    val mediaState by FocusStateManager.mediaState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val authManager = remember { BiometricAuthManager(activity) }

    val hours = (remainingMillis / (1000 * 60 * 60))
    val minutes = (remainingMillis / (1000 * 60)) % 60
    val seconds = (remainingMillis / 1000) % 60
    val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "FOCUS",
                color = Color.DarkGray,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = timeStr,
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Media Controller Widget
            if (mediaState.title.isNotEmpty()) {
                Text(
                    text = mediaState.title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = mediaState.artist,
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { FocusStateManager.sendMediaCommand(FocusStateManager.MediaCommand.PREVIOUS) },
                        enabled = mediaState.canSkipPrev
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                    }
                    
                    IconButton(
                        onClick = { 
                            if (mediaState.isPlaying) {
                                FocusStateManager.sendMediaCommand(FocusStateManager.MediaCommand.PAUSE)
                            } else {
                                FocusStateManager.sendMediaCommand(FocusStateManager.MediaCommand.PLAY)
                            }
                        }
                    ) {
                        Icon(
                            if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                            contentDescription = "Play/Pause", 
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { FocusStateManager.sendMediaCommand(FocusStateManager.MediaCommand.NEXT) },
                        enabled = mediaState.canSkipNext
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                    }
                }
            }
        }

        // Emergency Unlock
        TextButton(
            onClick = {
                coroutineScope.launch {
                    val success = authManager.authenticate()
                    if (success) {
                        val stopIntent = Intent(activity, FocusForegroundService::class.java).apply {
                            action = FocusForegroundService.ACTION_STOP_FOCUS
                        }
                        activity.startService(stopIntent)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text("Emergency Unlock", color = Color.DarkGray)
        }
    }
}
