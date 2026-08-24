package com.example.focuslockout.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityEvent
import com.example.focuslockout.MainActivity
import com.example.focuslockout.data.FocusStateManager

class FocusAccessibilityService : AccessibilityService() {

    private val allowedPackages = mutableSetOf<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        allowedPackages.add(packageName)
        allowedPackages.add("com.android.systemui")
        allowedPackages.add("com.google.android.permissioncontroller")
        allowedPackages.add("com.android.permissioncontroller")
        
        // Add default dialer
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        telecomManager.defaultDialerPackage?.let {
            allowedPackages.add(it)
        }
        // Common dialers
        allowedPackages.add("com.google.android.dialer")
        allowedPackages.add("com.android.incallui")
        allowedPackages.add("com.android.server.telecom")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        
        if (!FocusStateManager.isFocusActive.value) return
        if (FocusStateManager.isCallActive.value) return // Yield when call is active

        val eventPackage = event.packageName?.toString() ?: return

        if (!allowedPackages.contains(eventPackage)) {
            // Blocked app detected, bring Focus App to front
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // No-op
    }
}
