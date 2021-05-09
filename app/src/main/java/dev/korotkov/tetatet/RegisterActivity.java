package dev.korotkov.tetatet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ThrowOnExtraProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RegisterActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;

    String userId;

    TextView registerAvatarBtn;
    TextView registerInterestsBtn;

    // Interests map loaded from firebase
    HashMap<String, String> interests = new HashMap<>();
    HashMap<String, String> chosenInterests = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        startBackgroundAnimation();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        userId = firebaseAuth.getCurrentUser().getUid();

        registerAvatarBtn = (TextView) findViewById(R.id.register_avatar_btn);
        registerInterestsBtn = (TextView) findViewById(R.id.register_interests_btn);

        loadInterestsMapFromDatabase();

        registerAvatarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogAvatarChoice();
            }
        });

        registerInterestsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogInterestsChoice();
            }
        });
    }

    private void loadInterestsMapFromDatabase() {
        firebaseFirestore.collection("interests").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult().getDocuments()) {
                        String interestEmoji = document.get("emoji").toString();
                        String interestName = document.get("name").toString();

                        interests.put(document.getId(), interestEmoji + " " + interestName);
                    }

                } else {
                    makeDialogInfo("Error", "Cannot get interests list from database");
                    Log.i("INTERESTS", task.getException().getMessage());
                }
            }
        });
    }

    private void showDialogInterestsChoice() {
        // Create dialog from layout
        Dialog dialog = new Dialog(RegisterActivity.this);
        dialog.setContentView(R.layout.dialog_list_multiple_choice);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_all_white_smaller_radius_background));

        // Change the title
        ((TextView) dialog.findViewById(R.id.dialog_list_multiple_choice_title)).setText("Choose your interests");

        // Populate interests list
        ListView interestsViewList = dialog.findViewById(R.id.dialog_list_multiple_choice_list);

        // Get all interests names from interests map
        String[] interestsList = new String[interests.size()];
        interests.values().toArray(interestsList);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(RegisterActivity.this, android.R.layout.simple_list_item_1, interestsList);
        interestsViewList.setAdapter(adapter);

        // Show dialog
        dialog.show();

        interestsViewList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });

        dialog.findViewById(R.id.dialog_list_multiple_choice_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Close dialog
                dialog.dismiss();
            }
        });
    }

    private void showDialogAvatarChoice() {
        // Create dialog from layout
        Dialog dialog = new Dialog(RegisterActivity.this);
        dialog.setContentView(R.layout.dialog_list_single_choice);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_all_white_smaller_radius_background));

        // Change the title
        ((TextView) dialog.findViewById(R.id.dialog_list_single_choice_title)).setText("Choose your avatar");

        // Populate avatar list with emojis from resources
        ListView avatarList = dialog.findViewById(R.id.dialog_list_single_choice_list);
        final String[] avatars = getResources().getStringArray(R.array.avatar_emojis);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(RegisterActivity.this, android.R.layout.simple_list_item_1, avatars);
        avatarList.setAdapter(adapter);

        // Show dialog
        dialog.show();

        avatarList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Choose clicked avatar
                TextView registerAvatarEmoji= findViewById(R.id.register_avatar_emoji);
                TextView registerAvatarBtn = findViewById(R.id.register_avatar_btn);

                registerAvatarEmoji.setVisibility(View.VISIBLE);
                registerAvatarEmoji.setText(avatars[position]);
                registerAvatarBtn.setText("Change avatar");

                // Close dialog
                dialog.dismiss();
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
        // Background gradient animation
        ScrollView registerLayout = findViewById(R.id.register_layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) registerLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();
    }
}