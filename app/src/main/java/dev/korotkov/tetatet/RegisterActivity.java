package dev.korotkov.tetatet;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class RegisterActivity extends AppCompatActivity {

    TextView registerAvatarBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        startBackgroundAnimation();

        registerAvatarBtn = (TextView) findViewById(R.id.register_avatar_btn);

        registerAvatarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogAvatarChoice();
            }
        });
    }

    private void showDialogAvatarChoice() {
        // Create dialog from layout
        Dialog dialog = new Dialog(RegisterActivity.this);
        dialog.setContentView(R.layout.dialog_avatar_choice);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_all_white_background));

        // Populate avatar list with emojis from resources
        ListView avatarList = dialog.findViewById(R.id.dialog_avatar_list);
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

    private void startBackgroundAnimation() {
        // Background gradient animation
        ScrollView registerLayout = findViewById(R.id.register_layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) registerLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();
    }
}