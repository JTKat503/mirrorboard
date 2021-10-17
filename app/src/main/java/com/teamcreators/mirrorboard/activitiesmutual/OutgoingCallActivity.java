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
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A class that contains information about the outgoing call interface
 *
 * @author Jianwei Li
 */
public class OutgoingCallActivity extends AppCompatActivity {
    private PreferenceManager preferenceManager;
    private String inviterToken = null;
    private String meetingRoomID = null;
    private String callingType = null;
    private TextView contactName, contactPhoneNum;
    private ImageView contactAvatar;
    private int rejectionCount = 0;
    private int totalReceivers = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_outgoing);

        contactAvatar = findViewById(R.id.outgoingCall_avatar);
        contactName = findViewById(R.id.outgoingCall_name);
        contactPhoneNum = findViewById(R.id.outgoingCall_phoneNum);
        ImageView imageCallingType = findViewById(R.id.outgoingCall_imageCallingType);
        LinearLayout hangUp = findViewById(R.id.outgoingCall_cancel);
        preferenceManager = new PreferenceManager(getApplicationContext());
        callingType = getIntent().getStringExtra("type");
        User user = (User) getIntent().getSerializableExtra("user");

        // Call Timer, If no one answers the call after 50 seconds,
        // the call will be cancelled automatically
        new CountDownTimer(50000, 50000) {

            @Override
            public void onTick(long l) {}

            @Override
            public void onFinish() {
                if (user != null) {
                    cancelInvitation(user.token, null);
                }
            }
        }.start();

        // determines the icon of the type of call
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

        // gains token of logged-in user from server, then initiate a call invitation
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                inviterToken = task.getResult();
                if (callingType != null) {
                    // Initiate a multi-party call invitation
                    if (getIntent().getBooleanExtra("isMultiple", false)) {
                        Type type = new TypeToken<ArrayList<User>>() {}.getType();
                        ArrayList<User> receivers = new Gson().fromJson(
                                getIntent().getStringExtra("selectedUsers"), type);
                        if (receivers != null) {
                            totalReceivers = receivers.size();
                        }
                        initiateMeeting(callingType, null, receivers);
                    } else {
                        // Initiate a single call invitation
                        if (user != null) {
                            totalReceivers = 1;
                            initiateMeeting(callingType, user.token, null);
                        }
                    }
                }
            }
        });

        // hangup button
        hangUp.setOnClickListener(view -> {
            if (getIntent().getBooleanExtra("isMultiple", false)) {
                Type type = new TypeToken<ArrayList<User>>() {}.getType();
                ArrayList<User> receivers = new Gson().fromJson(
                        getIntent().getStringExtra("selectedUsers"), type);
                cancelInvitation(null, receivers);
            } else {
                if (user != null) {
                    cancelInvitation(user.token, null);
                }
            }
        });
    }

    /**
     * Construct the data of the invitation, and then send it to the other party.
     * @param meetingType types of call invitations
     * @param receiverToken the token of recipient who received the invitation
     * @param receivers invited contacts
     */
    private void initiateMeeting(String meetingType, String receiverToken, ArrayList<User> receivers) {
        try {
            JSONArray tokens = new JSONArray();
            if (receiverToken != null) {
                tokens.put(receiverToken);
            }
            if (receivers != null && receivers.size() > 0) {
                StringBuilder contactNames = new StringBuilder();
                for (int i = 0; i < receivers.size(); i++) {
                    tokens.put(receivers.get(i).token);
                    contactNames.append(receivers.get(i).name).append("\n");
                }
                contactAvatar.setVisibility(View.GONE);
                contactPhoneNum.setVisibility(View.GONE);
                contactName.setText(contactNames.toString());
            }
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
            meetingRoomID = preferenceManager.getString(Constants.KEY_USER_ID) + "_"
                    + UUID.randomUUID().toString().substring(0, 5);
            data.put(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoomID);
            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION);
        } catch (Exception e) {
            Toast.makeText(OutgoingCallActivity.this,
                    e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Send the token, profile and calling information of inviter to recipient
     * @param remoteMessageBody the token, inviter's profile and calling information
     * @param type the calling type (video/audio)
     */
    private void sendRemoteMessage(String remoteMessageBody, String type) {
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
                Constants.getRemoteMessageHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call,@NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    // if type.equals(Constants.REMOTE_MSG_INVITATION), invitation successfully sent
                    // if type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE),
                    // invitation accepted/rejected by sender
                    if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)) {
                        finish();
                    }
                } else {
                    Toast.makeText(OutgoingCallActivity.this,
                            response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call,@NonNull Throwable t) {
                Toast.makeText(OutgoingCallActivity.this,
                        t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Implement the facility to cancel call invitations from sender side, and inform the recipients.
     * @param receiverToken the token of recipient who received the invitation
     * @param receivers invited contacts
     */
    private void cancelInvitation(String receiverToken, ArrayList<User> receivers) {
        try {
            JSONArray tokens = new JSONArray();
            if (receiverToken != null) {
                tokens.put(receiverToken);
            }
            if (receivers != null && receivers.size() > 0) {
                for (User user : receivers) {
                    tokens.put(user.token);
                }
            }
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

    // Respond accordingly to the response of the call recipient.
    // If the other party rejects the call invitation, the invitation will be ended.
    // If the other party accepts the call invitation, the call room will be established.
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
                            builder.setAudioOnly(true);
                        }
                        JitsiMeetActivity.launch(OutgoingCallActivity.this, builder.build());
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else if (type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)) {
                    // call invitation rejected by one of the other parties
                    rejectionCount += 1;
                    if (rejectionCount == totalReceivers) {
                        finish();
                    }
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