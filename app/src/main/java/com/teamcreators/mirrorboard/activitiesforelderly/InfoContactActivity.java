package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.activitiesmutual.CallOutgoingActivity;
import com.teamcreators.mirrorboard.listeners.ItemsListener;
import com.teamcreators.mirrorboard.models.Hobby;
import com.teamcreators.mirrorboard.models.User;

public class InfoContactActivity extends AppCompatActivity {

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_contact);

        user = (User) getIntent().getSerializableExtra("user");

        Button makeVideoCall = findViewById(R.id.contactInfo_makeCall_button);
        Button editFriendsNickname = findViewById(R.id.contactInfo_editNickname_button);
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
                builder.setTitle("Delete Contact")
                        .setMessage("Are you sure you want to delete this contact?")
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

    /**
     * Initialize the information of the recipient of the video call and
     * start the video call, if can not find the recipient, display hint
     * @param user recipient of the video call
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

//    /**
//     * Initialize the information of the recipient of the audio call and
//     * start the audio call, if can not find the recipient, display hint
//     * @param user recipient of the video call
//     */
//    public void initiateAudioCall(User user) {
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
//    }
}