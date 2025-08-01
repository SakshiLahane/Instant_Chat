package com.example.instant_chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class Forget_Password extends AppCompatActivity {


    private static final String TAG = "ForgetPasswordScreen";

    // UI Elements
    private TextInputEditText emailEditText;
    private Button resetPasswordButton;
    private TextView backToLoginTextView;

    // Firebase Auth
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forget_password);


// Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements from XML
        emailEditText = findViewById(R.id.emailPhoneEditText);
        resetPasswordButton = findViewById(R.id.resetpassword);
        backToLoginTextView = findViewById(R.id.backtologin);

        // Set OnClickListener for the Reset Password Button
        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPasswordResetEmail();
            }
        });

        // Set OnClickListener for the "Back to Login" TextView
        backToLoginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Forget_Password.this, Login.class);
                startActivity(intent);
                finish(); // Finish this activity so user doesn't come back here with back button
            }
        });
    }

    /**
     * Sends a password reset email to the provided email address.
     */
    private void sendPasswordResetEmail() {
        String email = emailEditText.getText().toString().trim();

        // Validate if email field is empty
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email address is required.");
            emailEditText.requestFocus();
            return;
        }

        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Password reset email sent to " + email);
                            Toast.makeText(Forget_Password.this,"Password reset email sent to " + email,
                                    Toast.LENGTH_LONG).show();
                            // Optionally, navigate back to login screen after sending email
                            Intent intent = new Intent(Forget_Password.this, Login.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.w(TAG, "Failed to send reset email: " + task.getException().getMessage(), task.getException());
                            Toast.makeText(Forget_Password.this,"Failed to send reset email: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}


