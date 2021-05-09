package dev.korotkov.tetatet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.LayoutTransition;
import android.content.Intent;
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
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class LoginActivity extends AppCompatActivity {

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
    protected void onStart() {
        super.onStart();

        // If the user is already logged in, than skip this activity
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, OldCallActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        startBackgroundAnimation();

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

        // Set smooth transition for title when swapping between login and register layouts
        ((ViewGroup) findViewById(R.id.title_welcome)).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        // Set smooth transition for card
        ((ViewGroup) findViewById(R.id.bottom_card)).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        startLoginLayoutListeners();
        startRegisterLayoutListeners();
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

                Toast.makeText(LoginActivity.this, "Check your email address for instructions", Toast.LENGTH_LONG).show();
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
                        startActivity(new Intent(LoginActivity.this, OldCallActivity.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof FirebaseAuthInvalidUserException) {
                            loginEmailEditText.setError("This email is not registered");
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            loginPasswordEditText.setError("Wrong password");
                        } else {
                            //Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                            Toast.makeText(LoginActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
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
                        startActivity(new Intent(LoginActivity.this, OldCallActivity.class));
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
                            //Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                            Toast.makeText(LoginActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                        }

                        // Show register button
                        registerButton.setVisibility(View.VISIBLE);
                    }
                });
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