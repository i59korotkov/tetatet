package dev.korotkov.tetatet;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class EditAccountFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;

    String userId;
    UserData currentUserData;

    // Main view
    View mainView;

    TextView saveChangesBtn;

    // Main info
    EditText registerName;
    EditText registerAge;

    // Avatars
    TextView registerAvatarBtn;
    ItemWithEmoji chosenAvatar;

    // Description
    EditText registerDescription;

    // Interests
    TextView registerInterestsBtn;
    ListView chosenInterestsListView;
    ItemWithEmojiAdapter interestAdapter;
    ArrayList<ItemWithEmoji> chosenInterests = new ArrayList<>();

    // Languages
    TextView registerLanguagesBtn;
    ListView chosenLanguagesListView;
    ItemWithEmojiAdapter languagesAdapter;
    ArrayList<ItemWithEmoji> chosenLanguages = new ArrayList<>();

    // Data lists from database
    ArrayList<ItemWithEmoji> avatars = new ArrayList<>();
    ArrayList<ItemWithEmoji> interests = new ArrayList<>();
    ArrayList<ItemWithEmoji> languages = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_edit_account, container, false);
        mainView = view;

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Get data from bundle
        avatars = (ArrayList<ItemWithEmoji>) getArguments().getSerializable("avatars");
        interests = (ArrayList<ItemWithEmoji>) getArguments().getSerializable("interests");
        languages = (ArrayList<ItemWithEmoji>) getArguments().getSerializable("languages");
        currentUserData = (UserData) getArguments().getSerializable("user_data");

        userId = firebaseAuth.getCurrentUser().getUid();

        saveChangesBtn = (TextView) view.findViewById(R.id.register_finish_btn);

        registerAvatarBtn = (TextView) view.findViewById(R.id.register_avatar_btn);
        registerInterestsBtn = (TextView) view.findViewById(R.id.register_interests_btn);
        registerLanguagesBtn = (TextView) view.findViewById(R.id.register_languages_btn);

        registerName = (EditText) view.findViewById(R.id.register_name);
        registerAge = (EditText) view.findViewById(R.id.register_age);
        registerDescription = (EditText) view.findViewById(R.id.register_description);

        chosenInterestsListView = (ListView) view.findViewById(R.id.register_chosen_interests_list);
        chosenLanguagesListView = (ListView) view.findViewById(R.id.register_chosen_languages_list);

        // Set adapter for chosen interests
        interestAdapter = new ItemWithEmojiAdapter(getContext(), R.layout.adapter_item_with_emoji, chosenInterests);
        chosenInterestsListView.setAdapter(interestAdapter);

        // Set adapter for chosen languages
        languagesAdapter = new ItemWithEmojiAdapter(getContext(), R.layout.adapter_item_with_emoji, chosenLanguages);
        chosenLanguagesListView.setAdapter(languagesAdapter);

        fillFormWithCurrentUserInfo(view);

        registerAvatarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogAvatarChoice();
            }
        });

        registerInterestsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ItemWithEmojiAdapter adapter = new ItemWithEmojiAdapter(getContext(), R.layout.adapter_item_with_emoji, interests, chosenInterests);

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
                ItemWithEmojiAdapter adapter = new ItemWithEmojiAdapter(getContext(), R.layout.adapter_item_with_emoji, languages, chosenLanguages);

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

        saveChangesBtn.setOnClickListener(new View.OnClickListener() {
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
                saveChangesBtn.setVisibility(View.INVISIBLE);

                // Store user data in database
                currentUserData = new UserData(name, age, chosenAvatar.getId(), description, chosenInterestsIds, chosenLanguagesIds);

                firebaseFirestore.collection("users").document(userId).set(currentUserData).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        ((MainActivity) getActivity()).updateCurrentUserCardData();
                        ((MainActivity) getActivity()).changeFragment(-1);
                        saveChangesBtn.setVisibility(View.VISIBLE);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        makeDialogInfo("Error", e.getMessage());

                        // Show save changes button
                        saveChangesBtn.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        return view;
    }

    private void toggleItemWithEmoji(View view, boolean chosen) {
        if (chosen) {
            view.setBackgroundTintList(ColorStateList.valueOf(getActivity().getColor(R.color.blue)));
            ((TextView) view.findViewById(R.id.adapter_item_with_emoji_name)).setTextColor(getActivity().getColor(R.color.white));
        } else {
            view.setBackgroundTintList(ColorStateList.valueOf(getActivity().getColor(R.color.white)));
            ((TextView) view.findViewById(R.id.adapter_item_with_emoji_name)).setTextColor(getActivity().getColor(R.color.grey));
        }
    }

    private void fillFormWithCurrentUserInfo(View view) {
        registerName.setText(currentUserData.getName());
        registerAge.setText(currentUserData.getAge().toString());
        registerDescription.setText(currentUserData.getDescription());

        chosenAvatar = new ItemWithEmoji("000", "Unknown", "\uD83D\uDC64");
        for (ItemWithEmoji avatar : avatars) {
            if (currentUserData.getAvatarId().equals(avatar.getId())) chosenAvatar = avatar;
        }

        // Change avatar view
        TextView registerAvatarEmoji = view.findViewById(R.id.register_avatar_emoji);
        TextView registerAvatarBtn = view.findViewById(R.id.register_avatar_btn);

        registerAvatarEmoji.setVisibility(View.VISIBLE);
        registerAvatarEmoji.setText(chosenAvatar.getEmoji());
        registerAvatarBtn.setText("Change avatar");

        for (ItemWithEmoji interest : interests) {
            if (currentUserData.getInterestsIds().contains(interest.getId())) chosenInterests.add(interest);
        }
        interestAdapter.notifyDataSetChanged();

        for (ItemWithEmoji language : languages) {
            if (currentUserData.getLanguagesIds().contains(language.getId())) chosenLanguages.add(language);
        }
        languagesAdapter.notifyDataSetChanged();
    }

    private ListView showDialogMultipleChoice(ListAdapter adapter, String title) {
        // Create dialog from layout
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_list_multiple_choice);
        dialog.getWindow().setBackgroundDrawable(getActivity().getDrawable(R.drawable.rounded_all_white_smaller_radius_background));

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
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_list_single_choice);
        dialog.getWindow().setBackgroundDrawable(getContext().getDrawable(R.drawable.rounded_all_white_smaller_radius_background));

        // Change the title
        ((TextView) dialog.findViewById(R.id.dialog_list_single_choice_title)).setText("Choose your avatar");

        // Populate avatar list with emojis from resources
        ListView avatarList = dialog.findViewById(R.id.dialog_list_single_choice_list);
        ItemWithEmojiAdapter adapter = new ItemWithEmojiAdapter(getContext(), R.layout.adapter_item_with_emoji, avatars, 24);
        avatarList.setAdapter(adapter);

        // Show dialog
        dialog.show();

        avatarList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Save the avatar
                chosenAvatar = avatars.get(position);

                // Change view
                TextView registerAvatarEmoji= mainView.findViewById(R.id.register_avatar_emoji);
                TextView registerAvatarBtn = mainView.findViewById(R.id.register_avatar_btn);

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