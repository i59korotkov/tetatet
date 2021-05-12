package dev.korotkov.tetatet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class EditAccountInfoActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;

    String userId;

    TextView registerFinishBtn;

    // Main info
    EditText registerName;
    EditText registerAge;

    // Description
    EditText registerDescription;

    // Avatars
    TextView registerAvatarBtn;
    ArrayList<ItemWithEmoji> avatars = new ArrayList<>();
    ItemWithEmoji chosenAvatar;

    // Interests
    TextView registerInterestsBtn;
    ListView chosenInterestsListView;
    ItemWithEmojiAdapter interestAdapter;

    ArrayList<ItemWithEmoji> interests = new ArrayList<>();
    ArrayList<ItemWithEmoji> chosenInterests = new ArrayList<>();

    // Languages
    TextView registerLanguagesBtn;
    ListView chosenLanguagesListView;
    ItemWithEmojiAdapter languagesAdapter;

    ArrayList<ItemWithEmoji> languages = new ArrayList<>();
    ArrayList<ItemWithEmoji> chosenLanguages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account_info);

        startBackgroundAnimation();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        userId = firebaseAuth.getCurrentUser().getUid();

        registerFinishBtn = (TextView) findViewById(R.id.register_finish_btn);

        // Set finish btn text
        String finishBtnText = getIntent().getStringExtra("button_text");
        registerFinishBtn.setText(finishBtnText);

        registerAvatarBtn = (TextView) findViewById(R.id.register_avatar_btn);
        registerInterestsBtn = (TextView) findViewById(R.id.register_interests_btn);
        registerLanguagesBtn = (TextView) findViewById(R.id.register_languages_btn);

        registerName = (EditText) findViewById(R.id.register_name);
        registerAge = (EditText) findViewById(R.id.register_age);
        registerDescription = (EditText) findViewById(R.id.register_description);

        chosenInterestsListView = (ListView) findViewById(R.id.register_chosen_interests_list);
        chosenLanguagesListView = (ListView) findViewById(R.id.register_chosen_languages_list);

        loadInterestsFromDatabase();
        loadLanguagesFromDatabase();
        loadAvatarsFromDatabase();

        // Set adapter for chosen interests
        interestAdapter = new ItemWithEmojiAdapter(EditAccountInfoActivity.this, R.layout.adapter_item_with_emoji, chosenInterests);
        chosenInterestsListView.setAdapter(interestAdapter);

        // Set adapter for chosen languages
        languagesAdapter = new ItemWithEmojiAdapter(EditAccountInfoActivity.this, R.layout.adapter_item_with_emoji, chosenLanguages);
        chosenLanguagesListView.setAdapter(languagesAdapter);

        // If finish btn text is "Save changes" than fill edit texts with current user info from database
        if (finishBtnText.equals("Save changes")) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    fillFormWithCurrentUserInfo();
                }
            }, 200);
        }

        registerAvatarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogAvatarChoice();
            }
        });

        registerInterestsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ItemWithEmojiAdapter adapter = new ItemWithEmojiAdapter(EditAccountInfoActivity.this, R.layout.adapter_item_with_emoji, interests, chosenInterests);

                // Show dialog windows and set items clickable
                showDialogMultipleChoice(adapter, "Choose your interests").setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        boolean chosen = chosenInterests.contains(interests.get(position));

                        chosen = !chosen;
                        view.setTag(chosen?"true":"false");

                        if (chosen) {
                            chosenInterests.add(interests.get(position));
                            toggleItemWithEmoji(view, chosen);
                        } else {
                            chosenInterests.remove(interests.get(position));
                            toggleItemWithEmoji(view, chosen);
                        }

                        // Update chosen interests
                        interestAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        registerLanguagesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ItemWithEmojiAdapter adapter = new ItemWithEmojiAdapter(EditAccountInfoActivity.this, R.layout.adapter_item_with_emoji, languages, chosenLanguages);

                // Show dialog windows and set items clickable
                showDialogMultipleChoice(adapter, "Choose your languages").setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        boolean chosen = chosenLanguages.contains(languages.get(position));

                        chosen = !chosen;
                        view.setTag(chosen?"true":"false");

                        if (chosen) {
                            chosenLanguages.add(languages.get(position));
                            toggleItemWithEmoji(view, chosen);
                        } else {
                            chosenLanguages.remove(languages.get(position));
                            toggleItemWithEmoji(view, chosen);
                        }

                        // Update chosen interests
                        languagesAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        registerFinishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get register data
                String name = registerName.getText().toString().trim();
                String description = registerDescription.getText().toString();
                Integer age = 0;

                try {
                    age = Integer.parseInt(registerAge.getText().toString());
                } catch (NumberFormatException e) {
                    String errorMessage = "Age must be a number";
                    registerAge.setError(errorMessage);
                    makeDialogInfo("Error", errorMessage);
                    return;
                }

                // Check register data
                if (name.isEmpty()) {
                    String errorMessage = "Name is required";
                    registerName.setError(errorMessage);
                    makeDialogInfo("Error", errorMessage);
                    return;
                } else if (name.length() < 3) {
                    String errorMessage = "Name is too short";
                    registerName.setError(errorMessage);
                    makeDialogInfo("Error", errorMessage);
                    return;
                } else if (age < 1 || age > 100) {
                    String errorMessage = "Age must be a number from 1 to 100";
                    registerAge.setError(errorMessage);
                    makeDialogInfo("Error", errorMessage);
                    return;
                } else if (description.length() > 200) {
                    String errorMessage = "Description cannot be longer than 200 characters";
                    registerDescription.setError(errorMessage);
                    makeDialogInfo("Error", errorMessage);
                    return;
                } else if (chosenAvatar == null) {
                    String errorMessage = "You must choose your avatar";
                    makeDialogInfo("Error", errorMessage);
                    return;
                } else if (chosenLanguages.size() < 1) {
                    String errorMessage = "You must choose at least one language";
                    makeDialogInfo("Error", errorMessage);
                    return;
                }

                // Make interests and languages ids lists
                ArrayList<String> chosenInterestsIds = new ArrayList<>();
                ArrayList<String> chosenLanguagesIds = new ArrayList<>();

                for (ItemWithEmoji interest : chosenInterests) {
                    chosenInterestsIds.add(interest.getId());
                }

                for (ItemWithEmoji language : chosenLanguages) {
                    chosenLanguagesIds.add(language.getId());
                }

                // Hide register finish button to show the progress bar
                registerFinishBtn.setVisibility(View.INVISIBLE);

                // Store user data in database
                UserData userData = new UserData(name, age, chosenAvatar.getId(), description, chosenInterestsIds, chosenLanguagesIds);

                firebaseFirestore.collection("users").document(userId).set(userData).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        startActivity(new Intent(EditAccountInfoActivity.this, SearchActivity.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        makeDialogInfo("Error", e.getMessage());

                        // Show register finish button
                        registerFinishBtn.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void toggleItemWithEmoji(View view, boolean chosen) {
        if (chosen) {
            view.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.blue)));
            ((TextView) view.findViewById(R.id.adapter_item_with_emoji_name)).setTextColor(getColor(R.color.white));
        } else {
            view.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.white)));
            ((TextView) view.findViewById(R.id.adapter_item_with_emoji_name)).setTextColor(getColor(R.color.grey));
        }
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

    private void fillFormWithCurrentUserInfo() {
        firebaseFirestore.collection("users").document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                UserData userData = documentSnapshot.toObject(UserData.class);

                registerName.setText(userData.getName());
                registerAge.setText(userData.getAge().toString());
                registerDescription.setText(userData.getDescription());

                chosenAvatar = new ItemWithEmoji("000", "Unknown", "\uD83D\uDC64");
                for (ItemWithEmoji avatar : avatars) {
                    if (userData.getAvatarId().equals(avatar.getId())) chosenAvatar = avatar;
                }

                // Change avatar view
                TextView registerAvatarEmoji= findViewById(R.id.register_avatar_emoji);
                TextView registerAvatarBtn = findViewById(R.id.register_avatar_btn);

                registerAvatarEmoji.setVisibility(View.VISIBLE);
                registerAvatarEmoji.setText(chosenAvatar.getEmoji());
                registerAvatarBtn.setText("Change avatar");

                for (ItemWithEmoji interest : interests) {
                    if (userData.getInterestsIds().contains(interest.getId())) chosenInterests.add(interest);
                }
                interestAdapter.notifyDataSetChanged();

                for (ItemWithEmoji language : languages) {
                    if (userData.getLanguagesIds().contains(language.getId())) chosenLanguages.add(language);
                }
                languagesAdapter.notifyDataSetChanged();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                makeDialogInfo("Error", "Cannot load your account information. Error message: " + e.getMessage());
            }
        });
    }

    private ListView showDialogMultipleChoice(ListAdapter adapter, String title) {
        // Create dialog from layout
        Dialog dialog = new Dialog(EditAccountInfoActivity.this);
        dialog.setContentView(R.layout.dialog_list_multiple_choice);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_all_white_smaller_radius_background));

        // Change the title
        ((TextView) dialog.findViewById(R.id.dialog_list_multiple_choice_title)).setText(title);

        // Set adapter view
        ListView listView = dialog.findViewById(R.id.dialog_list_multiple_choice_list);
        listView.setAdapter(adapter);

        // Show dialog
        dialog.show();

        dialog.findViewById(R.id.dialog_list_multiple_choice_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Close dialog
                dialog.dismiss();
            }
        });

        return listView;
    }

    private void showDialogAvatarChoice() {
        // Create dialog from layout
        Dialog dialog = new Dialog(EditAccountInfoActivity.this);
        dialog.setContentView(R.layout.dialog_list_single_choice);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_all_white_smaller_radius_background));

        // Change the title
        ((TextView) dialog.findViewById(R.id.dialog_list_single_choice_title)).setText("Choose your avatar");

        // Populate avatar list with emojis from resources
        ListView avatarList = dialog.findViewById(R.id.dialog_list_single_choice_list);
        ItemWithEmojiAdapter adapter = new ItemWithEmojiAdapter(EditAccountInfoActivity.this, R.layout.adapter_item_with_emoji, avatars, 24);
        avatarList.setAdapter(adapter);

        // Show dialog
        dialog.show();

        avatarList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Save the avatar
                chosenAvatar = avatars.get(position);

                // Change view
                TextView registerAvatarEmoji= findViewById(R.id.register_avatar_emoji);
                TextView registerAvatarBtn = findViewById(R.id.register_avatar_btn);

                registerAvatarEmoji.setVisibility(View.VISIBLE);
                registerAvatarEmoji.setText(avatars.get(position).getEmoji());
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