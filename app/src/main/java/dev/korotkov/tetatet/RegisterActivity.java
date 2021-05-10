package dev.korotkov.tetatet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;

public class RegisterActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;

    String userId;

    TextView registerAvatarBtn;

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
        setContentView(R.layout.activity_register);

        startBackgroundAnimation();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        userId = firebaseAuth.getCurrentUser().getUid();

        registerAvatarBtn = (TextView) findViewById(R.id.register_avatar_btn);
        registerInterestsBtn = (TextView) findViewById(R.id.register_interests_btn);
        registerLanguagesBtn = (TextView) findViewById(R.id.register_languages_btn);

        chosenInterestsListView = (ListView) findViewById(R.id.register_chosen_interests_list);
        chosenLanguagesListView = (ListView) findViewById(R.id.register_chosen_languages_list);

        loadInterestsFromDatabase();
        loadLanguagesFromDatabase();

        // Set adapter for chosen interests
        interestAdapter = new ItemWithEmojiAdapter(RegisterActivity.this, R.layout.adapter_item_with_emoji, chosenInterests);
        chosenInterestsListView.setAdapter(interestAdapter);

        // Set adapter for chosen languages
        languagesAdapter = new ItemWithEmojiAdapter(RegisterActivity.this, R.layout.adapter_item_with_emoji, chosenLanguages);
        chosenLanguagesListView.setAdapter(languagesAdapter);

        registerAvatarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogAvatarChoice();
            }
        });

        registerInterestsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ItemWithEmojiAdapter adapter = new ItemWithEmojiAdapter(RegisterActivity.this, R.layout.adapter_item_with_emoji, interests);

                // Show dialog windows and set items clickable
                showDialogMultipleChoice(adapter, "Choose your interests").setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        boolean chosen = chosenInterests.contains(interests.get(position));

                        chosen = !chosen;
                        view.setTag(chosen?"true":"false");

                        if (chosen) {
                            chosenInterests.add(interests.get(position));
                            view.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.blue)));
                        } else {
                            chosenInterests.remove(interests.get(position));
                            view.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.white)));
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
                ItemWithEmojiAdapter adapter = new ItemWithEmojiAdapter(RegisterActivity.this, R.layout.adapter_item_with_emoji, languages);

                // Show dialog windows and set items clickable
                showDialogMultipleChoice(adapter, "Choose your languages").setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        boolean chosen = chosenLanguages.contains(languages.get(position));

                        chosen = !chosen;
                        view.setTag(chosen?"true":"false");

                        if (chosen) {
                            chosenLanguages.add(languages.get(position));
                            view.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.blue)));
                        } else {
                            chosenLanguages.remove(languages.get(position));
                            view.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.white)));
                        }

                        // Update chosen interests
                        languagesAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    // TODO: Remove
    private void populateAvatars() {
        ArrayList<String> avatarsList = new ArrayList<String>(Arrays.asList(
                "👶 Baby",
                "🧒 Child",
                "👦 Boy",
                "👧 Girl",
                "🧑 Person",
                "👱 Person: Blond Hair",
                "👨 Man",
                "🧔 Person: Beard",
                "👨‍🦰 Man: Red Hair",
                "👨‍🦱 Man: Curly Hair",
                "👨‍🦳 Man: White Hair",
                "👨‍🦲 Man: Bald",
                "👩 Woman",
                "👩‍🦰 Woman: Red Hair",
                "🧑‍🦰 Person: Red Hair",
                "👩‍🦱 Woman: Curly Hair",
                "🧑‍🦱 Person: Curly Hair",
                "👩‍🦳 Woman: White Hair",
                "🧑‍🦳 Person: White Hair",
                "👩‍🦲 Woman: Bald",
                "🧑‍🦲 Person: Bald",
                "👱‍♀️ Woman: Blond Hair",
                "👱‍♂️ Man: Blond Hair",
                "🧓 Older Person",
                "👴 Old Man",
                "👵 Old Woman",
                "🧑‍⚕️ Health Worker",
                "👨‍⚕️ Man Health Worker",
                "👩‍⚕️ Woman Health Worker",
                "🧑‍🎓 Student",
                "👨‍🎓 Man Student",
                "👩‍🎓 Woman Student",
                "🧑‍🏫 Teacher",
                "👨‍🏫 Man Teacher",
                "👩‍🏫 Woman Teacher",
                "🧑‍⚖️ Judge",
                "👨‍⚖️ Man Judge",
                "👩‍⚖️ Woman Judge",
                "🧑‍🌾 Farmer",
                "👨‍🌾 Man Farmer",
                "👩‍🌾 Woman Farmer",
                "🧑‍🍳 Cook",
                "👨‍🍳 Man Cook",
                "👩‍🍳 Woman Cook",
                "🧑‍🔧 Mechanic",
                "👨‍🔧 Man Mechanic",
                "👩‍🔧 Woman Mechanic",
                "🧑‍🏭 Factory Worker",
                "👨‍🏭 Man Factory Worker",
                "👩‍🏭 Woman Factory Worker",
                "🧑‍💼 Office Worker",
                "👨‍💼 Man Office Worker",
                "👩‍💼 Woman Office Worker",
                "🧑‍🔬 Scientist",
                "👨‍🔬 Man Scientist",
                "👩‍🔬 Woman Scientist",
                "🧑‍💻 Technologist",
                "👨‍💻 Man Technologist",
                "👩‍💻 Woman Technologist",
                "🧑‍🎤 Singer",
                "👨‍🎤 Man Singer",
                "👩‍🎤 Woman Singer",
                "🧑‍🎨 Artist",
                "👨‍🎨 Man Artist",
                "👩‍🎨 Woman Artist",
                "🧑‍✈️ Pilot",
                "👨‍✈️ Man Pilot",
                "👩‍✈️ Woman Pilot",
                "🧑‍🚀 Astronaut",
                "👨‍🚀 Man Astronaut",
                "👩‍🚀 Woman Astronaut",
                "🧑‍🚒 Firefighter",
                "👨‍🚒 Man Firefighter",
                "👩‍🚒 Woman Firefighter",
                "👮 Police Officer",
                "👮‍♂️ Man Police Officer",
                "👮‍♀️ Woman Police Officer",
                "🕵️ Detective",
                "🕵️‍♂️ Man Detective",
                "🕵️‍♀️ Woman Detective",
                "💂 Guard",
                "💂‍♂️ Man Guard",
                "💂‍♀️ Woman Guard",
                "👷 Construction Worker",
                "👷‍♂️ Man Construction Worker",
                "👷‍♀️ Woman Construction Worker",
                "🤴 Prince",
                "👸 Princess",
                "👳 Person Wearing Turban",
                "👳‍♂️ Man Wearing Turban",
                "👳‍♀️ Woman Wearing Turban",
                "👲 Person With Skullcap",
                "🧕 Woman with Headscarf",
                "🤵 Person in Tuxedo",
                "🤵‍♂️ Man in Tuxedo",
                "🤵‍♀️ Woman in Tuxedo",
                "👰 Person With Veil",
                "👰‍♂️ Man with Veil",
                "👰‍♀️ Woman with Veil"
        ));
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

    private ListView showDialogMultipleChoice(ListAdapter adapter, String title) {
        // Create dialog from layout
        Dialog dialog = new Dialog(RegisterActivity.this);
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