let remoteAudio = document.getElementById("remote-audio")

let peer
function init(userId) {
    peer = new Peer(userId, {
        host: '192.168.0.107',
        port: 9000,
        path: '/tetatet'
    })

    peer.on('open', () => {
        Android.onPeerConnected()
    })

    listen()
}

let localStream
function listen() {
    peer.on('call', (call) => {

        navigator.getUserMedia({
            audio: true, 
        }, (stream) => {

            localStream = stream

            call.answer(stream)
            call.on('stream', (remoteStream) => {
                remoteAudio.srcObject = remoteStream
            })

        })
        
    })
}

function startCall(otherUserId) {
    navigator.getUserMedia({
        audio: true,
    }, (stream) => {

        localStream = stream

        const call = peer.call(otherUserId, stream)
        call.on('stream', (remoteStream) => {
            remoteAudio.srcObject = remoteStream
        })

    })
}

function toggleAudio(b) {
    if (b == "true") {
        localStream.getAudioTracks()[0].enabled = true
    } else {
        localStream.getAudioTracks()[0].enabled = false
    }
} 

//192.168.0.107