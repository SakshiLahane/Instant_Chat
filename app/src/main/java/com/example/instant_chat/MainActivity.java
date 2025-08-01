package com.example.instant_chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;

public class MainActivity extends AppCompatActivity {

    LottieAnimationView lottieAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        lottieAnimationView = findViewById(R.id.lottieView);
        lottieAnimationView.playAnimation();

        // Launch Get_Started after animation
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, Get_Started.class);
            startActivity(intent);
            finish();
        }, 1500); // 1.5 seconds delay
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.framelayout); // Replace with actual fragment container ID if used

        if (currentFragment instanceof ChatsFragment) {
            // If user is in ChatsFragment, minimize the app
            moveTaskToBack(true);
        } else {
            super.onBackPressed();
        }
    }
}
