package com.example.instant_chat;

public class UserModel {
    private String uid;
    private String name;
    private String email;
    private String profileImageUrl;
    private long lastMessageTime;
    private String lastMessage; // Added this field
    private String type; // e.g., "admin", "user"

    // ✅ Default constructor (required for Firebase deserialization)
    public UserModel() {
        this.lastMessageTime = 0;
        this.lastMessage = "";
    }

    // ✅ Constructor without lastMessage and lastMessageTime (for new users)
    public UserModel(String uid, String name, String email, String profileImageUrl, String type) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.type = type;
        this.lastMessageTime = 0;
        this.lastMessage = "";
    }

    // ✅ Full constructor with all fields
    public UserModel(String uid, String name, String email, String profileImageUrl, long lastMessageTime, String lastMessage, String type) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.lastMessageTime = lastMessageTime;
        this.lastMessage = lastMessage != null ? lastMessage : "";
        this.type = type;
    }

    // ✅ Backward compatibility constructor (your old constructor)
    public UserModel(String uid, String name, String email, String profileImageUrl, long lastMessageTime, String type) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.lastMessageTime = lastMessageTime;
        this.type = type;
        this.lastMessage = "";
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

    public String getLastMessage() {
        return lastMessage != null ? lastMessage : "";
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

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage != null ? lastMessage : "";
    }

    public void setType(String type) {
        this.type = type;
    }

    // ✅ Helper methods
    public boolean hasMessages() {
        return lastMessageTime > 0;
    }

    public String getDisplayLastMessage() {
        if (lastMessage == null || lastMessage.isEmpty()) {
            return "No messages yet";
        }

        // Truncate long messages for display
        if (lastMessage.length() > 50) {
            return lastMessage.substring(0, 47) + "...";
        }

        return lastMessage;
    }

    // ✅ For debugging
    @Override
    public String toString() {
        return "UserModel{" +
                "uid='" + uid + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                ", lastMessageTime=" + lastMessageTime +
                ", lastMessage='" + lastMessage + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    // ✅ Equals and hashCode for proper comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserModel userModel = (UserModel) o;
        return uid != null && uid.equals(userModel.uid);
    }

    @Override
    public int hashCode() {
        return uid != null ? uid.hashCode() : 0;
    }
}