package com.teamcreators.mirrorboard.activitiesforelderly;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.activitiesmutual.CallOutgoingActivity;
import com.teamcreators.mirrorboard.adapters.UsersAdapterElderly;
import com.teamcreators.mirrorboard.listeners.UsersListener;
import com.teamcreators.mirrorboard.models.User;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivityElderly extends AppCompatActivity implements UsersListener {

    private PreferenceManager preferenceManager;
    private MaterialButton newContact, newRequests, exit, hobbies;
    private List<User> contacts;
    private UsersAdapterElderly contactsAdapter;
    private TextView textErrorMessage;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_elderly);

        preferenceManager = new PreferenceManager(getApplicationContext());
        newContact = findViewById(R.id.main_addContact_button);
        newRequests = findViewById(R.id.main_newRequests_button);
        hobbies = findViewById(R.id.main_hobbies_button);
        exit = findViewById(R.id.main_exit_button);

        // gains token from Messaging server then send it to database
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
                Intent intent = new Intent(MainActivityElderly.this, AddContactActivity.class);
                startActivity(intent);
            }
        });

        // checking new requests button
        newRequests.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivityElderly.this, InfoRequestActivity.class);
                startActivity(intent);
            }
        });

        // matching hobbies button
        hobbies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivityElderly.this, StartActivityActivity.class);
                startActivity(intent);
            }
        });

        // exit app button
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moveTaskToBack(true);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        });

        // building and loading contacts list
        RecyclerView contactsRecyclerView = findViewById(R.id.main_contacts_RecyclerView);
        textErrorMessage = findViewById(R.id.main_errorMessage_textView);
        contacts = new ArrayList<>();
        contactsAdapter = new UsersAdapterElderly(contacts, this);
        contactsRecyclerView.setAdapter(contactsAdapter);
        swipeRefreshLayout = findViewById(R.id.main_swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::getContacts);
        getContacts();
    }

    private void getContacts() {
        swipeRefreshLayout.setRefreshing(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        swipeRefreshLayout.setRefreshing(false);
                        String myUserID = preferenceManager.getString(Constants.KEY_USER_ID);
                        if (task.isSuccessful() && task.getResult() != null) {
                            contacts.clear();
                            for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                // display the contacts except the currently logged-in uer
                                if (myUserID.equals(documentSnapshot.getId())) {
                                    continue;
                                }
                                User contact = new User();
                                contact.name = documentSnapshot.getString(Constants.KEY_NAME);
                                contact.phone = documentSnapshot.getString(Constants.KEY_PHONE);
                                contact.avatarUri = documentSnapshot.getString(Constants.KEY_AVATAR_URI);
                                contact.token = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                contacts.add(contact);
                            }
                            if (contacts.size() > 0) {
                                contactsAdapter.notifyDataSetChanged();
                            } else {
                                textErrorMessage.setText(String.format("%s", "No contacts available"));
                                textErrorMessage.setVisibility(View.VISIBLE);
                            }
                        } else {
                            textErrorMessage.setText(String.format("%s", "No contacts available"));
                            textErrorMessage.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void sendFCMTokenToDatabase(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));

        documentReference.update(Constants.KEY_FCM_TOKEN, token).addOnFailureListener(e ->
                        Toast.makeText(MainActivityElderly.this,
                                "Unable to send token: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void initiateVideoMeeting(User user) {
//        if (user.token == null || user.token.trim().isEmpty()) {
//            Toast.makeText(
//                    this,
//                    user.name + " is not available",
//                    Toast.LENGTH_SHORT
//            ).show();
//        } else {
//            Intent intent = new Intent(getApplicationContext(), CallOutgoingActivity.class);
//            intent.putExtra("user", user);
//            intent.putExtra("type", "video");
//            startActivity(intent);
//        }
    }

    @Override
    public void initiateAudioMeeting(User user) {

    }

    @Override
    public void checkContactInformation(User user) {
        Intent intent = new Intent(getApplicationContext(), InfoContactActivity.class);
        intent.putExtra("user", user);
        startActivity(intent);
    }

    @Override
    public void onMultipleUsersAction(Boolean isMultipleUsersSelected) {

    }
}