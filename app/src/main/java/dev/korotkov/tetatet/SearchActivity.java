package dev.korotkov.tetatet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.LayoutTransition;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ThrowOnExtraProperties;

import java.util.ArrayList;
import java.util.Collection;

public class SearchActivity extends AppCompatActivity implements View.OnClickListener {

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    DatabaseReference firebaseRef;

    Boolean isBusy = false;

    String userId;
    UserData currentUserData;

    String incomingOfferId;
    UserData incomingOfferData;

    //
    RelativeLayout userCardView;
    LinearLayout bottomUserInfo;

    // User card views
    TextView avatarTextView;
    TextView mainInfoTextView;
    TextView languagesTextView;

    // Other views
    TextView interestsTextView;

    TextView descriptionTextView;

    // Start controls
    RelativeLayout startControls;
    TextView editBtn;
    TextView startBtn;

    // Search controls
    RelativeLayout searchControls;
    TextView callBtn;
    TextView skipBtn;
    TextView stopBtn;

    // Data lists from database
    ArrayList<ItemWithEmoji> avatars = new ArrayList<>();
    ArrayList<ItemWithEmoji> interests = new ArrayList<>();
    ArrayList<ItemWithEmoji> languages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        startBackgroundAnimation();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseRef = FirebaseDatabase.getInstance().getReference("search");

        // Load data from database
        loadInterestsFromDatabase();
        loadLanguagesFromDatabase();
        loadAvatarsFromDatabase();

        userId = firebaseAuth.getCurrentUser().getUid();

        avatarTextView = (TextView) findViewById(R.id.search_avatar);
        mainInfoTextView = (TextView) findViewById(R.id.search_main_info);
        languagesTextView = (TextView) findViewById(R.id.search_languages);

        interestsTextView = (TextView) findViewById(R.id.search_interests);

        descriptionTextView = (TextView) findViewById(R.id.search_description);

        startControls = (RelativeLayout) findViewById(R.id.start_controls);
        editBtn = (TextView) findViewById(R.id.search_edit_data);
        startBtn = (TextView) findViewById(R.id.search_start_btn);

        searchControls = (RelativeLayout) findViewById(R.id.search_controls);
        callBtn = (TextView) findViewById(R.id.search_call_btn);
        skipBtn = (TextView) findViewById(R.id.search_skip_btn);
        stopBtn = (TextView) findViewById(R.id.search_stop_btn);

        userCardView = (RelativeLayout) findViewById(R.id.search_user_card);
        bottomUserInfo = (LinearLayout) findViewById(R.id.search_bottom_user_info);

        // Set smooth transition
        userCardView.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        bottomUserInfo.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        // TODO Remove
        findViewById(R.id.test1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SearchActivity.this, "Test1", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.test2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SearchActivity.this, "Test2", Toast.LENGTH_SHORT).show();
            }
        });

        hideUserCard();

        // Set current user info 100 millis after activity started
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setCurrentUserCardData();
            }
        }, 200);

        editBtn.setOnClickListener(this);
        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        skipBtn.setOnClickListener(this);
        callBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.search_start_btn:
                Toast.makeText(SearchActivity.this, "Start btn clicked", Toast.LENGTH_SHORT).show();

                hideUserCard();

                // Add user to database
                firebaseRef.child(userId).child("status").setValue("searching");

                // Start listening for incoming offer
                listenForIncomingOffer();

                // Offer call to random user
                firebaseRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot == null || snapshot.getChildrenCount() <= 1) return;

                        ArrayList<String> searchingUsers = new ArrayList<>();

                        for (DataSnapshot user : snapshot.getChildren()) {
                            searchingUsers.add(user.getValue().toString());
                        }

                        String randomUserId = searchingUsers.get((int) Math.random() * searchingUsers.size());

                        firebaseRef.child(randomUserId).child("offer").setValue(userId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
                break;
            case R.id.search_stop_btn:
                Toast.makeText(SearchActivity.this, "Stop btn clicked", Toast.LENGTH_SHORT).show();

                hideUserCard();

                // Stop listening for incoming offers
                firebaseRef.child(userId).child("status").setValue("stopped");

                // Remove user from database
                firebaseRef.child(userId).removeValue();

                changeSearchControls();

                setCurrentUserCardData();

                showUserCard();
                break;
            case R.id.search_skip_btn:
                Toast.makeText(SearchActivity.this, "Skip btn clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.search_call_btn:
                Toast.makeText(SearchActivity.this, "Call btn clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.search_edit_data:
                Intent intent = new Intent(SearchActivity.this, EditAccountInfoActivity.class);
                intent.putExtra("button_text", "Save changes");
                startActivity(intent);
                break;
        }
    }

    private void listenForIncomingOffer() {
        firebaseRef.child(userId).child("offer").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() == null || snapshot.getValue().toString().equals("searching") || snapshot.getValue().toString().equals("stopped"))
                    return;

                isBusy = true;

                String otherUserId = snapshot.getValue().toString();
                incomingOfferId = otherUserId;

                DocumentReference otherUserDocumentReference = firebaseFirestore.collection("users").document(otherUserId);

                otherUserDocumentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        // Get user data from document
                        incomingOfferData = documentSnapshot.toObject(UserData.class);

                        // Change search controls
                        changeSearchControls();

                        // Show incoming user
                        setUserCardData(incomingOfferData);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        isBusy = false;
                        // Ignore incoming offer if error occurs
                        firebaseRef.child(userId).child("status").setValue("searching");
                        listenForIncomingOffer();
                    }
                });

                // Remove listener
                firebaseRef.child(userId).child("offer").removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setUserCardData(UserData userData) {
        String userAvatar = "\uD83E\uDDD1";
        for (ItemWithEmoji avatar : avatars) {
            if (userData.getAvatarId().equals(avatar.getId())) userAvatar = avatar.getEmoji();
        }

        String commonInterests = "";
        for (ItemWithEmoji interest : interests) {
            // TODO: Add common interests
            if (userData.getInterestsIds().contains(interest.getId())) commonInterests += " " + interest.getEmoji();
        }

        String userLanguages = "";
        for (ItemWithEmoji language : languages) {
            if (userData.getLanguagesIds().contains(language.getId())) userLanguages += language.getEmoji() + " ";
        }

        // Set user data to view
        avatarTextView.setText(userAvatar);
        mainInfoTextView.setText(userData.getName() + ", " + userData.getAge().toString());
        languagesTextView.setText(userLanguages);

        if (commonInterests.isEmpty()) interestsTextView.setText("You do not have any common interests");
        else interestsTextView.setText("Common interests:" + commonInterests);

        if (userData.getDescription() == null || userData.getDescription().isEmpty()) {
            descriptionTextView.setVisibility(View.GONE);
        } else {
            descriptionTextView.setVisibility(View.VISIBLE);
            descriptionTextView.setText(userData.getDescription());
        }

        showUserCard();
    }

    private void hideUserCard() {
        userCardView.startAnimation(AnimationUtils.loadAnimation(SearchActivity.this, R.anim.hide_down));
        userCardView.setVisibility(View.GONE);

        bottomUserInfo.startAnimation(AnimationUtils.loadAnimation(SearchActivity.this, R.anim.hide_down));
        bottomUserInfo.setVisibility(View.GONE);
    }

    private void showUserCard() {
        userCardView.setVisibility(View.VISIBLE);
        userCardView.startAnimation(AnimationUtils.loadAnimation(SearchActivity.this, R.anim.show_up));

        bottomUserInfo.setVisibility(View.VISIBLE);
        bottomUserInfo.startAnimation(AnimationUtils.loadAnimation(SearchActivity.this, R.anim.show_up));
    }

    private void changeSearchControls() {
        if (startControls.getVisibility() == View.VISIBLE) {
            startControls.setVisibility(View.GONE);
            searchControls.setVisibility(View.VISIBLE);
        } else {
            startControls.setVisibility(View.VISIBLE);
            searchControls.setVisibility(View.GONE);
        }
    }

    private void setCurrentUserCardData() {
        DocumentReference userDocumentReference = firebaseFirestore.collection("users").document(userId);

        userDocumentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // Get user data from document
                currentUserData = documentSnapshot.toObject(UserData.class);

                String userAvatar = "\uD83D\uDC64";
                for (ItemWithEmoji avatar : avatars) {
                    if (currentUserData.getAvatarId().equals(avatar.getId())) userAvatar = avatar.getEmoji();
                }

                String userInterests = "";
                for (ItemWithEmoji interest : interests) {
                    if (currentUserData.getInterestsIds().contains(interest.getId())) userInterests += " " + interest.getEmoji();
                }

                String userLanguages = "";
                for (ItemWithEmoji language : languages) {
                    if (currentUserData.getLanguagesIds().contains(language.getId())) userLanguages += language.getEmoji() + " ";
                }

                // Set user data to view
                avatarTextView.setText(userAvatar);
                mainInfoTextView.setText(currentUserData.getName() + ", " + currentUserData.getAge().toString());
                languagesTextView.setText(userLanguages);

                if (userInterests.isEmpty()) interestsTextView.setText("You have not chosen any interest");
                else interestsTextView.setText("Your interests:" + userInterests);

                if (currentUserData.getDescription() == null || currentUserData.getDescription().isEmpty()) descriptionTextView.setVisibility(View.GONE);
                else descriptionTextView.setText(currentUserData.getDescription());

                showUserCard();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                makeDialogInfo("Error", e.getMessage());
            }
        });
    }

    private void loadInterestsFromDatabase() {
        firebaseFirestore.collection("interests").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult().getDocuments()) {
                        interests.add(new ItemWithEmoji(document.getId(), document.getString("name"), document.getString("emoji")));
                    }
                } else {
                    makeDialogInfo("Error", "Cannot get interests list from database");
                    Log.i("INTERESTS", task.getException().getMessage());
                }
            }
        });
    }

    private void loadLanguagesFromDatabase() {
        firebaseFirestore.collection("languages").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult().getDocuments()) {
                        languages.add(new ItemWithEmoji(document.getId(), document.getString("name"), document.getString("emoji")));
                    }
                } else {
                    makeDialogInfo("Error", "Cannot get languages list from database");
                    Log.i("LANGUAGES", task.getException().getMessage());
                }
            }
        });
    }

    private void loadAvatarsFromDatabase() {
        firebaseFirestore.collection("avatars").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult().getDocuments()) {
                        avatars.add(new ItemWithEmoji(document.getId(), document.getString("name"), document.getString("emoji")));
                    }
                } else {
                    makeDialogInfo("Error", "Cannot get languages list from database");
                    Log.i("LANGUAGES", task.getException().getMessage());
                }
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

    private void startBackgroundAnimation() {
        RelativeLayout registerLayout = findViewById(R.id.search_layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) registerLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();
    }
}