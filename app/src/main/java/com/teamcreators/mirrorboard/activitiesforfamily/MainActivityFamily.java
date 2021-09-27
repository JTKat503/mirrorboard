package com.teamcreators.mirrorboard.activitiesforfamily;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

public class MainActivityFamily extends AppCompatActivity {

    private PreferenceManager preferenceManager;
    private MaterialButton newContact, newRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_family);

        preferenceManager = new PreferenceManager(getApplicationContext());
        newContact = findViewById(R.id.mainFamily_addContact_button);
        newRequests = findViewById(R.id.mainFamily_newRequests_button);

        // gain token from server
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    sendFCMTokenToDatabase(task.getResult());
                }
            }
        });

        // creating new contact button
        newContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivityFamily.this, AddContactActivityFamily.class);
                startActivity(intent);
            }
        });

        // checking new requests button
        newRequests.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivityFamily.this, InfoRequestActivityFamily.class);
                startActivity(intent);
            }
        });

    }


    private void sendFCMTokenToDatabase(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));

        documentReference.update(Constants.KEY_FCM_TOKEN, token).addOnFailureListener(e ->
                Toast.makeText(MainActivityFamily.this, "Unable to send token: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}