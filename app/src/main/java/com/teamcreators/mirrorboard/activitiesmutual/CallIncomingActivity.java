package com.teamcreators.mirrorboard.activitiesmutual;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.network.ApiClient;
import com.teamcreators.mirrorboard.network.ApiService;
import com.teamcreators.mirrorboard.utilities.Constants;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallIncomingActivity extends AppCompatActivity {

    private String callingType = null;
    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_incoming);

        ImageView imageCallingType = findViewById(R.id.incomingCall_imageMeetingType);
        TextView contactName = findViewById(R.id.incomingCall_userName_textView);
        TextView contactPhoneNum = findViewById(R.id.incomingCall_phoneNum_textView);
        ImageView contactAvatar = findViewById(R.id.incomingCall_profileImage);

        callingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        String inviterName = getIntent().getStringExtra(Constants.KEY_NAME);
        String inviterPhoneNum = getIntent().getStringExtra(Constants.KEY_PHONE);
        String inviterAvatarUri = getIntent().getStringExtra(Constants.KEY_AVATAR_URI);

        if (callingType != null) {
            if (callingType.equals("video")) {
                imageCallingType.setImageResource(R.drawable.ic_round_videocam_48);
            } else {
                imageCallingType.setImageResource(R.drawable.ic_round_call_48);
            }
        }
        if (inviterName != null) {
            contactName.setText(inviterName);
        }
        if (inviterPhoneNum != null) {
            contactPhoneNum.setText(inviterPhoneNum);
        }
        if (inviterAvatarUri != null) {
            Glide.with(this)
                    .load(Uri.parse(inviterAvatarUri))
                    .fitCenter()
                    .error(R.drawable.blank_profile)
                    .into(contactAvatar);
        }

        // setting for ringtone
        Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        mediaPlayer = MediaPlayer.create(this, ringtone);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        // answer call button
        LinearLayout answerCall = findViewById(R.id.incomingCall_answer_layout);
        answerCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.stop();
                sendInvitationResponse(
                        Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                        getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
                );
            }
        });

        // reject call button
        LinearLayout rejectCall = findViewById(R.id.incomingCall_reject_button);
        rejectCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.stop();
                sendInvitationResponse(
                        Constants.REMOTE_MSG_INVITATION_REJECTED,
                        getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
                );
            }
        });
    }

    // // 构建针对通话邀请的反应的数据
    private void sendInvitationResponse(String type, String receiverToken) {
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);
            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();
            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, type);
            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            sendRemoteMessage(body.toString(), type);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // implementing the facility to accept or reject call invitation
    private void sendRemoteMessage(String remoteMessageBody, String type) {
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
                Constants.getRemoteMessageHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                        // accept call invitation, build Jitsi Meet meeting room
                        try {
                            URL serverURL = new URL("https://meet.jit.si");
                            JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                            builder.setServerURL(serverURL);
                            builder.setWelcomePageEnabled(false);
                            builder.setRoom(getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM));
                            if (callingType.equals("audio")) {
                                builder.setVideoMuted(true);
//                                builder.setAudioOnly(true);
                            }
                            JitsiMeetActivity.launch(CallIncomingActivity.this, builder.build());
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(CallIncomingActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        // Call invitation declined by receiver
                        finish();
                    }
                } else {
                    Toast.makeText(CallIncomingActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call,@NonNull Throwable t) {
                Toast.makeText(CallIncomingActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null) {
                if (type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)) {
                    mediaPlayer.stop();
//                    Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show();
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