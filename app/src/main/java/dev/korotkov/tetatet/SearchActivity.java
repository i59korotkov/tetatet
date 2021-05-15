package dev.korotkov.tetatet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.LayoutTransition;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity implements View.OnClickListener {

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    DatabaseReference firebaseRef;

    Boolean isBusy = false;

    String userId;
    UserData currentUserData;

    String otherUserId;
    UserData otherUserData;

    // Current user card views
    RelativeLayout currentUserCard;
    TextView currentAvatar;
    TextView currentMainInfo;
    TextView currentLanguages;
    TextView currentInterests;
    TextView currentDescription;

    // Start controls
    RelativeLayout startControls;
    TextView editBtn;
    TextView startBtn;

    // Other user card views
    RelativeLayout otherUserCard;
    TextView otherAvatar;
    TextView otherMainInfo;
    TextView otherLanguages;
    TextView otherInterests;
    TextView otherDescription;

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

        // Get data from intent
        avatars = (ArrayList<ItemWithEmoji>) getIntent().getSerializableExtra("avatars");
        interests = (ArrayList<ItemWithEmoji>) getIntent().getSerializableExtra("interests");
        languages = (ArrayList<ItemWithEmoji>) getIntent().getSerializableExtra("languages");
        currentUserData = (UserData) getIntent().getSerializableExtra("user_data");

        userId = firebaseAuth.getCurrentUser().getUid();

        // Current user card views
        currentUserCard = findViewById(R.id.current_user_card);
        currentAvatar = findViewById(R.id.current_avatar);
        currentMainInfo = findViewById(R.id.current_main_info);
        currentLanguages = findViewById(R.id.current_languages);
        currentInterests = findViewById(R.id.current_interests);
        currentDescription = findViewById(R.id.current_description);

        // Other user card views
        otherUserCard = findViewById(R.id.other_user_card);
        otherAvatar = findViewById(R.id.other_avatar);
        otherMainInfo = findViewById(R.id.other_main_info);
        otherLanguages = findViewById(R.id.other_languages);
        otherInterests = findViewById(R.id.other_interests);
        otherDescription = findViewById(R.id.other_description);

        // Start controls
        startControls = findViewById(R.id.start_controls);
        editBtn = findViewById(R.id.edit_btn);
        startBtn = findViewById(R.id.start_btn);

        // Search controls
        searchControls = findViewById(R.id.search_controls);
        callBtn = findViewById(R.id.call_btn);
        skipBtn = findViewById(R.id.skip_btn);
        stopBtn = findViewById(R.id.stop_btn);

        // Set smooth transition
        currentUserCard.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        otherUserCard.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        setCurrentUserCardData();

        editBtn.setOnClickListener(this);
        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        skipBtn.setOnClickListener(this);
        callBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.start_btn:
                hideCurrentUserCard();

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
            case R.id.stop_btn:
                hideOtherUserCard();

                // Stop listening for incoming offers
                firebaseRef.child(userId).child("status").setValue("stopped");

                // Remove user from database
                firebaseRef.child(userId).removeValue();

                showCurrentUserCard();
                break;
            case R.id.skip_btn:
                Toast.makeText(SearchActivity.this, "Skip btn clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.call_btn:
                Toast.makeText(SearchActivity.this, "Call btn clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.edit_btn:
                Intent intent = new Intent(SearchActivity.this, EditAccountActivity.class);
                intent.putExtra("button_text", "Save changes");
                intent.putExtra("avatars", avatars);
                intent.putExtra("interests", interests);
                intent.putExtra("languages", languages);
                intent.putExtra("user_data", currentUserData);
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

                otherUserId = snapshot.getValue().toString();

                DocumentReference otherUserDocumentReference = firebaseFirestore.collection("users").document(otherUserId);

                otherUserDocumentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        // Get user data from document
                        otherUserData = documentSnapshot.toObject(UserData.class);

                        // Set other user data
                        setOtherUserCardData(otherUserData);

                        // Show other user card
                        showOtherUserCard();
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

    private void setOtherUserCardData(UserData userData) {
        String userAvatar = "\uD83E\uDDD1";
        for (ItemWithEmoji avatar : avatars) {
            if (userData.getAvatarId().equals(avatar.getId())) userAvatar = avatar.getEmoji();
        }

        String commonInterests = "";
        for (ItemWithEmoji interest : interests) {
            // TODO: Add common interests only
            if (userData.getInterestsIds().contains(interest.getId()) && currentUserData.getInterestsIds().contains(interest.getId())) commonInterests += " " + interest.getEmoji();
        }

        String userLanguages = "";
        for (ItemWithEmoji language : languages) {
            if (userData.getLanguagesIds().contains(language.getId())) userLanguages += language.getEmoji() + " ";
        }

        // Set user data to view
        otherAvatar.setText(userAvatar);
        otherMainInfo.setText(userData.getName() + ", " + userData.getAge().toString());
        otherLanguages.setText(userLanguages);

        if (commonInterests.isEmpty()) otherInterests.setText("You do not have any common interests");
        else otherInterests.setText("Common interests:" + commonInterests);

        if (userData.getDescription() == null || userData.getDescription().isEmpty()) {
            otherDescription.setVisibility(View.GONE);
        } else {
            otherDescription.setVisibility(View.VISIBLE);
            otherDescription.setText(userData.getDescription());
        }
    }

    private void hideCurrentUserCard() {
        currentUserCard.startAnimation(AnimationUtils.loadAnimation(SearchActivity.this, R.anim.hide_down));
        currentUserCard.setVisibility(View.GONE);
    }

    private void showCurrentUserCard() {
        currentUserCard.setVisibility(View.VISIBLE);
        currentUserCard.startAnimation(AnimationUtils.loadAnimation(SearchActivity.this, R.anim.show_up));
    }

    private void hideOtherUserCard() {
        otherUserCard.startAnimation(AnimationUtils.loadAnimation(SearchActivity.this, R.anim.hide_down));
        otherUserCard.setVisibility(View.GONE);
    }

    private void showOtherUserCard() {
        otherUserCard.setVisibility(View.VISIBLE);
        otherUserCard.startAnimation(AnimationUtils.loadAnimation(SearchActivity.this, R.anim.show_up));
    }

    private void setCurrentUserCardData() {
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
        currentAvatar.setText(userAvatar);
        currentMainInfo.setText(currentUserData.getName() + ", " + currentUserData.getAge().toString());
        currentLanguages.setText(userLanguages);

        if (userInterests.isEmpty()) currentInterests.setText("You have not chosen any interest");
        else currentInterests.setText("Your interests:" + userInterests);

        if (currentUserData.getDescription() == null || currentUserData.getDescription().isEmpty()) currentDescription.setVisibility(View.GONE);
        else currentDescription.setText(currentUserData.getDescription());
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