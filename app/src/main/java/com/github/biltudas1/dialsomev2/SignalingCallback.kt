package com.github.biltudas1.dialsomev2

interface SignalingCallback {
    fun onOfferCreated(sdp: String)
    fun onAnswerCreated(sdp: String)
    fun onIceCandidateCreated(sdpMid: String, sdpMLineIndex: Int, candidate: String)
    fun onCallEnded()
}