package com.example.focuslockout.ui.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.focuslockout.MainActivity
import com.example.focuslockout.service.FocusForegroundService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import com.example.focuslockout.service.FocusAccessibilityService
import android.content.pm.PackageManager

val NeonCyan = Color(0xFF00F0FF)
val DeepSpaceBlue = Color(0xFF0A0E17)
val VoidBlack = Color(0xFF040609)
val NeonPink = Color(0xFFFF003C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(activity: MainActivity) {
    var totalMinutes by remember { mutableStateOf(15f) }
    
    var permissionsGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        permissionsGranted = checkAllPermissions(context)
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(VoidBlack, DeepSpaceBlue)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SYSTEM OVERRIDE",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "INITIALIZE LOCKOUT SEQUENCE",
            color = Color.LightGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        // Futuristic Time Slider
        Text(
            text = "SET DURATION",
            color = NeonCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val hours = (totalMinutes.toInt()) / 60
        val mins = (totalMinutes.toInt()) % 60
        Text(
            text = String.format("%02d:%02d:00", hours, mins),
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
        )
        
        Slider(
            value = totalMinutes,
            onValueChange = { totalMinutes = it },
            valueRange = 1f..240f,
            steps = 239,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = Color.DarkGray
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1m", color = Color.Gray, fontFamily = FontFamily.Monospace)
            Text("4h", color = Color.Gray, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Permissions Dashboard
        PermissionsDashboard(context) { 
            permissionsGranted = checkAllPermissions(context) 
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (checkAllPermissions(context)) {
                    val durationMillis = (totalMinutes.toLong() * 60 * 1000L)
                    val serviceIntent = Intent(context, FocusForegroundService::class.java).apply {
                        putExtra(FocusForegroundService.EXTRA_DURATION_MILLIS, durationMillis)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } else {
                    permissionsGranted = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (permissionsGranted) 20.dp else 0.dp,
                    shape = RoundedCornerShape(8.dp),
                    ambientColor = NeonCyan,
                    spotColor = NeonCyan
                )
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, if (permissionsGranted) NeonCyan else Color.DarkGray, RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            enabled = permissionsGranted
        ) {
            Text(
                text = if (permissionsGranted) "ENGAGE LOCKOUT" else "SYSTEMS PENDING",
                color = if (permissionsGranted) NeonCyan else Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

@Composable
fun PermissionsDashboard(context: Context, onRecheck: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYSTEM DIAGNOSTICS",
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = onRecheck) {
                    Text("SCAN", color = NeonCyan, fontFamily = FontFamily.Monospace)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PermissionItem(
                title = "Accessibility Core",
                isGranted = isAccessibilityServiceEnabled(context),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
            PermissionItem(
                title = "Overlay Protocol",
                isGranted = Settings.canDrawOverlays(context),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                }
            )
            PermissionItem(
                title = "Media Telemetry",
                isGranted = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )
            
            val phoneStateGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
            PermissionItem(
                title = "Comms Intercept",
                isGranted = phoneStateGranted,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }
            )
        }
    }
}

@Composable
fun PermissionItem(title: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) NeonCyan else NeonPink,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title, 
            modifier = Modifier.weight(1f), 
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
        if (!isGranted) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink.copy(alpha = 0.2f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("FIX", color = NeonPink, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        } else {
            Text("OK", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    for (service in enabledServices) {
        if (service.resolveInfo.serviceInfo.packageName == context.packageName &&
            service.resolveInfo.serviceInfo.name == FocusAccessibilityService::class.java.name) {
            return true
        }
    }
    return false
}

fun checkAllPermissions(context: Context): Boolean {
    return isAccessibilityServiceEnabled(context) &&
           Settings.canDrawOverlays(context) &&
           NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) &&
           ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
}
