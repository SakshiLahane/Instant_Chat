package com.example.instant_chat;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Floating_Button extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<UserModel> userList = new ArrayList<>();
    private UserAdapter userAdapter;
    private String currentUserUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floating_button);

        // Setup Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // ✅ FIX: Provide all 3 parameters to UserAdapter constructor
        userAdapter = new UserAdapter(this, userList, user -> {
            // Click action (optional)
//            Toast.makeText(this, "Clicked: " + user.getName(), Toast.LENGTH_SHORT).show();
        });

        // Optional: handle long-click delete
        userAdapter.setOnUserLongClickListener(user -> {
            FirebaseFirestore.getInstance().collection("users")
                    .document(user.getUid())
                    .delete()
                    .addOnSuccessListener(unused -> Toast.makeText(this, "User deleted from Firestore", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
        });

        recyclerView.setAdapter(userAdapter);
        currentUserUid = FirebaseAuth.getInstance().getUid();
        loadAllUsers();
    }

    private void loadAllUsers() {
        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        UserModel user = doc.toObject(UserModel.class);
                        if (user != null && !user.getUid().equals(currentUserUid)) {
                            userList.add(user);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
