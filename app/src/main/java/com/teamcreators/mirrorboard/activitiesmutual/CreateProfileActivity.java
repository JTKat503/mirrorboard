package com.teamcreators.mirrorboard.activitiesmutual;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
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
import com.teamcreators.mirrorboard.activitiesforelderly.MainActivityElderly;
import com.teamcreators.mirrorboard.activitiesforfamily.MainActivityFamily;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 *
 */
public class CreateProfileActivity extends AppCompatActivity {

    private EditText nickName;
    private ImageView avatar;
    private Uri avatarUri;
    private String mode, phone, password;
    private PreferenceManager preferenceManager;
    ActivityResultLauncher<Intent> chooseImageLauncher;
    ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

        // receive info from previous interface
        Bundle bundle = getIntent().getExtras();
        mode = bundle.getString("mode");
        phone = bundle.getString("phone");
        password = bundle.getString("password");

        nickName = findViewById(R.id.createProfile_nickname);
        avatar = findViewById(R.id.createProfile_profileImage);
        preferenceManager = new PreferenceManager(getApplicationContext());


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
                        avatarUri = result.getUriContent();
                        avatar.setImageURI(avatarUri);
                    }
                    else {
                        Log.e("CIL", Objects.requireNonNull(result.getError()).getMessage());
                    }
                });

        // imageView for loading avatar
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Dynamic permission application
                Dexter.withContext(CreateProfileActivity.this)
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                Intent intent = new Intent(Intent.ACTION_PICK);
                                intent.setType("image/*");
                                chooseImageLauncher.launch(Intent.createChooser(intent, "Select Image File"));
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
        findViewById(R.id.createProfile_takePicture_button).setOnClickListener(view -> {
            Dexter.withContext(CreateProfileActivity.this)
                    .withPermissions(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA)
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

        // back button, back to CreateAccount interface
        findViewById(R.id.createProfile_back_button).setOnClickListener(view -> {
            // After backing, save the information filled in the CreateAccount interface
            onBackPressed();
            finish();
        });

        // creating account button
        findViewById(R.id.createProfile_create_button).setOnClickListener(view -> {
            if (nickName.getText().toString().trim().isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }
            // allows users to use the default profile avatar
            if (avatarUri == null) {
                Resources resources = CreateProfileActivity.this.getResources();
                avatarUri = (new Uri.Builder())
                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                        .authority(resources.getResourcePackageName(R.drawable.blank_profile))
                        .appendPath(resources.getResourceTypeName(R.drawable.blank_profile))
                        .appendPath(resources.getResourceEntryName(R.drawable.blank_profile))
                        .build();
            }
            uploadImageToFirebaseStorage();
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
     * Start cropping activity for pre-acquired image saved on the device
     * @param imagePath The local path of the image to be cropped
     */
    private void startCroppingFromGallery(Uri imagePath) {
        CropImageContractOptions options = new CropImageContractOptions(imagePath, new CropImageOptions());
        options.setActivityTitle("Crop Image")
                .setAspectRatio(1,1)
                .setRequestedSize(250,250)
                .setOutputCompressFormat(Bitmap.CompressFormat.PNG);
        cropImageLauncher.launch(options);
    }

    /**
     * Upload cropped image to Firebase storage as user's avatar
     * If successful, call the function signUp()
     */
    private void uploadImageToFirebaseStorage() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("Uploading Image...");
        dialog.show();

        final String randomKey = UUID.randomUUID().toString();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference uploader = storage.getReference().child("images/" + randomKey);

        uploader.putFile(avatarUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        dialog.dismiss();
                        Snackbar.make(findViewById(android.R.id.content), "Image Uploaded", Snackbar.LENGTH_SHORT).show();
                        signUp(uploader);
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
                        Toast.makeText(getApplicationContext(), "Failed to Upload", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Save user registration information to the database,
     * and update the locally stored user information using 'preferenceManager'
     * If successful, call the function startUserMode()
     * @param uploader the content of uploaded avatar information
     */
    private void signUp(StorageReference uploader) {
        uploader.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                HashMap<String, Object> user = new HashMap<>();
                user.put(Constants.KEY_MODE, mode);
                user.put(Constants.KEY_PHONE, phone);
                user.put(Constants.KEY_PASSWORD, password);
                user.put(Constants.KEY_NAME, nickName.getText().toString());
                user.put(Constants.KEY_AVATAR_URI, uri.toString());

                FirebaseFirestore database = FirebaseFirestore.getInstance();
                final String subPath = phone;
                database.collection(Constants.KEY_COLLECTION_USERS)
                        .document(subPath)
                        .set(user)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, phone);
                        preferenceManager.putString(Constants.KEY_MODE, mode);
                        preferenceManager.putString(Constants.KEY_PHONE, phone);
                        preferenceManager.putString(Constants.KEY_NAME, nickName.getText().toString());
//                        preferenceManager.putString(Constants.KEY_PASSWORD, password);
                        preferenceManager.putString(Constants.KEY_AVATAR_URI, uri.toString());
                        startUserMode();
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CreateProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

//                database.collection(Constants.KEY_COLLECTION_USERS)
//                        .add(user)
//                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                            @Override
//                            public void onSuccess(DocumentReference documentReference) {
//                                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
//                                preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
//                                preferenceManager.putString(Constants.KEY_PHONE, phone);
//                                preferenceManager.putString(Constants.KEY_NAME, nickName.getText().toString());
//                                preferenceManager.putString(Constants.KEY_PASSWORD, PW);
//                                preferenceManager.putString(Constants.KEY_AVATAR_URI, uri.toString());
//                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                startActivity(intent);
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Toast.makeText(CreateProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                            }
//                        });
            }
        });
    }

    /**
     * Enter the application mode selected by user
     */
    private void startUserMode() {
        Intent intent;
        if (mode != null && mode.equals(Constants.MODE_ELDERLY)) {
            intent = new Intent(getApplicationContext(), MainActivityElderly.class);
        } else {
            intent = new Intent(getApplicationContext(), MainActivityFamily.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

}