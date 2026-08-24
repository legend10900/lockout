package com.example.focuslockout

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.example.focuslockout.data.FocusStateManager
import com.example.focuslockout.ui.focus.FocusScreen
import com.example.focuslockout.ui.setup.SetupScreen
import com.example.focuslockout.theme.FocusLockoutTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Full screen, keep on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContent {
            FocusLockoutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isFocusActive by FocusStateManager.isFocusActive.collectAsState()
                    
                    if (isFocusActive) {
                        FocusScreen(activity = this)
                    } else {
                        SetupScreen(activity = this)
                    }
                }
            }
        }
    }
}
