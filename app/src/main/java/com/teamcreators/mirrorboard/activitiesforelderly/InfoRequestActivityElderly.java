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
import com.google.android.material.snackbar.Snackbar;
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
 * @author Xuannan Huang
 */
public class InfoRequestActivityElderly extends AppCompatActivity {
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    private TextView noRequest, contactName, contactNumber;
    private ImageView profileImage;
    private Button addContact, removeRequest, goBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_request_elderly);

        db = FirebaseFirestore.getInstance();
        addContact = findViewById(R.id.elderly_infoRequest_add);
        removeRequest = findViewById(R.id.elderly_infoRequest_remove);
        noRequest = findViewById(R.id.elderly_infoRequest_errorMessage);
        contactName = findViewById(R.id.elderly_infoRequest_name);
        contactNumber = findViewById(R.id.elderly_infoRequest_phone);
        profileImage = findViewById(R.id.elderly_infoRequest_avatar);
        goBack = findViewById(R.id.elderly_infoRequest_back);
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
                // get the sender phone
                String senderPhone = contactNumber.getText().toString();
                // get current user ID
                String receiverPhone = preferenceManager.getString(Constants.KEY_PHONE);
                // get and update the receiver's friend list
                // add the sender phone number to receiver friend list
                getAndUpdateFriendList(receiverPhone, senderPhone);
                // get and update the sender's friend list
                // add the receiver phone number to sender friend list
                getAndUpdateFriendList(senderPhone, receiverPhone);
                // show the message to tell the user Contact added
                Snackbar.make(findViewById(android.R.id.content), "Contact added", Snackbar.LENGTH_SHORT).show();
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
                AlertDialog.Builder builder = new AlertDialog.Builder(InfoRequestActivityElderly.this);
                builder.setMessage("Are you sure you want to delete this request?")
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                TextView tempPhone = findViewById(R.id.elderly_infoRequest_phone);
                                String senderPhone = tempPhone.getText().toString();
                                deleteRequestAndUpdateRequestNumber(senderPhone);
                                showUserProfile();
                            }
                        }).show();
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
                    Glide.with(InfoRequestActivityElderly.this)
                            .load(documentSnapshot.getString(Constants.KEY_AVATAR_URI))
                            .error(R.drawable.blank_profile)
                            .into(profileImage);
                    // ** @author  below added by Jianwei Li */
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
                    // ** @author  above added by Jianwei Li */
                }
            }
        });
    }

    /**
     * get and update the receiver's friend list, add the sender phone number to receiver friend list
     * get and update the sender's friend list, add the receiver phone number to sender friend list
     * @param userPhone
     * @param phoneNumber
     */
    private void getAndUpdateFriendList(String userPhone, String phoneNumber) {
        // get the receiver's friend list
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(userPhone)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            List<String> UserFriendsList = new ArrayList<>();
                            if (document.exists()) {
                                UserFriendsList = (List<String>) document.get(Constants.KEY_FRIENDS);
                            }
                            UserFriendsList.add(phoneNumber);
                            // update the friends list
                            addFriendToList(userPhone, UserFriendsList);
                        } else {
                            Toast.makeText(getApplicationContext(), "Get failed with " +
                                    task.getException(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
    }

    /**
     * Update current user's contacts list
     * @param userPhone current user's phone number
     * @param friendsList contacts list to be updated to database
     */
    public void addFriendToList(String userPhone, List<String> friendsList) {
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(userPhone) // current user
                .update(Constants.KEY_FRIENDS, friendsList);
    }

    /**
     * delete the request after accepting or removing
     * @param senderPhone
     */
    public void deleteRequestAndUpdateRequestNumber(String senderPhone) {
        // delete the request from the database
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_PHONE))
                .collection("requests")
                .document(senderPhone).delete();
        // update the requests number
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .update(Constants.KEY_NUM_OF_REQUESTS, FieldValue.increment(-1));
    }
}