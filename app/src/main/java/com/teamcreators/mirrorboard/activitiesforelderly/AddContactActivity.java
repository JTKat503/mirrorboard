package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author  all below added by Xuannan
 */
public class AddContactActivity extends AppCompatActivity {
    private String receiverUserPhone, senderUserPhone, nickName;
    private Button sendRequest, goBack;
    private final String TAG = "AddContactActivity";
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;
    boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact_elderly);

        db = FirebaseFirestore.getInstance();
        goBack = findViewById(R.id.addContact_goBack_button);
        sendRequest = findViewById(R.id.addContact_sendRequest_button);
        sendRequest.setSelected(false);
        // save the current user information
        preferenceManager = new PreferenceManager(getApplicationContext());
        // current user's phone
        senderUserPhone = preferenceManager.getString(Constants.KEY_PHONE);

        // Click the send request button
        sendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get the receiver's Phone
                EditText getPhone = findViewById(R.id.addContact_phoneNum);
                receiverUserPhone = getPhone.getText().toString();
                // get the nick name
                EditText inputNickName = findViewById(R.id.addContact_nickname);
                nickName = inputNickName.getText().toString();
                getContactsIDs();

            }
        });

        // go back button
        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
        });
    }

    private void manageChatRequests() {
        sendRequest.setEnabled(false);
        Map<String, Object> sender = new HashMap<>();
        sender.put(Constants.KEY_PHONE, preferenceManager.getString(Constants.KEY_PHONE));
        sender.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
        sender.put(Constants.KEY_AVATAR_URI, preferenceManager.getString(Constants.KEY_AVATAR_URI));
        // need to add the user photo
        db.collection("users").document(receiverUserPhone).collection("requests").document(senderUserPhone)
                .set(sender)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            sendRequest.setEnabled(true);
                            sendRequest.setText(R.string.request_sent);
                            sendRequest.setSelected(true);
                        }
                    }
                });
        increaseRequests();
    }

    private void getContactsIDs() {
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                List<String> myFriendsIDs = (List<String>) document.get(Constants.KEY_FRIENDS);
                                sendRequest(myFriendsIDs);
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        "Failed to get contacts list", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            Toast.makeText(getApplicationContext(), "Get failed with " +
                                    task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void sendRequest(List<String> myFriendsIDs) {
        // search user from the data base
        if (TextUtils.isEmpty(receiverUserPhone)) {         // check the input number
            Toast.makeText(AddContactActivity.this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
        } else if (receiverUserPhone.equals(senderUserPhone)) {     // check the input number == own number
            Toast.makeText(AddContactActivity.this, "You cannot add yourself", Toast.LENGTH_SHORT).show();
        } else if (myFriendsIDs.contains(receiverUserPhone)) {      // Check if the number is your friend
            Toast.makeText(AddContactActivity.this, "You are already friends.", Toast.LENGTH_SHORT).show();
        } else {
            db.collection(Constants.KEY_COLLECTION_USERS)
                    .whereEqualTo(Constants.KEY_PHONE, receiverUserPhone)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if (queryDocumentSnapshots.isEmpty()) {
                                // user does not exist
                                Toast.makeText(AddContactActivity.this, "User does not exist", Toast.LENGTH_SHORT).show();
                            } else {
                                // user exist, send the request
                                manageChatRequests();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "onFailure: " + e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void increaseRequests() {
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(receiverUserPhone)
                .collection("requests")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.getId().equals(senderUserPhone)){
                                    flag = true;
                                }
                            }
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Error getting documents: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        if (!flag) {
            db.collection(Constants.KEY_COLLECTION_USERS)
                    .document(receiverUserPhone)
                    .update(Constants.KEY_NUM_OF_REQUESTS, FieldValue.increment(1));
        }
    }
}