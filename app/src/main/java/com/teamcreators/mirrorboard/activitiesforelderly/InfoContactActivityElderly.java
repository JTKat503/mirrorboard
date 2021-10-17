package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.activitiesmutual.OutgoingCallActivity;
import com.teamcreators.mirrorboard.models.User;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.NetworkConnection;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.List;

/**
 * A class that displays contact information, including the functions
 * of making video calls, modifying nickname and deleting contact
 *
 * @author Jianwei Li & Donghong Zhuang
 */
public class InfoContactActivityElderly extends AppCompatActivity {
    private User user;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_contact_elderly);

        user = (User) getIntent().getSerializableExtra("user");
        preferenceManager = new PreferenceManager(getApplicationContext());
        Button makeVideoCall = findViewById(R.id.elderly_contactInfo_makeCall);
        Button editContactName = findViewById(R.id.elderly_contactInfo_editName);
        Button removeContact = findViewById(R.id.elderly_contactInfo_removeContact);
        Button goBack = findViewById(R.id.elderly_contactInfo_back);
        Button exitApp = findViewById(R.id.elderly_contactInfo_exitApp);
        LinearLayout offlineWarning = findViewById(R.id.elderly_contactInfo_offlineWarning);

        // Monitor network connection changes
        NetworkConnection networkConnection = new NetworkConnection(getApplicationContext());
        networkConnection.observe(this, isConnected -> {
            if (isConnected) {
                offlineWarning.setVisibility(View.GONE);
            } else {
                offlineWarning.setVisibility(View.VISIBLE);
            }
        });

        TextView contactName = findViewById(R.id.elderly_contactInfo_name);
        contactName.setText(user.name);
        contactName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        contactName.setMarqueeRepeatLimit(1);
        contactName.setSelected(true);

        ImageView contactAvatar = findViewById(R.id.elderly_contactInfo_avatar);
        Glide.with(this)
                .load(Uri.parse(user.avatarUri))
                .fitCenter()
                .error(R.drawable.blank_profile)
                .into(contactAvatar);

        // video call button
        makeVideoCall.setOnClickListener(view -> isAvailableForVideoCall(user));

        // editing friends nickname button
        editContactName.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(InfoContactActivityElderly.this);
            final EditText newName = new EditText(InfoContactActivityElderly.this);
            newName.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setTitle("Enter a new name")
                    .setView(newName)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        String name = newName.getText().toString();
                        if (!name.trim().isEmpty()) {
                            preferenceManager.putString(user.phone, name);
                        }
                    }).show();
        });

        // removing contact button
        removeContact.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(InfoContactActivityElderly.this);
            builder.setTitle("Delete Contact")
                    .setMessage("Are you sure you want to delete this contact?")
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        removeContact();
                        onBackPressed();
                        finish();
                    }).show();
        });

        // going back button
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
     * Delete the specified contact from the current user's contacts list
     * @author Donghong Zhuang
     */
    private void removeContact() {
        String myID = preferenceManager.getString(Constants.KEY_USER_ID);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(myID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> friends = (List<String>) document.get(Constants.KEY_FRIENDS);
                            friends.remove(user.phone);
                            database.collection(Constants.KEY_COLLECTION_USERS)
                                    .document(myID)
                                    .update(Constants.KEY_FRIENDS, friends);
                            preferenceManager.clearString(user.phone);
                        }
                    }
                });
    }

    /**
     * Check whether the contact's notification function is turned on,
     * if it is turned on, make a video call, if not, do not dial
     * @param user the contact to be called
     * @author Jianwei Li
     */
    private void isAvailableForVideoCall(User user) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(user.phone)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            if (document.getBoolean(Constants.KEY_NOTICE_ON)) {
                                initiateVideoCall(user);
                            } else {
                                Toast.makeText(
                                        InfoContactActivityElderly.this,
                                        user.name + " is not available",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(
                                    InfoContactActivityElderly.this,
                                    user.name + " is not available",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(
                                InfoContactActivityElderly.this,
                                user.name + " is not available",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Initialize the information of the recipient of the video call and
     * start the video call, if can not find the recipient, display hint
     * @param user recipient of the video call
     * @author Jianwei Li
     */
    private void initiateVideoCall(User user) {
        if (user.token == null || user.token.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    user.name + " is not available",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Intent intent = new Intent(getApplicationContext(), OutgoingCallActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("type", "video");
            startActivity(intent);
        }
    }
}