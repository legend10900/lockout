package com.example.focuslockout.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuslockout.data.BlockerPreferences
import com.example.focuslockout.service.ShortsReelsBlockerService
import com.example.focuslockout.ui.setup.NeonCyan
import com.example.focuslockout.ui.setup.NeonPink
import com.example.focuslockout.ui.setup.VoidBlack
import kotlinx.coroutines.launch
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager

@Composable
fun ShortsBlockerSettingsScreen(
    blockerPreferences: BlockerPreferences,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val blockYoutube by blockerPreferences.blockYoutubeShorts.collectAsState(initial = true)
    val blockInstagram by blockerPreferences.blockInstagramReels.collectAsState(initial = true)
    
    val context = LocalContext.current
    var isServiceEnabled by remember { mutableStateOf(isBlockerServiceEnabled(context)) }

    // Periodically recheck service
    LaunchedEffect(Unit) {
        isServiceEnabled = isBlockerServiceEnabled(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("< BACK", color = NeonCyan, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "ANTI-DOOMSCROLL",
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.weight(1.5f)) // Balance out the row
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Service Health Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(
                    1.dp, 
                    if (isServiceEnabled) NeonCyan.copy(alpha = 0.3f) else NeonPink.copy(alpha = 0.5f), 
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "SERVICE STATUS",
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isServiceEnabled) "ACTIVE" else "OFFLINE",
                        color = if (isServiceEnabled) NeonCyan else NeonPink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    
                    if (!isServiceEnabled) {
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink.copy(alpha = 0.2f))
                        ) {
                            Text("ENABLE", color = NeonPink, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Button(
                            onClick = { isServiceEnabled = isBlockerServiceEnabled(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.1f))
                        ) {
                            Text("REFRESH", color = NeonCyan, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Toggles
        Text(
            text = "INTERCEPTION TARGETS",
            color = NeonCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        BlockerToggleRow(
            title = "YouTube Shorts",
            description = "Ejects user to previous screen when Shorts are detected.",
            isChecked = blockYoutube,
            onCheckedChange = { 
                coroutineScope.launch { blockerPreferences.setBlockYoutubeShorts(it) } 
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        BlockerToggleRow(
            title = "Instagram Reels",
            description = "Intercepts and blocks Reels player while keeping DMs accessible.",
            isChecked = blockInstagram,
            onCheckedChange = { 
                coroutineScope.launch { blockerPreferences.setBlockInstagramReels(it) } 
            }
        )
    }
}

@Composable
fun BlockerToggleRow(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp
            )
            Text(
                text = description,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp, end = 16.dp)
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

fun isBlockerServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    for (service in enabledServices) {
        if (service.resolveInfo.serviceInfo.packageName == context.packageName &&
            service.resolveInfo.serviceInfo.name == ShortsReelsBlockerService::class.java.name) {
            return true
        }
    }
    return false
}
