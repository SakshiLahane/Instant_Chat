package com.example.instant_chat;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Home extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;

            int id = item.getItemId();

            if (id == R.id.chat) {
                selectedFragment = new ChatsFragment();
            } else if (id == R.id.updates) {
                selectedFragment = new Updates();
            } else if (id == R.id.communities) {
                selectedFragment = new Communities();
            } else if (id == R.id.calls) {
                selectedFragment = new Calls();
            } else {
                selectedFragment = new ChatsFragment(); // Default fallback
            }

            loadFragment(selectedFragment);
            return true;
        });

        // Set default fragment on app launch
        if (savedInstanceState == null) {
            loadFragment(new ChatsFragment());
            bottomNavigationView.setSelectedItemId(R.id.chat);
        }
    }

    // Fragment loading method
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framelayout, fragment)
                .commit();
    }
}
