package com.github.biltudas1.dialsomev2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class CallForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "OngoingCallChannel"
        private const val NOTIF_ID = 2001
        const val ACTION_START_FOREGROUND = "START_CALL_SERVICE"
        const val ACTION_STOP_FOREGROUND = "STOP_CALL_SERVICE"
        const val ACTION_HANG_UP = "HANG_UP_FROM_NOTIF"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> {
                val callerName = intent.getStringExtra("caller_name") ?: "Unknown"
                startOngoingCallNotification(callerName)
            }
            ACTION_HANG_UP -> {
                // Broadcast to your WebRTCClient / UI that the user pressed hang up
                sendBroadcast(Intent("com.github.biltudas1.dialsomev2.END_CALL_ACTION"))
                stopSelfService()
            }
            ACTION_STOP_FOREGROUND -> {
                stopSelfService()
            }
        }
        return START_STICKY
    }

    private fun startOngoingCallNotification(callerName: String) {
        createNotificationChannel()

        // 1. Intent to open the app back to the active call screen when tapped
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            action = "RETURN_TO_CALL"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Intent for the "Hang Up" button on the notification
        val hangUpIntent = Intent(this, CallForegroundService::class.java).apply {
            action = ACTION_HANG_UP
        }
        val hangUpPendingIntent = PendingIntent.getService(
            this, 1, hangUpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ongoing Call")
            .setContentText("Talking to $callerName")
            .setSmallIcon(android.R.drawable.ic_menu_call) // You can replace this with your own mic/call icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hang Up", hangUpPendingIntent)
            .build()

        // Android 14+ requires explicitly declaring the service type when starting it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun stopSelfService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ongoing Calls",
                NotificationManager.IMPORTANCE_LOW // LOW importance ensures it doesn't make a sound, just shows silently
            ).apply {
                description = "Keeps the microphone active during a call"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}