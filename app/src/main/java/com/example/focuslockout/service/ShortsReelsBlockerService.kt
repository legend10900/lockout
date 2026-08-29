package com.example.focuslockout.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.example.focuslockout.data.BlockerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShortsReelsBlockerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastCheckTime = 0L
    private val DEBOUNCE_MS = 50L // Fast for overlay tracking
    
    private lateinit var blockerPreferences: BlockerPreferences
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var isShortsBlockEnabled = true
    private var isReelsBlockEnabled = true
    
    private var overlayManager: ReelOverlayManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        blockerPreferences = BlockerPreferences(this)
        overlayManager = ReelOverlayManager(this)
        
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
        
        val isYoutube = packageName.contains("youtube", ignoreCase = true)
        val isInstagram = packageName.contains("insta", ignoreCase = true)
        
        if (!isYoutube && !isInstagram) {
            overlayManager?.hideOverlay()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCheckTime < DEBOUNCE_MS) return
        lastCheckTime = currentTime

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            overlayManager?.hideOverlay()
            return
        }

        if (isYoutube && isShortsBlockEnabled) {
            handleYoutube(rootNode, packageName)
        } else if (isInstagram && isReelsBlockEnabled) {
            handleInstagram(rootNode, packageName)
        } else {
            overlayManager?.hideOverlay()
        }
        
        rootNode.recycle()
    }

    private fun handleYoutube(rootNode: AccessibilityNodeInfo, packageName: String) {
        val idsToTry = listOf(
            "com.google.android.youtube:id/reel_recycler",
            "$packageName:id/reel_recycler"
        )
        var foundShorts = false
        for (id in idsToTry) {
            val reelRecyclers = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (reelRecyclers.isNotEmpty()) {
                reelRecyclers.forEach { it.recycle() }
                foundShorts = true
                break
            }
        }
        
        if (!foundShorts) {
            val shortsTabs = rootNode.findAccessibilityNodeInfosByText("Shorts")
            for (tab in shortsTabs) {
                if (tab.isSelected && tab.isClickable) {
                    foundShorts = true
                }
                tab.recycle()
            }
        }

        if (foundShorts) {
            var homeClicked = false
            val homeTabs = rootNode.findAccessibilityNodeInfosByText("Home")
            for (tab in homeTabs) {
                if (tab.contentDescription?.toString()?.contains("Home", ignoreCase = true) == true) {
                    if (clickNodeOrParent(tab)) {
                        homeClicked = true
                    }
                }
                tab.recycle()
            }
            if (!homeClicked) {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            handler.post { Toast.makeText(applicationContext, "Shorts Blocked", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun handleInstagram(rootNode: AccessibilityNodeInfo, packageName: String) {
        var inReelsTab = false
        val reelsTabs = rootNode.findAccessibilityNodeInfosByText("Reels")
        for (tab in reelsTabs) {
            if (tab.isSelected && tab.contentDescription?.toString()?.contains("Reels", ignoreCase = true) == true) {
                inReelsTab = true
            }
            tab.recycle()
        }

        // If user explicitly clicks the Reels tab, kick them back to Home Tab
        if (inReelsTab) {
            overlayManager?.hideOverlay()
            var homeClicked = false
            val homeTabs = rootNode.findAccessibilityNodeInfosByText("Home")
            for (tab in homeTabs) {
                if (tab.contentDescription?.toString()?.contains("Home", ignoreCase = true) == true) {
                    if (clickNodeOrParent(tab)) {
                        homeClicked = true
                    }
                }
                tab.recycle()
            }
            if (!homeClicked) {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            return
        }

        // We are on the Home Tab (or Feed). Add Black Overlay over the Reel Video!
        val idsToTry = listOf(
            "com.instagram.android:id/clips_video_container",
            "$packageName:id/clips_video_container",
            "com.instagram.android:id/clips_viewer_view_pager",
            "$packageName:id/clips_viewer_view_pager"
        )
        
        var foundVideoRect: Rect? = null
        for (id in idsToTry) {
            val videoContainers = rootNode.findAccessibilityNodeInfosByViewId(id)
            for (video in videoContainers) {
                val rect = Rect()
                video.getBoundsInScreen(rect)
                // Filter out tiny or invalid bounds
                if (rect.height() > 200 && rect.width() > 200) {
                    if (foundVideoRect == null) {
                        foundVideoRect = rect
                    }
                }
                video.recycle()
            }
            if (foundVideoRect != null) break
        }

        if (foundVideoRect != null) {
            overlayManager?.showOverlay(foundVideoRect)
        } else {
            overlayManager?.hideOverlay()
        }
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                parent.recycle()
                return true
            }
            val oldParent = parent
            parent = parent.parent
            oldParent?.recycle()
        }
        return false
    }

    override fun onInterrupt() {
        overlayManager?.hideOverlay()
    }
}

class ReelOverlayManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    
    fun showOverlay(rect: Rect) {
        if (overlayView == null) {
            overlayView = FrameLayout(context).apply {
                setBackgroundColor(Color.BLACK)
                addView(TextView(context).apply {
                    text = "REEL BLOCKED"
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    textSize = 18f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                    gravity = Gravity.CENTER
                })
            }
            val params = WindowManager.LayoutParams(
                rect.width(),
                rect.height(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = rect.left
                y = rect.top
            }
            windowManager.addView(overlayView, params)
        } else {
            val params = overlayView!!.layoutParams as WindowManager.LayoutParams
            if (params.width != rect.width() || params.height != rect.height() || params.x != rect.left || params.y != rect.top) {
                params.width = rect.width()
                params.height = rect.height()
                params.x = rect.left
                params.y = rect.top
                windowManager.updateViewLayout(overlayView, params)
            }
        }
    }
    
    fun hideOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}
