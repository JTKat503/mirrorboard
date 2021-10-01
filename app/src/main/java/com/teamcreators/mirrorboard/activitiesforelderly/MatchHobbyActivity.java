package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.activitiesmutual.CallOutgoingActivity;
import com.teamcreators.mirrorboard.models.Hobby;
import com.teamcreators.mirrorboard.models.User;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MatchHobbyActivity extends AppCompatActivity {

    private Hobby hobby;
    private DocumentSnapshot matchedStranger = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_hobby);

        hobby = (Hobby) getIntent().getSerializableExtra("hobby");

        Button matchFriend = findViewById(R.id.matchHobby_findCall_button);
        Button removeHobby = findViewById(R.id.matchHobby_removeHobby_button);
        Button goBack = findViewById(R.id.matchHobby_goBack_button);

        TextView hobbyName = findViewById(R.id.matchHobby_hobbyName);
        hobbyName.setText(hobby.name);
        hobbyName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        hobbyName.setMarqueeRepeatLimit(1);
        hobbyName.setSelected(true);

        ImageView hobbyIcon = findViewById(R.id.matchHobby_hobbyIcon);
        Glide.with(this)
                .load(hobby.drawable)
                .fitCenter()
                .error(R.drawable.blank_hobby_image)
                .into(hobbyIcon);

        // match a friend (button) based on a specific hobby
        matchFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getMyContacts();
                if (matchedStranger != null) {
                    User recipient = new User();
                    recipient.name = matchedStranger.getString(Constants.KEY_NAME);
                    recipient.phone = matchedStranger.getId();
                    recipient.token = matchedStranger.getString(Constants.KEY_FCM_TOKEN);
                    recipient.avatarUri = matchedStranger.getString(Constants.KEY_AVATAR_URI);
                    initiateVideoCall(recipient);
                }
            }
        });

        // removing current hobby button
        removeHobby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // to do
                onBackPressed();
                finish();
            }
        });

        // goBack button
        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
        });
    }

    /**
     * Implementation of ItemsAdapter interface method.
     * Initialize the information of the recipient of the video call and
     * start the video call, if can not find the recipient, display hint
     * @param user recipient of the video call
     */
    private void initiateVideoCall(User user) {
        Intent intent = new Intent(getApplicationContext(), CallOutgoingActivity.class);
        intent.putExtra("user", user);
        intent.putExtra("type", "video");
        startActivity(intent);
    }

    private void randomlyGetMatchedStranger(List<String> myFriends) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        // 联合查询语句有问题，不可运行
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereArrayContains(Constants.KEY_HOBBIES, hobby.name)
                .whereNotIn(Constants.KEY_PHONE, myFriends)
                .whereNotIn(Constants.KEY_FCM_TOKEN, Arrays.asList("", null))

                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                            int randomNum = new Random().nextInt(task.getResult().getDocuments().size());
                            matchedStranger = task.getResult().getDocuments().get(randomNum);
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Failed to find a friend", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
    }

    private void getMyContacts() {
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
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
                                List<String> myFriends = (List<String>) document.get(Constants.KEY_FRIENDS);
                                randomlyGetMatchedStranger(myFriends);
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        "Failed to get friends list", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Get failed with " +
                                    task.getException(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
    }
}