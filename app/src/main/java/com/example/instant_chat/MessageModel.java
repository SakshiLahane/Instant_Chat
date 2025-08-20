package com.example.instant_chat;

import java.util.HashMap;
import java.util.Map;

public class MessageModel {

    // Make all fields private for proper encapsulation
    private String messageId;
    private String senderId;
    private String receiverId;
    private String message;
    private Long timestamp;
    private boolean seen;
    private boolean delivered; // Renamed for consistency with other booleans
    private boolean deleted;
    private Map<String, Boolean> deletedFor;
    private String type;
    private String mediaUrl;
    private String fileName;

    // Public no-argument constructor required for Firebase
    public MessageModel() {
        this.deletedFor = new HashMap<>();
    }

    // Constructor for creating new messages with essential fields
    public MessageModel(String senderId, String receiverId, String message, Long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.seen = false;
        this.delivered = false; // Initialized to false
        this.deleted = false;
        this.deletedFor = new HashMap<>();
        this.type = "text";
    }

    // Full constructor for more complex initializations (e.g., media messages)
    public MessageModel(String senderId, String receiverId, String message, Long timestamp, boolean seen, boolean delivered, boolean deleted, Map<String, Boolean> deletedFor, String type, String mediaUrl, String fileName) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.seen = seen;
        this.delivered = delivered;
        this.deleted = deleted;
        this.deletedFor = deletedFor != null ? deletedFor : new HashMap<>();
        this.type = type;
        this.mediaUrl = mediaUrl;
        this.fileName = fileName;
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

    public Long getTimestamp() {
        return timestamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public boolean isDelivered() { // Keeping this getter for backward compatibility, but 'delivered' is the field name
        return delivered;
    }

    public boolean getDelivered() { // Added a getter with "get" prefix for Firebase
        return delivered;
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

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public void setDelivered(boolean delivered) { // Updated to use the 'delivered' field name
        this.delivered = delivered;
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