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
            // Prevent app closure loop: Try to click the "Home" tab to return to the feed.
            // If the Home tab isn't visible (e.g. opened from a DM), fallback to BACK button.
            val homeClicked = tryClickHomeTab(rootNode)
            if (!homeClicked) {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            
            handler.post {
                Toast.makeText(applicationContext, blockReason, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun tryClickHomeTab(node: AccessibilityNodeInfo): Boolean {
        val homeNodes = node.findAccessibilityNodeInfosByText("Home")
        for (home in homeNodes) {
            if (home.isClickable) {
                home.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                home.recycle()
                return true
            }
            var parent = home.parent
            while (parent != null) {
                if (parent.isClickable) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    parent.recycle()
                    home.recycle()
                    return true
                }
                val oldParent = parent
                parent = parent.parent
                oldParent?.recycle()
            }
            home.recycle()
        }
        return false
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
        // STRICT CHECK: clips_viewer_view_pager is the full-screen vertical pager.
        // We removed clips_video_container because it triggers on inline feed videos.
        val clipsPager = node.findAccessibilityNodeInfosByViewId("$packageName:id/clips_viewer_view_pager")
        var isReels = clipsPager.isNotEmpty()
        clipsPager.forEach { it.recycle() }
        
        // Fallback: Check if the Reels tab in the bottom nav is currently selected.
        if (!isReels) {
            val reelsTabs = node.findAccessibilityNodeInfosByText("Reels")
            for (tab in reelsTabs) {
                if (tab.isSelected) {
                    isReels = true
                }
                tab.recycle()
            }
        }
        
        return isReels
    }

    override fun onInterrupt() {
    }
}
