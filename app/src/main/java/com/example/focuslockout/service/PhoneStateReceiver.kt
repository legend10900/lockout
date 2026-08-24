package com.example.focuslockout.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.example.focuslockout.MainActivity
import com.example.focuslockout.data.FocusStateManager

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING,
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    FocusStateManager.setCallActive(true)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    FocusStateManager.setCallActive(false)
                    // Re-engage focus lock if active
                    if (FocusStateManager.isFocusActive.value) {
                        val reengageIntent = Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        context.startActivity(reengageIntent)
                    }
                }
            }
        }
    }
}
