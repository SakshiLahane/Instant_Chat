package com.example.instant_chat;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<UserModel> userList = new ArrayList<>();
    private Map<String, UserModel> userMap = new HashMap<>();
    private Map<String, MessageModel> lastMessages = new HashMap<>(); // Store last message for each user
    private UserAdapter userAdapter;
    private String currentUserUid;
    private FloatingActionButton fab;
    private EditText searchView;
    private Uri photoUri;
    private File photoFile;
    private String selectedReceiverId = null;

    // ValueEventListener references for cleanup
    private ValueEventListener chatsListener;
    private ValueEventListener chatListListener;

    // Single camera launcher
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && photoUri != null) {
                    sendImageToUser(photoUri);
                } else {
                    Toast.makeText(getContext(), "Camera cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> qrScannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String scannedData = result.getData().getStringExtra("qr_result");
                    if (scannedData != null && !scannedData.isEmpty()) {
                        openChatWithUser(scannedData);
                    }
                }
            }
    );

    // Launcher for FloatingButton activity
    private final ActivityResultLauncher<Intent> floatingButtonLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Refresh the user list when returning from FloatingButton
                    refreshUserList();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        // Initialize views with null checks
        try {
            initializeViews(view);
            setupRecyclerView();
            setupEventListeners(view);

            currentUserUid = FirebaseAuth.getInstance().getUid();

            // Check if user is authenticated
            if (currentUserUid != null) {
                Log.d("ChatsFragment", "Current user: " + currentUserUid);
                loadChatsWithMessages(); // Changed from listenToMessagesRealTime()
            } else {
                Toast.makeText(getContext(), "Authentication required", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("ChatsFragment", "Error in onCreateView", e);
            Toast.makeText(getContext(), "Error initializing chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        searchView = view.findViewById(R.id.searchBar);
        fab = view.findViewById(R.id.fabContacts);

        // Verify all required views exist
        if (recyclerView == null || searchView == null || fab == null) {
            throw new IllegalStateException("Required views not found in layout");
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userAdapter = new UserAdapter(getContext(), userList, user -> {
            if (user != null) {
                selectedReceiverId = user.getUid();
                Log.d("ChatsFragment", "Selected user: " + user.getName());
            }
        });
        recyclerView.setAdapter(userAdapter);
    }

    private void setupEventListeners(View view) {
        // Floating Action Button - to show all users for new chat
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), Floating_Button.class);
            floatingButtonLauncher.launch(intent);
        });

        // Search functionality
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Camera icon
        ImageView cameraIcon = view.findViewById(R.id.camera);
        if (cameraIcon != null) {
            cameraIcon.setOnClickListener(v -> checkCameraPermissionAndOpen());
        }

        // QR Scanner icon
        ImageView qrIcon = view.findViewById(R.id.scanqrcode);
        if (qrIcon != null) {
            qrIcon.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), QrScannerActivity.class);
                qrScannerLauncher.launch(intent);
            });
        }

        // Menu dots
        ImageView menuDotsIcon = view.findViewById(R.id.menu_dots);
        if (menuDotsIcon != null) {
            menuDotsIcon.setOnClickListener(this::showChatPopupMenu);
        }
    }

    // NEW METHOD: Load only users who have exchanged messages with current user
    private void loadChatsWithMessages() {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        chatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("ChatsFragment", "Loading chats, total messages: " + snapshot.getChildrenCount());

                // Clear previous data
                userMap.clear();
                userList.clear(); // Clear the userList as well
                lastMessages.clear();

                // Set to store unique user IDs who have chatted with current user
                Set<String> chattedUserIds = new HashSet<>();

                // Process all messages to find users who have chatted with current user
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    try {
                        MessageModel message = messageSnapshot.getValue(MessageModel.class);

                        if (message == null || message.getSenderId() == null || message.getReceiverId() == null) {
                            Log.w("ChatsFragment", "Skipping invalid message: " + messageSnapshot.getKey());
                            continue; // Skip to the next message
                        }

                        String otherUserId = null;
                        if (message.getSenderId().equals(currentUserUid)) {
                            otherUserId = message.getReceiverId();
                        } else if (message.getReceiverId().equals(currentUserUid)) {
                            otherUserId = message.getSenderId();
                        }

                        if (otherUserId != null) {
                            chattedUserIds.add(otherUserId);

                            MessageModel currentLastMessage = lastMessages.get(otherUserId);
                            if (currentLastMessage == null || message.getTimestamp() > currentLastMessage.getTimestamp()) {
                                lastMessages.put(otherUserId, message);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("ChatsFragment", "Error processing message " + messageSnapshot.getKey(), e);
                        // Do not show a toast for a single bad message, just log it.
                    }
                }

                Log.d("ChatsFragment", "Found " + chattedUserIds.size() + " users with messages");

                // Now load user details for only those users who have chatted
                if (!chattedUserIds.isEmpty()) {
                    loadUserDetails(chattedUserIds);
                } else {
                    // No chats found, update adapter to show an empty list
                    updateUserList();
                    Log.d("ChatsFragment", "No chats found, showing empty list.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatsFragment", "Failed to load chats: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load chats: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        chatsRef.addValueEventListener(chatsListener);
    }

    // Load user details only for users who have exchanged messages
    private void loadUserDetails(Set<String> userIds) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        for (String userId : userIds) {
            firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            UserModel user = documentSnapshot.toObject(UserModel.class);
                            if (user != null && user.getUid() != null) {
                                // Set last message time from our stored last messages
                                MessageModel lastMessage = lastMessages.get(userId);
                                if (lastMessage != null) {
                                    user.setLastMessageTime(lastMessage.getTimestamp());
                                    user.setLastMessage(lastMessage.getMessage()); // If UserModel has this field
                                }

                                userMap.put(user.getUid(), user);
                                Log.d("ChatsFragment", "Loaded user: " + user.getName() + " with last message time: " + user.getLastMessageTime());

                                // Update UI after each user is loaded
                                updateUserList();
                            }
                        } else {
                            Log.w("ChatsFragment", "User document not found: " + userId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ChatsFragment", "Failed to load user: " + userId, e);
                    });
        }
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.CAMERA}, 100);
        } else {
            openCamera();
        }
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
                cameraLauncher.launch(intent);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed to create file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void filterUsers(String query) {
        if (query == null) query = "";

        List<UserModel> filteredList = new ArrayList<>();
        for (UserModel user : userMap.values()) {
            if (user != null && user.getName() != null && user.getEmail() != null) {
                if (user.getName().toLowerCase().contains(query.toLowerCase()) ||
                        user.getEmail().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(user);
                }
            }
        }

        Collections.sort(filteredList, (u1, u2) ->
                Long.compare(u2.getLastMessageTime(), u1.getLastMessageTime()));

        userList.clear();
        userList.addAll(filteredList);
        userAdapter.notifyDataSetChanged();
    }

    private void openChatWithUser(String scannedUid) {
        if (scannedUid == null || scannedUid.trim().isEmpty()) {
            Toast.makeText(getContext(), "Invalid user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
                .document(scannedUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        if (user != null) {
                            selectedReceiverId = user.getUid();

                            // Open chat activity directly
                            Intent intent = new Intent(getContext(), Chatting.class);
                            intent.putExtra("receiverUid", user.getUid());
                            intent.putExtra("name", user.getName());
                            startActivity(intent);

                            Toast.makeText(getContext(), "Opening chat with " + user.getName(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "User not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error finding user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showChatPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.chats_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.group) {
                Toast.makeText(getContext(), "New group selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.community) {
                Toast.makeText(getContext(), "New community selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.broadcast) {
                Toast.makeText(getContext(), "New broadcast selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.linkeddevice) {
                Toast.makeText(getContext(), "Linked device selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.starred) {
                Toast.makeText(getContext(), "Starred messages selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.payments) {
                Toast.makeText(getContext(), "Payments selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.readall) {
                Toast.makeText(getContext(), "Read all messages selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.settings) {
                Toast.makeText(getContext(), "Settings selected", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void sendImageToUser(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(getContext(), "No image to send", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedReceiverId == null) {
            Toast.makeText(getContext(), "Please select a user first", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference storageRef = FirebaseStorage.getInstance().getReference("chat_images")
                .child(System.currentTimeMillis() + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            sendMessage(downloadUri.toString(), "image", selectedReceiverId);
                            Toast.makeText(getContext(), "Image sent!", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendMessage(String content, String type, String receiverId) {
        String senderId = FirebaseAuth.getInstance().getUid();
        if (senderId == null || receiverId == null || content == null) {
            Toast.makeText(getContext(), "Unable to send message", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        String messageId = chatsRef.push().getKey();

        if (messageId == null) {
            Toast.makeText(getContext(), "Failed to generate message ID", Toast.LENGTH_SHORT).show();
            return;
        }

        MessageModel message = new MessageModel(senderId, receiverId, content, System.currentTimeMillis());
        message.setMessageId(messageId);
        message.setType(type);
        message.setSeen(false);
        message.setDelivered(false);
        message.setDeleted(false);
        message.setDeletedFor(new HashMap<>());

        chatsRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ChatsFragment", "Message sent successfully");
                    // Update chat list
                    updateChatList(senderId, receiverId);
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatsFragment", "Failed to send message", e);
                    Toast.makeText(getContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateChatList(String senderId, String receiverId) {
        DatabaseReference chatListRef = FirebaseDatabase.getInstance().getReference("chatList");
        chatListRef.child(senderId).child(receiverId).setValue(true);
        chatListRef.child(receiverId).child(senderId).setValue(true);
    }

    private void updateUserList() {
        userList.clear();
        userList.addAll(userMap.values());

        // Sort by last message time (most recent first)
        Collections.sort(userList, (u1, u2) ->
                Long.compare(u2.getLastMessageTime(), u1.getLastMessageTime()));

        Log.d("ChatsFragment", "Updated user list with " + userList.size() + " users");

        if (userAdapter != null) {
            userAdapter.notifyDataSetChanged();
        }
    }

    // Method to refresh user list (called when returning from FloatingButton)
    private void refreshUserList() {
        if (currentUserUid != null) {
            Log.d("ChatsFragment", "Refreshing user list");
            loadChatsWithMessages(); // Reload chats with messages
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("ChatsFragment", "onDestroy called");

        // Clean up listeners to prevent memory leaks
        if (chatsListener != null) {
            FirebaseDatabase.getInstance().getReference("chats").removeEventListener(chatsListener);
        }
        if (chatListListener != null) {
            FirebaseDatabase.getInstance().getReference("chatList").removeEventListener(chatListListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(getContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}