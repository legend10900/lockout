package com.example.focuslockout.ui.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.focuslockout.MainActivity
import com.example.focuslockout.service.FocusForegroundService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import com.example.focuslockout.service.FocusAccessibilityService
import android.content.pm.PackageManager

@Composable
fun SetupScreen(activity: MainActivity) {
    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(15) }
    
    var permissionsGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Periodically re-check permissions when coming back
    LaunchedEffect(Unit) {
        permissionsGranted = checkAllPermissions(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Focus Setup", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        // Duration Picker
        Text("Select Duration", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { hours = 0; minutes = 15 }) { Text("15m") }
            Button(onClick = { hours = 0; minutes = 30 }) { Text("30m") }
            Button(onClick = { hours = 1; minutes = 0 }) { Text("1h") }
            Button(onClick = { hours = 2; minutes = 0 }) { Text("2h") }
        }
        
        Text(
            text = "Duration: ${hours}h ${minutes}m",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Permissions Dashboard
        PermissionsDashboard(context) { 
            permissionsGranted = checkAllPermissions(context) 
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (checkAllPermissions(context)) {
                    val durationMillis = (hours * 60 * 60 * 1000L) + (minutes * 60 * 1000L)
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
                .height(56.dp),
            enabled = permissionsGranted
        ) {
            Text("ENTER FOCUS MODE")
        }
        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

@Composable
fun PermissionsDashboard(context: Context, onRecheck: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Required Permissions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            PermissionItem(
                title = "Accessibility (App Blocking)",
                isGranted = isAccessibilityServiceEnabled(context),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
            PermissionItem(
                title = "Display over other apps",
                isGranted = Settings.canDrawOverlays(context),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                }
            )
            PermissionItem(
                title = "Notification Listener (Media)",
                isGranted = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )
            
            val phoneStateGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
            PermissionItem(
                title = "Phone State (Allow Calls)",
                isGranted = phoneStateGranted,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }
            )
            
            Button(onClick = onRecheck, modifier = Modifier.align(Alignment.End)) {
                Text("Refresh")
            }
        }
    }
}

@Composable
fun PermissionItem(title: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, modifier = Modifier.weight(1f))
        if (!isGranted) {
            TextButton(onClick = onClick) {
                Text("Grant")
            }
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
