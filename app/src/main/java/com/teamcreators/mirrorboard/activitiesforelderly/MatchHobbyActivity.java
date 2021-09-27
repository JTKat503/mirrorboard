package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.teamcreators.mirrorboard.R;

public class MatchHobbyActivity extends AppCompatActivity {

    private Button matchFriend, removeHobby, goBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_hobby);

        matchFriend = findViewById(R.id.matchHobby_findCall_button);
        removeHobby = findViewById(R.id.matchHobby_removeHobby_button);
        goBack = findViewById(R.id.matchHobby_goBack_button);

        // match a friend (button) based on a specific hobby
        matchFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // to do

                Intent intent = new Intent(MatchHobbyActivity.this, ZDeprecatedCallActivityActivity.class);
                startActivity(intent);
            }
        });

        // remove current hobby button
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
}