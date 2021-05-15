package dev.korotkov.tetatet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
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

    String[] permissions = {
            Manifest.permission.RECORD_AUDIO
    };
    int requestCode = 1;

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

    // Emojis
    String emojiSmileClosed = "\uD83D\uDE42";
    String emojiSmileOpened = "\uD83D\uDE00";
    String emojiZipped = "\uD83E\uDD10";

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

        // Check permissions
        if (!isPermissionGranted()) {
            currentUserEmoji.setText(emojiZipped);
            isCurrentUserMuted = true;
            askPermission();
        }

        currentUserEmoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPermissionGranted()) {
                    makeDialogInfo("Warning", "The app needs access to your microphone. You can provide it in the settings");
                    return;
                }

                isCurrentUserMuted = !isCurrentUserMuted;

                if (isCurrentUserMuted) {
                    firebaseRef.child(callId).child(userId).setValue(statusMuted);
                    //if (callId.equals(userId)) firebaseRef.child(callId).child("receiverStatus").setValue(statusMuted);
                    //else firebaseRef.child(callId).child("callerStatus").setValue(statusMuted);

                    callJavascriptFunction("javascript:toggleAudio(\"" + !isCurrentUserMuted + "\")");

                    currentUserEmoji.setText(emojiZipped);
                    currentUserText.setText("You (muted)");
                } else {
                    firebaseRef.child(callId).child(userId).setValue(statusNormal);
                    //if (callId.equals(userId)) firebaseRef.child(callId).child("receiverStatus").setValue(statusNormal);
                    //else firebaseRef.child(callId).child("callerStatus").setValue(statusNormal);

                    callJavascriptFunction("javascript:toggleAudio(\"" + !isCurrentUserMuted + "\")");

                    currentUserEmoji.setText(emojiSmileClosed);
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

    private void askPermission() {
        ActivityCompat.requestPermissions(this, permissions, requestCode);

    }

    private boolean isPermissionGranted() {

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
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
                otherUserEmoji.setText(emojiSmileClosed);
                otherUserText.setText("Name");
                isOtherUserMuted = false;
                break;
            case "muted":
                otherUserEmoji.setText(emojiZipped);
                otherUserText.setText("Name (muted)");
                isOtherUserMuted = true;
                break;
        }
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
            currentUserEmoji.setText(emojiSmileClosed);
        } else {
            currentUserEmoji.setText(emojiSmileOpened);
        }
    }

    private void changeOtherUserEmoji(String soundLevel) {
        if (soundLevel.equals("null") || isOtherUserMuted) return;

        Double soundLevelDouble = Double.parseDouble(soundLevel);

        if (soundLevelDouble < 0.01) {
            otherUserEmoji.setText(emojiSmileClosed);
        } else {
            otherUserEmoji.setText(emojiSmileOpened);
        }
    }

    private void makeDialogInfo(String title, String description) {
        // Create dialog from layout
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_info);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_all_white_smaller_radius_background));

        // Change the title
        ((TextView) dialog.findViewById(R.id.dialog_info_title)).setText(title);

        // Change description
        ((TextView) dialog.findViewById(R.id.dialog_info_description)).setText(description);

        // Show dialog
        dialog.show();

        dialog.findViewById(R.id.dialog_info_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Close dialog
                dialog.dismiss();
            }
        });
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