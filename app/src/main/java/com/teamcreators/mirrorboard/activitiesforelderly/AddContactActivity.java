package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.teamcreators.mirrorboard.R;

public class AddContactActivity extends AppCompatActivity {

    private Button sendRequest, goBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact_elderly);

        sendRequest = findViewById(R.id.addContact_sendRequest_button);
        goBack = findViewById(R.id.addContact_goBack_button);

        // send request button
        sendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // to do
                // Toast hint on success
            }
        });

        // go back button
        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
        });



    }
}