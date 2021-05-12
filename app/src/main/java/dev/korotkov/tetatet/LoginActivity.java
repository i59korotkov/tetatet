package dev.korotkov.tetatet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    String[] permissions = {
            Manifest.permission.RECORD_AUDIO
    };
    int requestCode = 1;

    FirebaseAuth firebaseAuth;

    boolean isLoginLayoutActive = true;

    // Login linear layout
    LinearLayout loginLinearLayout;

    EditText loginEmailEditText;
    EditText loginPasswordEditText;

    Button loginButton;

    TextView loginToRegister;
    TextView resetPassword;

    // Register linear layout
    LinearLayout registerLinerLayout;

    EditText registerEmailEditText;
    EditText registerPasswordEditText;
    EditText registerPasswordConfirmEditText;

    Button registerButton;

    TextView registerToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        startBackgroundAnimation();

        // Check permissions
        if (!isPermissionGranted()) {
            askPermission();
        }

        firebaseAuth = FirebaseAuth.getInstance();

        // Login linear layout
        loginLinearLayout = (LinearLayout) findViewById(R.id.login_liner_layout);

        loginButton = (Button) findViewById(R.id.login_btn);

        loginEmailEditText = (EditText) findViewById(R.id.login_email);
        loginPasswordEditText = (EditText) findViewById(R.id.login_password);

        loginToRegister = (TextView) findViewById(R.id.login_to_register);
        resetPassword = (TextView) findViewById(R.id.login_reset_password);

        // Register linear layout
        registerLinerLayout = (LinearLayout) findViewById(R.id.register_liner_layout);

        registerButton = (Button) findViewById(R.id.register_btn);

        registerEmailEditText = (EditText) findViewById(R.id.register_email);
        registerPasswordEditText = (EditText) findViewById(R.id.register_password);
        registerPasswordConfirmEditText = (EditText) findViewById(R.id.register_password_confirm);

        registerToLogin = (TextView) findViewById(R.id.register_to_login);

        checkAuthentication();

        startLoginLayoutListeners();
        startRegisterLayoutListeners();
    }

    private void checkAuthentication() {
        // Set smooth transition for title when swapping between login and register layouts
        ((ViewGroup) findViewById(R.id.title_welcome)).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        // Set smooth transition for card
        ((ViewGroup) findViewById(R.id.bottom_card)).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        // Hide login layout
        loginLinearLayout.setVisibility(View.GONE);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // If the user is already logged in
            switchToAnotherActivity(currentUser);
        } else {
            // Show login layout if user is not logged int
            loginLinearLayout.setVisibility(View.VISIBLE);
            loginLinearLayout.startAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.show_up));
        }
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

    private void startLoginLayoutListeners() {
        loginToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoginLayoutActive) return;

                isLoginLayoutActive = false;

                // Hide login layout
                loginLinearLayout.startAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.hide_down));
                loginLinearLayout.setVisibility(View.GONE);

                // Show register layout
                registerLinerLayout.setVisibility(View.VISIBLE);
                registerLinerLayout.startAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.show_up));
            }
        });

        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoginLayoutActive) return;

                String email = loginEmailEditText.getText().toString().trim();

                // Check email
                if (email.isEmpty()) {
                    loginEmailEditText.setError("Email is required to reset password");
                    return;
                }

                // Send email to reset password
                firebaseAuth.sendPasswordResetEmail(email);

                makeDialogInfo("Password reset", "An email with instructions was sent to your address");
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoginLayoutActive) return;

                // Get login data
                String email = loginEmailEditText.getText().toString().trim();
                String password = loginPasswordEditText.getText().toString().trim();

                // Check login data
                if (email.isEmpty()) {
                    loginEmailEditText.setError("Email is required");
                    return;
                } else if (password.isEmpty()) {
                    loginPasswordEditText.setError("Password is required");
                    return;
                } else if (password.length() < 6) {
                    loginPasswordEditText.setError("Password is too short");
                    return;
                }

                // Hide login button to show the progress bar
                loginButton.setVisibility(View.INVISIBLE);

                // Login into account with FireBase
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        switchToAnotherActivity(FirebaseAuth.getInstance().getCurrentUser());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof FirebaseAuthInvalidUserException) {
                            loginEmailEditText.setError("This email is not registered");
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            loginPasswordEditText.setError("Wrong password");
                        } else {
                            makeDialogInfo("Error", "Something went wrong");
                        }

                        // Show login button
                        loginButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void startRegisterLayoutListeners() {
        registerToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoginLayoutActive) return;

                isLoginLayoutActive = true;

                // Hide register layout
                registerLinerLayout.startAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.hide_down));
                registerLinerLayout.setVisibility(View.GONE);

                // Show login layout
                loginLinearLayout.setVisibility(View.VISIBLE);
                loginLinearLayout.startAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.show_up));
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoginLayoutActive) return;

                // Get register data
                String email = registerEmailEditText.getText().toString().trim();
                String password = registerPasswordEditText.getText().toString().trim();
                String passwordConfirm = registerPasswordConfirmEditText.getText().toString().trim();

                // Check register data
                if (email.isEmpty()) {
                    registerEmailEditText.setError("Email is required");
                    return;
                } else if (password.isEmpty()) {
                    registerPasswordEditText.setError("Password is required");
                    return;
                } else if (password.length() < 6) {
                    registerPasswordEditText.setError("Password is too short");
                    return;
                } else if (passwordConfirm.isEmpty()) {
                    registerPasswordConfirmEditText.setError("Confirm your password");
                    return;
                } else if (!password.equals(passwordConfirm)) {
                    registerPasswordConfirmEditText.setError("Password mismatch");
                    return;
                }

                // Hide register button to show the progress bar
                registerButton.setVisibility(View.INVISIBLE);

                // Create user account in FireBase
                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Intent intent = new Intent(LoginActivity.this, EditAccountInfoActivity.class);
                        intent.putExtra("button_text", "Finish registration");
                        startActivity(intent);
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            registerEmailEditText.setError("Email is already registered");
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            registerEmailEditText.setError("Incorrect email format");
                        } else {
                            makeDialogInfo("Error", "Something went wrong");
                        }

                        // Show register button
                        registerButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
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

    private void switchToAnotherActivity(FirebaseUser firebaseUser) {
        DocumentReference userReference = FirebaseFirestore.getInstance().collection("users").document(firebaseUser.getUid());

        userReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.getResult().exists()) {
                    // If he completed the registration go to the Main Activity
                    startActivity(new Intent(LoginActivity.this, SearchActivity.class));
                } else {
                    // If he did not complete the registration go to the Register Activity
                    Intent intent = new Intent(LoginActivity.this, EditAccountInfoActivity.class);
                    intent.putExtra("button_text", "Finish registration");
                    startActivity(intent);
                    finish();
                }
                finish();
            }
        });
    }

    private void startBackgroundAnimation() {
        RelativeLayout registerLayout = findViewById(R.id.login_layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) registerLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();
    }
}