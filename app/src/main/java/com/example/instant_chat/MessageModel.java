package com.example.instant_chat;

import java.util.HashMap;
import java.util.Map;

public class MessageModel {
    public String messageId;
    public String senderId;
    public String receiverId;
    public String message; // Used for text or caption for media
    public Long timestamp; // Using Long for Firebase compatibility
    public boolean seen;
    public boolean isDelivered;
    public boolean deleted; // Flag for "deleted for everyone"
    public Map<String, Boolean> deletedFor; // Map to track who deleted the message for themselves
    public String type; // e.g., "text", "image", "video", "deleted_sender"
    public String mediaUrl; // URL for images, videos, etc.
    public String fileName; // Optional, for documents or specific media names (if needed)


    public MessageModel() {
        // Initialize collections to avoid NullPointerExceptions later if data isn't present in Firebase
        this.deletedFor = new HashMap<>(); // Ensures it's never null when retrieving from Firebase
    }


    public MessageModel(String senderId, String receiverId, String message, Long timestamp, boolean seen, boolean isDelivered) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.seen = seen;
        this.isDelivered = isDelivered;
        this.deleted = false; // Default for new messages is not deleted for everyone
        this.deletedFor = new HashMap<>(); // Initialize for new messages
        this.type = "text"; // Default type for this constructor
    }

    public MessageModel(String senderUid, String receiverUid, String messageText, long l) {
    }

    // --- Getters ---
    public String getMessageId() {
        return messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getMessage() {
        return message;
    }

    public Long getTimestamp() { // Use Long here as it's defined as Long
        return timestamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Map<String, Boolean> getDeletedFor() {
        return deletedFor;
    }

    public String getType() {
        return type;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public String getFileName() {
        return fileName;
    }

    // --- Setters ---
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setSenderId(String senderId) { // Added missing setter
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) { // Added missing setter
        this.receiverId = receiverId;
    }

    public void setMessage(String message) { // Added missing setter
        this.message = message;
    }

    public void setTimestamp(Long timestamp) { // Use Long here
        this.timestamp = timestamp;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setDeletedFor(Map<String, Boolean> deletedFor) {
        this.deletedFor = deletedFor;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}