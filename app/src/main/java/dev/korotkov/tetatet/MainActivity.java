package dev.korotkov.tetatet;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    String[] permissions = {
            Manifest.permission.RECORD_AUDIO
    };
    int requestCode = 1;

    ViewPager2 viewPager;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    DatabaseReference firebaseSearchRef;
    DatabaseReference firebaseCallRef;

    UserData currentUserData;
    String currentUserId;

    // Data lists from database
    ArrayList<ItemWithEmoji> avatars = new ArrayList<>();
    ArrayList<ItemWithEmoji> interests = new ArrayList<>();
    ArrayList<ItemWithEmoji> languages = new ArrayList<>();

    // Fragments
    SearchFragment searchFragment = new SearchFragment();
    EditAccountFragment editAccountFragment = new EditAccountFragment();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBackgroundAnimation();

        // Check permissions
        if (!isPermissionGranted()) {
            askPermission();
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseSearchRef = FirebaseDatabase.getInstance().getReference("search");
        firebaseCallRef = FirebaseDatabase.getInstance().getReference("calls");

        // Get data from intent
        avatars = (ArrayList<ItemWithEmoji>) getIntent().getSerializableExtra("avatars");
        interests = (ArrayList<ItemWithEmoji>) getIntent().getSerializableExtra("interests");
        languages = (ArrayList<ItemWithEmoji>) getIntent().getSerializableExtra("languages");
        currentUserData = (UserData) getIntent().getSerializableExtra("user_data");

        currentUserId = firebaseAuth.getCurrentUser().getUid();

        // Create bundle for fragments
        Bundle bundle = new Bundle();
        bundle.putSerializable("avatars", avatars);
        bundle.putSerializable("interests", interests);
        bundle.putSerializable("languages", languages);
        bundle.putSerializable("user_data", currentUserData);

        viewPager = findViewById(R.id.viewpager);
        viewPager.setPageTransformer(new MarginPageTransformer(convertDpToPx(16)));
        //viewPager.setOffscreenPageLimit(3);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);

        // Add EditAccount fragment
        editAccountFragment.setArguments(bundle);
        adapter.addFragment(editAccountFragment);

        // Add Search fragment
        searchFragment.setArguments(bundle);
        adapter.addFragment(searchFragment);

        // Set fragment adapter for view pager
        viewPager.setAdapter(adapter);

        // Set default fragment as search fragment
        viewPager.setCurrentItem(1, false);
    }

    private void askPermission() {
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    public boolean isPermissionGranted() {

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    public void changeFragment(int steps) {
        viewPager.setCurrentItem(viewPager.getCurrentItem() + steps);
    }

    public void updateCurrentUserCardData() {
        firebaseFirestore.collection("users").document(currentUserId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                currentUserData = documentSnapshot.toObject(UserData.class);
                searchFragment.currentUserData = currentUserData;

                searchFragment.setCurrentUserCardData();
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

    public int convertDpToPx(float dp) {
        return Math.round(dp * this.getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        firebaseSearchRef.child(currentUserId).removeValue();

        super.onDestroy();
    }

}
