package com.example.instant_chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {
    FirebaseAuth mAuth;
    TextInputEditText emailTV, passTV;
    @Override
    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser()!=null){
            startActivity(new Intent(this, Home.class));
            finish();
        }
    }
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        emailTV = findViewById(R.id.emailPhoneEditText);
        passTV = findViewById(R.id.passwordEditText);
        findViewById(R.id.signin).setOnClickListener(v-> {
            String e = emailTV.getText().toString().trim();
            String p = passTV.getText().toString().trim();
            if (TextUtils.isEmpty(e)||TextUtils.isEmpty(p)){
                Toast.makeText(this,"Fill all fields",Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.signInWithEmailAndPassword(e,p).addOnCompleteListener(task-> {
                if(task.isSuccessful()) {
                    startActivity(new Intent(this, Home.class));
                    finish();
                } else Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            });
        });
        findViewById(R.id.signuphere).setOnClickListener(v ->
                startActivity(new Intent(this, Register.class))
        );
    }
}
