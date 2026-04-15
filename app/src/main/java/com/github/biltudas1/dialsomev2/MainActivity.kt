package com.github.biltudas1.dialsomev2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import org.json.JSONObject
import android.media.AudioManager
import android.content.Context

class MainActivity : AppCompatActivity(), SignalingCallback {

    private lateinit var webRTCClient: WebRTCClient
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    // TODO: REPLACE THIS WITH YOUR COMPUTER'S LOCAL IP ADDRESS (e.g., 192.168.1.5)
    private val SIGNALING_SERVER_URL = "wss://nextserver.biltu.workers.dev"

    private lateinit var tvStatus: TextView
    private lateinit var etTargetUserId: EditText
    private lateinit var btnCall: Button
    private lateinit var btnAccept: Button
    private lateinit var btnEndCall: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        tvStatus = findViewById(R.id.tvStatus)
        etTargetUserId = findViewById(R.id.etTargetUserId)
        btnCall = findViewById(R.id.btnCall)
        btnAccept = findViewById(R.id.btnAccept)
        btnEndCall = findViewById(R.id.btnEndCall)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            initWebRTC()
            connectToSignalingServer()
        }

        btnCall.setOnClickListener {
            if (::webRTCClient.isInitialized) {
                tvStatus.text = "Status: Calling..."
                webRTCClient.createOffer()
            } else {
                Toast.makeText(this, "Please grant microphone permission first", Toast.LENGTH_SHORT).show()
            }
        }

        btnAccept.setOnClickListener {
            if (::webRTCClient.isInitialized) {
                tvStatus.text = "Status: Call Accepted"
                btnAccept.isEnabled = false
                // Note: The answer is handled automatically when receiving the offer
            }
        }

        btnEndCall.setOnClickListener {
            if (::webRTCClient.isInitialized) {
                tvStatus.text = "Status: Call Ended"
                webRTCClient.endCall()
                initWebRTC()
            }
        }
    }

    private fun initWebRTC() {
        webRTCClient = WebRTCClient(this, this)
        webRTCClient.createPeerConnection()
    }

    // --- WebSocket Logic ---
    private fun connectToSignalingServer() {
        val request = Request.Builder().url(SIGNALING_SERVER_URL).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runOnUiThread { tvStatus.text = "Status: Connected to Server" }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread { handleSignalingMessage(text) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("Signaling", "WebSocket Error", t)
                runOnUiThread { tvStatus.text = "Status: Server Connection Failed" }
            }
        })
    }

    private fun handleSignalingMessage(message: String) {
        val json = JSONObject(message)
        when (json.getString("type")) {
            "offer" -> {
                tvStatus.text = "Incoming Call..."
                btnAccept.isEnabled = true
                webRTCClient.handleRemoteOffer(json.getString("sdp"))
            }
            "answer" -> {
                tvStatus.text = "Call Connected!"
                webRTCClient.handleRemoteAnswer(json.getString("sdp"))
            }
            "candidate" -> {
                webRTCClient.addRemoteIceCandidate(
                    json.getString("sdpMid"),
                    json.getInt("sdpMLineIndex"),
                    json.getString("sdp")
                )
            }
        }
    }

    // --- SignalingCallback Implementation ---
    override fun onOfferCreated(sdp: String) {
        val json = JSONObject().apply {
            put("type", "offer")
            put("sdp", sdp)
        }
        webSocket?.send(json.toString())
    }

    override fun onAnswerCreated(sdp: String) {
        val json = JSONObject().apply {
            put("type", "answer")
            put("sdp", sdp)
        }
        webSocket?.send(json.toString())
    }

    override fun onIceCandidateCreated(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        val json = JSONObject().apply {
            put("type", "candidate")
            put("sdpMid", sdpMid)
            put("sdpMLineIndex", sdpMLineIndex)
            put("sdp", candidate)
        }
        webSocket?.send(json.toString())
    }

    override fun onCallEnded() {
        runOnUiThread {
            tvStatus.text = "Status: Call Ended/Disconnected"
            webRTCClient.endCall()
        }
    }
}