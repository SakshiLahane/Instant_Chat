package com.example.instant_chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Register extends AppCompatActivity {

    private TextInputEditText nameTV, emailTV, passTV;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.
                getCurrentUser() != null) {
            startActivity(new Intent(this, Profile.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameTV = findViewById(R.id.NameEditText);
        emailTV = findViewById(R.id.emailPhoneEditText);
        passTV = findViewById(R.id.passwordEditText);

        findViewById(R.id.signupbutton).setOnClickListener(v -> registerUser());

        findViewById(R.id.loginbutton).setOnClickListener(v ->
                startActivity(new Intent(this, Login.class))
        );
    }

    private void registerUser() {
        String name = nameTV.getText() != null ? nameTV.getText().toString().trim() : "";
        String email = emailTV.getText() != null ? emailTV.getText().toString().trim() : "";
        String pass = passTV.getText() != null ? passTV.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.length() < 6) {
            passTV.setError("Min 6 characters");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    String uid = mAuth.getCurrentUser().getUid();

                    // Placeholder/default image, you can later update it in profile
                    String defaultImageUrl = "https://example.com/default_avatar.png";
                    UserModel user = new UserModel(uid, name, email, defaultImageUrl, 0L, "user");
                    db.collection("users").document(uid).set(user)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, Profile.class));
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Auth error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
