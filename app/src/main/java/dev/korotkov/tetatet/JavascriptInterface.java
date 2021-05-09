package dev.korotkov.tetatet;

public class JavascriptInterface {

    OldCallActivity oldCallActivity;

    public JavascriptInterface(OldCallActivity oldCallActivity) {
        this.oldCallActivity = oldCallActivity;
    }

    @android.webkit.JavascriptInterface
    public void onPeerConnected() {
        oldCallActivity.onPeerConnected();
    }

}
