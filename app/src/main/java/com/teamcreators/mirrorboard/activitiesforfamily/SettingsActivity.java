package com.teamcreators.mirrorboard.activitiesforfamily;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.activitiesmutual.LoginActivity;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.NetworkConnection;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * A class for setting user avatars, names and management notifications for relatives
 *
 * @author Jianwei Li
 */
public class SettingsActivity extends AppCompatActivity {
    private Uri newAvatarUri;
    private SwitchCompat notificationSwitch;
    private PreferenceManager preferenceManager;
    ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferenceManager = new PreferenceManager(getApplicationContext());
        notificationSwitch = findViewById(R.id.settings_notificationSwitch);
        Button takePhoto = findViewById(R.id.settings_takePhoto);
        Button editName = findViewById(R.id.settings_editName);
        Button signOut = findViewById(R.id.settings_logout);
        ImageView goBack = findViewById(R.id.settings_back);
        Button exitApp = findViewById(R.id.settings_exitApp);
        LinearLayout offlineWarning = findViewById(R.id.settings_offlineWarning);

        // Monitor network connection changes
        NetworkConnection networkConnection = new NetworkConnection(getApplicationContext());
        networkConnection.observe(this, isConnected -> {
            if (isConnected) {
                offlineWarning.setVisibility(View.GONE);
            } else {
                offlineWarning.setVisibility(View.VISIBLE);
            }
        });

        cropImageLauncher = registerForActivityResult(
                new CropImageContract(), result -> {
                    if (result.isSuccessful()) {
                        newAvatarUri = result.getUriContent();
                        if (newAvatarUri != null) {
                            uploadImageToFirebaseStorage();
                        }
                    }
                });

        // retaking picture and uploading avatar button
        takePhoto.setOnClickListener(view -> {
            newAvatarUri = null;
            Dexter.withContext(SettingsActivity.this)
                    .withPermissions(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA
                    )
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                            startCroppingFromCamera();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                            permissionToken.continuePermissionRequest();
                        }
                    }).check();
        });

        // editing nickname button
        editName.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
            final EditText newName = new EditText(SettingsActivity.this);
            newName.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setTitle("Enter a new name")
                    .setView(newName)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        String name = newName.getText().toString();
                        if (!name.trim().isEmpty()) {
                            updateUserName(name);
                        }
                    }).show();
        });

        // switch button for notification
        initiateNotificationStatus();
        notificationSwitch.setOnClickListener(view ->
                setNotificationTo(notificationSwitch.isChecked()));

        // goBack button
        goBack.setOnClickListener(view -> {
            onBackPressed();
            finish();
        });

        // signing out button
        signOut.setOnClickListener(view -> signOut());

        // exit app button on the offline warning page
        exitApp.setOnClickListener(view -> {
            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });
    }

    /**
     * Start camera to get image for cropping and then use the image in cropping activity
     */
    private void startCroppingFromCamera() {
        CropImageContractOptions options = new CropImageContractOptions(null, new CropImageOptions());
        options.setActivityTitle("Crop Image")
                .setAspectRatio(1,1)
                .setRequestedSize(500,500)
                .setOutputCompressFormat(Bitmap.CompressFormat.PNG);
        cropImageLauncher.launch(options);
    }

    /**
     * Upload cropped image to Firebase storage as user's avatar
     * If successful, call the function updateUserAvatar()
     */
    private void uploadImageToFirebaseStorage() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("Uploading image...");
        dialog.show();
        final String randomKey = UUID.randomUUID().toString();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference uploader = storage.getReference().child("images/" + randomKey);

        uploader.putFile(newAvatarUri)
                .addOnSuccessListener(taskSnapshot -> {
                    dialog.dismiss();
                    updateUserAvatar(uploader);
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    dialog.setMessage("Percentage: " + (int)progress + "%");
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(
                            getApplicationContext(),
                            "Failed to upload",
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Update the database and local user avatar to the new avatar
     * @param uploader Instance of Firebase storage reference
     */
    private void updateUserAvatar(StorageReference uploader) {
        uploader.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    String userID = preferenceManager.getString(Constants.KEY_USER_ID);
                    FirebaseFirestore database = FirebaseFirestore.getInstance();
                    database.collection(Constants.KEY_COLLECTION_USERS)
                            .document(userID)
                            .update(Constants.KEY_AVATAR_URI, uri.toString())
                            .addOnSuccessListener(unused -> {
                                preferenceManager.putString(Constants.KEY_AVATAR_URI, uri.toString());
                                Snackbar.make(
                                        findViewById(android.R.id.content),
                                        "New avatar saved",
                                        Snackbar.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            SettingsActivity.this,
                                            "Failed to save new avatar: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                SettingsActivity.this,
                                "Failed to save new avatar: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Update the database and local user name to the new nick name
     * @param newName New nick name that needs to be updated
     */
    private void updateUserName(String newName) {
        String userID = preferenceManager.getString(Constants.KEY_USER_ID);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userID)
                .update(Constants.KEY_NAME, newName)
                .addOnSuccessListener(unused -> {
                    preferenceManager.putString(Constants.KEY_NAME, newName);
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "New name saved",
                            Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                SettingsActivity.this,
                                "Failed to save new name: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Check whether the user's notification status is turned on
     * If the value of notice_on field stored in the user's database is true -> ON
     * otherwise -> OFF
     */
    private void initiateNotificationStatus() {
        String myID = preferenceManager.getString(Constants.KEY_USER_ID);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(myID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            boolean noticeStatus = document.getBoolean(Constants.KEY_NOTICE_ON);
                            notificationSwitch.setChecked(noticeStatus);
                        }
                    } else {
                        Toast.makeText(
                                getApplicationContext(),
                                "Failed to access database",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Set the value of the field that whether allows notification or not
     * in the user's database to the given value
     * @param value The value (true/false) of the notification field to be set
     */
    private void setNotificationTo(boolean value) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_NOTICE_ON, value);
        documentReference.update(updates)
                .addOnFailureListener(e ->
                        Toast.makeText(
                                SettingsActivity.this,
                                "Failed with: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Log the user out of the app
     */
    private void signOut() {
        Snackbar.make(findViewById(
                android.R.id.content),
                "Logging Out...",
                Snackbar.LENGTH_SHORT).show();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
        // delete user's FCM token from database while signing out
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clearPreferences();
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                SettingsActivity.this,
                                "Unable to log out: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}