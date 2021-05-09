package dev.korotkov.tetatet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class OldCallActivity extends AppCompatActivity {

    String[] permissions = {
            Manifest.permission.RECORD_AUDIO
    };
    int requestCode = 1;

    String username;
    String uniqueId;
    String friendsUsername;

    boolean isPeerConnected = false;

    DatabaseReference firebaseRef;
    FirebaseAuth firebaseAuth;

    boolean isAudio = true;

    Button callBtn;
    ImageView toggleAudioBtn;

    WebView webView;

    RelativeLayout callLayout;
    TextView incomingCallTxt;
    ImageView acceptBtn;
    ImageView rejectBtn;

    RelativeLayout inputLayout;
    EditText friendsNameEdit;

    LinearLayout callControlLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_old_call);

        if (!isPermissionGranted()) {
            askPermission();
        }

        firebaseRef = FirebaseDatabase.getInstance().getReference("users");
        firebaseAuth = FirebaseAuth.getInstance();

        callBtn = (Button) findViewById(R.id.callBtn);
        toggleAudioBtn = (ImageView) findViewById(R.id.toggleAudioBtn);

        webView = (WebView) findViewById(R.id.webView);

        callLayout = (RelativeLayout) findViewById(R.id.callLayout);
        incomingCallTxt = (TextView) findViewById(R.id.incomingCallTxt);
        acceptBtn = (ImageView) findViewById(R.id.acceptBtn);
        rejectBtn = (ImageView) findViewById(R.id.rejectBtn);

        inputLayout = (RelativeLayout) findViewById(R.id.inputLayout);
        friendsNameEdit = (EditText) findViewById(R.id.friendNameEdit);

        callControlLayout = (LinearLayout) findViewById(R.id.callControlLayout);

        //username = getIntent().getStringExtra("username");
        username = firebaseAuth.getUid();

        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                friendsUsername = friendsNameEdit.getText().toString();
                sendCallRequest();
            }
        });

        toggleAudioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                isAudio = !isAudio;
                callJavascriptFunction("javascript:toggleAudio(\"" + isAudio + "\")");
                toggleAudioBtn.setImageResource(isAudio ? R.drawable.ic_baseline_mic_24 : R.drawable.ic_baseline_mic_off_24);
            }
        });

        setupWebView();
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

    private void sendCallRequest() {
        if (!isPeerConnected) {
            Toast.makeText(this, "You are not connected. Check your internet", Toast.LENGTH_LONG).show();
            return;
        }

        firebaseRef.child(friendsUsername).child("incoming").setValue(username);
        firebaseRef.child(friendsUsername).child("isAvailable").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null && snapshot.getValue().toString() == "true") {
                    listenForConnId();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForConnId() {
        firebaseRef.child(friendsUsername).child("connId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() == null)
                    return;

                switchToControls();

                callJavascriptFunction("javascript:startCall(\"" + snapshot.getValue().toString() + "\")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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
        //webView.addJavascriptInterface(new JavascriptInterface(this), "Android");

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
        uniqueId = getUniqueId();

        callJavascriptFunction("javascript:init(\"" + uniqueId + "\")");
        firebaseRef.child(username).child("incoming").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                onCallRequest((String) snapshot.getValue());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void onCallRequest(String caller) {
        if (caller == null) return;

        callLayout.setVisibility(View.VISIBLE);
        incomingCallTxt.setText(caller + " is calling...");

        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseRef.child(username).child("connId").setValue(uniqueId);
                firebaseRef.child(username).child("isAvailable").setValue(true);

                callLayout.setVisibility(View.GONE);
                switchToControls();
            }
        });

        rejectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseRef.child(username).child("incoming").setValue(null);
                callLayout.setVisibility(View.GONE);
            }
        });
    }

    private void switchToControls() {
        inputLayout.setVisibility(View.GONE);
        callControlLayout.setVisibility(View.VISIBLE);
    }

    private String getUniqueId() {
        return firebaseAuth.getUid();
    }

    private void callJavascriptFunction(String functionString) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                //Toast.makeText(getApplicationContext(), functionString, Toast.LENGTH_LONG).show();

                webView.evaluateJavascript(functionString, null);
            }
        });
    }

    void onPeerConnected() {
        isPeerConnected = true;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        firebaseRef.child(username).setValue(null);
        webView.loadUrl("about:blank");
        super.onDestroy();
    }
}