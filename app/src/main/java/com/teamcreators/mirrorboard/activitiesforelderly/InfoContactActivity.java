package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.activitiesmutual.CallOutgoingActivity;
import com.teamcreators.mirrorboard.listeners.UsersListener;
import com.teamcreators.mirrorboard.models.User;

public class InfoContactActivity extends AppCompatActivity implements UsersListener {

    private Button makeVideoCall, editFriendsNickname, removeContact, goBack;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_contact);

        user = (User) getIntent().getSerializableExtra("user");

        makeVideoCall = findViewById(R.id.contactInfo_makeCall_button);
        editFriendsNickname = findViewById(R.id.contactInfo_editNickname_button);
        removeContact = findViewById(R.id.contactInfo_removeContact_button);
        goBack = findViewById(R.id.contactInfo_goBack_button);

        TextView contactName = findViewById(R.id.contactInfo_contactName);
        contactName.setText(user.name);

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
                if (user.token == null || user.token.trim().isEmpty()) {
                    Toast.makeText(
                            getApplicationContext(),
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
        });

        // editing friends nickname button
        editFriendsNickname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // to do
            }
        });

        // removing contact button
        removeContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(InfoContactActivity.this);
                builder.setMessage("Are you sure you want to delete this contact?")
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // delete contact, go back to main page
                                // to do - delete contact operation
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

    @Override
    public void initiateVideoMeeting(User user) {
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

    @Override
    public void initiateAudioMeeting(User user) {
//        if (user.token == null || user.token.trim().isEmpty()) {
//            Toast.makeText(
//                    this,
//                    user.name + " is not available",
//                    Toast.LENGTH_SHORT
//            ).show();
//        } else {
//            Intent intent = new Intent(getApplicationContext(), CallOutgoingActivity.class);
//            intent.putExtra("user", user);
//            intent.putExtra("type", "audio");
//            startActivity(intent);
//        }
    }

    @Override
    public void displayContactInformation(User user) {

    }

    @Override
    public void onMultipleUsersAction(Boolean isMultipleUsersSelected) {

    }
}