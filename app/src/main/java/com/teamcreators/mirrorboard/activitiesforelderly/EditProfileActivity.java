package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.activitiesmutual.LoginActivity;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * A class for setting users' avatars, names for elderly
 *
 * @author Jianwei Li
 */
public class EditProfileActivity extends AppCompatActivity {
    private EditText newName;
    private ImageView avatar;
    private Uri newAvatarUri;
    private PreferenceManager preferenceManager;
    ActivityResultLauncher<Intent> chooseImageLauncher;
    ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Button takePicture = findViewById(R.id.editProfile_takePicture_button);
        Button keepChanges = findViewById(R.id.editProfile_keepChanges_button);
        Button goBack = findViewById(R.id.editProfile_goBack_button);
        Button signOut = findViewById(R.id.editProfile_signOut_button);
        avatar = findViewById(R.id.editProfile_profileImage);
        newName = findViewById(R.id.editProfile_newNickname);
        preferenceManager = new PreferenceManager(getApplicationContext());

        // pre-setting user's avatar, loading avatar from Firebase storage
        Uri avatarUri = Uri.parse(preferenceManager.getString(Constants.KEY_AVATAR_URI));
        Glide.with(this)
                .load(avatarUri)
                .fitCenter()
                .error(R.drawable.blank_profile)
                .into(avatar);

        // pre-setting the hint of user's name
        String oldName = preferenceManager.getString(Constants.KEY_NAME);
        if (oldName != null && !oldName.isEmpty()) {
            newName.setHint(oldName);
        }

        // an alternative method for startActivityForResult() & onActivityResult()
        chooseImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imagePath = result.getData().getData();
                    startCroppingFromGallery(imagePath);
                }
            }
        });

        cropImageLauncher = registerForActivityResult(
                new CropImageContract(), result -> {
                    if (result.isSuccessful()) {
                        newAvatarUri = result.getUriContent();
                        avatar.setImageURI(newAvatarUri);
                    }
                });

        // imageView for resetting avatar
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Dynamic Permission Application
                Dexter.withContext(EditProfileActivity.this)
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                Intent intent = new Intent(Intent.ACTION_PICK);
                                intent.setType("image/*");
                                chooseImageLauncher.launch(Intent.createChooser(intent, "Select Image"));
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                                permissionToken.continuePermissionRequest();
                            }
                        }).check();
            }
        });

        // takePicture button
        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Dynamic Permission Application
                Dexter.withContext(EditProfileActivity.this)
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

        // keepChanges button
        keepChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newNickName = newName.getText().toString();
                if (newNickName.trim().isEmpty() && newAvatarUri == null) {
                    Toast.makeText(getApplicationContext(), "No new changes to save", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newNickName.trim().isEmpty() && !(newAvatarUri == null)) {
                    // update new name and new avatar to database
                    uploadImageToFirebaseStorage(newNickName);
                } else if (!newNickName.trim().isEmpty()) {
                    // update new name
                    updateUserName(newNickName);
                }else if (!(newAvatarUri == null)) {
                    // update new avatar
                    uploadImageToFirebaseStorage(null);
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

        // sign out
        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });
    }

    /**
     * start cropping activity for pre-acquired image saved on the device
     * @param imagePath Local path (URI) of the image to be acquired from gallery
     */
    private void startCroppingFromGallery(Uri imagePath) {
        CropImageContractOptions options = new CropImageContractOptions(imagePath, new CropImageOptions());
        options.setActivityTitle("Crop Image")
                .setAspectRatio(1,1)
                .setRequestedSize(500,500)
                .setOutputCompressFormat(Bitmap.CompressFormat.PNG);
        cropImageLauncher.launch(options);
    }

    /**
     * start camera to get image for cropping and then use the image in cropping activity
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
                        Toast.makeText(EditProfileActivity.this,
                                "Unable to log out: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Upload cropped image to Firebase storage as user's avatar
     * If successful:
     *      if newName is null, call the function updateUserAvatar()
     *      if newName is not null, call the function updateUserNameAndAvatar()
     * @param newName New nick name that needs to be updated
     */
    private void uploadImageToFirebaseStorage(String newName) {
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
                        if (newName == null) {
                            updateUserAvatar(uploader);
                        } else {
                            updateUserNameAndAvatar(uploader, newName);
                        }
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
                        Toast.makeText(EditProfileActivity.this,
                                "Failed to save new name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(EditProfileActivity.this,
                                                "Failed to save new avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditProfileActivity.this,
                                "Failed to save new avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Update the database and local username and avatar to new username and avatar
     * @param uploader Instance of Firebase storage reference
     * @param newName New nick name that needs to be updated
     */
    private void updateUserNameAndAvatar(StorageReference uploader, String newName) {
        uploader.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String userID = preferenceManager.getString(Constants.KEY_USER_ID);
                        FirebaseFirestore database = FirebaseFirestore.getInstance();
                        database.collection(Constants.KEY_COLLECTION_USERS)
                                .document(userID)
                                .update(
                                        Constants.KEY_NAME, newName,
                                        Constants.KEY_AVATAR_URI, uri.toString()
                                )
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        preferenceManager.putString(Constants.KEY_AVATAR_URI, uri.toString());
                                        Snackbar.make(findViewById(android.R.id.content),
                                                "New name and avatar saved", Snackbar.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(EditProfileActivity.this,
                                                "Failed to save new name and avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditProfileActivity.this,
                                "Failed to save new name and avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}