package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.activitiesmutual.OutgoingCallActivity;
import com.teamcreators.mirrorboard.models.Hobby;
import com.teamcreators.mirrorboard.models.User;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.NetworkConnection;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A class that matches contacts with the same hobby according to the hobby selected by the user.
 *
 * @author Jianwei Li
 */
public class MatchHobbyActivity extends AppCompatActivity {
    private Hobby hobby;
    private Button matchFriend;
    private HashSet<String> hobbies;
    private PreferenceManager preferenceManager;
    private ProgressBar matchFriendProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_hobby);

        hobby = (Hobby) getIntent().getSerializableExtra("hobby");
        preferenceManager = new PreferenceManager(getApplicationContext());
        hobbies = preferenceManager.getStringSet(Constants.KEY_HOBBIES);
        preferenceManager = new PreferenceManager(getApplicationContext());
        matchFriend = findViewById(R.id.matchHobby_findCall);
        matchFriendProgressBar = findViewById(R.id.matchHobby_progressbar);
        Button removeHobby = findViewById(R.id.matchHobby_removeHobby);
        Button goBack = findViewById(R.id.matchHobby_back);
        Button exitApp = findViewById(R.id.matchHobby_exitApp);
        LinearLayout offlineWarning = findViewById(R.id.matchHobby_offlineWarning);

        // Monitor network connection changes
        NetworkConnection networkConnection = new NetworkConnection(getApplicationContext());
        networkConnection.observe(this, isConnected -> {
            if (isConnected) {
                matchFriend.setVisibility(View.VISIBLE);
                removeHobby.setVisibility(View.VISIBLE);
                goBack.setVisibility(View.VISIBLE);
                offlineWarning.setVisibility(View.GONE);
            } else {
                offlineWarning.setVisibility(View.VISIBLE);
                matchFriend.setVisibility(View.GONE);
                removeHobby.setVisibility(View.GONE);
                goBack.setVisibility(View.GONE);
            }
        });

        // setting selected hobby's name
        TextView hobbyName = findViewById(R.id.matchHobby_hobbyName);
        hobbyName.setText(hobby.name);
        hobbyName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        hobbyName.setMarqueeRepeatLimit(1);
        hobbyName.setSelected(true);

        // setting selected hobby's icon
        ImageView hobbyIcon = findViewById(R.id.matchHobby_hobbyIcon);
        Glide.with(this)
                .load(hobby.icon)
                .fitCenter()
                .error(R.drawable.blank_hobby_image)
                .into(hobbyIcon);

        // match a friend (button) based on a specific hobby
        matchFriend.setOnClickListener(view -> {
            matchFriend.setVisibility(View.INVISIBLE);
            matchFriendProgressBar.setVisibility(View.VISIBLE);
            getContactsIDs();
        });

        // removing current hobby button
        removeHobby.setOnClickListener(view -> {
            hobbies.remove(hobby.name);
            String myID = preferenceManager.getString(Constants.KEY_USER_ID);
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(myID)
                    .update(Constants.KEY_HOBBIES, new ArrayList<>(hobbies))
                    .addOnSuccessListener(unused ->
                            preferenceManager.putStringSet(Constants.KEY_HOBBIES, hobbies))
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Sync failed",
                                    Toast.LENGTH_SHORT).show());
            onBackPressed();
            finish();
        });

        // goBack button
        goBack.setOnClickListener(view -> {
            onBackPressed();
            finish();
        });

        // exit app button on the offline warning page
        exitApp.setOnClickListener(view -> {
            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });
    }

    /**
     * Implementation of ItemsAdapter interface method.
     * Initialize the information of the recipient of the video call and
     * start the video call, if can not find the recipient, display hint
     * @param user recipient of the video call
     */
    private void initiateVideoCall(User user) {
        Intent intent = new Intent(getApplicationContext(), OutgoingCallActivity.class);
        intent.putExtra("user", user);
        intent.putExtra("type", "video");
        startActivity(intent);
        matchFriendProgressBar.setVisibility(View.INVISIBLE);
        matchFriend.setVisibility(View.VISIBLE);
    }

    /**
     * Randomly match a user who also have the selected hobby from users who are not friends,
     * and initiate a video call to the matched user
     * If cannot find such an user, show prompt message.
     * @param myFriendsIDs Phone numbers of all my contacts
     */
    private void randomlyGetMatchedStranger(List<String> myFriendsIDs) {
        String myID = preferenceManager.getString(Constants.KEY_USER_ID);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereArrayContains(Constants.KEY_HOBBIES, hobby.name)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<QueryDocumentSnapshot> strangers = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (!myFriendsIDs.contains(document.getId())
                                    && !document.getId().equals(myID)
                                    && document.getString(Constants.KEY_FCM_TOKEN) != null) {
                                strangers.add(document);
                            }
                        }
                        if (strangers.size() > 0) {
                            int randomIndex = (int) (Math.random() * strangers.size());
                            DocumentSnapshot matchedStranger = strangers.get(randomIndex);
                            sendInvitationTo(matchedStranger);
                        } else {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "No matchable user",
                                    Toast.LENGTH_SHORT).show();
                            matchFriendProgressBar.setVisibility(View.INVISIBLE);
                            matchFriend.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(
                                getApplicationContext(),
                                "No matchable user",
                                Toast.LENGTH_SHORT).show();
                        matchFriendProgressBar.setVisibility(View.INVISIBLE);
                        matchFriend.setVisibility(View.VISIBLE);
                    }
                });
    }

    /**
     * Get the IDs of all contacts of this user
     * IDs are stored in a list in the form of String
     */
    private void getContactsIDs() {
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
                            randomlyGetMatchedStranger(myFriendsIDs);
                        } else {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Failed to get contacts list",
                                    Toast.LENGTH_SHORT).show();
                            matchFriendProgressBar.setVisibility(View.INVISIBLE);
                            matchFriend.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(
                                getApplicationContext(),
                                "Get failed with " + task.getException(),
                                Toast.LENGTH_SHORT).show();
                        matchFriendProgressBar.setVisibility(View.INVISIBLE);
                        matchFriend.setVisibility(View.VISIBLE);
                    }
                });
    }

    /**
     * Send a video invitation to matched stranger
     * @param matchedStranger the matched stranger
     */
    private void sendInvitationTo(DocumentSnapshot matchedStranger) {
        if (matchedStranger != null) {
            User recipient = new User();
            recipient.name = matchedStranger.getString(Constants.KEY_NAME);
            recipient.phone = matchedStranger.getId();
            recipient.token = matchedStranger.getString(Constants.KEY_FCM_TOKEN);
            recipient.avatarUri = matchedStranger.getString(Constants.KEY_AVATAR_URI);
            initiateVideoCall(recipient);
        }
    }
}