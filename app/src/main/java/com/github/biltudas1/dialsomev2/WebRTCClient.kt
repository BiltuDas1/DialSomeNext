package com.github.biltudas1.dialsomev2

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRTCClient(
    private val context: Context,
    private val signalingCallback: SignalingCallback
) {
    companion object {
        private const val TAG = "WebRTCClient"
        // Use standard Google STUN servers to bypass NAT
        private val STUN_SERVERS = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )
    }

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    init {
        initWebRTC()
    }

    /**
     * Step 1: Initialize the PeerConnectionFactory and Audio Devices
     */
    private fun initWebRTC() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        // Setup JavaAudioDeviceModule for Echo Cancellation and Noise Suppression
        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(JavaAudioDeviceModule.isBuiltInAcousticEchoCancelerSupported())
            .setUseHardwareNoiseSuppressor(JavaAudioDeviceModule.isBuiltInNoiseSuppressorSupported())
            .createAudioDeviceModule()

        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()

        audioDeviceModule.release() // Factory takes ownership
    }

    /**
     * Step 2: Create the Peer Connection
     */
    fun createPeerConnection() {
        val config = PeerConnection.RTCConfiguration(STUN_SERVERS)

        peerConnection = factory?.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                Log.d(TAG, "New ICE Candidate generated")
                signalingCallback.onIceCandidateCreated(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                Log.d(TAG, "ICE Connection State: $state")
                if (state == PeerConnection.IceConnectionState.DISCONNECTED || state == PeerConnection.IceConnectionState.FAILED) {
                    signalingCallback.onCallEnded()
                }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(dataChannel: DataChannel) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
                Log.d(TAG, "Remote audio track received")
                // Track is automatically played by WebRTC's audio engine
            }
        })

        createLocalAudioTrack()
    }

    /**
     * Step 3: Capture the microphone
     */
    private fun createLocalAudioTrack() {
        val audioConstraints = MediaConstraints()
        audioSource = factory?.createAudioSource(audioConstraints)
        localAudioTrack = factory?.createAudioTrack("ARDAMSa0", audioSource)

        localAudioTrack?.setEnabled(true)

        // Add our microphone track to the connection
        val mediaStreamIds = listOf("ARDAMS")
        peerConnection?.addTrack(localAudioTrack, mediaStreamIds)
    }

    /**
     * Step 4: Call this when making an OUTGOING call
     */
    fun createOffer() {
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                peerConnection?.setLocalDescription(this, sessionDescription)
                signalingCallback.onOfferCreated(sessionDescription.description)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) { Log.e(TAG, "Offer creation failed: $error") }
            override fun onSetFailure(error: String) { Log.e(TAG, "Set local description failed: $error") }
        }, MediaConstraints())
    }

    /**
     * Step 5: Call this when receiving an INCOMING call offer
     */
    fun handleRemoteOffer(sdp: String) {
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                // Once remote offer is set, create our answer
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answerDescription: SessionDescription) {
                        peerConnection?.setLocalDescription(this, answerDescription)
                        signalingCallback.onAnswerCreated(answerDescription.description)
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(e: String) {}
                    override fun onSetFailure(e: String) {}
                }, MediaConstraints())
            }
            override fun onSetFailure(e: String) {}
            override fun onCreateSuccess(desc: SessionDescription) {}
            override fun onCreateFailure(e: String) {}
        }, sessionDescription)
    }

    /**
     * Step 6: Handle incoming Answers and ICE candidates from the backend
     */
    fun handleRemoteAnswer(sdp: String) {
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onSetFailure(e: String) {}
            override fun onCreateSuccess(desc: SessionDescription) {}
            override fun onCreateFailure(e: String) {}
        }, sessionDescription)
    }

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        peerConnection?.addIceCandidate(candidate)
    }

    /**
     * Utilities: Mute and Cleanup
     */
    fun setMute(isMuted: Boolean) {
        localAudioTrack?.setEnabled(!isMuted)
    }

    fun endCall() {
        peerConnection?.close()
        peerConnection = null
        audioSource?.dispose()
        audioSource = null
        localAudioTrack?.dispose()
        localAudioTrack = null
    }
}