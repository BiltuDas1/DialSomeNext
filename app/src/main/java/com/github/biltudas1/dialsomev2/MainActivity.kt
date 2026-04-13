package com.github.biltudas1.dialsomev2

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var layoutHome: View
    private lateinit var layoutIncomingCall: View
    private lateinit var layoutActiveCall: View

    private lateinit var tvIncomingCallerName: TextView
    private lateinit var tvActiveCallerName: TextView

    private var currentRoomId: String? = null
    private var isMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure the screen wakes up and shows over the lock screen for incoming calls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_main)

        // Bind Views
        layoutHome = findViewById(R.id.layoutHome)
        layoutIncomingCall = findViewById(R.id.layoutIncomingCall)
        layoutActiveCall = findViewById(R.id.layoutActiveCall)
        tvIncomingCallerName = findViewById(R.id.tvIncomingCallerName)
        tvActiveCallerName = findViewById(R.id.tvActiveCallerName)

        setupClickListeners()

        // Handle the intent that launched the app
        handleIntent(intent)
    }

    // This catches intents if the activity is already running in the background
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val callerName = intent.getStringExtra("caller_name") ?: "Unknown"
        currentRoomId = intent.getStringExtra("room_id")

        when (action) {
            "SHOW_INCOMING_CALL" -> {
                Log.d("DialSomeUI", "Showing Incoming Call UI")
                tvIncomingCallerName.text = callerName
                showState(incoming = true)
            }
            "ACCEPT_CALL" -> {
                Log.d("DialSomeUI", "User accepted call from notification")
                acceptCall(callerName)
            }
            "RETURN_TO_CALL" -> {
                Log.d("DialSomeUI", "Returning to active call")
                showState(active = true)
            }
            else -> {
                // Default app launch, show home/contacts
                showState(home = true)
            }
        }
    }

    private fun setupClickListeners() {
        // Incoming Call Buttons
        findViewById<FloatingActionButton>(R.id.btnAccept).setOnClickListener {
            val callerName = tvIncomingCallerName.text.toString()
            acceptCall(callerName)
        }

        findViewById<FloatingActionButton>(R.id.btnDecline).setOnClickListener {
            rejectCall()
        }

        // Active Call Buttons
        findViewById<FloatingActionButton>(R.id.btnHangUp).setOnClickListener {
            endCall()
        }

        findViewById<ImageButton>(R.id.btnMute).setOnClickListener {
            isMuted = !isMuted
            // TODO: Call webRTCClient.setMute(isMuted) from Step 6 here
            it.alpha = if (isMuted) 0.5f else 1.0f
        }

        findViewById<ImageButton>(R.id.btnSpeaker).setOnClickListener {
            // TODO: Toggle AudioManager speakerphone state
        }
    }

    private fun acceptCall(callerName: String) {
        // 1. Dismiss the ringing notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(1001) // INCOMING_CALL_NOTIF_ID

        // 2. Update State
        CallStateManager.isIncomingCallRinging = false
        CallStateManager.isCallActive = true

        // 3. Update UI
        tvActiveCallerName.text = "Talking to $callerName"
        showState(active = true)

        // 4. Start Foreground Service to keep microphone alive (Step 7)
        val serviceIntent = Intent(this, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_START_FOREGROUND
            putExtra("caller_name", callerName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // 5. TODO: Initialize WebRTCClient (Step 6) and send Answer to Backend
    }

    private fun rejectCall() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(1001)

        CallStateManager.isIncomingCallRinging = false
        showState(home = true)

        // TODO: Send API request to backend saying call was rejected
    }

    private fun endCall() {
        CallStateManager.isCallActive = false
        showState(home = true)

        // Stop Foreground Service
        val stopIntent = Intent(this, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_STOP_FOREGROUND
        }
        startService(stopIntent)

        // TODO: Call webRTCClient.endCall() and tell Backend call ended
    }

    private fun showState(home: Boolean = false, incoming: Boolean = false, active: Boolean = false) {
        layoutHome.visibility = if (home) View.VISIBLE else View.GONE
        layoutIncomingCall.visibility = if (incoming) View.VISIBLE else View.GONE
        layoutActiveCall.visibility = if (active) View.VISIBLE else View.GONE
    }
}