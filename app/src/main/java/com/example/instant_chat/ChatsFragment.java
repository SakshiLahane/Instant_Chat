package com.example.instant_chat;

import static android.app.Activity.RESULT_OK;
import static androidx.browser.customtabs.CustomTabsClient.getPackageName;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<UserModel> userList = new ArrayList<>();
    private Map<String, UserModel> userMap = new HashMap<>(); // ✅ Avoid duplicates
    private UserAdapter userAdapter;
    private String currentUserUid;
    private FloatingActionButton fab;

    private EditText searchView;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Uri photoUri;

    private File photoFile;

    private String selectedReceiverId = null;



    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && photoUri != null) {
                    sendImageToUser(photoUri);
                } else {
                    Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> qrScannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String scannedData = result.getData().getStringExtra("qr_result");
                    if (scannedData != null) {
                        Toast.makeText(getContext(), "Scanned: " + scannedData, Toast.LENGTH_SHORT).show();

                        // TODO: Use scanned data to open a chat
                        openChatWithUser(scannedData);
                    }
                }
            }
    );

    private void openChatWithUser(String scannedUid) {
        // Find user in Firestore
        FirebaseFirestore.getInstance().collection("users")
                .document(scannedUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        if (user != null) {
                            // Set the selected user so messages can be sent
                            selectedReceiverId = user.getUid();
                            Toast.makeText(getContext(), "Chatting with " + user.getName(), Toast.LENGTH_SHORT).show();
                            // Optionally navigate to a chat screen
                            // openChatActivity(user); // If you have a ChatActivity
                        }
                    } else {
                        Toast.makeText(getContext(), "User not found!", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userAdapter = new UserAdapter(getContext(), userList, user -> {
            selectedReceiverId = user.getUid();
//            Toast.makeText(getContext(), "Selected: " + user.getName(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(userAdapter);

        currentUserUid = FirebaseAuth.getInstance().getUid();
        searchView = view.findViewById(R.id.searchBar);

        ImageView menuDotsIcon = view.findViewById(R.id.menu_dots); // Make sure this ID matches your XML


        fab = view.findViewById(R.id.fabContacts);
        fab.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), Floating_Button.class));
        });

        if (currentUserUid != null) {
            listenToMessagesRealTime(); // ✅ Start real-time message monitoring
        }

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            private void filterUsers(String query) {
                List<UserModel> filteredList = new ArrayList<>();
                for (UserModel user : userMap.values()) {
                    if (user.getName().toLowerCase().contains(query.toLowerCase()) ||
                            user.getEmail().toLowerCase().contains(query.toLowerCase())) {
                        filteredList.add(user);
                    }
                }

                // Sort by last message time again
                Collections.sort(filteredList, (u1, u2) -> Long.compare(u2.getLastMessageTime(), u1.getLastMessageTime()));

                userList.clear();
                userList.addAll(filteredList);
                userAdapter.notifyDataSetChanged();
            }


            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ImageView cameraIcon = view.findViewById(R.id.camera);
        cameraIcon.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{android.Manifest.permission.CAMERA}, 100);
            } else {
                openCamera();
            }
        });
        ImageView qrIcon = view.findViewById(R.id.scanqrcode);
        qrIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), QrScannerActivity.class);
            qrScannerLauncher.launch(intent);
        });

        // ⬅️ Upload image
        ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && photoUri != null) {
                        sendImageToUser(photoUri);  // ⬅️ Upload image
                    }
                });

        menuDotsIcon.setOnClickListener(this::showChatPopupMenu); // Using a method reference


        return view;
}

    private void showChatPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        // Inflate your menu XML file
        popupMenu.getMenuInflater().inflate(R.menu.chats_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId(); // Get the ID of the clicked menu item

                if (itemId == R.id.group) {
                    Toast.makeText(getContext(), "New group selected", Toast.LENGTH_SHORT).show();
                    // TODO: Implement logic for creating a new group
                    return true;
                } else if (itemId == R.id.community) {
                    Toast.makeText(getContext(), "New community selected", Toast.LENGTH_SHORT).show();
                    // TODO: Implement logic for creating a new community
                    return true;
                } else if (itemId == R.id.broadcast) {
                    Toast.makeText(getContext(), "New broadcast selected", Toast.LENGTH_SHORT).show();
                    // TODO: Implement logic for creating a new broadcast
                    return true;
                } else if (itemId == R.id.linkeddevice) {
                    Toast.makeText(getContext(), "Linked device selected", Toast.LENGTH_SHORT).show();
                    // TODO: Implement navigation or action for linked devices
                    return true;
                } else if (itemId == R.id.starred) {
                    Toast.makeText(getContext(), "Starred messages selected", Toast.LENGTH_SHORT).show();
                    // TODO: Implement navigation to starred messages
                    return true;
                } else if (itemId == R.id.payments) {
                    Toast.makeText(getContext(), "Payments selected", Toast.LENGTH_SHORT).show();
                    // TODO: Implement navigation or action for payments
                    return true;
                } else if (itemId == R.id.readall) {
                    Toast.makeText(getContext(), "Read all messages selected", Toast.LENGTH_SHORT).show();
                    // TODO: Implement logic to mark all messages as read (e.g., update Firebase)
                    return true;
                } else if (itemId == R.id.settings) {
                    Toast.makeText(getContext(), "Settings selected", Toast.LENGTH_SHORT).show();
                    // TODO: Implement navigation to the Settings screen
                    return true;
                }
                return false; // Return false if the item was not handled
            }
        });

        popupMenu.show(); // Display the popup menu
    }

    // ... rest of your ChatsFragment class (e.g., openCamera, sendMessage, etc.)


    private void sendImageToUser(Uri imageUri) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("chat_images")
                .child(System.currentTimeMillis() + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    // ✅ Replace this with actual selected user ID
                    String receiverId = getSelectedReceiverUserId();

                    if (receiverId != null) {
                        sendMessage(downloadUri.toString(), "image", receiverId);
                    } else {
                        Toast.makeText(getContext(), "Receiver not selected", Toast.LENGTH_SHORT).show();
                    }
                }))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Image upload failed", Toast.LENGTH_SHORT).show());
    }

    private String getSelectedReceiverUserId() {
        return selectedReceiverId;
    }



    private void sendMessage(String content, String type, String receiverId) {
        String senderId = FirebaseAuth.getInstance().getUid();
        if (senderId == null || receiverId == null) return;

        MessageModel message = new MessageModel();
        message.messageId = FirebaseDatabase.getInstance().getReference().push().getKey();
        message.senderId = senderId;
        message.receiverId = receiverId;
        message.message = content;
        message.timestamp = System.currentTimeMillis();
        message.seen = false;
        message.isDelivered = false;
        message.deleted = false;
        message.deletedFor = new HashMap<>();

        FirebaseDatabase.getInstance().getReference("chats")
                .child(message.messageId)
                .setValue(message);
    }


    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_";
                File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);

                photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        photoFile
                );

                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                cameraLauncher.launch(intent); // ✅ Modern launcher
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed to create file", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void listenToMessagesRealTime() {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userMap.clear();

                // Get all users from Firestore
                FirebaseFirestore.getInstance().collection("users")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                UserModel user = doc.toObject(UserModel.class);
                                if (user != null && !user.getUid().equals(currentUserUid)) {
                                    long lastTime = 0;

                                    for (DataSnapshot ds : snapshot.getChildren()) {
                                        MessageModel msg = ds.getValue(MessageModel.class);
                                        if (msg == null) continue;

                                        boolean isBetween = (msg.senderId.equals(currentUserUid) && msg.receiverId.equals(user.getUid())) ||
                                                (msg.senderId.equals(user.getUid()) && msg.receiverId.equals(currentUserUid));

                                        if (isBetween && msg.timestamp > lastTime) {
                                            lastTime = msg.timestamp;
                                        }
                                    }

                                    user.setLastMessageTime(lastTime);
                                    userMap.put(user.getUid(), user);
                                }
                            }

                            // Update list and sort
                            userList.clear();
                            userList.addAll(userMap.values());
                            Collections.sort(userList, (u1, u2) -> Long.compare(u2.getLastMessageTime(), u1.getLastMessageTime()));
                            userAdapter.notifyDataSetChanged();
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load chats", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
