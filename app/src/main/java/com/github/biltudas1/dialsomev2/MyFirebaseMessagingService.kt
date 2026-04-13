package com.github.biltudas1.dialsomev2

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "DialSomeFCM"
        private const val INCOMING_CALL_NOTIF_ID = 1001
        private const val MISSED_CALL_NOTIF_ID = 1002
    }

    /**
     * Called when a new token is generated.
     * Replaces the old C++ notifyNewToken(token).
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token generated: $token")

        // Send to your Python backend using Retrofit and Coroutines
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // NOTE: Make sure your ApiClient is configured to hold DialsomeApiService
                ApiClient.apiService.updateFcmToken(FcmUpdateRequest(fcm_token = token))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send FCM token to server", e)
            }
        }
    }

    /**
     * Called when a message is received from the FastAPI backend.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (remoteMessage.data.isNotEmpty()) {
            val data = remoteMessage.data
            val type = data["type"]

            when (type) {
                "incoming_call" -> {
                    val roomId = data["room_id"] ?: return
                    val callerEmail = data["caller_email"] ?: "Unknown"
                    val callerName = data["caller_name"] ?: "Unknown"

                    Log.d(TAG, "FCM Signal: $type Room: $roomId From: $callerEmail($callerName)")
                    CallStateManager.isIncomingCallRinging = true
                    wakeUpApp(callerEmail, roomId, callerName)
                }
                "end_call" -> {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(INCOMING_CALL_NOTIF_ID)

                    val callerName = data["caller_name"] ?: "Unknown"

                    // Only show missed call if the call was NEVER answered
                    if (CallStateManager.isIncomingCallRinging && !CallStateManager.isCallActive) {
                        showMissedCallNotification(nm, callerName)
                    }

                    // Reset the state
                    CallStateManager.isIncomingCallRinging = false
                    CallStateManager.isCallActive = false

                    // Broadcast to UI that the call has ended (so the IncomingCallScreen can close)
                    val endCallIntent = Intent("CALL_ENDED_FCM")
                    sendBroadcast(endCallIntent)
                }
            }
        }
    }

    private fun showMissedCallNotification(nm: NotificationManager, callerName: String) {
        val channelId = "MissedCalls"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Missed Calls",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }

        val missedCallBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Missed Call")
            .setContentText("You missed a call from $callerName")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        nm.notify(MISSED_CALL_NOTIF_ID, missedCallBuilder.build())
    }

    private fun wakeUpApp(callerEmail: String, roomId: String, roomName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "IncomingCalls"

        // 1. Accept Intent (Opens MainActivity to answer)
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACCEPT_CALL"
            putExtra("room_id", roomId)
            putExtra("caller_email", callerEmail)
            putExtra("caller_name", roomName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val acceptPending = PendingIntent.getActivity(
            this, 10, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Reject Intent (Sent to a Broadcast Receiver)
        val rejectIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = "REJECT_CALL"
            putExtra("caller_email", callerEmail)
        }
        val rejectPending = PendingIntent.getBroadcast(
            this, 11, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Full-Screen Intent (Wakes up the screen)
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            action = "SHOW_INCOMING_CALL"
            putExtra("room_id", roomId)
            putExtra("caller_email", callerEmail)
            putExtra("caller_name", roomName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val fullScreenPending = PendingIntent.getActivity(
            this, 12, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
                setSound(ringtoneUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Incoming Call")
            .setContentText("Call from $roomName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(fullScreenPending)
            .setFullScreenIntent(fullScreenPending, true)
            .setSound(ringtoneUri)
            .setOngoing(true)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_call, "Accept", acceptPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reject", rejectPending)

        val notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT // Keeps the ringtone looping
        notificationManager.notify(INCOMING_CALL_NOTIF_ID, notification)
    }
}