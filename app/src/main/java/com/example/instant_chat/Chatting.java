package com.example.instant_chat;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chatting extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText inputMessage;
    private ImageButton sendBtn;
    private TextView chatUserName, chatStatus;
    private ImageButton attachBtn, galleryBtn, cameraBtn, emojiBtn;
    private RelativeLayout messageInputContainer;

    private List<MessageModel> messageList = new ArrayList<>();
    private MessageAdapter adapter;

    private String senderUid;
    private String receiverUid;
    private String receiverName;

    private DatabaseReference userRef;
    private ValueEventListener statusListener;
    private ValueEventListener messagesListener;
    private Handler typingHandler = new Handler();
    private Runnable stopTypingRunnable;
    private boolean isAttachmentVisible = false;

    ActivityResultLauncher<Intent> galleryLauncher;
    ActivityResultLauncher<Uri> cameraLauncher;
    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting);

        try {
            if (!validateUserAuthentication() || !validateIntentData() || !initializeViews()) {
                return;
            }

            checkFirebaseConnection();

            setupRecyclerView();
            setupEventListeners();
            setupActivityResultLaunchers();

            if (receiverName != null && !receiverName.isEmpty()) {
                chatUserName.setText(receiverName);
            } else {
                chatUserName.setText("User");
            }

            loadMessages();
            observeReceiverStatus();
            handleTyping();

        } catch (Exception e) {
            Log.e("Chatting", "Error in onCreate", e);
            Toast.makeText(this, "Error opening chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void handleTyping() {
        if (senderUid == null || receiverUid == null) {
            Log.e("Chatting", "Cannot handle typing: senderUid or receiverUid is null");
            return;
        }

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users").child(senderUid);

        stopTypingRunnable = () -> myRef.child("typingTo").setValue(null);

        inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    myRef.child("typingTo").setValue(receiverUid);
                    typingHandler.removeCallbacks(stopTypingRunnable);
                    typingHandler.postDelayed(stopTypingRunnable, 2000); // 2-second delay
                } else {
                    myRef.child("typingTo").setValue(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void observeReceiverStatus() {
        if (receiverUid == null) {
            Log.e("Chatting", "Cannot observe receiver status: receiverUid is null");
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(receiverUid);
        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    String typingTo = snapshot.child("typingTo").getValue(String.class);

                    if (typingTo != null && typingTo.equals(senderUid)) {
                        chatStatus.setText("Typing...");
                    } else if ("online".equalsIgnoreCase(status)) {
                        chatStatus.setText("Online");
                    } else {
                        Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);
                        if (lastSeen != null) {
                            chatStatus.setText("Last seen: " + TimeUtilsLastSeen.formatTimestamp(lastSeen));
                        } else {
                            chatStatus.setText("Offline");
                        }
                    }
                } else {
                    chatStatus.setText("Offline");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Chatting", "Failed to read receiver status: " + error.getMessage());
            }
        };

        userRef.addValueEventListener(statusListener);
    }

    private boolean validateUserAuthentication() {
        senderUid = FirebaseAuth.getInstance().getUid();
        if (senderUid == null) {
            Toast.makeText(this, "Authentication required. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        return true;
    }

    private boolean validateIntentData() {
        receiverUid = getIntent().getStringExtra("receiverUid");
        receiverName = getIntent().getStringExtra("name");

        if (receiverUid == null || receiverUid.isEmpty()) {
            Toast.makeText(this, "Invalid user data. Please try again.", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }

        if (receiverUid.equals(senderUid)) {
            Toast.makeText(this, "Cannot chat with yourself", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        return true;
    }

    private boolean initializeViews() {
        try {
            recyclerView = findViewById(R.id.chatRecyclerView);
            inputMessage = findViewById(R.id.messageBox);
            sendBtn = findViewById(R.id.sendBtn);
            chatUserName = findViewById(R.id.chatUserName);
            chatStatus = findViewById(R.id.chatStatus);
            attachBtn = findViewById(R.id.attachBtn);
            galleryBtn = findViewById(R.id.galleryBtn);
            cameraBtn = findViewById(R.id.cameraBtn);
            emojiBtn = findViewById(R.id.emojiBtn);
            messageInputContainer = findViewById(R.id.messageInputContainer);

            if (recyclerView == null || inputMessage == null || sendBtn == null ||
                    chatUserName == null || chatStatus == null || attachBtn == null ||
                    galleryBtn == null || cameraBtn == null || messageInputContainer == null) {
                throw new IllegalStateException("Required views not found in layout");
            }

            galleryBtn.setVisibility(View.GONE);
            galleryBtn.setAlpha(0f);
            cameraBtn.setVisibility(View.GONE);
            cameraBtn.setAlpha(0f);

            return true;
        } catch (Exception e) {
            Log.e("Chatting", "Error initializing views", e);
            Toast.makeText(this, "Layout error. Please check the layout file.", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(this, messageList, senderUid);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        adapter.setOnMessageLongClickListener(this::showDeleteDialog);
    }

    private void setupEventListeners() {
        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        ImageButton videoCallBtn = findViewById(R.id.videocall);
        if (videoCallBtn != null) {
            videoCallBtn.setOnClickListener(v -> {
                String roomName = receiverUid;
                notifyReceiverForCall(roomName);

                Intent intent = new Intent(Chatting.this, Videocall.class);
                intent.putExtra("channelName", roomName);
                startActivity(intent);
            });
        }

        sendBtn.setOnClickListener(v -> {
            String text = inputMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
            } else {
                Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show();
            }
        });

        attachBtn.setOnClickListener(v -> toggleAttachmentButtons());
        galleryBtn.setOnClickListener(v -> openGallery());
        cameraBtn.setOnClickListener(v -> openCamera());
    }

    private void setupActivityResultLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        if (selectedImage != null) {
                            sendImageMessage(selectedImage);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                isSuccess -> {
                    if (isSuccess && imageUri != null) {
                        sendImageMessage(imageUri);
                    }
                });
    }

    private void openCamera() {
        try {
            imageUri = createImageUri();
            if (imageUri != null) {
                cameraLauncher.launch(imageUri);
            } else {
                Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("Chatting", "Error opening camera", e);
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri createImageUri() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "Chat_Image_" + System.currentTimeMillis());
            values.put(MediaStore.Images.Media.DESCRIPTION, "Image from instant chat");
            return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            Log.e("Chatting", "Error creating image URI", e);
            return null;
        }
    }

    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        } catch (Exception e) {
            Log.e("Chatting", "Error opening gallery", e);
            Toast.makeText(this, "Gallery not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleAttachmentButtons() {
        RelativeLayout.LayoutParams messageBoxParams = (RelativeLayout.LayoutParams) inputMessage.getLayoutParams();
        TransitionManager.beginDelayedTransition(messageInputContainer, new AutoTransition().setDuration(250));

        if (!isAttachmentVisible) {
            cameraBtn.setVisibility(View.VISIBLE);
            galleryBtn.setVisibility(View.VISIBLE);

            galleryBtn.animate().alpha(1f).setDuration(250).start();
            cameraBtn.animate().alpha(1f).setDuration(250).start();

            messageBoxParams.removeRule(RelativeLayout.END_OF);
            messageBoxParams.removeRule(RelativeLayout.START_OF);
            messageBoxParams.addRule(RelativeLayout.START_OF, R.id.galleryBtn);
            inputMessage.setLayoutParams(messageBoxParams);

        } else {
            galleryBtn.animate().alpha(0f).setDuration(250).start();
            cameraBtn.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> {
                        galleryBtn.setVisibility(View.GONE);
                        cameraBtn.setVisibility(View.GONE);

                        messageBoxParams.removeRule(RelativeLayout.END_OF);
                        messageBoxParams.removeRule(RelativeLayout.START_OF);
                        messageBoxParams.addRule(RelativeLayout.START_OF, R.id.attachBtn);
                        inputMessage.setLayoutParams(messageBoxParams);
                    })
                    .start();
        }
        isAttachmentVisible = !isAttachmentVisible;
    }

    private void showDeleteDialog(MessageModel message) {
        if (message == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Message");

        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        if (message.getSenderId().equals(currentUserId)) {
            builder.setNeutralButton("Delete for everyone", (dialog, which) -> {
                deleteMessageForEveryone(message.getMessageId(), message.getSenderId());
            });
        }

        builder.setPositiveButton("Delete for me", (dialog, which) -> {
            deleteMessageForMe(message.getMessageId(), currentUserId, message.getSenderId());
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void deleteMessageForMe(String messageId, String currentUserId, String senderId) {
        if (currentUserId == null || messageId == null) {
            Toast.makeText(this, "Error: Invalid message data", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference msgRef = FirebaseDatabase.getInstance().getReference("chats").child(messageId);
        msgRef.child("deletedFor").child(currentUserId).setValue(true)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Message deleted for you", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.e("Chatting", "Failed to delete message for me", e);
                    Toast.makeText(this, "Failed to delete message", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteMessageForEveryone(String messageId, String senderId) {
        if (messageId == null) {
            Toast.makeText(this, "Error: Invalid message ID", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference messageRef = FirebaseDatabase.getInstance().getReference("chats").child(messageId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("message", "You deleted this message");
        map.put("type", "deleted_sender");
        map.put("deleted", true);

        messageRef.updateChildren(map)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Message deleted for everyone", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.e("Chatting", "Failed to delete message for everyone", e);
                    Toast.makeText(this, "Failed to delete message", Toast.LENGTH_SHORT).show();
                });
    }

    private void notifyReceiverForCall(String roomName) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("calls").child(receiverUid);
        HashMap<String, Object> callData = new HashMap<>();
        callData.put("from", senderUid);
        callData.put("room", roomName);
        callData.put("type", "video");
        callData.put("timestamp", System.currentTimeMillis());

        ref.setValue(callData)
                .addOnFailureListener(e -> Log.e("Chatting", "Failed to notify receiver for call", e));
    }

    /**
     * Corrected sendMessage method.
     * This method now properly updates the chat list with the message text.
     */
    private void sendMessage(String messageText) {
        if (messageText.trim().isEmpty()) {
            Toast.makeText(this, "Cannot send empty message", Toast.LENGTH_SHORT).show();
            return;
        }

        sendBtn.setEnabled(false);

        String chatId = getChatId(senderUid, receiverUid);
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);
        String messageId = chatRef.push().getKey();

        if (messageId == null) {
            sendBtn.setEnabled(true);
            return;
        }

        MessageModel message = new MessageModel(senderUid, receiverUid, messageText, System.currentTimeMillis());
        message.setMessageId(messageId);
        message.setType("text");
        message.setSeen(false);
        message.setDelivered(false);
        message.setDeleted(false);
        message.setDeletedFor(new HashMap<>());

        chatRef.child(messageId).setValue(message)
                .addOnSuccessListener(unused -> {
                    inputMessage.setText("");
                    sendBtn.setEnabled(true);
                    updateChatList(messageText);
                })
                .addOnFailureListener(e -> {
                    sendBtn.setEnabled(true);
                    Toast.makeText(Chatting.this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }


    private void updateChatList(String lastMessage) {
        if (senderUid == null || receiverUid == null || lastMessage == null) {
            Log.e("Chatting", "Invalid parameters for updateChatList");
            return;
        }

        Log.d("Chatting", "Updating chat list with message: " + lastMessage);

        DatabaseReference chatListRef = FirebaseDatabase.getInstance().getReference("chatList");
        long currentTimestamp = System.currentTimeMillis();

        // Create chat data for sender (shows receiver's info)
        Map<String, Object> senderChatData = new HashMap<>();
        senderChatData.put("chatId", receiverUid);
        senderChatData.put("lastMessage", lastMessage);
        senderChatData.put("timestamp", currentTimestamp);
        senderChatData.put("name", receiverName != null ? receiverName : "User");

        // Create chat data for receiver (shows sender's info)
        Map<String, Object> receiverChatData = new HashMap<>();
        receiverChatData.put("chatId", senderUid);
        receiverChatData.put("lastMessage", lastMessage);
        receiverChatData.put("timestamp", currentTimestamp);

        // Get sender's name for receiver's chat list
        FirebaseDatabase.getInstance().getReference("users").child(senderUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String senderName = snapshot.child("name").getValue(String.class);
                        receiverChatData.put("name", senderName != null ? senderName : "User");

                        // Update both chat lists
                        chatListRef.child(senderUid).child(receiverUid).setValue(senderChatData)
                                .addOnSuccessListener(aVoid -> Log.d("Chatting", "Sender chat list updated"))
                                .addOnFailureListener(e -> Log.e("Chatting", "Failed to update sender chat list", e));

                        chatListRef.child(receiverUid).child(senderUid).setValue(receiverChatData)
                                .addOnSuccessListener(aVoid -> Log.d("Chatting", "Receiver chat list updated"))
                                .addOnFailureListener(e -> Log.e("Chatting", "Failed to update receiver chat list", e));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Chatting", "Failed to get sender name", error.toException());
                        receiverChatData.put("name", "User");

                        // Still update chat lists even if name fetch fails
                        chatListRef.child(senderUid).child(receiverUid).setValue(senderChatData);
                        chatListRef.child(receiverUid).child(senderUid).setValue(receiverChatData);
                    }
                });
    }

    private void sendImageMessage(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, "Invalid image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Store message text before clearing
        String messageText = inputMessage.getText().toString().trim();
        String finalMessageText = messageText.isEmpty() ? "📷 Image" : messageText;

        // Show progress
        sendBtn.setEnabled(false);
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("chat_images")
                .child(System.currentTimeMillis() + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();

                                DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
                                String messageId = chatsRef.push().getKey();
                                if (messageId == null) {
                                    Toast.makeText(this, "Failed to generate message ID", Toast.LENGTH_SHORT).show();
                                    sendBtn.setEnabled(true);
                                    return;
                                }

                                // Create image message
                                MessageModel message = new MessageModel(senderUid, receiverUid, finalMessageText, System.currentTimeMillis());
                                message.setMessageId(messageId);
                                message.setType("image");
                                message.setMediaUrl(imageUrl);
                                message.setSeen(false);
                                message.setDelivered(false);
                                message.setDeleted(false);
                                message.setDeletedFor(new HashMap<>());

                                chatsRef.child(messageId).setValue(message)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("Chatting", "Image message sent successfully");

                                            // Update chat list first, passing the correct message text
                                            updateChatList(finalMessageText);

                                            // Then update UI
                                            runOnUiThread(() -> {
                                                inputMessage.setText(""); // This line is now correctly placed after the chat list update
                                                sendBtn.setEnabled(true);
                                                Toast.makeText(this, "Image sent!", Toast.LENGTH_SHORT).show();
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Chatting", "Failed to send image message", e);
                                            runOnUiThread(() -> {
                                                sendBtn.setEnabled(true);
                                                Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show();
                                            });
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Chatting", "Failed to get download URL", e);
                                runOnUiThread(() -> {
                                    sendBtn.setEnabled(true);
                                    Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
                                });
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Chatting", "Image upload failed", e);
                    runOnUiThread(() -> {
                        sendBtn.setEnabled(true);
                        Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    });
                });
    }

    // Fixed loadMessages method with better error handling
    private void loadMessages() {
        String chatId = getChatId(senderUid, receiverUid);
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);

        messagesListener = chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    MessageModel msg = ds.getValue(MessageModel.class);
                    if (msg != null && (msg.getDeletedFor() == null || !Boolean.TRUE.equals(msg.getDeletedFor().get(senderUid)))) {
                        messageList.add(msg);
                    }
                }
                messageList.sort((m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Chatting", "Failed to load messages", error.toException());
                Toast.makeText(Chatting.this, "Error loading chats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Add this method to check Firebase connection (call in onCreate)
    private void checkFirebaseConnection() {
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                Log.d("Firebase", "Connected: " + connected);
                if (!connected) {
                    runOnUiThread(() ->
                            Toast.makeText(Chatting.this, "No internet connection", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Connection listener cancelled", error.toException());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMyStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateMyStatus("offline");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null && FirebaseDatabase.getInstance() != null) {
            FirebaseDatabase.getInstance().getReference("chats").removeEventListener(messagesListener);
        }
        if (statusListener != null && userRef != null) {
            userRef.removeEventListener(statusListener);
        }
        if (typingHandler != null && stopTypingRunnable != null) {
            typingHandler.removeCallbacks(stopTypingRunnable);
        }
        updateMyStatus("offline");
    }

    private void updateMyStatus(String status) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String myUid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(myUid);

            HashMap<String, Object> map = new HashMap<>();
            map.put("status", status); // "online", "offline", or "typing..."
            map.put("lastSeen", System.currentTimeMillis()); // optional

            userRef.updateChildren(map)
                    .addOnFailureListener(e -> {
                        Log.e("StatusUpdate", "Failed to update status", e);
                    });
        }
    }
}