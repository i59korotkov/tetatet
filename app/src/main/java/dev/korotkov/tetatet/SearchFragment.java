package dev.korotkov.tetatet;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

public class SearchFragment extends Fragment implements View.OnClickListener {

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    DatabaseReference firebaseSearchRef;
    DatabaseReference firebaseCallRef;

    String currentUserId;
    UserData currentUserData;

    String otherUserId;
    UserData otherUserData;

    // Logout button
    TextView logoutBtn;

    // Current user card views
    RelativeLayout currentUserCard;
    TextView currentAvatar;
    TextView currentMainInfo;
    TextView currentLanguages;
    TextView currentInterests;
    TextView currentDescription;

    // Start controls
    RelativeLayout startControls;
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

    LinearLayout cancelLayout;
    TextView cancelBtn;

    // Data lists from database
    ArrayList<ItemWithEmoji> avatars = new ArrayList<>();
    ArrayList<ItemWithEmoji> interests = new ArrayList<>();
    ArrayList<ItemWithEmoji> languages = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseSearchRef = FirebaseDatabase.getInstance().getReference("search");
        firebaseCallRef = FirebaseDatabase.getInstance().getReference("calls");

        // Get data from bundle
        avatars = (ArrayList<ItemWithEmoji>) getArguments().getSerializable("avatars");
        interests = (ArrayList<ItemWithEmoji>) getArguments().getSerializable("interests");
        languages = (ArrayList<ItemWithEmoji>) getArguments().getSerializable("languages");
        currentUserData = (UserData) getArguments().getSerializable("user_data");

        currentUserId = firebaseAuth.getCurrentUser().getUid();

        // Logout button
        logoutBtn = view.findViewById(R.id.logout_btn);

        // Current user card views
        currentUserCard = view.findViewById(R.id.current_user_card);
        currentAvatar = view.findViewById(R.id.current_avatar);
        currentMainInfo = view.findViewById(R.id.current_main_info);
        currentLanguages = view.findViewById(R.id.current_languages);
        currentInterests = view.findViewById(R.id.current_interests);
        currentDescription = view.findViewById(R.id.current_description);

        // Other user card views
        otherUserCard = view.findViewById(R.id.other_user_card);
        otherAvatar = view.findViewById(R.id.other_avatar);
        otherMainInfo = view.findViewById(R.id.other_main_info);
        otherLanguages = view.findViewById(R.id.other_languages);
        otherInterests = view.findViewById(R.id.other_interests);
        otherDescription = view.findViewById(R.id.other_description);

        // Start controls
        startControls = view.findViewById(R.id.start_controls);
        startBtn = view.findViewById(R.id.start_btn);

        // Search controls
        searchControls = view.findViewById(R.id.search_controls);
        callBtn = view.findViewById(R.id.call_btn);
        skipBtn = view.findViewById(R.id.skip_btn);
        stopBtn = view.findViewById(R.id.stop_btn);

        cancelLayout = view.findViewById(R.id.cancel_layout);
        cancelBtn = view.findViewById(R.id.cancel_btn);

        // Set smooth transition
        currentUserCard.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        otherUserCard.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        setCurrentUserCardData();

        hideOtherUserCard();
        otherUserCard.setVisibility(View.VISIBLE);

        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        skipBtn.setOnClickListener(this);
        callBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);

        // Logout
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeDialogInfo("Info", "You need to hold \"Logout\" button to logout");
            }
        });

        logoutBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Logout user from Firebase Auth
                firebaseAuth.signOut();

                // Make vibration
                Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.EFFECT_HEAVY_CLICK));

                // Switch to login activity
                Intent intent = new Intent(SearchFragment.this.getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();

                return false;
            }
        });


        return view;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.start_btn:
                // If permissions are not granted than reject search start and make dialog window
                if (!((MainActivity) getActivity()).isPermissionGranted()) {
                    makeDialogInfo("Warning", "The app needs access to your microphone. You can provide it in the settings");
                    return;
                }

                hideCurrentUserCard();

                startSearching();

                break;
            case R.id.cancel_btn:
                stopSearching();
                break;
            case R.id.stop_btn:
                // Check if call exists and reject
                firebaseCallRef.child(currentUserId).removeValue();
                //firebaseCallRef.child(currentUserId).child(currentUserId).setValue(statusRejected);
                firebaseCallRef.child(otherUserId).removeValue();

                stopSearching();
                break;
            case R.id.skip_btn:
                hideOtherUserCard();

                // Check if call exists and reject
                firebaseCallRef.child(currentUserId).removeValue();
                //firebaseCallRef.child(currentUserId).child(currentUserId).setValue(statusRejected);
                firebaseCallRef.child(otherUserId).removeValue();

                // Remove user from search list
                firebaseSearchRef.child(currentUserId).removeValue();

                // Start searching again
                startSearching();
                break;
            case R.id.call_btn:
                if (v.getVisibility() != View.VISIBLE) break;

                // Check if call already exists
                firebaseCallRef.child(currentUserId).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            startCallAsReceiver();
                        } else {
                            startCallAsCaller();
                        }
                    }
                });

                break;
        }
    }

    private void startCallAsCaller() {
        // Hide call btn to show progress bar
        callBtn.setVisibility(View.INVISIBLE);

        // Remove user from search list
        firebaseSearchRef.child(currentUserId).removeValue();

        // Create call offer in database
        firebaseCallRef.child(otherUserId).child("caller").setValue(currentUserId);

        // Start activity only when call is accepted
        firebaseCallRef.child(otherUserId).child(otherUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() == null) return;
                firebaseCallRef.child(otherUserId).child(otherUserId).removeEventListener(this);

                if (snapshot.getValue().toString().equals(Status.WAITING.name())) {
                    // Start call activity if user accepted offer
                    Intent intent = new Intent(SearchFragment.this.getActivity(), CallActivity.class);
                    intent.putExtra("current_id", currentUserId);
                    intent.putExtra("other_id", otherUserId);
                    intent.putExtra("current_data", currentUserData);
                    intent.putExtra("other_data", otherUserData);
                    intent.putExtra("call_id", otherUserId);
                    startActivity(intent);
                    // Slide animation
                    getActivity().overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);


                    // Show current user card
                    hideOtherUserCard();
                    showCurrentUserCard();
                } else if (snapshot.getValue().toString().equals(Status.REJECTED.name())) {
                    // Remove call from database
                    firebaseCallRef.child(otherUserId).removeValue();

                    // Start searching again
                    hideOtherUserCard();

                    startSearching();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void startCallAsReceiver() {
        // Hide call btn to show progress bar
        callBtn.setVisibility(View.INVISIBLE);

        // Remove user from search list
        firebaseSearchRef.child(currentUserId).removeValue();

        // Notify caller that we are ready to receive call
        firebaseCallRef.child(currentUserId).child(currentUserId).setValue(Status.WAITING);

        // Start activity only when call is generated
        firebaseCallRef.child(currentUserId).child(otherUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null && snapshot.getValue().toString().equals(Status.WAITING.name())) {
                    firebaseCallRef.child(currentUserId).child(otherUserId).removeEventListener(this);

                    // Start call activity when call is ready
                    Intent intent = new Intent(SearchFragment.this.getActivity(), CallActivity.class);
                    intent.putExtra("current_id", currentUserId);
                    intent.putExtra("other_id", otherUserId);
                    intent.putExtra("current_data", currentUserData);
                    intent.putExtra("other_data", otherUserData);
                    intent.putExtra("call_id", currentUserId);
                    startActivity(intent);
                    // Slide animation
                    getActivity().overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);

                    // Show current user card
                    hideOtherUserCard();
                    showCurrentUserCard();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void startSearching() {
        // Show cancel button
        cancelLayout.setVisibility(View.VISIBLE);

        // Show call btn to hide progress bar
        callBtn.setVisibility(View.VISIBLE);

        // Check if anyone is searching now
        firebaseSearchRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot != null) {
                    // Remove this listener when got the value
                    firebaseSearchRef.removeEventListener(this);

                    if (snapshot.getChildrenCount() > 0) {
                        // If someone is searching than send offer to call
                        ArrayList<String> searchingUsers = new ArrayList<>();

                        for (DataSnapshot user : snapshot.getChildren()) {
                            ArrayList<String> commonLanguages = (ArrayList<String>) user.child("languages").getValue();
                            commonLanguages.retainAll(currentUserData.getLanguagesIds());
                            // If users have common languages
                            if (commonLanguages.size() > 0 && user.child("status").getValue().toString().equals(Status.SEARCHING.name()))
                                searchingUsers.add(user.getKey());
                        }

                        // If users not found start than add yourself to search list
                        if (searchingUsers.size() == 0) {
                            listenForIncomingOffer();
                            return;
                        }

                        otherUserId = searchingUsers.get((int) Math.random() * searchingUsers.size());

                        firebaseSearchRef.child(otherUserId).child("offer").setValue(currentUserId);
                        firebaseSearchRef.child(otherUserId).child("status").setValue(Status.OFFERED);

                        // Get other user data and show his card
                        firebaseFirestore.collection("users").document(otherUserId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                // Get user data from document
                                otherUserData = documentSnapshot.toObject(UserData.class);

                                // Set other user data
                                setOtherUserCardData(otherUserData);

                                // Show other user card
                                showOtherUserCard();
                            }
                        });
                    } else {
                        // If no one is searching now than add yourself to search list
                        listenForIncomingOffer();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void stopSearching() {
        hideOtherUserCard();
        cancelLayout.setVisibility(View.GONE);
        showCurrentUserCard();

        // Remove user from search list
        firebaseSearchRef.child(currentUserId).removeValue();
    }

    private void listenForIncomingOffer() {
        firebaseSearchRef.child(currentUserId).child("status").setValue(Status.SEARCHING);
        firebaseSearchRef.child(currentUserId).child("languages").setValue(currentUserData.getLanguagesIds());

        firebaseSearchRef.child(currentUserId).child("offer").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() == null) return;
                // Remove this listener
                firebaseSearchRef.child(currentUserId).child("offer").removeEventListener(this);

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
                        // Ignore incoming offer if error occurs
                        firebaseSearchRef.child(currentUserId).child("offer").removeValue();
                        firebaseSearchRef.child(currentUserId).child("status").setValue(Status.SEARCHING);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void setOtherUserCardData(UserData userData) {
        String userAvatar = "\uD83E\uDDD1";
        for (ItemWithEmoji avatar : avatars) {
            if (userData.getAvatarId().equals(avatar.getId())) userAvatar = avatar.getEmoji();
        }

        String commonInterests = "";
        for (ItemWithEmoji interest : interests) {
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
        //currentUserCard.startAnimation(AnimationUtils.loadAnimation(SearchActivity.this, R.anim.hide_down));
        currentUserCard.animate().translationY(1500).setDuration(200);
    }

    private void showCurrentUserCard() {
        //currentUserCard.startAnimation(AnimationUtils.loadAnimation(SearchActivity.this, R.anim.show_up));
        currentUserCard.animate().translationY(0).setDuration(500);
    }

    private void hideOtherUserCard() {
        //otherUserCard.startAnimation(AnimationUtils.loadAnimation(SearchActivity.this, R.anim.hide_down));
        otherUserCard.animate().translationY(1500).setDuration(200);
    }

    private void showOtherUserCard() {
        //otherUserCard.startAnimation(AnimationUtils.loadAnimation(SearchActivity.this, R.anim.show_up));
        otherUserCard.animate().translationY(0).setDuration(500);
    }

    public void setCurrentUserCardData() {
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
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_info);
        dialog.getWindow().setBackgroundDrawable(getActivity().getDrawable(R.drawable.rounded_all_white_smaller_radius_background));

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

}