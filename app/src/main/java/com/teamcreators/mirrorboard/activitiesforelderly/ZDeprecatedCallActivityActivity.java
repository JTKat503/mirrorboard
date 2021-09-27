package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teamcreators.mirrorboard.R;

public class ZDeprecatedCallActivityActivity extends AppCompatActivity {

    private TextView hold;
    private LinearLayout addFriend, hangUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.z_deprecated_activity_call_activity);

        // hold button
        hold = findViewById(R.id.activityCall_hold_textView);
        hold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hold.setSelected(!hold.isSelected());
            }
        });

        // hang up button
        hangUp =  findViewById(R.id.activityCall_hangup_layout);
        hangUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // to do
//                Toast.makeText(getApplicationContext(), "Layout clicked", Toast.LENGTH_SHORT).show();
            }
        });

        // add friend button
        addFriend =  findViewById(R.id.activityCall_addFriend_layout);
        addFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // to do
            }
        });


    }
}