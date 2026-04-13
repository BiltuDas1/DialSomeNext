package com.github.biltudas1.dialsomev2

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "REJECT_CALL") {
            // Dismiss the ringing notification
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(1001) // INCOMING_CALL_NOTIF_ID

            // Reset states
            CallStateManager.isIncomingCallRinging = false
            CallStateManager.isCallActive = false

            // TODO: Here you will eventually make an API call to tell the server you declined the call.
        }
    }
}