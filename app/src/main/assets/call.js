let remoteAudio = document.getElementById("remote-audio")

var localStream;

var audioContext = null;

var localMeter = null;
var mediaLocalStreamSource = null;

var remoteMeter = null;
var remoteLocalStreamSource = null;

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

    audioContext = new AudioContext();

    listen()
}

function listen() {
    peer.on('call', (call) => {

        navigator.getUserMedia({
            audio: true,
        }, (stream) => {

            localStream = stream

            // Create local volume meter
            mediaLocalStreamSource = audioContext.createMediaStreamSource(stream);

            localMeter = createAudioMeter(audioContext);
            mediaLocalStreamSource.connect(localMeter);

            call.answer(stream)
            call.on('stream', (remoteStream) => {
                remoteAudio.srcObject = remoteStream

                // Create remote volume meter
                mediaRemoteStreamSource = audioContext.createMediaStreamSource(remoteStream);

                remoteMeter = createAudioMeter(audioContext);
                mediaRemoteStreamSource.connect(remoteMeter);
            })

        })

    })
}

/*
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
*/

function startCall(otherUserId) {
    navigator.getUserMedia({
        audio: true,
    }, (stream) => {

        localStream = stream

        // Create local volume meter
        mediaLocalStreamSource = audioContext.createMediaStreamSource(stream);

        localMeter = createAudioMeter(audioContext);
        mediaLocalStreamSource.connect(localMeter);

        const call = peer.call(otherUserId, stream)
        call.on('stream', (remoteStream) => {
            remoteAudio.srcObject = remoteStream

            // Create remote volume meter
            mediaRemoteStreamSource = audioContext.createMediaStreamSource(remoteStream);

            remoteMeter = createAudioMeter(audioContext);
            mediaRemoteStreamSource.connect(remoteMeter);
        })

    })
}

function getLocalLevel( time ) {
    // check if we're currently clipping
    if (localMeter.checkClipping())
        console.log("clipping");

    return localMeter.volume;
}

function getRemoteLevel( time ) {
    // check if we're currently clipping
    if (remoteMeter.checkClipping())
        console.log("clipping");

    return remoteMeter.volume;
}

/*
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
*/

function toggleAudio(b) {
    if (b == "true") {
        localStream.getAudioTracks()[0].enabled = true
    } else {
        localStream.getAudioTracks()[0].enabled = false
    }
}

function createAudioMeter(audioContext,clipLevel,averaging,clipLag) {
	var processor = audioContext.createScriptProcessor(512);
	processor.onaudioprocess = volumeAudioProcess;
	processor.clipping = false;
	processor.lastClip = 0;
	processor.volume = 0;
	processor.clipLevel = clipLevel || 0.98;
	processor.averaging = averaging || 0.95;
	processor.clipLag = clipLag || 750;

	// this will have no effect, since we don't copy the input to the output,
	// but works around a current Chrome bug.
	processor.connect(audioContext.destination);

	processor.checkClipping =
		function(){
			if (!this.clipping)
				return false;
			if ((this.lastClip + this.clipLag) < window.performance.now())
				this.clipping = false;
			return this.clipping;
		};

	processor.shutdown =
		function(){
			this.disconnect();
			this.onaudioprocess = null;
		};

	return processor;
}

function volumeAudioProcess( event ) {
	var buf = event.inputBuffer.getChannelData(0);
    var bufLength = buf.length;
	var sum = 0;
    var x;

	// Do a root-mean-square on the samples: sum up the squares...
    for (var i=0; i<bufLength; i++) {
    	x = buf[i];
    	if (Math.abs(x)>=this.clipLevel) {
    		this.clipping = true;
    		this.lastClip = window.performance.now();
    	}
    	sum += x * x;
    }

    // ... then take the square root of the sum.
    var rms =  Math.sqrt(sum / bufLength);

    // Now smooth this out with the averaging factor applied
    // to the previous sample - take the max here because we
    // want "fast attack, slow release."
    this.volume = Math.max(rms, this.volume*this.averaging);
}

//192.168.0.107