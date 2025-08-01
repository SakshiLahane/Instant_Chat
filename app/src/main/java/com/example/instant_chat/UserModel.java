package com.example.instant_chat;

public class UserModel {
    private String uid;
    private String name;
    private String email;
    private String profileImageUrl;
    private long lastMessageTime;
    private String type; // e.g., "admin", "user"

    // ✅ Default constructor (required for Firebase deserialization)
    public UserModel() {}

    // ✅ Full constructor with all fields
    public UserModel(String uid, String name, String email, String profileImageUrl, long lastMessageTime, String type) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.lastMessageTime = lastMessageTime;
        this.type = type;
    }

    // ✅ Getters
    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public String getType() {
        return type;
    }

    // ✅ Setters
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public void setType(String type) {
        this.type = type;
    }
}
