package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.teamcreators.mirrorboard.R;

public class StartActivityActivity extends AppCompatActivity {

    private Button editProfile, addHobby, exit, family;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_activity);

        editProfile = findViewById(R.id.startActivity_editProfile_button);
        addHobby = findViewById(R.id.startActivity_addHobby_button);
        family = findViewById(R.id.startActivity_family_button);

        // editing Profile button
        editProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StartActivityActivity.this, EditProfileActivity.class);
                startActivity(intent);
            }
        });

        // adding new hobbies button
        addHobby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StartActivityActivity.this, AddHobbyActivity.class);
                startActivity(intent);
            }
        });

        // going back to main interface button
        family.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(StartActivityActivity.this, MainActivity.class);
//                startActivity(intent);
                onBackPressed();
                finish();
            }
        });

        // exit app button
        exit = findViewById(R.id.startActivity_exit_button);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moveTaskToBack(true);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        });


    }
}