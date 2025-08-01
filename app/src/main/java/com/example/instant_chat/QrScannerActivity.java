package com.example.instant_chat; // Make sure this matches your package name

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.Toast;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.io.InputStream;
import java.util.Arrays; // Added for setting decode formats
import java.util.Collection; // Added for setting decode formats
import java.util.List;
import com.google.zxing.BarcodeFormat; // Added for setting decode formats
import com.journeyapps.barcodescanner.DefaultDecoderFactory;


public class QrScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 101; // New request code for clarity

    private DecoratedBarcodeView barcodeView;
    private ImageButton flashButton;
    private boolean isFlashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.scanner_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide default title
        }

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish()); // Go back when back button is clicked

        barcodeView = findViewById(R.id.barcode_scanner);
        flashButton = findViewById(R.id.flashButton);
        ImageButton uploadButton = findViewById(R.id.uploadImageButton);


        // Configure BarcodeView for QR codes only (optional, but good for specific scanners)
        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));


        flashButton.setOnClickListener(v -> toggleFlash());

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanner();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }

        uploadButton.setOnClickListener(v -> {
            // Check for Android 13 (API 33) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU is API 33
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                            READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE); // Re-using the same request code
                } else {
                    pickImageFromGallery();
                }
            } else { // For Android 12 (API 32) and below
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
                } else {
                    pickImageFromGallery();
                }
            }
        });
    }


    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    decodeQRCodeFromImage(selectedImageUri);
                }
            });

    private void decodeQRCodeFromImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap == null) {
                Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
                return;
            }

            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0,
                    bitmap.getWidth(), bitmap.getHeight());

            LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            MultiFormatReader reader = new MultiFormatReader(); // Direct instantiation
            Result result = null;
            try {
                result = reader.decode(binaryBitmap); // No cast needed
                String scannedText = result.getText();
                Toast.makeText(this, "QR from Gallery: " + scannedText, Toast.LENGTH_LONG).show();
                // TODO: Handle scanned QR content here (e.g., pass to another activity)
                // For now, let's just finish
                finish();
            } catch (NotFoundException e) {
                Toast.makeText(this, "No QR code found in the selected image.", Toast.LENGTH_LONG).show();
                Log.e("QrScannerActivity", "No QR code found in image: " + e.getMessage());
            } catch (Exception e) { // Catch any other decoding errors
                Toast.makeText(this, "Error decoding QR from image.", Toast.LENGTH_SHORT).show();
                Log.e("QrScannerActivity", "Error decoding QR from image: " + e.getMessage());
            } finally {
                if (inputStream != null) {
                    inputStream.close(); // Ensure input stream is closed
                }
            }

        } catch (Exception e) {
            Toast.makeText(this, "Failed to decode QR from image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("QrScannerActivity", "Exception in decodeQRCodeFromImage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startScanner() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    barcodeView.pause(); // Pause scanning after a result is found
                    String scannedContent = result.getText();
                    Toast.makeText(QrScannerActivity.this, "Scanned: " + scannedContent, Toast.LENGTH_LONG).show();
                    Log.d("QrScannerActivity", "Scanned QR Code: " + scannedContent);

                    // TODO: Handle scanned QR content here
                    finish(); // Close the scanner activity after scanning
                }
            }

            @Override
            public void possibleResultPoints(List<com.google.zxing.ResultPoint> resultPoints) {
                // Optional: Draw scan points
            }
        });

        barcodeView.setVisibility(View.VISIBLE);
        barcodeView.resume(); // Start scanning
        updateFlashButtonState(); // Set flash button state
    }


    private void toggleFlash() {
        if (isFlashOn) {
            barcodeView.setTorchOff();
            isFlashOn = false;
        } else {
            barcodeView.setTorchOn();
            isFlashOn = true;
        }
        updateFlashButtonState();
    }

    private void updateFlashButtonState() {
        // Check if device has a flash
        boolean hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        flashButton.setVisibility(hasFlash ? View.VISIBLE : View.GONE);

        if (hasFlash) {
            if (isFlashOn) {
                flashButton.setImageResource(R.drawable.ic_flash_on_white_24dp);
            } else {
                flashButton.setImageResource(R.drawable.ic_flash_off_white_24dp);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure camera resumes when activity comes to foreground
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause camera when activity goes to background
        barcodeView.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanner(); // Permission granted, start scanner
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_LONG).show();
                finish(); // Close activity if permission is denied
            }
        } else if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) { // Handle gallery permission
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery();
            } else {
                Toast.makeText(this, "Storage permission is required to pick images from gallery.", Toast.LENGTH_LONG).show();
            }
        }
    }
}