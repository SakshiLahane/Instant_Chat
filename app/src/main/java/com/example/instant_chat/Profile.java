package com.example.instant_chat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

public class Profile extends AppCompatActivity {

    ImageView profileImage;
    TextView editProfile, nameText, aboutText;
    Button nextButton;
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;

    FirebaseAuth auth;
    FirebaseFirestore firestore;
    StorageReference storageRef;
    Uri imageUri;

    String[] aboutOptions = {
            "Student", "Working", "On break", "Available", "Battery about to die",
            "In a meeting", "Urgent calls only", "At College", "Enter your own About"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileImage = findViewById(R.id.profileImage);
        editProfile = findViewById(R.id.editProfile);
        nameText = findViewById(R.id.nameText);
        aboutText = findViewById(R.id.aboutText);
        nextButton = findViewById(R.id.nextbtn);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        loadSavedData(); // local cache
        loadUserDataFromFirebase(); // from Firestore

        nameText.setOnClickListener(v -> showNameDialog());
        aboutText.setOnClickListener(v -> showAboutDialog());
        editProfile.setOnClickListener(this::openImageOptions);
        nextButton.setOnClickListener(v -> startActivity(new Intent(Profile.this, Home.class)));
    }

    private void loadSavedData() {
        SharedPreferences prefs = getSharedPreferences("profile_data", MODE_PRIVATE);
        nameText.setText(prefs.getString("name", ""));
        aboutText.setText(prefs.getString("about", ""));
        String profileUri = prefs.getString("profile_uri", "");

        if (!profileUri.isEmpty()) {
            if (profileUri.startsWith("https://")) {
                Glide.with(this).load(profileUri).placeholder(R.drawable.person).into(profileImage);
            } else {
                profileImage.setImageURI(Uri.parse(profileUri));
            }
        }
    }

    private void loadUserDataFromFirebase() {
        String userId = auth.getCurrentUser().getUid();

        firestore.collection("Users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String about = document.getString("about");
                        String imageUrl = document.getString("profileImage");

                        if (name != null) {
                            nameText.setText(name);
                            saveToPrefs("name", name);
                        }
                        if (about != null) {
                            aboutText.setText(about);
                            saveToPrefs("about", about);
                        }
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl).placeholder(R.drawable.person).into(profileImage);
                            saveToPrefs("profile_uri", imageUrl);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show());
    }

    private void saveToPrefs(String key, String value) {
        SharedPreferences prefs = getSharedPreferences("profile_data", MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    private void showNameDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter your name");

        new AlertDialog.Builder(this)
                .setTitle("Edit Name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        nameText.setText(name);
                        saveNameToFirebase(name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveNameToFirebase(String name) {
        String userId = auth.getCurrentUser().getUid();

        firestore.collection("Users").document(userId)
                .update("name", name)
                .addOnSuccessListener(unused -> {
                    saveToPrefs("name", name);
                    Toast.makeText(this, "Name saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save name: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Status");

        builder.setItems(aboutOptions, (dialog, which) -> {
            if (aboutOptions[which].equals("Enter your own About")) {
                showCustomAboutInput();
            } else {
                aboutText.setText(aboutOptions[which]);
                saveAboutToFirebase(aboutOptions[which]);
            }
        });

        builder.show();
    }

    private void showCustomAboutInput() {
        EditText input = new EditText(this);
        input.setHint("Enter your own status");

        new AlertDialog.Builder(this)
                .setTitle("Custom About")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String about = input.getText().toString().trim();
                    if (!about.isEmpty()) {
                        aboutText.setText(about);
                        saveAboutToFirebase(about);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveAboutToFirebase(String about) {
        String userId = auth.getCurrentUser().getUid();

        firestore.collection("Users").document(userId)
                .update("about", about)
                .addOnSuccessListener(unused -> {
                    saveToPrefs("about", about);
                    Toast.makeText(this, "About saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save about: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    public void openImageOptions(View view) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.activity_profile_bottomsheet, null);
        dialog.setContentView(sheetView);

        sheetView.findViewById(R.id.cameraOption).setOnClickListener(v -> {
            dialog.dismiss();
            openCamera();
        });

        sheetView.findViewById(R.id.galleryOption).setOnClickListener(v -> {
            dialog.dismiss();
            openGallery();
        });

        sheetView.findViewById(R.id.avatarOption).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "Avatar option clicked", Toast.LENGTH_SHORT).show();
        });

        sheetView.findViewById(R.id.aiImageOption).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "AI Image option clicked", Toast.LENGTH_SHORT).show();
        });

        sheetView.findViewById(R.id.deleteImage).setOnClickListener(v -> {
            dialog.dismiss();
            profileImage.setImageResource(R.drawable.person);
            saveToPrefs("profile_uri", "");
        });

        sheetView.findViewById(R.id.closeButton).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openCamera() {
        File photoFile = new File(getExternalFilesDir(null), "camera_pic_" + System.currentTimeMillis() + ".jpg");
        imageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(cameraIntent, REQUEST_CAMERA);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && imageUri != null) {
                startCrop(imageUri);
            } else if (requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
                startCrop(data.getData());
            } else if (requestCode == UCrop.REQUEST_CROP && data != null) {
                Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    profileImage.setImageURI(resultUri);
                    saveToPrefs("profile_uri", resultUri.toString());
                    uploadImageToFirebase(resultUri);
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
            Throwable cropError = UCrop.getError(data);
            Toast.makeText(this, "Crop Error: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startCrop(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg"));

        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setCompressionQuality(80);
        options.setToolbarTitle("Crop Image");
        options.setFreeStyleCropEnabled(true);

        UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withOptions(options)
                .start(this);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        String userId = auth.getCurrentUser().getUid();
        StorageReference fileRef = storageRef.child("profile_images/" + userId + ".jpg");

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    firestore.collection("Users").document(userId)
                            .update("profileImage", uri.toString());
                    Toast.makeText(this, "Profile uploaded", Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
