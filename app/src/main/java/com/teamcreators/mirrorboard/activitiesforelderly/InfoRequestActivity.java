package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * the page for showing the requests
 * if the user accept or remove request,
 * then will show the next new request or show "NO new request"
 *
 * @author Xuannan
 */
public class InfoRequestActivity extends AppCompatActivity {

    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    private TextView noRequest, contactName, contactNumber;
    private ImageView profileImage;
    private List<String> receiverUserFriends = new ArrayList<>();
    private List<String> senderUserFriends = new ArrayList<>();
    private Button addContact, removeRequest, goBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_request_elderly);

        addContact = findViewById(R.id.requestInfo_addContact_button);
        removeRequest = findViewById(R.id.requestInfo_removeRequest_button);
        db = FirebaseFirestore.getInstance();
        noRequest = findViewById(R.id.no_request);
        contactName = findViewById(R.id.requestInfo_contactName);
        contactNumber = findViewById(R.id.requestInfo_contactNumber);
        profileImage = findViewById(R.id.requestInfo_profileImage);
        goBack = findViewById(R.id.requestInfo_goBack_button);
        // save current user info
        preferenceManager = new PreferenceManager(getApplicationContext());
        // show the request user
        showUserProfile();
        contactName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        contactName.setMarqueeRepeatLimit(1);
        contactName.setSelected(true);

        // adding contact button
        addContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get the sender name
                TextView tempName = findViewById(R.id.requestInfo_contactName);
                String senderName = tempName.getText().toString();
                // get the sender phone
                TextView tempPhone = findViewById(R.id.requestInfo_contactNumber);
                String senderPhone = tempPhone.getText().toString();
                String myID = preferenceManager.getString(Constants.KEY_USER_ID);
                // get the receiver's friend list
                db.collection(Constants.KEY_COLLECTION_USERS)
                        .document(myID)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        receiverUserFriends = (List<String>) document.get(Constants.KEY_FRIENDS);
                                    }
                                    receiverUserFriends.add(senderPhone);
                                } else {
                                    Toast.makeText(getApplicationContext(), "Get failed with " +
                                            task.getException(), Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }
                        });
                // get the sender's friend list
                db.collection(Constants.KEY_COLLECTION_USERS)
                        .document(senderPhone)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        senderUserFriends  = (List<String>) document.get(Constants.KEY_FRIENDS);
                                    }
                                    senderUserFriends.add(preferenceManager.getString(Constants.KEY_PHONE));
                                } else {
                                    Toast.makeText(getApplicationContext(), "Get failed with " +
                                            task.getException(), Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }
                        });
                // add the new friends to the friend collection
                // add the sender phone number to receiver friend list
                // add the receiver phone number to sender friend list
                db.collection(Constants.KEY_COLLECTION_USERS)
                        .document(preferenceManager.getString(Constants.KEY_PHONE)) // current user
                        .update(Constants.KEY_FRIENDS, receiverUserFriends)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    db.collection(Constants.KEY_COLLECTION_USERS)
                                            .document(senderPhone)
                                            .update(Constants.KEY_FRIENDS, senderUserFriends)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        db.collection(Constants.KEY_COLLECTION_USERS)
                                                                .document(preferenceManager.getString(Constants.KEY_PHONE)) // current user
                                                                .update(Constants.KEY_FRIENDS, receiverUserFriends)
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if (task.isSuccessful()) {
                                                                            Toast.makeText(InfoRequestActivity.this, "Contact added", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                }
                                            });
                                } else {
                                    Toast.makeText(getApplicationContext(), "Cannot add friend list " +
                                            task.getException(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                // after adding the new friend
                // delete the request and update the number of requests
                deleteRequestAndUpdateRequestNumber(senderPhone);
                // show the next request or no request
                showUserProfile();
            }
        });

        // removing a Request button
        removeRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(InfoRequestActivity.this);
                builder.setMessage("Are you sure you want to delete this request?")
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                TextView tempPhone = findViewById(R.id.requestInfo_contactNumber);
                                String senderPhone = tempPhone.getText().toString();
                                deleteRequestAndUpdateRequestNumber(senderPhone);
                                showUserProfile();
                            }
                        }).show();
//                showUserProfile();
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
    }

    /**
     * show the request information
     * after the accept or remove the request, will show the next request
     * if no new request, will show the message "No new request"
     *
     * @author added by Xuannan
     */
    private void showUserProfile() {
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_PHONE))
                .collection("requests").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                String senderName;
                String senderPhone;
                if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                    DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                    senderName = documentSnapshot.getString(Constants.KEY_NAME);
                    senderPhone = documentSnapshot.getString(Constants.KEY_PHONE);
                    contactName.setText(senderName);
                    contactNumber.setText(senderPhone);
                    Glide.with(InfoRequestActivity.this)
                            .load(documentSnapshot.getString(Constants.KEY_AVATAR_URI))
                            .error(R.drawable.blank_profile)
                            .into(profileImage);
                    // ** @author  below added by Jianwei */
                    profileImage.setVisibility(View.VISIBLE);
                    contactName.setVisibility(View.VISIBLE);
                    contactNumber.setVisibility(View.VISIBLE);
                    addContact.setVisibility(View.VISIBLE);
                    removeRequest.setVisibility(View.VISIBLE);
                    goBack.setVisibility(View.VISIBLE);
                } else {
                    profileImage.setVisibility(View.INVISIBLE);
                    contactName.setVisibility(View.INVISIBLE);
                    contactNumber.setVisibility(View.INVISIBLE);
                    addContact.setVisibility(View.INVISIBLE);
                    removeRequest.setVisibility(View.INVISIBLE);
                    goBack.setVisibility(View.INVISIBLE);
                    noRequest.setText(R.string.no_new_requests);
                    noRequest.setVisibility(View.VISIBLE);
                    goBack.setVisibility(View.VISIBLE);
                    // ** @author  above added by Jianwei */
                }
            }
        });
    }

    /**
     * delete the request after accepting or removing
     * @param senderPhone
     *
     * @author added by Xuannan
     */
    public void deleteRequestAndUpdateRequestNumber(String senderPhone) {
        // delete the request from the database
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_PHONE))
                .collection("requests")
                .document(senderPhone).delete();
//        Toast.makeText(InfoRequestActivity.this, "Delete", Toast.LENGTH_SHORT).show();

        // update the requests number
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .update(Constants.KEY_NUM_OF_REQUESTS, FieldValue.increment(-1));
    }
}