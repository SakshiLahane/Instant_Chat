package com.example.instant_chat;

public class userUpload {
    public String name;
    public String email;
    public String uid;

    public userUpload() {
        // Required for Firestore
    }

    public userUpload(String name, String email, String uid) {
        this.name = name;
        this.email = email;
        this.uid = uid;
    }
}
