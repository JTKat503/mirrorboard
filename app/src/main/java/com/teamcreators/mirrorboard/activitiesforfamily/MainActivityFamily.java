package com.teamcreators.mirrorboard.activitiesforfamily;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.activitiesmutual.OutgoingCallActivity;
import com.teamcreators.mirrorboard.adapters.UsersAdapter;
import com.teamcreators.mirrorboard.listeners.ItemsListener;
import com.teamcreators.mirrorboard.models.Hobby;
import com.teamcreators.mirrorboard.models.User;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.NetworkConnection;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that displays the main interface of the APP for relatives,
 * including a contact list, adding new contacts and viewing requests functions
 *
 * @author Jianwei Li & Xuannan Huang
 */
public class MainActivityFamily extends AppCompatActivity implements ItemsListener {
    private PreferenceManager preferenceManager;
    private List<User> contacts;
    private UsersAdapter contactsAdapter;
    private TextView errorMessage, requestsNumber;
    private SwipeRefreshLayout contactsLayout;
    private ImageView conference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_family);

        preferenceManager = new PreferenceManager(getApplicationContext());
        errorMessage = findViewById(R.id.family_main_errorMessage);
        conference = findViewById(R.id.family_main_conference);
        contactsLayout = findViewById(R.id.family_main_contactsLayout);
        ImageView settings = findViewById(R.id.family_main_settings);
        Button newContact = findViewById(R.id.family_main_addContact);
        Button newRequests = findViewById(R.id.family_main_newRequests);
        Button exitApp = findViewById(R.id.family_main_exitApp);
        LinearLayout offlineWarning = findViewById(R.id.family_main_offlineWarning);

        // Monitor network connection changes
        NetworkConnection networkConnection = new NetworkConnection(getApplicationContext());
        networkConnection.observe(this, isConnected -> {
            if (isConnected) {
                offlineWarning.setVisibility(View.GONE);
            } else {
                offlineWarning.setVisibility(View.VISIBLE);
            }
        });

        // building and loading contacts list
        RecyclerView contactsView = findViewById(R.id.family_main_contactsView);
        contacts = new ArrayList<>();
        contactsAdapter = new UsersAdapter(contacts, this);
        contactsView.setAdapter(contactsAdapter);
        getContactsIDs();

        // refreshing contacts list
        contactsLayout.setOnRefreshListener(this::getContactsIDs);
        // show the real time of the number of requests
        numberOfRequestsRealtime(); // ** @author this line added by Xuannan Huang*/

        // gains token from Messaging server then send it to database
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                sendFCMTokenToDatabase(task.getResult());
            }
        });

        // creating new contact button
        newContact.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivityFamily.this, AddContactActivityFamily.class);
            startActivity(intent);
        });

        // checking new requests button
        newRequests.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivityFamily.this, InfoRequestActivityFamily.class);
            startActivity(intent);
        });

        // settings button
        settings.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivityFamily.this, SettingsActivity.class);
            startActivity(intent);
        });

        // exit app button on the offline warning page
        exitApp.setOnClickListener(view -> {
            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });
    }

    /**
     * Get the IDs of all contacts of this user
     * IDs are stored in a list in the form of String
     * @author Jianwei Li
     */
    private void getContactsIDs() {
        // check if internet connection is available
        if (!isNetworkAvailable()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityFamily.this);
            builder.setTitle("No Internet Connection")
                    .setMessage("Please reconnect and try again.")
                    .setPositiveButton(android.R.string.yes, null).show();
            contactsLayout.setRefreshing(false);
            return;
        }
        String myID = preferenceManager.getString(Constants.KEY_USER_ID);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(myID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> myFriendsIDs = (List<String>) document.get(Constants.KEY_FRIENDS);
                            getContacts(myFriendsIDs);
                        } else {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Failed to get contacts list",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(
                                getApplicationContext(),
                                "Get failed with " + task.getException(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Get all this user's contacts information from the database
     * and load the contacts list
     * @author Jianwei Li
     */
    private void getContacts(List<String> contactsIDs) {
        contacts.clear();
        contactsLayout.setRefreshing(true);
        if (contactsIDs == null || contactsIDs.isEmpty()) {
            contactsLayout.setRefreshing(false);
            errorMessage.setText(String.format("%s", "No Contacts"));
            errorMessage.setVisibility(View.VISIBLE);
        } else {
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            database.collection(Constants.KEY_COLLECTION_USERS)
                    .whereIn(Constants.KEY_PHONE, contactsIDs)
                    .get()
                    .addOnCompleteListener(task -> {
                        contactsLayout.setRefreshing(false);
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                User contact = new User();
                                contact.phone = document.getString(Constants.KEY_PHONE);
                                contact.token = document.getString(Constants.KEY_FCM_TOKEN);
                                contact.avatarUri = document.getString(Constants.KEY_AVATAR_URI);
                                String nickName = preferenceManager.getString(contact.phone);
                                if (nickName == null) {
                                    contact.name = document.getString(Constants.KEY_NAME);
                                } else {
                                    contact.name = nickName;
                                }
                                contacts.add(contact);
                            }
                            if (contacts.size() > 0) {
                                contactsAdapter.notifyDataSetChanged();
                                errorMessage.setVisibility(View.GONE);
                            } else {
                                errorMessage.setText(String.format("%s", "No contacts"));
                                errorMessage.setVisibility(View.VISIBLE);
                            }
                        } else {
                            errorMessage.setText(String.format("%s", "No contacts"));
                            errorMessage.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    /**
     * Once the login is successful, the personal FCM token
     * used for communication is sent to the database for storage
     * @param token the personal FCM token used for communication
     * @author Jianwei Li
     */
    private void sendFCMTokenToDatabase(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
        // update toke to database
        documentReference.update(Constants.KEY_FCM_TOKEN, token).addOnFailureListener(e ->
                Toast.makeText(
                        MainActivityFamily.this,
                        "Unable to send token: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    /**
     * Implementation of ItemsAdapter interface method.
     * Jump to the InfoContactActivityFamily and display the personal information of the selected user
     * @param user the contact selected by clicking, whose information is to be displayed
     * @author Jianwei Li
     */
    @Override
    public void displayContactInformation(User user) {
        Intent intent = new Intent(getApplicationContext(), InfoContactActivityFamily.class);
        intent.putExtra("user", user);
        startActivity(intent);
    }

    /**
     * No need to implement method
     */
    @Override
    public void displayHobbyInformation(Hobby hobby) {}

    /**
     * Implementation of ItemsAdapter interface method.
     * If multiple contacts are selected, start CallOutgoingActivity,
     * and pass the selected contacts to the next activity.
     * @param isMultipleUsersSelected boolean:  true -> multiple contacts are selected
     *                                         false -> no contact is selected
     * @author Jianwei Li
     */
    @Override
    public void onMultipleUsersAction(Boolean isMultipleUsersSelected) {
        if (isMultipleUsersSelected) {
            conference.setVisibility(View.VISIBLE);
            conference.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), OutgoingCallActivity.class);
                    intent.putExtra(
                            "selectedUsers",
                            new Gson().toJson(contactsAdapter.getSelectedUsers()));
                    intent.putExtra("type", "video");
                    intent.putExtra("isMultiple", true);
                    startActivity(intent);
                }
            });
        } else {
            conference.setVisibility(View.GONE);
        }
    }

    /**
     * the function for getting the realtime updates for number of requests
     * It will show on the main page with a red dot and a number
     * @author Xuannan Huang
     */
    public void numberOfRequestsRealtime(){
        // get the TextView of the red dot
        requestsNumber = findViewById(R.id.family_main_numOfRequests);
        // get the Firestore database
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(
                                MainActivityFamily.this,
                                "Listen failed.",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        if (snapshot != null && snapshot.exists()) {
                            long numberOfRequests = (long) snapshot.get(Constants.KEY_NUM_OF_REQUESTS);

                            if (numberOfRequests == 0) {
                                requestsNumber.setVisibility(View.GONE);
                            } else {
                                requestsNumber.setVisibility(View.VISIBLE);
                                requestsNumber.setText(numberToString(numberOfRequests));
                            }
                        } else {
                            Toast.makeText(
                                    MainActivityFamily.this,
                                    "Cannot get data.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * change the number to string
     * @param numberOfRequests the number will be changed
     * @return string of number
     * @author Xuannan Huang
     */
    private String numberToString(long numberOfRequests) {
        if (numberOfRequests <= 99) {
            return String.valueOf(numberOfRequests);
        } else {
            return "99+";
        }
    }

    /**
     * Check if the device is connected to the network
     * @return true if is connected, false if not
     * @author Jianwei Li
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}