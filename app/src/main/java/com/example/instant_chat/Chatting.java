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
import android.util.TypedValue;
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
import androidx.transition.AutoTransition; // NEW: Import for transition
import androidx.transition.TransitionManager; // NEW: Import for transition

import com.google.firebase.auth.FirebaseAuth;
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

public class Chatting extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText inputMessage;
    private ImageButton sendBtn;
    private TextView chatUserName, chatStatus;
    private ImageButton attachBtn, galleryBtn, cameraBtn, emojiBtn;
    private RelativeLayout messageInputContainer; // Reference to the parent RelativeLayout of input bar

    private List<MessageModel> messageList = new ArrayList<>();
    private MessageAdapter adapter;

    private String senderUid;
    private String receiverUid;
    private String receiverName;

    private DatabaseReference userRef;
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

        // Initialize views
        recyclerView = findViewById(R.id.chatRecyclerView);
        inputMessage = findViewById(R.id.messageBox);
        sendBtn = findViewById(R.id.sendBtn);
        chatUserName = findViewById(R.id.chatUserName);
        chatStatus = findViewById(R.id.chatStatus);
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton videoCallBtn = findViewById(R.id.videocall);
        attachBtn = findViewById(R.id.attachBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        cameraBtn = findViewById(R.id.cameraBtn);
        emojiBtn = findViewById(R.id.emojiBtn);
        messageInputContainer = findViewById(R.id.messageInputContainer); // The parent RelativeLayout

        // No need to set initial margin here, as layout_toStartOf in XML already defines it.
        // RelativeLayout.LayoutParams messageBoxParams = (RelativeLayout.LayoutParams) inputMessage.getLayoutParams();
        // messageBoxParams.setMarginEnd(initialMessageBoxEndMarginPx);
        // inputMessage.setLayoutParams(messageBoxParams);


        // Set up action bar and navigation
        backButton.setOnClickListener(v -> finish());

        // Get UIDs and user name from the intent
        senderUid = FirebaseAuth.getInstance().getUid();
        receiverUid = getIntent().getStringExtra("receiverUid");
        receiverName = getIntent().getStringExtra("name");

        if (receiverName != null) {
            chatUserName.setText(receiverName);
        } else {
            chatUserName.setText("User");
        }

        // Set up RecyclerView and adapter
        adapter = new MessageAdapter(this, messageList, senderUid);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Set the long click listener for the adapter to delete messages
        adapter.setOnMessageLongClickListener(this::showDeleteDialog);

        // Load messages, observe status, and handle typing indicators
        loadMessages();
        observeReceiverStatus();
        handleTyping();

        // Handle button clicks
        sendBtn.setOnClickListener(v -> {
            String text = inputMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
            } else {
                Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show();
            }
        });

        videoCallBtn.setOnClickListener(v -> {
            // Use the receiver's UID as the channel name for simplicity
            String roomName = receiverUid;
            notifyReceiverForCall(roomName);

            Intent intent = new Intent(Chatting.this, Videocall.class);
            intent.putExtra("channelName", roomName);
            startActivity(intent);
        });

        attachBtn.setOnClickListener(v -> toggleAttachmentButtons());

        // Initial setup for hidden buttons if not already in XML
        // This is handled by XML now, but good to double check if something breaks initial state
        galleryBtn.setVisibility(View.GONE);
        galleryBtn.setAlpha(0f);
        cameraBtn.setVisibility(View.GONE);
        cameraBtn.setAlpha(0f);

        // Gallery picker
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        sendImageMessage(selectedImage);
                    }
                });

        // Camera capture
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                isSuccess -> {
                    if (isSuccess && imageUri != null) {
                        sendImageMessage(imageUri);
                    }
                });

        galleryBtn.setOnClickListener(v -> openGallery());
        cameraBtn.setOnClickListener(v -> openCamera());

    }

    private void openCamera() {
        imageUri = createImageUri(); // see below
        cameraLauncher.launch(imageUri);
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }
    // CORRECTED sendImageMessage method - using "chats" node
    private void sendImageMessage(Uri imageUri) {
        String messageText = inputMessage.getText().toString().trim();

        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("chat_images")
                .child(System.currentTimeMillis() + ".jpg");

        ref.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();

                                // Use your actual "chats" node
                                DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
                                String messageId = chatsRef.push().getKey();
                                if (messageId == null) return;

                                MessageModel message = new MessageModel(
                                        senderUid,
                                        receiverUid,
                                        messageText.isEmpty() ? "" : messageText, // Caption or empty
                                        System.currentTimeMillis()
                                );

                                // Set image-specific properties
                                message.setMessageId(messageId);
                                message.setType("image");
                                message.setMediaUrl(imageUrl);
                                message.setSeen(false);
                                message.setDelivered(false);
                                message.setDeleted(false);
                                message.setDeletedFor(new HashMap<>());

                                // Save to chats node
                                chatsRef.child(messageId).setValue(message)
                                        .addOnSuccessListener(aVoid -> {
                                            inputMessage.setText(""); // Clear input

                                            // Update chatList for both users
                                            DatabaseReference chatListRef = FirebaseDatabase.getInstance().getReference("chatList");
                                            chatListRef.child(senderUid).child(receiverUid).setValue(true);
                                            chatListRef.child(receiverUid).child(senderUid).setValue(true);

                                            Toast.makeText(this, "Image sent!", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to send image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            Log.e("Chatting", "Failed to send image", e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    /**
     * Converts DP to Pixels.
     * @param dp The DP value to convert.
     * @return The equivalent value in pixels.
     */
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    /**
     * Toggles the visibility and animation of attachment buttons (gallery and camera).
     * Also adjusts the messageBox width to prevent overlap.
     */
    private void toggleAttachmentButtons() {
        // Get LayoutParams for messageBox to change its rules dynamically
        RelativeLayout.LayoutParams messageBoxParams = (RelativeLayout.LayoutParams) inputMessage.getLayoutParams();

        // Start a transition on the parent layout to animate layout changes
        // The duration here applies to how long the layout changes (like messageBox shrinking/expanding) take.
        TransitionManager.beginDelayedTransition(messageInputContainer, new AutoTransition().setDuration(250)); // Adjusted duration for smoother feel

        if (!isAttachmentVisible) {
            // SHOW attachment buttons and adjust messageBox position

            // 1. Make camera and gallery visible (layout will be affected by TransitionManager)
            cameraBtn.setVisibility(View.VISIBLE);
            galleryBtn.setVisibility(View.VISIBLE);

            // 2. Animate alpha for fade-in effect (translationX is not needed here as layout_toStartOf handles positioning)
            galleryBtn.animate().alpha(1f).setDuration(250).start();
            cameraBtn.animate().alpha(1f).setDuration(250).start();

            // 3. Crucially: Change messageBox's layout_toStartOf property
            // This tells RelativeLayout to make space for galleryBtn.
            messageBoxParams.removeRule(RelativeLayout.END_OF); // Remove any conflicting rule
            messageBoxParams.removeRule(RelativeLayout.START_OF); // Remove old layout_toStartOf rule (e.g., @id/attachBtn)
            messageBoxParams.addRule(RelativeLayout.START_OF, R.id.galleryBtn); // Set new rule to be to the start of galleryBtn
            inputMessage.setLayoutParams(messageBoxParams); // Apply new layout params
            // inputMessage.requestLayout(); // TransitionManager typically handles this, but can be explicit if needed

        } else {
            // HIDE attachment buttons and revert messageBox position

            // 1. Animate alpha for fade-out effect
            galleryBtn.animate().alpha(0f).setDuration(250).start();
            cameraBtn.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> {
                        // After both animations complete, set visibility to GONE
                        // and reset the messageBox constraint.
                        // This `withEndAction` ensures layout change only happens after hide animation
                        galleryBtn.setVisibility(View.GONE);
                        cameraBtn.setVisibility(View.GONE);

                        messageBoxParams.removeRule(RelativeLayout.END_OF); // Clear conflicting rules
                        messageBoxParams.removeRule(RelativeLayout.START_OF); // Remove old layout_toStartOf rule (e.g., @id/galleryBtn)
                        messageBoxParams.addRule(RelativeLayout.START_OF, R.id.attachBtn); // Reset to be to the start of attachBtn
                        inputMessage.setLayoutParams(messageBoxParams); // Apply new layout params
                        // inputMessage.requestLayout(); // TransitionManager typically handles this
                    })
                    .start();
        }
        isAttachmentVisible = !isAttachmentVisible;
    }

    // --- (rest of your methods: showDeleteDialog, deleteMessageForMe, deleteMessageForEveryone,
    // notifyReceiverForCall, sendMessage, loadMessages, observeReceiverStatus, updateMyStatus, handleTyping) ---

    // (Your existing methods from here onwards are good to go, no changes needed for them)

    /**
     * Displays an AlertDialog to the user for deleting a message.
     * @param message The MessageModel object to be deleted.
     */
    private void showDeleteDialog(MessageModel message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Chatting.this);
        builder.setTitle("Delete Message");

        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        // Option 1: Delete for Everyone (only available for the sender)
        if (message.getSenderId().equals(currentUserId)) {
            builder.setNeutralButton("Delete for everyone", (dialog, which) -> {
                deleteMessageForEveryone(message.getMessageId(), message.getSenderId());
            });
        }

        // Option 2: Delete for Me
        builder.setPositiveButton("Delete for me", (dialog, which) -> {
            deleteMessageForMe(message.getMessageId(), currentUserId, message.getSenderId());
        });

        // Option 3: Cancel
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    /**
     * Deletes a message only for the current user by setting a flag in the database.
     * The message will no longer be visible to the current user.
     * @param messageId The unique ID of the message.
     * @param currentUserId The UID of the current user.
     * @param senderId The UID of the message sender.
     */
    private void deleteMessageForMe(String messageId, String currentUserId, String senderId) {
        if (currentUserId == null) {
            Toast.makeText(this, "Error: Current user not identified.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference msgRef = FirebaseDatabase.getInstance().getReference("chats").child(messageId);
        msgRef.child("deletedFor").child(currentUserId).setValue(true)
                .addOnSuccessListener(aVoid -> {
                    if (!currentUserId.equals(senderId)) {
                        Toast.makeText(Chatting.this, "This message was deleted.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Chatting.this, "Message deleted for you.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(Chatting.this, "Failed to delete message for you.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Replaces a message's content with a "deleted" message for all participants in the chat.
     * @param messageId The unique ID of the message.
     * @param senderId The UID of the message sender.
     */
    private void deleteMessageForEveryone(String messageId, String senderId) {
        DatabaseReference messageRef = FirebaseDatabase.getInstance().getReference("chats").child(messageId);

        HashMap<String, Object> map = new HashMap<>();
        map.put("message", "You deleted this message");
        map.put("type", "deleted_sender");
        map.put("deleted", true);

        messageRef.updateChildren(map)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Chatting.this, "You deleted this message", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(Chatting.this, "Failed to delete message for everyone.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Sends a notification to the receiver's Firebase node to alert them of an incoming video call.
     * @param roomName The channel name for the video call.
     */
    private void notifyReceiverForCall(String roomName) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("calls").child(receiverUid);
        HashMap<String, Object> callData = new HashMap<>();
        callData.put("from", senderUid);
        callData.put("room", roomName);
        callData.put("type", "video");
        callData.put("timestamp", System.currentTimeMillis());

        ref.setValue(callData);
    }

    /**
     * Sends a text message to the database.
//     * @param text The message content.
     */
    // In Chatting.java, inside sendMessage(String text) method:
    private void sendMessage(String messageText) {
        if (messageText.trim().isEmpty()) return;
        if (senderUid == null || receiverUid == null) return;

        // Use your actual "chats" node
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        String messageId = chatsRef.push().getKey();
        if (messageId == null) return;

        MessageModel message = new MessageModel(
                senderUid,
                receiverUid,
                messageText,
                System.currentTimeMillis()
        );
        message.setMessageId(messageId);
        message.setSeen(false);
        message.setDelivered(false);
        message.setDeleted(false);
        message.setDeletedFor(new HashMap<>());

        chatsRef.child(messageId).setValue(message)
                .addOnSuccessListener(unused -> {
                    inputMessage.setText(""); // Clear input

                    // Update chatList for both users
                    DatabaseReference chatListRef = FirebaseDatabase.getInstance().getReference("chatList");
                    chatListRef.child(senderUid).child(receiverUid).setValue(true);
                    chatListRef.child(receiverUid).child(senderUid).setValue(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Chatting.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    Log.e("Chatting", "Failed to send message", e);
                });
    }
    /**
     * Listens for and loads messages from Firebase for the current chat.
     * It also handles updating the "seen" and "delivered" status of received messages.
     */
    // CORRECTED loadMessages method - using "chats" node
    private void loadMessages() {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    MessageModel msg = ds.getValue(MessageModel.class);
                    if (msg == null || senderUid == null || receiverUid == null) continue;

                    // Skip messages deleted for the current user
                    if (msg.getDeletedFor() != null && msg.getDeletedFor().containsKey(senderUid)) {
                        continue;
                    }

                    // Check if the message belongs to this specific chat
                    boolean sentByMeToReceiver = msg.getSenderId().equals(senderUid) && msg.getReceiverId().equals(receiverUid);
                    boolean receivedByMeFromSender = msg.getSenderId().equals(receiverUid) && msg.getReceiverId().equals(senderUid);

                    if (sentByMeToReceiver || receivedByMeFromSender) {
                        messageList.add(msg);

                        // Mark received messages as delivered and seen
                        if (receivedByMeFromSender) {
                            String messageId = ds.getKey();
                            if (!msg.isDelivered()) {
                                chatsRef.child(messageId).child("delivered").setValue(true);
                            }
                            if (!msg.isSeen()) {
                                chatsRef.child(messageId).child("seen").setValue(true);
                            }
                        }
                    }
                }

                adapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Chatting.this, "Error loading messages", Toast.LENGTH_SHORT).show();
                Log.e("Chatting", "Error loading messages", error.toException());
            }
        });
    }

    /**
     * Observes the receiver's status (online, typing, or last seen) and updates the UI accordingly.
     */
    private void observeReceiverStatus() {
        userRef = FirebaseDatabase.getInstance().getReference("users").child(receiverUid);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.child("status").getValue(String.class);
                Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);

                if (status != null) {
                    if (status.equals("online")) {
                        chatStatus.setText("Online");
                    } else if (status.equals("typing...")) {
                        chatStatus.setText("typing...");
                    } else if (lastSeen != null) {
                        chatStatus.setText("Last seen " + TimeUtilsLastSeen.formatLastSeen(lastSeen));
                    } else {
                        chatStatus.setText("Offline");
                    }
                } else if (lastSeen != null) {
                    chatStatus.setText("Last seen " + TimeUtilsLastSeen.formatLastSeen(lastSeen));
                } else {
                    chatStatus.setText("Offline");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Log.e("Chatting", "Failed to observe receiver status", error.toException());
            }
        });
    }

    /**
     * Updates the current user's status in the database (online, offline, typing...).
     * @param status The status string to set.
     */
    private void updateMyStatus(String status) {
        if (senderUid != null) {
            DatabaseReference userStatusRef = FirebaseDatabase.getInstance().getReference("users").child(senderUid);

            HashMap<String, Object> statusMap = new HashMap<>();
            statusMap.put("status", status);

            if (status.equals("offline")) {
                statusMap.put("lastSeen", System.currentTimeMillis());
            }

            userStatusRef.updateChildren(statusMap);
        }
    }

    /**
     * Sets up a TextWatcher to handle typing indicators.
     * A "typing..." status is sent, which reverts to "online" after a 2-second delay if no more text is entered.
     */
    private void handleTyping() {
        stopTypingRunnable = () -> updateMyStatus("online");

        inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateMyStatus("typing...");
                typingHandler.removeCallbacks(stopTypingRunnable);
                typingHandler.postDelayed(stopTypingRunnable, 2000);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
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
}