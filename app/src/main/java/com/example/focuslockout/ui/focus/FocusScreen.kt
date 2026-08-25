package com.example.focuslockout.ui.focus

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuslockout.MainActivity
import com.example.focuslockout.auth.BiometricAuthManager
import com.example.focuslockout.data.FocusStateManager
import com.example.focuslockout.service.FocusForegroundService
import kotlinx.coroutines.launch

val NeonCyan = Color(0xFF00F0FF)
val DeepSpaceBlue = Color(0xFF0A0E17)
val VoidBlack = Color(0xFF000000)

@Composable
fun FocusScreen(activity: MainActivity) {
    val remainingMillis by FocusStateManager.remainingTimeMillis.collectAsState()
    val totalDurationMillis by FocusStateManager.totalDurationMillis.collectAsState()
    val mediaState by FocusStateManager.mediaState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val authManager = remember { BiometricAuthManager(activity) }

    val hours = (remainingMillis / (1000 * 60 * 60))
    val minutes = (remainingMillis / (1000 * 60)) % 60
    val seconds = (remainingMillis / 1000) % 60
    val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    val progress = if (totalDurationMillis > 0) {
        remainingMillis.toFloat() / totalDurationMillis.toFloat()
    } else {
        0f
    }

    // Breathing animation for the outer ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "NEURAL LOCK ACTIVATED",
                color = NeonCyan.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(300.dp)
            ) {
                // Pulse Ring
                Canvas(modifier = Modifier.fillMaxSize().scale(pulseScale)) {
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.1f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Progress Arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.DarkGray,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = NeonCyan,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeStr,
                        color = Color.White,
                        fontSize = 48.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "REMAINING",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        letterSpacing = 4.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Futuristic Media Controller
            if (mediaState.title.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NOW PLAYING",
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = mediaState.title,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = mediaState.artist,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { FocusStateManager.sendMediaCommand(FocusStateManager.MediaCommand.PREVIOUS) },
                                enabled = mediaState.canSkipPrev,
                                modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.SkipPrevious, "Previous", tint = if(mediaState.canSkipPrev) Color.White else Color.DarkGray)
                            }
                            
                            IconButton(
                                onClick = { 
                                    if (mediaState.isPlaying) FocusStateManager.sendMediaCommand(FocusStateManager.MediaCommand.PAUSE)
                                    else FocusStateManager.sendMediaCommand(FocusStateManager.MediaCommand.PLAY)
                                },
                                modifier = Modifier.size(56.dp).background(NeonCyan.copy(alpha = 0.2f), CircleShape).border(1.dp, NeonCyan, CircleShape)
                            ) {
                                Icon(
                                    if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                    "Play/Pause", 
                                    tint = NeonCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = { FocusStateManager.sendMediaCommand(FocusStateManager.MediaCommand.NEXT) },
                                enabled = mediaState.canSkipNext,
                                modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.SkipNext, "Next", tint = if(mediaState.canSkipNext) Color.White else Color.DarkGray)
                            }
                        }
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
            Text(
                "ABORT SEQUENCE", 
                color = Color.DarkGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )
        }
    }
}
