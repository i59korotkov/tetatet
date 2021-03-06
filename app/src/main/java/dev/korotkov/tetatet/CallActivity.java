package dev.korotkov.tetatet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CallActivity extends AppCompatActivity {

    DatabaseReference firebaseRef;

    String currentUserId;
    UserData currentUserData;
    String otherUserId;
    UserData otherUserData;
    String callId;

    TextView finishBtn;

    // Call data
    boolean isPeerConnected = false;
    boolean callStarted = false;

    WebView webView;

    // Current user
    TextView currentUserEmoji;
    TextView currentUserText;

    boolean currentUserMuted = false;

    // Other user
    TextView otherUserEmoji;
    TextView otherUserText;

    String otherUserStatus = Status.NORMAL.name();

    // Emojis
    String emojiSmileClosed = "\uD83D\uDE42";
    String emojiSmileOpened = "\uD83D\uDE00";
    String emojiZipped = "\uD83E\uDD10";
    String emojiDead = "\uD83D\uDE35";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        startBackgroundAnimation();

        firebaseRef = FirebaseDatabase.getInstance().getReference("calls");

        // Get data from intent
        currentUserId = getIntent().getStringExtra("current_id");
        currentUserData = (UserData) getIntent().getSerializableExtra("current_data");
        otherUserId = getIntent().getStringExtra("other_id");
        otherUserData = (UserData) getIntent().getSerializableExtra("other_data");
        callId = getIntent().getStringExtra("call_id");

        webView = (WebView) findViewById(R.id.call_webview);

        finishBtn = (TextView) findViewById(R.id.finish_btn);

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

        setupWebView();

        currentUserEmoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentUserMuted = !currentUserMuted;

                if (currentUserMuted) {
                    firebaseRef.child(callId).child(currentUserId).setValue(Status.MUTED);
                    //if (callId.equals(userId)) firebaseRef.child(callId).child("receiverStatus").setValue(statusMuted);
                    //else firebaseRef.child(callId).child("callerStatus").setValue(statusMuted);

                    callJavascriptFunction("javascript:toggleAudio(\"" + !currentUserMuted + "\")");

                    currentUserEmoji.setText(emojiZipped);
                    currentUserText.setText("You are muted");
                } else {
                    firebaseRef.child(callId).child(currentUserId).setValue(Status.NORMAL);
                    //if (callId.equals(userId)) firebaseRef.child(callId).child("receiverStatus").setValue(statusNormal);
                    //else firebaseRef.child(callId).child("callerStatus").setValue(statusNormal);

                    callJavascriptFunction("javascript:toggleAudio(\"" + !currentUserMuted + "\")");

                    currentUserEmoji.setText(emojiSmileClosed);
                    currentUserText.setText("You");
                }
            }
        });

        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        startSoundCheckRunnable();
    }

    private void startCall() {
        firebaseRef.child(callId).child(currentUserId).setValue(Status.WAITING);
        firebaseRef.child(callId).child(otherUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!callStarted && snapshot.getValue() != null && snapshot.getValue().toString().equals(Status.NORMAL.name())) {
                    if (!isPeerConnected) {
                        //Toast.makeText(CallActivity.this, "You are not connected. Check your internet", Toast.LENGTH_LONG).show();
                        return;
                    }

                    callJavascriptFunction("javascript:startCall(\"" + otherUserId + "\")");

                    callStarted = true;

                    firebaseRef.child(callId).child(currentUserId).setValue(Status.NORMAL);

                    listenOtherUserStatus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void answerCall() {
        firebaseRef.child(callId).child(currentUserId).setValue(Status.NORMAL);

        listenOtherUserStatus();
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
        callJavascriptFunction("javascript:init(\"" + currentUserId + "\")");

        // TODO: Maybe remove to another place?
        // When peer is ready, check if current user is caller
        if (callId.equals(currentUserId)) {
            answerCall();
        } else {
            startCall();
        }
    }

    private void listenOtherUserStatus() {
        firebaseRef.child(callId).child(otherUserId).addValueEventListener(new ValueEventListener() {
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
        if (status.equals(Status.NORMAL.name()))
        {
            otherUserEmoji.setText(emojiSmileClosed);
            otherUserText.setText(otherUserData.getName());
            otherUserStatus = status;
        }
        else if (status.equals(Status.MUTED.name()))
        {
            otherUserEmoji.setText(emojiZipped);
            otherUserText.setText(otherUserData.getName() + " is muted");
            otherUserStatus = status;
        }
        else if (status.equals(Status.DISCONNECTED.name()))
        {
            // Make vibration
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));

            otherUserEmoji.setText(emojiDead);
            otherUserText.setText(otherUserData.getName() + " disconnected");
            otherUserStatus = status;
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
        if (soundLevel.equals("null") || currentUserMuted) return;

        Double soundLevelDouble = Double.parseDouble(soundLevel);

        if (soundLevelDouble < 0.01) {
            currentUserEmoji.setText(emojiSmileClosed);
        } else {
            currentUserEmoji.setText(emojiSmileOpened);
        }
    }

    private void changeOtherUserEmoji(String soundLevel) {
        if (soundLevel.equals("null") || !otherUserStatus.equals(Status.NORMAL.name())) return;

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

    /*
    @Override
    protected void onPause() {
        super.onPause();

        if (!currentUserMuted) {
            firebaseRef.child(callId).child(currentUserId).setValue(statusMuted);
            //if (callId.equals(userId)) firebaseRef.child(callId).child("receiverStatus").setValue(statusMuted);
            //else firebaseRef.child(callId).child("callerStatus").setValue(statusMuted);

            callJavascriptFunction("javascript:toggleAudio(\"" + !currentUserMuted + "\")");

            currentUserEmoji.setText(emojiZipped);
            currentUserText.setText("You are muted");
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (!currentUserMuted) {
            firebaseRef.child(callId).child(currentUserId).setValue(statusNormal);
            //if (callId.equals(userId)) firebaseRef.child(callId).child("receiverStatus").setValue(statusNormal);
            //else firebaseRef.child(callId).child("callerStatus").setValue(statusNormal);

            callJavascriptFunction("javascript:toggleAudio(\"" + !currentUserMuted + "\")");

            currentUserEmoji.setText(emojiSmileClosed);
            currentUserText.setText("You");
        }
    }
    */

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        firebaseRef.child(callId).child(otherUserId).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue().toString().equals(Status.DISCONNECTED.name())) {
                    // Remove call from database
                    firebaseRef.child(callId).removeValue();
                } else {
                    // Set current user status to disconnected
                    firebaseRef.child(callId).child(currentUserId).setValue(Status.DISCONNECTED);
                }
            }
        });
        webView.loadUrl("about:blank");

        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        // Slide animation
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
    }
}