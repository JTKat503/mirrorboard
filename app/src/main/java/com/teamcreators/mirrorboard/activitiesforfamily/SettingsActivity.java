package com.teamcreators.mirrorboard.activitiesforfamily;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.activitiesforelderly.EditProfileActivity;
import com.teamcreators.mirrorboard.activitiesmutual.LoginActivity;
import com.teamcreators.mirrorboard.utilities.Constants;
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
        notificationSwitch = findViewById(R.id.family_settings_notificationSwitch);
        Button takePhoto = findViewById(R.id.family_settings_takePhoto);
        Button editName = findViewById(R.id.family_settings_editName);
        Button signOut = findViewById(R.id.family_settings_logout);
        ImageView goBack = findViewById(R.id.family_settings_back);

        cropImageLauncher = registerForActivityResult(
                new CropImageContract(), result -> {
                    if (result.isSuccessful()) {
                        newAvatarUri = result.getUriContent();
                        if (newAvatarUri != null) {
                            uploadImageToFirebaseStorage();
                        }
                    }
                    else {
                        Toast.makeText(getApplicationContext(),
                                "Failed to start cropper", Toast.LENGTH_SHORT).show();
                    }
                });

        // retaking picture and uploading avatar button
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });

        // editing nickname button
        editName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                final EditText newName = new EditText(SettingsActivity.this);
                newName.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setTitle("Enter a new name")
                        .setView(newName)
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String name = newName.getText().toString();
                                if (!name.trim().isEmpty()) {
                                    updateUserName(name);
                                }
                            }
                        }).show();
            }
        });

        // switch button for notification
        notificationSwitch.setChecked(false);
        isNotificationOn();
        notificationSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (notificationSwitch.isChecked()) {
                    // gains token from Messaging server then send it to database
                    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
                        @Override
                        public void onComplete(@NonNull Task<String> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                sendFCMTokenToDatabase(task.getResult());
                            }
                        }
                    });
                } else {
                    removeToken();
                }
            }
        });

        // goBack button
        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
        });

        // signing out button
        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });
    }

    /**
     * Start camera to get image for cropping and then use the image in cropping activity
     */
    private void startCroppingFromCamera() {
        CropImageContractOptions options = new CropImageContractOptions(null, new CropImageOptions());
        options.setActivityTitle("Crop Image")
                .setAspectRatio(1,1)
                .setRequestedSize(250,250)
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
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        dialog.dismiss();
                        updateUserAvatar(uploader);
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        dialog.setMessage("Percentage: " + (int)progress + "%");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Failed to upload", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Update the database and local user avatar to the new avatar
     * @param uploader Instance of Firebase storage reference
     */
    private void updateUserAvatar(StorageReference uploader) {
        uploader.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String userID = preferenceManager.getString(Constants.KEY_USER_ID);
                        FirebaseFirestore database = FirebaseFirestore.getInstance();
                        database.collection(Constants.KEY_COLLECTION_USERS)
                                .document(userID)
                                .update(Constants.KEY_AVATAR_URI, uri.toString())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        preferenceManager.putString(Constants.KEY_AVATAR_URI, uri.toString());
                                        Snackbar.make(findViewById(android.R.id.content),
                                                "New avatar saved", Snackbar.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(SettingsActivity.this,
                                                "Failed to save new avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SettingsActivity.this,
                                "Failed to save new avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        preferenceManager.putString(Constants.KEY_NAME, newName);
                        Snackbar.make(findViewById(android.R.id.content),
                                "New name saved", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SettingsActivity.this,
                                "Failed to save new name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Set the notification switch to on
     */
    private void setNotificationSwitchOn() {
        notificationSwitch.setChecked(true);
    }

    /**
     * Check whether the user's notification function is turned on
     * If there is a token stored in the user's database -> ON
     * otherwise -> OFF
     */
    private void isNotificationOn() {
        String myID = preferenceManager.getString(Constants.KEY_USER_ID);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(myID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String token = document.getString(Constants.KEY_FCM_TOKEN);
                                if (token != null && !token.trim().isEmpty()) {
                                    setNotificationSwitchOn();
                                }
                            }
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Failed to access database", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Send the personal FCM token used for communication to the database for storage
     * @param token the personal FCM token used for communication
     */
    private void sendFCMTokenToDatabase(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));

        documentReference.update(Constants.KEY_FCM_TOKEN, token).addOnFailureListener(e ->
                Toast.makeText(SettingsActivity.this,
                        "Unable to send token: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Remove the user's token from the database
     */
    private void removeToken() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
        // delete user's FCM token from database
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SettingsActivity.this,
                                "Unable to switch off: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Log the user out of the app
     */
    private void signOut() {
        Snackbar.make(findViewById(android.R.id.content), "Logging Out...", Snackbar.LENGTH_SHORT).show();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));

        // delete user's FCM token from database while signing out
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        preferenceManager.clearPreferences();
                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SettingsActivity.this,
                                "Unable to log out: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


}