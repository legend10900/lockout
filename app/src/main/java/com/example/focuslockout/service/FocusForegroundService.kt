package com.example.focuslockout.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.focuslockout.MainActivity
import com.example.focuslockout.R
import com.example.focuslockout.data.FocusStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusForegroundService : Service() {

    private val CHANNEL_ID = "focus_channel_id"
    private val NOTIFICATION_ID = 1
    
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null
    
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FocusLockout::TimerWakeLock")
        wakeLock?.acquire(10 * 60 * 60 * 1000L /*10 hours max*/)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_FOCUS) {
            stopFocus()
            return START_NOT_STICKY
        }

        val durationMillis = intent?.getLongExtra(EXTRA_DURATION_MILLIS, 0L) ?: 0L
        if (durationMillis <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(durationMillis))
        
        FocusStateManager.setTotalDurationMillis(durationMillis)
        FocusStateManager.setRemainingTimeMillis(durationMillis)
        FocusStateManager.setFocusActive(true)

        startTimer(durationMillis)

        return START_STICKY
    }

    private fun startTimer(durationMillis: Long) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            val endTime = System.currentTimeMillis() + durationMillis
            while (isActive) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    FocusStateManager.setRemainingTimeMillis(0)
                    onFocusComplete()
                    break
                }
                
                FocusStateManager.setRemainingTimeMillis(remaining)
                updateNotification(remaining)
                
                delay(1000)
            }
        }
    }

    private fun onFocusComplete() {
        // Haptic feedback
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 200, 100, 200), -1)
        }

        // Play sound
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stopFocus()
    }

    private fun stopFocus() {
        FocusStateManager.setFocusActive(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(remainingMillis: Long): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        
        val hours = (remainingMillis / (1000 * 60 * 60))
        val minutes = (remainingMillis / (1000 * 60)) % 60
        val seconds = (remainingMillis / 1000) % 60
        val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Mode Active")
            .setContentText("Remaining time: $timeStr")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(remainingMillis: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(remainingMillis))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Mode Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        timerJob?.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP_FOCUS = "com.example.focuslockout.action.STOP_FOCUS"
        const val EXTRA_DURATION_MILLIS = "extra_duration_millis"
    }
}
