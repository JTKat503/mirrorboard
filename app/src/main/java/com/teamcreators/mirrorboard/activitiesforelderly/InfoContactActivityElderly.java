package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.activitiesmutual.CallOutgoingActivity;
import com.teamcreators.mirrorboard.models.User;
import com.teamcreators.mirrorboard.utilities.Constants;
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
        Button makeVideoCall = findViewById(R.id.contactInfo_makeCall_button);
        Button editContactName = findViewById(R.id.contactInfo_editNickname_button);
        Button removeContact = findViewById(R.id.contactInfo_removeContact_button);
        Button goBack = findViewById(R.id.contactInfo_goBack_button);

        TextView contactName = findViewById(R.id.contactInfo_contactName);
        contactName.setText(user.name);
        contactName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        contactName.setMarqueeRepeatLimit(1);
        contactName.setSelected(true);

        ImageView contactAvatar = findViewById(R.id.contactInfo_profileImage);
        Glide.with(this)
                .load(Uri.parse(user.avatarUri))
                .fitCenter()
                .error(R.drawable.blank_profile)
                .into(contactAvatar);

        // video call button
        makeVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initiateVideoCall(user);
            }
        });

        // editing friends nickname button
        editContactName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(InfoContactActivityElderly.this);
                final EditText newName = new EditText(InfoContactActivityElderly.this);
                newName.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setTitle("Enter a new name")
                        .setView(newName)
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String name = newName.getText().toString();
                                if (!name.trim().isEmpty()) {
                                    preferenceManager.putString(user.phone, name);
                                }
                            }
                        }).show();
            }
        });

        // removing contact button
        removeContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(InfoContactActivityElderly.this);
                builder.setTitle("Delete Contact")
                        .setMessage("Are you sure you want to delete this contact?")
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                removeContact();
                                onBackPressed();
                                finish();
                            }
                        }).show();
            }
        });

        // going back button
        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
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
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
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
            Intent intent = new Intent(getApplicationContext(), CallOutgoingActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("type", "video");
            startActivity(intent);
        }
    }
}