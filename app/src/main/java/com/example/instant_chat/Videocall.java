package com.example.instant_chat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class Videocall extends AppCompatActivity {

    private static final String APP_ID = "13b27dcc228f4e0e9a5c049cbb1eb740"; // Your App ID
    private static final String TOKEN = "007eJxTYKiQqL07V2DFnb/G74X3zTbPOHFN7eGWyWFqP7VnJ4o6C75WYDA0TjIyT0lONjKySDNJNUi1TDRNNjCxTE5KMkxNMjcxEApuyGgIZGTY/u0OCyMDBIL43AwlqcUlyRmJeXmpOQwMAIquI5o="; // A *valid, non-expired* token

    private String channelName;
    private RtcEngine rtcEngine;

    private FrameLayout localContainer, remoteContainer;
    private SurfaceView localView, remoteView;

    private ImageButton joinBtn, leaveBtn;
    private boolean autoJoin = false; // Flag to determine if we should auto-join

    private final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d("Agora", "Join success: " + channel + ", uid: " + uid);
            runOnUiThread(() -> {
                Toast.makeText(Videocall.this, "Joined channel: " + channel, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            Log.d("Agora", "User joined: " + uid);
            runOnUiThread(() -> setupRemoteVideo(uid));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            Log.d("Agora", "User offline: " + uid + ", reason: " + reason);
            runOnUiThread(() -> {
                if (remoteView != null) {
                    remoteContainer.removeView(remoteView);
                    remoteView = null;
                }
                Toast.makeText(Videocall.this, "User " + uid + " left the call.", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onError(int err) {
            Log.e("Agora", "Agora Error: " + err);
            runOnUiThread(() -> {
                Toast.makeText(Videocall.this, "Agora Error: " + err + " - Check logs.", Toast.LENGTH_LONG).show();
                // Common errors: 101 (INVALID_APP_ID), 102 (INVALID_CHANNEL_NAME), 109 (TOKEN_EXPIRED), 110 (INVALID_TOKEN)
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videocall);

        localContainer = findViewById(R.id.local_video_view_container);
        remoteContainer = findViewById(R.id.remote_video_view_container);
        joinBtn = findViewById(R.id.btn_join);
        leaveBtn = findViewById(R.id.btn_leave);

        channelName = getIntent().getStringExtra("channelName");
        autoJoin = getIntent().getBooleanExtra("autoJoinCall", false); // ✅ NEW: Get auto-join flag

        if (channelName == null || channelName.isEmpty()) {
            Toast.makeText(this, "Error: Channel name not provided.", Toast.LENGTH_LONG).show();
            Log.e("Videocall", "Channel name is null or empty!");
            finish();
            return;
        }

        if (checkSelfPermission()) {
            initializeAgoraEngine();
            if (autoJoin) { // ✅ NEW: Automatically join if autoJoin is true
                joinChannel();
                joinBtn.setVisibility(View.GONE); // Hide join button as it's auto-joined
            }
        } else {
            requestPermissions();
        }

        joinBtn.setOnClickListener(v -> {
            if (rtcEngine != null) {
                joinChannel();
                joinBtn.setVisibility(View.GONE);
                Toast.makeText(this, "Attempting to join call...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Agora Engine not initialized. Check permissions.", Toast.LENGTH_LONG).show();
            }
        });

        leaveBtn.setOnClickListener(v -> {
            leaveChannel();
            finish();
        });
    }

    private void initializeAgoraEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = APP_ID;
            config.mEventHandler = rtcEventHandler;

            rtcEngine = RtcEngine.create(config);
            rtcEngine.enableVideo();
            setupLocalVideo();
            Log.d("Agora", "Agora Engine initialized and local video set up.");

        } catch (Exception e) {
            Log.e("Agora", "Agora SDK Init Failed: " + e.getMessage(), e);
            Toast.makeText(this, "Video call initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Do not throw RuntimeException here, handle gracefully
        }
    }

    private void joinChannel() {
        if (rtcEngine == null) {
            Toast.makeText(this, "RTC Engine is null, cannot join channel.", Toast.LENGTH_SHORT).show();
            return;
        }
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;

        if (channelName != null && !channelName.isEmpty()) {
            rtcEngine.joinChannel(TOKEN, channelName, 0, options);
            Log.d("Agora", "Attempting to join channel: " + channelName);
        } else {
            Toast.makeText(this, "Invalid channel name.", Toast.LENGTH_SHORT).show();
            Log.e("Agora", "Channel name is null or empty, cannot join.");
        }
    }

    private void setupLocalVideo() {
        if (rtcEngine == null) return;

        if (localContainer.getChildCount() > 0) {
            localContainer.removeAllViews();
        }
        localView = new SurfaceView(getBaseContext());
        localView.setZOrderMediaOverlay(true);
        localContainer.addView(localView);

        VideoCanvas videoCanvas = new VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0);
        rtcEngine.setupLocalVideo(videoCanvas);
        rtcEngine.startPreview();
        Log.d("Agora", "Local video preview started.");
    }

    private void setupRemoteVideo(int uid) {
        if (rtcEngine == null) return;

        if (remoteContainer.getChildCount() > 0) {
            remoteContainer.removeAllViews();
        }
        remoteView = new SurfaceView(getBaseContext());
        remoteContainer.addView(remoteView);

        VideoCanvas videoCanvas = new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid);
        rtcEngine.setupRemoteVideo(videoCanvas);
        Log.d("Agora", "Remote video setup for UID: " + uid);
    }

    private void leaveChannel() {
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            Log.d("Agora", "Left channel.");
        }
        if (localContainer != null && localView != null) {
            localContainer.removeView(localView);
            localView = null;
        }
        if (remoteContainer != null && remoteView != null) {
            remoteContainer.removeView(remoteView);
            remoteView = null;
        }
        Toast.makeText(this, "Call ended.", Toast.LENGTH_SHORT).show();
    }

    private boolean checkSelfPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        }, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (checkSelfPermission()) {
            initializeAgoraEngine();
            if (autoJoin) { // If permissions are granted later, try to auto-join
                joinChannel();
                joinBtn.setVisibility(View.GONE);
            }
        } else {
            Toast.makeText(this, "Permissions are required for video call.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveChannel();
        if (rtcEngine != null) {
            RtcEngine.destroy();
            rtcEngine = null;
        }
        Log.d("Agora", "RTC Engine destroyed.");
    }
}