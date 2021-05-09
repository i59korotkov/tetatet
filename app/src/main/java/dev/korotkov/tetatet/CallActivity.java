package dev.korotkov.tetatet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CallActivity extends AppCompatActivity {

    DatabaseReference firebaseRef;

    String userId = "EtfAOpNqmcglBxNGy67Ce322pDn2";
    String otherId = "pR2AcoZoIkar4nU4ZFB6neAbmmT2";
    String callId;

    // Call data
    boolean isPeerConnected = false;

    WebView webView;

    String statusWaiting = "waiting";
    String statusNormal = "normal";
    String statusMuted = "muted";
    String statusDisconnected = "disconnected";
    String statusRejected = "rejected";

    // Current user
    TextView currentUserEmoji;
    TextView currentUserText;

    boolean isCurrentUserMuted = false;

    // Other user
    TextView otherUserEmoji;
    TextView otherUserText;

    boolean isAnotherUserMuted = false;

    // Emoji codes
    int emojiSmileClosed = 0x1F642;
    int emojiZipped = 0x1F910;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        firebaseRef = FirebaseDatabase.getInstance().getReference("calls");

        webView = (WebView) findViewById(R.id.call_webview);

        // Current user
        currentUserEmoji = (TextView) findViewById(R.id.call_current_user_emoji);
        currentUserText = (TextView) findViewById(R.id.call_current_user_text);

        // Other user
        otherUserEmoji = (TextView) findViewById(R.id.call_another_user_emoji);
        otherUserText = (TextView) findViewById(R.id.call_another_user_text);

        // Show big emojis
        //findViewById(R.id.call_current_user_emoji).setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //findViewById(R.id.call_another_user_emoji).setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        currentUserEmoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCurrentUserMuted = !isCurrentUserMuted;

                if (isCurrentUserMuted) {
                    firebaseRef.child(callId).child(userId).setValue(statusMuted);
                    //if (callId.equals(userId)) firebaseRef.child(callId).child("receiverStatus").setValue(statusMuted);
                    //else firebaseRef.child(callId).child("callerStatus").setValue(statusMuted);

                    callJavascriptFunction("javascript:toggleAudio(\"" + !isCurrentUserMuted + "\")");

                    currentUserEmoji.setText(getEmoji(emojiZipped));
                    currentUserText.setText("You (muted)");
                } else {
                    firebaseRef.child(callId).child(userId).setValue(statusNormal);
                    //if (callId.equals(userId)) firebaseRef.child(callId).child("receiverStatus").setValue(statusNormal);
                    //else firebaseRef.child(callId).child("callerStatus").setValue(statusNormal);

                    callJavascriptFunction("javascript:toggleAudio(\"" + !isCurrentUserMuted + "\")");

                    currentUserEmoji.setText(getEmoji(emojiSmileClosed));
                    currentUserText.setText("You");
                }
            }
        });

        findViewById(R.id.call_call_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCallRequest(otherId);
            }
        });

        setupWebView();
    }

    private void setupWebView() {

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.addJavascriptInterface(new JavascriptInterface(this), "Android");

        loadCall();
    }

    private void loadCall() {
        String filePath = "file:android_asset/call.html";
        webView.loadUrl(filePath);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                initializePeer();
            }
        });
    }

    private void initializePeer() {
        callJavascriptFunction("javascript:init(\"" + userId + "\")");
        firebaseRef.child(userId).child("caller").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() == null) return;

                callId = userId;
                firebaseRef.child(callId).child(userId).setValue(statusNormal);

                listenOtherUserStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendCallRequest(String receiverId) {
        if (!isPeerConnected) {
            Toast.makeText(this, "You are not connected. Check your internet", Toast.LENGTH_LONG).show();
            return;
        }

        firebaseRef.child(receiverId).child("caller").setValue(userId);
        firebaseRef.child(receiverId).child(userId).setValue(statusWaiting);
        firebaseRef.child(receiverId).child(receiverId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // TODO: Check if call is already started
                if (snapshot.getValue() != null && snapshot.getValue().toString().equals(statusNormal) && callId == null) {

                    callId = receiverId;
                    firebaseRef.child(callId).child(userId).setValue(statusNormal);
                    callJavascriptFunction("javascript:startCall(\"" + receiverId + "\")");

                    listenOtherUserStatus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenOtherUserStatus() {
        firebaseRef.child(callId).child(otherId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() == null) return;

                updateOtherUserStatus(snapshot.getValue().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void updateOtherUserStatus(String status) {
        switch (status) {
            case "normal":
                otherUserEmoji.setText(getEmoji(emojiSmileClosed));
                otherUserText.setText("Name");
                break;
            case "muted":
                otherUserEmoji.setText(getEmoji(emojiZipped));
                otherUserText.setText("Name (muted)");
                break;
        }
    }

    private String getEmoji(int unicode) {
        return new String(Character.toChars(unicode));
    }

    void onPeerConnected() {
        isPeerConnected = true;
    }

    private void callJavascriptFunction(String functionString) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.evaluateJavascript(functionString, null);
            }
        });
    }
}