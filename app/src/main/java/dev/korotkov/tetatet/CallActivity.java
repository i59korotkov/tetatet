package dev.korotkov.tetatet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
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

    boolean isOtherUserMuted = false;

    // Emoji codes
    int emojiSmileClosed = 0x1F642;
    int emojiSmileOpened = 0x1F600;
    int emojiZipped = 0x1F910;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        startBackgroundAnimation();

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

        // Set volume controls to music
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

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

        startSoundCheckRunnable();
    }

    private void startSoundCheckRunnable() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (callId != null) {
                        callJavascriptFunction("javascript:getLocalLevel()");
                        callJavascriptFunction("javascript:getRemoteLevel()");
                    }

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
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
                isOtherUserMuted = false;
                break;
            case "muted":
                otherUserEmoji.setText(getEmoji(emojiZipped));
                otherUserText.setText("Name (muted)");
                isOtherUserMuted = true;
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
                webView.evaluateJavascript(functionString, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        switch (functionString) {
                            case "javascript:getLocalLevel()":
                                changeCurrentUserEmoji(value);
                                break;
                            case "javascript:getRemoteLevel()":
                                changeOtherUserEmoji(value);
                                break;
                        }
                    }
                });
            }
        });
    }

    private void changeCurrentUserEmoji(String soundLevel) {
        if (soundLevel.equals("null") || isCurrentUserMuted) return;

        Double soundLevelDouble = Double.parseDouble(soundLevel);

        if (soundLevelDouble < 0.01) {
            currentUserEmoji.setText(getEmoji(emojiSmileClosed));
        } else {
            currentUserEmoji.setText(getEmoji(emojiSmileOpened));
        }
    }

    private void changeOtherUserEmoji(String soundLevel) {
        if (soundLevel.equals("null") || isOtherUserMuted) return;

        Double soundLevelDouble = Double.parseDouble(soundLevel);

        if (soundLevelDouble < 0.01) {
            otherUserEmoji.setText(getEmoji(emojiSmileClosed));
        } else {
            otherUserEmoji.setText(getEmoji(emojiSmileOpened));
        }
    }

    private void startBackgroundAnimation() {
        // Background gradient animation
        RelativeLayout registerLayout = findViewById(R.id.call_layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) registerLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();
    }
}