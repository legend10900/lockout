package com.example.focuslockout.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.focuslockout.data.BlockerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ShortsReelsBlockerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastCheckTime = 0L
    private val DEBOUNCE_MS = 300L
    
    private lateinit var blockerPreferences: BlockerPreferences
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var isShortsBlockEnabled = true
    private var isReelsBlockEnabled = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        blockerPreferences = BlockerPreferences(applicationContext)
        
        serviceScope.launch {
            blockerPreferences.blockYoutubeShorts.collect { isShortsBlockEnabled = it }
        }
        serviceScope.launch {
            blockerPreferences.blockInstagramReels.collect { isReelsBlockEnabled = it }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        val packageName = event.packageName?.toString() ?: return
        
        // Flexible matching for modded clients (e.g., InstaPro, Instander, ReVanced)
        val isYoutube = packageName.contains("youtube", ignoreCase = true)
        val isInstagram = packageName.contains("insta", ignoreCase = true)
        
        if (!isYoutube && !isInstagram) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCheckTime < DEBOUNCE_MS) {
            return
        }
        lastCheckTime = currentTime

        val rootNode = rootInActiveWindow ?: return
        
        var shouldBlock = false
        var blockReason = ""

        if (isYoutube && isShortsBlockEnabled) {
            if (isYoutubeShorts(rootNode, packageName)) {
                shouldBlock = true
                blockReason = "Shorts Blocked"
            }
        } else if (isInstagram && isReelsBlockEnabled) {
            if (isInstagramReels(rootNode, packageName)) {
                shouldBlock = true
                blockReason = "Reels Blocked"
            }
        }
        
        rootNode.recycle()

        if (shouldBlock) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            handler.post {
                Toast.makeText(applicationContext, blockReason, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isYoutubeShorts(node: AccessibilityNodeInfo, packageName: String): Boolean {
        // Find nodes matching typical Shorts identifiers
        val reelRecyclers = node.findAccessibilityNodeInfosByViewId("$packageName:id/reel_recycler")
        if (reelRecyclers.isNotEmpty()) {
            reelRecyclers.forEach { it.recycle() }
            return true
        }

        // Checking for tab or descriptions indicating Shorts
        val shortsTabs = node.findAccessibilityNodeInfosByText("Shorts")
        var foundShorts = false
        for (tab in shortsTabs) {
            if (tab.isSelected && tab.isClickable) { // Selected Shorts tab
                foundShorts = true
            }
            tab.recycle()
        }
        
        return foundShorts
    }

    private fun isInstagramReels(node: AccessibilityNodeInfo, packageName: String): Boolean {
        val clipsViewers = node.findAccessibilityNodeInfosByViewId("$packageName:id/clips_video_container")
        val clipsPager = node.findAccessibilityNodeInfosByViewId("$packageName:id/clips_viewer_view_pager")
        
        val isReels = clipsViewers.isNotEmpty() || clipsPager.isNotEmpty()
        
        clipsViewers.forEach { it.recycle() }
        clipsPager.forEach { it.recycle() }
        
        return isReels
    }

    override fun onInterrupt() {
    }
}
