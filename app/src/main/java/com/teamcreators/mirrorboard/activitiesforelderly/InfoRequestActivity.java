package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
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

public class InfoRequestActivity extends AppCompatActivity {

    // ** @author  below added by Xuannan */
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    private TextView NoRequest, contactName, contactNumber;
    private ImageView profileImage;
    private List<String> receiverUserFriends = new ArrayList<>();
    private List<String> senderUserFriends = new ArrayList<>();
    // ** @author  above added by Xuannan */
    private Button addContact, removeRequest, nextRequest, goBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_request_elderly);

        addContact = findViewById(R.id.requestInfo_addContact_button);
        removeRequest = findViewById(R.id.requestInfo_removeRequest_button);
        // ** @author  below added by Xuannan */
        db = FirebaseFirestore.getInstance();
        NoRequest = findViewById(R.id.No_request);
        contactName = findViewById(R.id.requestInfo_contactName);
        contactNumber = findViewById(R.id.requestInfo_contactNumber);
        profileImage = findViewById(R.id.requestInfo_profileImage);
        nextRequest = findViewById(R.id.requestInfo_nextRequest_button);
        goBack = findViewById(R.id.requestInfo_goBack_button);
        // save the current user information
        preferenceManager = new PreferenceManager(getApplicationContext());
        // show the request user
        showUserProfile();
        // ** @author  above added by Xuannan */

        contactName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        contactName.setMarqueeRepeatLimit(1);
        contactName.setSelected(true);

        // adding contact button
        // ** @author  below added by Xuannan */
        addContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get the sender name
                TextView tempName = findViewById(R.id.requestInfo_contactName);
                String senderName = tempName.getText().toString();
                // can add the photo
                // get the sender phone
                TextView tempPhone = findViewById(R.id.requestInfo_contactNumber);
                String senderPhone = tempPhone.getText().toString();
                String myID = preferenceManager.getString(Constants.KEY_USER_ID);
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
//                                                                            Toast.makeText(InfoRequestActivity.this, "Success Adding", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                }
                                            });
                                } else {
                                    Toast.makeText(getApplicationContext(), "cannot add friend list " +
                                            task.getException(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                deleteRequestAndUpdateRequestNumber(senderPhone);
                // show the next request or no request
                showUserProfile();
                // ** @author  above added by Xuannan */
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
                // ** @author  below added by Xuannan */
                                // delete request, go back to main page
                                TextView tempPhone = findViewById(R.id.requestInfo_contactNumber);
                                String senderPhone = tempPhone.getText().toString();
                                deleteRequestAndUpdateRequestNumber(senderPhone);
                                showUserProfile();
                            }
                        }).show();
                showUserProfile();
                // ** @author  above added by Xuannan */
            }
        });

        // next Request button
        nextRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // to do
                // if this is the last request, show hint
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
     *
     * @author  below added by Xuannan
     */
    private void showUserProfile() {
        // haven't add the photo
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
                } else {
                    NoRequest.setVisibility(View.VISIBLE);
                    contactName.setVisibility(View.INVISIBLE);
                    contactNumber.setVisibility(View.INVISIBLE);
                    profileImage.setVisibility(View.INVISIBLE);
                    addContact.setVisibility(View.INVISIBLE);
                    removeRequest.setVisibility(View.INVISIBLE);
                    nextRequest.setVisibility(View.INVISIBLE);

                }
            }
        });

    }


    /**
     * @param senderPhone
     *
     * @author  below added by Xuannan
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