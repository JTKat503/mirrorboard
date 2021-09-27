package com.teamcreators.mirrorboard.activitiesmutual;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.models.User;
import com.teamcreators.mirrorboard.network.ApiClient;
import com.teamcreators.mirrorboard.network.ApiService;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallOutgoingActivity extends AppCompatActivity {

    private PreferenceManager preferenceManager;
    private String inviterToken = null;
    String meetingRoomID = null;
    String callingType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_outgoing);

        ImageView imageCallingType = findViewById(R.id.outgoingCall_imageCallingType);
        ImageView contactAvatar = findViewById(R.id.outgoingCall_profileImage);
        TextView contactName = findViewById(R.id.outgoingCall_userName_textView);
        TextView contactPhoneNum = findViewById(R.id.outgoingCall_phoneNum_textView);
        LinearLayout hangUp = findViewById(R.id.outgoingCall_hangup_layout);

        preferenceManager = new PreferenceManager(getApplicationContext());
        callingType = getIntent().getStringExtra("type");
        User user = (User) getIntent().getSerializableExtra("user");

        if (callingType != null) {
            if (callingType.equals("video")) {
                imageCallingType.setImageResource(R.drawable.ic_round_videocam_48);
            } else {
                imageCallingType.setImageResource(R.drawable.ic_round_call_48);
            }
        }
        if (user != null) {
            Glide.with(this)
                    .load(Uri.parse(user.avatarUri))
                    .fitCenter()
                    .error(R.drawable.blank_profile)
                    .into(contactAvatar);
            contactName.setText(user.name);
            contactPhoneNum.setText(user.phone);
        }

        // gains token of logged-in user from server
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    inviterToken = task.getResult();
                    // start call invitation
                    if (callingType != null && user != null) {
                        initiateMeeting(callingType, user.token);
                    }
                }
            }
        });

//        // presses hold button, changes text color to blue, re-press, changes color to white
//        hold.findViewById(R.id.outgoingCall_hold_textView);
//        hold.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                hold.setSelected(!hold.isSelected());
//            }
//        });

        // hangup button
        hangUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user != null) {
                    cancelInvitation(user.token);
                }
            }
        });
    }

    // 构建邀请通话的数据,然后发送给对方
    private void initiateMeeting(String meetingType, String receiverToken) {
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);
            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();
            // construct invitation content
            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType);
            data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
            data.put(Constants.KEY_PHONE, preferenceManager.getString(Constants.KEY_PHONE));
            data.put(Constants.KEY_AVATAR_URI,preferenceManager.getString(Constants.KEY_AVATAR_URI));
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, inviterToken);
            // set meeting room ID
            meetingRoomID = preferenceManager.getString(Constants.KEY_USER_ID) + "_" +
                            UUID.randomUUID().toString().substring(0, 5);
            data.put(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoomID);
            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION);
        } catch (Exception e) {
            Toast.makeText(CallOutgoingActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // send the token and calling info of inviter to receiver
    private void sendRemoteMessage(String remoteMessageBody, String type) {
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
                Constants.getRemoteMessageHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call,@NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    // if type.equals(Constants.REMOTE_MSG_INVITATION), invitation successfully sent
                    // if type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE), invitation canceled by sender
                    if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)) {
                        finish();
                    }
                } else {
                    Toast.makeText(CallOutgoingActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call,@NonNull Throwable t) {
                Toast.makeText(CallOutgoingActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    // implementing the facility to cancel call invitation from sender side
    private void cancelInvitation(String receiverToken) {
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);
            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();
            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, Constants.REMOTE_MSG_INVITATION_CANCELLED);
            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_RESPONSE);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null) {
                if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                    // outgoing call is accepted by the other party, start meeting room
                    try {
                        URL serverURL = new URL("https://meet.jit.si");
                        JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                        builder.setServerURL(serverURL);
                        builder.setWelcomePageEnabled(false);
                        builder.setRoom(meetingRoomID);
                        if (callingType.equals("audio")) {
                            builder.setVideoMuted(true);
//                            builder.setAudioOnly(true);
                        }
                        JitsiMeetActivity.launch(CallOutgoingActivity.this, builder.build());
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else if (type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)) {
                    // call invitation rejected by the other party
                    finish();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }
}